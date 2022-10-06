package io.okhi.android_background_geofencing.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_core.models.OkHiException;

public class BackgroundGeofenceForegroundRestartWorker extends Worker {
    public BackgroundGeofenceForegroundRestartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(getApplicationContext());
            boolean canStartForegroundService = BackgroundGeofencing.canStartForegroundService(getApplicationContext());
            if (setting != null && setting.isWithForegroundService() && canStartForegroundService) {
                if (!BackgroundGeofencing.isForegroundServiceRunning(getApplicationContext())) {
                    try {
                        BackgroundGeofencing.startForegroundService(getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                BackgroundGeofenceUtil.cancelForegroundRestartWorker(getApplicationContext());
            }
        } catch (BackgroundGeofencingException e) {
            e.printStackTrace();
        } finally {
            return Result.success();
        }
    }
}
