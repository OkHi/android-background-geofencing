package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_core.OkHi;

public class GPSLocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().matches("android.location.PROVIDERS_CHANGED")){
            boolean isLocationServicesEnabled = OkHi.isLocationServicesEnabled(context);
            boolean isForegroundServiceRunning = BackgroundGeofencing.isForegroundServiceRunning(context);
            Intent locationServicesSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            if (!isLocationServicesEnabled && isForegroundServiceRunning) {
                int color = Color.argb(255, 255, 0, 0);
                BackgroundGeofencingNotification.updateNotification(
                        context,
                        "Turn on GPS",
                        "Please turn on GPS to continue with the verification",
                        color,
                        locationServicesSettingsIntent
                );
            } else if (isForegroundServiceRunning) {
                BackgroundGeofencingNotification.resetNotification(context);
            }  // Do nothing
        }
    }
}
