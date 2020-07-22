package io.okhi.android_background_geofencing;


import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceRestartWorker;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;

public class BackgroundGeofencing {
    public static void init(Context context) {
        // remove any existing work to prevent db read conflicts
        WorkManager.getInstance(context).cancelAllWork();

        // TODO: refactor this to static methods to get request work
        OneTimeWorkRequest failedGeofencesRestartWork = new OneTimeWorkRequest.Builder(BackgroundGeofenceRestartWorker.class)
                .setConstraints(Constant.GEOFENCE_WORK_MANAGER_CONSTRAINTS)
                .addTag(Constant.GEOFENCE_RESTART_WORK_TAG)
                .setInitialDelay(5, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        Constant.GEOFENCE_RESTART_WORK_BACKOFF_DELAY,
                        Constant.GEOFENCE_RESTART_WORK_BACKOFF_DELAY_TIME_UNIT
                )
                .build();

        OneTimeWorkRequest geofenceTransitionUploadWorkRequest = new OneTimeWorkRequest.Builder(BackgroundGeofenceTransitionUploadWorker.class)
                .setConstraints(Constant.GEOFENCE_WORK_MANAGER_CONSTRAINTS)
                .addTag(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_TAG)
                .setInitialDelay(5, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY_TIME_UNIT
                )
                .build();

        WorkManager.getInstance(context).beginUniqueWork(Constant.GEOFENCE_INIT_WORK_NAME, ExistingWorkPolicy.REPLACE, geofenceTransitionUploadWorkRequest)
                .then(failedGeofencesRestartWork)
                .enqueue();
    }
}
