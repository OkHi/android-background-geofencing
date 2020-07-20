package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.GeofencingEvent;

import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceUploadWorker;

public class BackgroundGeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("Broadcast", "Received a geofence event");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            // TODO: handle failed geofences
            return;
        }
        scheduleOneTimeGeofenceProcessWork(geofencingEvent, context);
    }

    private void scheduleOneTimeGeofenceProcessWork(GeofencingEvent geofencingEvent, Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build();
            String payload = new BackgroundGeofenceTransition(geofencingEvent).toJSON();
            Data geofenceProcessWorkData = new Data.Builder()
                    .putString(Constant.GEOFENCE_TRANSITION_EVENT_JSON_PAYLOAD, payload)
                    .build();
            OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(BackgroundGeofenceUploadWorker.class)
                    .setConstraints(constraints)
                    .setInputData(geofenceProcessWorkData)
                    .addTag(Constant.GEOFENCE_TRANSITION_UPLOAD_REQUEST)
                    .setInitialDelay(Constant.GEOFENCE_UPLOAD_WORK_INITIAL_DELAY, Constant.GEOFENCE_UPLOAD_WORK_INITIAL_DELAY_TIME_UNIT)
                    .setBackoffCriteria(
                            BackoffPolicy.LINEAR,
                            Constant.GEOFENCE_UPLOAD_WORK_BACK_OFF_DELAY,
                            Constant.GEOFENCE_UPLOAD_WORK_BACK_OFF_DELAY_TIME_UNIT
                    )
                    .build();
            WorkManager.getInstance(context).enqueue(uploadWorkRequest);
            Log.v("Broadcast", "Geofence one-time work request queued up");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
