package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Objects;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_core.models.OkHiException;

public class DeviceRebootBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "DeviceRebootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            Log.v(TAG, "Device reboot detected");
            // TODO: refactor this to a static method
            ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(context);
            for (final BackgroundGeofence geofence : geofences) {
                geofence.restart(context, new RequestHandler() {
                    @Override
                    public void onSuccess() {
                        Log.v(TAG, "Successfully restarted: " + geofence.getId());
                        BackgroundGeofence.setIsFailing(geofence.getId(), false, context);
                    }

                    @Override
                    public void onError(OkHiException e) {
                        Log.v(TAG, "Failed to start: " + geofence.getId());
                        BackgroundGeofence.setIsFailing(geofence.getId(), true, context);
                    }
                });
            }
            BackgroundGeofencing.init(context);
        }
    }
}
