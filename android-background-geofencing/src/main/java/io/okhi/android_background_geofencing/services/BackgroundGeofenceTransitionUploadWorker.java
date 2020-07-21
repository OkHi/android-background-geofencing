package io.okhi.android_background_geofencing.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;

public class BackgroundGeofenceTransitionUploadWorker extends Worker {
    public BackgroundGeofenceTransitionUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // check if we've hit our max attempts
        if (getRunAttemptCount() > Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_MAX_ATTEMPTS) {
            return Result.failure();
        }

        // get webhook configuration
        BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(getApplicationContext());

        // get all transitions from db
        ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofencingDB.getAllGeofenceTransitions(getApplicationContext());

        // check if we have a webhook and transitions to upload
        if (webHook == null || transitions.isEmpty()) {
            return Result.success();
        }

        // keep track of upload status
        boolean uploadSuccess = true;

        try {
            // attempt to upload all transitions
            for (BackgroundGeofenceTransition transition: transitions) {
                if (transition.syncUpload(webHook)) {
                    // remove from db if upload succeeds
                    BackgroundGeofencingDB.removeGeofenceTransition(transition, getApplicationContext());
                } else {
                    // break if upload process fails
                    uploadSuccess = false;
                    break;
                }
            }

            // check status, attempt retry based on back-off policy
            if (uploadSuccess) {
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // retry if we get an IO exception
            return Result.retry();
        } catch (Exception e) {
            // fail if we have any other exceptions
            return Result.failure();
        }
    }
}
