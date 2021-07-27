package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.GeofencingEvent;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundService;
import io.okhi.android_core.models.OkHiCoreUtil;

public class BackgroundGeofenceBroadcastReceiver extends BroadcastReceiver {

    private String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        boolean isNotificationAvailable = BackgroundGeofencingDB.getNotification(context) != null;
        boolean isInBackground = !BackgroundGeofenceUtil.isAppOnForeground(context);
        BackgroundGeofenceTransition transition = null;
        if (geofencingEvent.hasError()) {
            BackgroundGeofence.setIsFailing(geofencingEvent, true, context);
        } else {
            transition = new BackgroundGeofenceTransition.Builder(geofencingEvent).build();
            transition.save(context);
            BackgroundGeofenceUtil.log(context, TAG, "Received a " + transition.getTransitionEvent() + " geofence event");
        }
        if (isNotificationAvailable && isInBackground) {
            startForegroundTask(context, transition);
        } else {
            scheduleBackgroundWork(context);
        }
    }

    private void startForegroundTask(Context context, BackgroundGeofenceTransition transition) {
        try {
            Intent serviceIntent = new Intent(context, BackgroundGeofenceForegroundService.class);
            if (transition == null) {
                serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_GEOFENCE_EVENT);
            } else {
                serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_GEOFENCE_EVENT);
                serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_TRANSITION_SIGNATURE, transition.getSignature());
            }
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleBackgroundWork(Context context) {
        BackgroundGeofenceTransition.asyncUploadAllTransitions(context);
    }

}
