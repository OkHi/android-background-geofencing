package io.okhi.android_background_geofencing.models;

import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;

import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundRestartWorker;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;
import io.okhi.android_core.models.OkHiPlayService;

public class BackgroundGeofenceUtil {
    public static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBackgroundLocationPermissionGranted(Context context) {
        return OkHiPermissionService.isBackgroundLocationPermissionGranted(context);
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return OkHiPlayService.isGooglePlayServicesAvailable(context);
    }

    public static boolean isLocationServicesEnabled(Context context) {
        return OkHiLocationService.isLocationServicesEnabled(context);
    }

    public static boolean canRestartGeofences(Context context) {
        return isBackgroundLocationPermissionGranted(context) && isGooglePlayServicesAvailable(context) && isLocationServicesEnabled(context);
    }

    public static void getCurrentLocation(Context context, final ResultHandler<Location> handler) {
        // TODO: remove throw signature from core library
        try {
            OkHiLocationService.getCurrentLocation(context, new OkHiRequestHandler<Location>() {
                @Override
                public void onResult(Location result) {
                    handler.onSuccess(result);
                }

                @Override
                public void onError(OkHiException exception) {
                    handler.onError(new BackgroundGeofencingException(exception.getCode(), exception.getMessage()));
                }
            });
        } catch (OkHiException e) {
            e.printStackTrace();
            handler.onError(new BackgroundGeofencingException(e.getCode(), e.getMessage()));
        }
    }

    public static void scheduleForegroundRestartWorker (Context context, int initialDelay, TimeUnit unit) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(BackgroundGeofenceForegroundRestartWorker.class, 1, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, unit)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(Constant.FOREGROUND_SERVICE_UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    public static void cancelForegroundRestartWorker (Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constant.FOREGROUND_SERVICE_UNIQUE_WORK);
    }
}
