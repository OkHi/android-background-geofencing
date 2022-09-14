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
import io.okhi.android_core.OkHi;

public class GPSLocationReceiver extends BroadcastReceiver {
    private static String ACTION_MATCH = "android.location.PROVIDERS_CHANGED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(ACTION_MATCH)) {
            boolean isLocationServicesEnabled = OkHi.isLocationServicesEnabled(context);
            boolean isForegroundServiceRunning = BackgroundGeofencing.isForegroundServiceRunning(context);
            boolean isConnected = BackgroundGeofenceUtil.isNetworkAvailable(context);
            boolean isLocationPermissionGranted = OkHi.isLocationPermissionGranted(context);
            Intent locationServicesSettingsIntent;
            if (isConnected) {
                boolean isBackgroundLocationPermissionGranted = OkHi.isBackgroundLocationPermissionGranted(context);
                locationServicesSettingsIntent = new Intent(context, OkHiWebViewActivity.class);
                locationServicesSettingsIntent.putExtra("locationPermissionLevel", isBackgroundLocationPermissionGranted ? "always" : isLocationPermissionGranted ? "whenInUse" : "denied");
                locationServicesSettingsIntent.putExtra("locationServicesAvailable", isLocationServicesEnabled);
            } else {
                locationServicesSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            }
            if (!isLocationServicesEnabled && isForegroundServiceRunning) {
                int color = Color.argb(255, 255, 0, 0);
                BackgroundGeofencingNotification.updateNotification(context,"Address Verification stopped","Enable location services to continue with verification", color, locationServicesSettingsIntent);
            } else if (isForegroundServiceRunning) {
                BackgroundGeofencingNotification.resetNotification(context);
            }
        }
    }
}
