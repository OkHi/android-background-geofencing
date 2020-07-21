package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.GeofencingEvent;

import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;

public class BackgroundGeofenceBroadcastReceiver extends BroadcastReceiver {

    private String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            // TODO: implement restart strategy for failed geofences
        } else {
            BackgroundGeofenceTransition transition = new BackgroundGeofenceTransition.BackgroundGeofenceTransitionBuilder(geofencingEvent).build();
            Log.v(TAG, "Received a " + transition.getTransitionEvent() + "geofence event");
            transition.save(context);
            scheduleOneTimeUploadWork(context);
        }
    }

    public void scheduleOneTimeUploadWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(BackgroundGeofenceTransitionUploadWorker.class)
                .setConstraints(constraints)
                .addTag(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_TAG)
                .setInitialDelay(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY, Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY_TIME_UNIT)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY_TIME_UNIT
                )
                .build();
        WorkManager.getInstance(context).enqueue(uploadWorkRequest);
        Log.v(TAG, "Geofence one-time work request queued up");
    }
}
