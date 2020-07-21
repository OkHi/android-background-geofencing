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
import io.okhi.android_background_geofencing.models.Constant;

public class BackgroundGeofenceRestartWorker extends Worker {

    private static String TAG = "RestartWorker";

    public BackgroundGeofenceRestartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long lastGeofenceTransitionEventTimestamp = BackgroundGeofencingDB.getLastGeofenceTransitionEventTimestamp(getApplicationContext());
        boolean isWithinThreshold = lastGeofenceTransitionEventTimestamp < 0 || System.currentTimeMillis() - lastGeofenceTransitionEventTimestamp < Constant.GEOFENCE_TRANSITION_TIME_STAMP_THRESHOLD;
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(getApplicationContext());
        ArrayList<BackgroundGeofence> failedGeofences = new ArrayList<>();
        boolean isLocationServicesEnabled = BackgroundGeofencingLocationService.isLocationServicesEnabled(getApplicationContext());
        boolean isLocationPermissionGranted = BackgroundGeofencingPermissionService.isLocationPermissionGranted(getApplicationContext());

        for (BackgroundGeofence geofence: geofences) {
            if (geofence.isFailing()) {
                failedGeofences.add(geofence);
            }
        }

        if (isWithinThreshold && failedGeofences.isEmpty()) {
            return Result.success();
        }

        if (!isLocationServicesEnabled || !isLocationPermissionGranted) {
            return Result.retry();
        }

        if (!isWithinThreshold) {
            restartGeofences(geofences, getApplicationContext());
        } else {
            restartGeofences(failedGeofences, getApplicationContext());
        }

        return Result.retry();
    }

    private void restartGeofences(ArrayList<BackgroundGeofence> geofences, final Context context) {
        if (geofences != null && !geofences.isEmpty()) {
            for(final BackgroundGeofence geofence: geofences) {
                geofence.restart(context, new RequestHandler() {
                    @Override
                    public void onSuccess() {
                        Log.v(TAG, "Successfully restarted: " + geofence.getId());
                        BackgroundGeofence.setIsFailing(geofence.getId(), false, context);
                    }
                    @Override
                    public void onError() {
                        Log.v(TAG, "Failed to restart: " + geofence.getId());
                        BackgroundGeofence.setIsFailing(geofence.getId(), true, context);
                    }
                });
            }
        }
    }
}