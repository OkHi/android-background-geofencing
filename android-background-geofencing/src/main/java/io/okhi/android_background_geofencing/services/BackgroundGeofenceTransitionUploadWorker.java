package io.okhi.android_background_geofencing.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_core.models.OkHiCoreUtil;

public class BackgroundGeofenceTransitionUploadWorker extends Worker {

    private static Lock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();

    public BackgroundGeofenceTransitionUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            boolean result = uploadTransitions(getApplicationContext());
            if (result) {
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    // TODO: is this the best place to put this?
    public static boolean uploadTransitions(Context context) throws JSONException {
        lock.lock();
        // get webhook configuration
        BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(context);
        // get all transitions from db
        ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofencingDB.getAllGeofenceTransitions(context);
        // check if we have a webhook and transitions to upload
        if (webHook == null || transitions.isEmpty()) {
            return true;
        }
        // keep track of upload status
        boolean uploadSuccess = true;
        try {
            // attempt to upload all transitions
            for (BackgroundGeofenceTransition transition : transitions) {
                if (transition != null) {
                    BackgroundGeofence geofence = transition.getTriggeringGeofence(context);
                    if (geofence == null) {
                        BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                    } else {
                        if (!geofence.isWithAppOpenTracking() && transition.getGeoPointSource().equals("appOpen")) {
                            BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                        } else if (!geofence.isWithNativeGeofenceTracking() && transition.getGeoPointSource().equals("geofence")) {
                            BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                        } else if (!geofence.isWithForegroundWatchTracking() && transition.getGeoPointSource().equals("foregroundWatch")) {
                            BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                        } else if (!geofence.isWithForegroundPingTracking() && transition.getGeoPointSource().equals("foregroundPing")) {
                            BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                        } else {
                            boolean result = transition.syncUpload(context, webHook);
                            if (result) {
                                // remove from db if upload succeeds
                                BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                            }
                            else {
                                // break if upload process fails
                                uploadSuccess = false;
                                break;
                            }
                        }
                    }
                } else {
                    BackgroundGeofencingDB.removeGeofenceTransition(transition, context);
                }
            }
            // check status, attempt retry based on back-off policy
            if (uploadSuccess) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            // retry if we get an IO exception
            return false;
        } catch (Exception e) {
            // fail if we have any other exceptions
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
