package io.okhi.android_background_geofencing.services;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;

public class BackgroundGeofenceForegroundService extends Service {
    private static String TAG = "BGFS";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BackgroundGeofencingNotification backgroundGeofencingNotification = BackgroundGeofencingDB.getNotification(getApplicationContext());
        backgroundGeofencingNotification.createNotificationChannel(getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, backgroundGeofencingNotification.getNotification(getApplicationContext()));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Thread uploadWork = new Thread(new Runnable() {
                @Override
                public void run() {
                    performUpload();
                }
            });
            uploadWork.start();
            uploadWork.join();
            Log.v(TAG, "Upload work complete.");
            Thread restartWork = new Thread(new Runnable() {
                @Override
                public void run() {
                    performRestart();
                }
            });
            restartWork.start();
            restartWork.join();
            Log.v(TAG, "Restart work complete.");
            stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    void performUpload() {
        try {
            boolean result = BackgroundGeofenceTransitionUploadWorker.uploadTransitions(getApplicationContext());
            if (!result) {
                BackgroundGeofenceTransition.scheduleGeofenceTransitionUploadWork(getApplicationContext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: refactor this to something cleaner
    void performRestart() {
        try {
            ArrayList<BackgroundGeofence> failedGeofences = new ArrayList<>();
            if (BackgroundGeofenceUtil.canRestartGeofences(getApplicationContext())) {
                ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(getApplicationContext());
                for (BackgroundGeofence geofence : geofences) {
                    if (geofence.isFailing()) {
                        failedGeofences.add(geofence);
                    }
                }
                if (!failedGeofences.isEmpty()) {
                    BackgroundGeofenceRestartWorker.restartGeofences(failedGeofences, getApplicationContext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
