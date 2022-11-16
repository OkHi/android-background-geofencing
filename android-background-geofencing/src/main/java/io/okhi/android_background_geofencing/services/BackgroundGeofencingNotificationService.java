package io.okhi.android_background_geofencing.services;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.activities.BackgroundGeofencingWebViewActivity;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_core.OkHi;

public class BackgroundGeofencingNotificationService {
  private static BackgroundGeofencingNotification backgroundGeofencingNotification;
  private enum IntentSettings {
    LOCATION_SERVICES,
    APP_SETTINGS
  }

  public static void notifyEnableLocationServices(Context context) {
    boolean isLocationServicesEnabled = OkHi.isLocationServicesEnabled(context);
    boolean isForegroundServiceRunning = BackgroundGeofencing.isForegroundServiceRunning(context);
    Intent locationServicesSettingsIntent = getNotificationIntent(context, IntentSettings.LOCATION_SERVICES);
    if (!isLocationServicesEnabled && isForegroundServiceRunning) {
      BackgroundGeofencingNotification.updateNotification(context, Constant.PERSISTENT_NOTIFICATION_GENERIC_ERROR_TITLE,Constant.PERSISTENT_NOTIFICATION_LOCATION_SERVICES_ERROR_TEXT, Constant.PERSISTENT_NOTIFICATION_ERROR_COLOR, locationServicesSettingsIntent);
    }
  }

  private static Intent getNotificationIntent(Context context, IntentSettings settings) {
    boolean isConnected = BackgroundGeofenceUtil.isNetworkAvailable(context);
    boolean canLaunchWebView = BackgroundGeofencingWebViewActivity.canLaunchWebView(context);
    Intent locationServicesSettingsIntent;
    if (isConnected && canLaunchWebView) {
      locationServicesSettingsIntent = new Intent(context, BackgroundGeofencingWebViewActivity.class);
    } else if (settings == IntentSettings.LOCATION_SERVICES) {
      locationServicesSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    } else {
      locationServicesSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      Uri uri = Uri.fromParts("package", context.getPackageName(), null);
      locationServicesSettingsIntent.setData(uri);
    }
    return locationServicesSettingsIntent;
  }
}
