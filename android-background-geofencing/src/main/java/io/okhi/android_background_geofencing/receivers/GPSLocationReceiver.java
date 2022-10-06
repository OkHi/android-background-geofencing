package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.activity.OkHiWebViewActivity;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotificationService;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_core.OkHi;

public class GPSLocationReceiver extends BroadcastReceiver {
    private static String ACTION_MATCH = "android.location.PROVIDERS_CHANGED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(ACTION_MATCH)) {
            if (!OkHi.isLocationServicesEnabled(context)) {
                BackgroundGeofencingNotificationService.notifyEnableLocationServices(context);
            } else if (!OkHi.isBackgroundLocationPermissionGranted(context)) {
                BackgroundGeofencingNotificationService.notifyEnableBackgroundLocationPermission(context);
            } else {
                BackgroundGeofencingNotification.resetNotification(context);
            }
        }
    }
}
