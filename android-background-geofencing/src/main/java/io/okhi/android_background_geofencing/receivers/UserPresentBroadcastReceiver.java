package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSource;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;

public class UserPresentBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "UserPresentBroadcastReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        BackgroundGeofenceUtil.log(context, TAG, "Device reboot detected");
        // TODO: refactor this to a static method
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.NATIVE_GEOFENCE);
        for (final BackgroundGeofence geofence : geofences) {
            geofence.restart(context, new RequestHandler() {
                @Override
                public void onSuccess() {
                    BackgroundGeofenceUtil.log(context, TAG, "Successfully restarted: " + geofence.getId());
                    BackgroundGeofence.setIsFailing(geofence.getId(), false, context);
                }

                @Override
                public void onError(BackgroundGeofencingException e) {
                    BackgroundGeofenceUtil.log(context, TAG, "Failed to start: " + geofence.getId());
                    BackgroundGeofence.setIsFailing(geofence.getId(), true, context);
                }
            });
        }
        BackgroundGeofencing.performBackgroundWork(context);

        BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(context);
        if (setting != null && setting.isWithForegroundService() && !BackgroundGeofencing.isForegroundServiceRunning(context)) {
            try {
                BackgroundGeofencing.startForegroundService(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
            BackgroundGeofenceUtil.scheduleForegroundRestartWorker(context, 1, TimeUnit.HOURS);
        }
    }
}
