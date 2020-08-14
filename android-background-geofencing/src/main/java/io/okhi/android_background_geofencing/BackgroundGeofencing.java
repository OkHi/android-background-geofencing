package io.okhi.android_background_geofencing;

import android.content.Context;
import android.location.Location;

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
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceRestartWorker;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;

public class BackgroundGeofencing {
    public static void init(final Context context) {
        WorkManager.getInstance(context).cancelAllWork();
        BackgroundGeofencingWebHook webhook = BackgroundGeofencingDB.getWebHook(context);
        if (webhook != null) {
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
    }

    private static void performInitWork(final Context context, final RequestHandler handler) {
        BackgroundGeofencingLocationService.getCurrentLocation(context, new ResultHandler<Location>() {
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
