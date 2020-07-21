package io.okhi.android_background_geofencing.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackgroundGeofenceTransitionUploadWorker extends Worker {
    public BackgroundGeofenceTransitionUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // check if we've exceeded max attempts - if so fail it, we'll try again later
        // get webhook configuration
        // get all geofence transitions from db
        // upload each of them to webhook sequentially
        // remove successfully uploaded geofence transitions from db
        return Result.success();
    }
}
