package io.okhi.android_background_geofencing.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackgroundGeofenceUploadWorker extends Worker {
    public BackgroundGeofenceUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.v("UploadMe", "upload upload upload");
        return Result.success();
    }
}
