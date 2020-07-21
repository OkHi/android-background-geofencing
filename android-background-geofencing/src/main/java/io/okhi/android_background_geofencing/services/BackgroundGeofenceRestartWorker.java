package io.okhi.android_background_geofencing.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingPermissionService;

public class BackgroundGeofenceRestartWorker extends Worker {

    private static String TAG = "RestartWorker";

    public BackgroundGeofenceRestartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllFailingGeofences(getApplicationContext());
        if (geofences.isEmpty()) {
            return Result.success();
        }
        boolean isLocationServicesEnabled = BackgroundGeofencingLocationService.isLocationServicesEnabled(getApplicationContext());
        boolean isLocationPermissionGranted = BackgroundGeofencingPermissionService.isLocationPermissionGranted(getApplicationContext());
        if (isLocationServicesEnabled && isLocationPermissionGranted) {
            for(final BackgroundGeofence geofence: geofences) {
                geofence.restart(getApplicationContext(), new RequestHandler() {
                    @Override
                    public void onSuccess() {
                        BackgroundGeofence.setIsFailing(geofence.getId(), false, getApplicationContext());
                        Log.v(TAG, "Successfully restarted geofence: " + geofence.getId());
                    }
                    @Override
                    public void onError() {
                        Log.v(TAG, "Failed to restart geofence: " + geofence.getId());
                    }
                });
            }
        }
        return Result.retry();
    }
}
