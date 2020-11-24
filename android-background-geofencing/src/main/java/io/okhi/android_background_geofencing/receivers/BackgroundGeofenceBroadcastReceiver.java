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
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundService;

public class BackgroundGeofenceBroadcastReceiver extends BroadcastReceiver {

    private String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        boolean isNotificationAvailable = BackgroundGeofencingDB.getNotification(context) != null;
        boolean isInBackground = !BackgroundGeofenceUtil.isAppOnForeground(context);
        if (geofencingEvent.hasError()) {
            BackgroundGeofence.setIsFailing(geofencingEvent, true, context);
        } else {
            BackgroundGeofenceTransition transition = new BackgroundGeofenceTransition.Builder(geofencingEvent).build();
            Log.v(TAG, "Received a " + transition.getTransitionEvent() + " geofence event");
            transition.save(context);
        }
        if (isNotificationAvailable && isInBackground) {
            startForegroundTask(context);
        } else {
            scheduleBackgroundWork(context, geofencingEvent);
        }
    }

    private void startForegroundTask(Context context) {
        Intent serviceIntent = new Intent(context, BackgroundGeofenceForegroundService.class);
        serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_GEOFENCE_EVENT);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private void scheduleBackgroundWork(Context context, GeofencingEvent geofencingEvent) {
        if (geofencingEvent.hasError()) {
            BackgroundGeofence.scheduleGeofenceRestartWork(context);
        } else {
            BackgroundGeofenceTransition.scheduleGeofenceTransitionUploadWork(context);
        }
    }

}
