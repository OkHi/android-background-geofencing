package io.okhi.android_background_geofencing;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceDeviceMeta;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundService;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceRestartWorker;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;

public class BackgroundGeofencing {

    public static void init(final Context context, BackgroundGeofencingNotification notification) {
        WorkManager.getInstance(context).cancelAllWork();
        BackgroundGeofencingDB.saveNotification(notification, context);
        BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(context);
        BackgroundGeofenceDeviceMeta deviceMeta = new BackgroundGeofenceDeviceMeta(context);
        Log.v("BackgroundGeofencing", deviceMeta.toJSON());
        boolean isAppOnForeground = BackgroundGeofenceUtil.isAppOnForeground(context);
        boolean hasWebhook = BackgroundGeofencingDB.getWebHook(context) != null;
        boolean canRestartGeofences  = BackgroundGeofenceUtil.canRestartGeofences(context);
        boolean canPerformInitWork = isAppOnForeground && hasWebhook && canRestartGeofences;
        if (canPerformInitWork) {
            performInitWork(context, new RequestHandler() {
                @Override
                public void onSuccess() {
                    performBackgroundWork(context);
                }

                @Override
                public void onError(BackgroundGeofencingException exception) {
                    performBackgroundWork(context);
                }
            });
        } else {
            performBackgroundWork(context);
        }
        if (setting != null && setting.isWithForegroundService()) {
            try {
                startForegroundService(context);
            } catch (BackgroundGeofencingException e) {
                e.printStackTrace();
            }
        }
    }

    private static void performInitWork(final Context context, final RequestHandler handler) {
        BackgroundGeofenceUtil.getCurrentLocation(context, new ResultHandler<Location>() {
            @Override
            public void onSuccess(Location location) {
                triggerInitGeofenceEvents(location, context, handler);
            }
            @Override
            public void onError(BackgroundGeofencingException exception) {
                handler.onError(exception);
            }
        });
    }

    private static void triggerInitGeofenceEvents(Location location, Context context, RequestHandler handler) {
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(context);
        if (!geofences.isEmpty()) {
            ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofenceTransition.generateTransitions(
                    Constant.INIT_GEOFENCE_TRANSITION_SOURCE_NAME,
                    location,
                    geofences,
                    false,
                    context
            );
            for (BackgroundGeofenceTransition transition : transitions) {
                transition.save(context);
            }
        }
        handler.onSuccess();
    }

    private static void performBackgroundWork(Context context) {
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
                .setConstraints(Constant.GEOFENCE_WORK_MANAGER_INIT_CONSTRAINTS)
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

    public static void stopForegroundService (Context context) {
        if (!isForegroundServiceRunning(context)) {
            return;
        }
        BackgroundGeofencingDB.saveSetting(new BackgroundGeofenceSetting.Builder().setWithForegroundService(false).build(), context);
        Intent serviceIntent = new Intent(context, BackgroundGeofenceForegroundService.class);
        serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_STOP);
        ContextCompat.startForegroundService(context, serviceIntent);
        BackgroundGeofenceUtil.cancelForegroundRestartWorker(context);
    }

    public static void startForegroundService (Context context) throws BackgroundGeofencingException {
        boolean hasGeofences = !BackgroundGeofencingDB.getAllGeofences(context).isEmpty();
        boolean isBackgroundLocationPermissionGranted = BackgroundGeofenceUtil.isBackgroundLocationPermissionGranted(context);
        boolean isGooglePlayServicesAvailable = BackgroundGeofenceUtil.isGooglePlayServicesAvailable(context);
        boolean isLocationServicesEnabled = BackgroundGeofenceUtil.isLocationServicesEnabled(context);
        boolean isNotificationAvailable = BackgroundGeofencingDB.getNotification(context) != null;
        if (isForegroundServiceRunning(context)) {
            return;
        }
        if (!hasGeofences || !isBackgroundLocationPermissionGranted || !isGooglePlayServicesAvailable || !isLocationServicesEnabled || !isNotificationAvailable) {
            String message = !hasGeofences ? "No saved geofences" :
                    !isBackgroundLocationPermissionGranted ? "Background location permission not granted" :
                            !isGooglePlayServicesAvailable ? "Google play services are currently unavailable" :
                                    !isNotificationAvailable ? "Notification configuration unavailable" :
                                            "Location services are unavailable" ;
            throw new BackgroundGeofencingException(BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE, message);
        }
        BackgroundGeofencingDB.saveSetting(new BackgroundGeofenceSetting.Builder().setWithForegroundService(true).build(), context);
        Intent serviceIntent = new Intent(context, BackgroundGeofenceForegroundService.class);
        serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_START_STICKY);
        ContextCompat.startForegroundService(context, serviceIntent);
        BackgroundGeofenceUtil.scheduleForegroundRestartWorker(context, 1, TimeUnit.HOURS);
    }

    public static boolean isForegroundServiceRunning (Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BackgroundGeofenceForegroundService.class.getName().equals(service.service.getClassName())) {
                if (service.foreground || service.started) {
                    return true;
                }

            }
        }
        return false;
    }
}
