package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(context);
        if (setting != null && setting.isWithForegroundService() && !BackgroundGeofencing.isForegroundServiceRunning(context)) {
            try {
                if(BackgroundGeofencing.canStartForegroundService(context)){
                    BackgroundGeofencing.startForegroundService(context);

                    BackgroundGeofenceUtil.scheduleForegroundRestartWorker(context, 1, TimeUnit.HOURS);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
