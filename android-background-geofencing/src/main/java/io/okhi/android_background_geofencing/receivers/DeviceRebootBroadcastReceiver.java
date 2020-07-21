package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;

public class DeviceRebootBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "DeviceRebootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v(TAG, "Device reboot detected");
        // TODO: refactor this to a static method
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(context);
        for (final BackgroundGeofence geofence: geofences) {
            geofence.restart(context, new RequestHandler() {
                @Override
                public void onSuccess() {
                    Log.v(TAG, "Successfully restarted: " + geofence.getId());
                    BackgroundGeofence.setIsFailing(geofence.getId(), false, context);
                }

                @Override
                public void onError() {
                    Log.v(TAG, "Failed to start: " + geofence.getId());
                    BackgroundGeofence.setIsFailing(geofence.getId(), true, context);
                }
            });
        }
        // schedule to restart any failing geofences
        BackgroundGeofence.scheduleGeofenceRestartWork(context);

        // schedule to upload any stored geofence transitions
        BackgroundGeofenceTransition.scheduleGeofenceTransitionUploadWork(context);
    }
}
