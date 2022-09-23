package io.okhi.android_background_geofencing.models;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.activity.OkHiWebViewActivity;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
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

  public static void notifyEnableBackgroundLocationPermission(Context context) {
    boolean isForegroundServiceRunning = BackgroundGeofencing.isForegroundServiceRunning(context);
    boolean isBackgroundLocationPermissionGranted = OkHi.isBackgroundLocationPermissionGranted(context);
    Intent locationServicesSettingsIntent = getNotificationIntent(context, IntentSettings.APP_SETTINGS);
    if (!isBackgroundLocationPermissionGranted && isForegroundServiceRunning) {
      BackgroundGeofencingNotification.updateNotification(context, Constant.PERSISTENT_NOTIFICATION_GENERIC_ERROR_TITLE,Constant.PERSISTENT_NOTIFICATION_LOCATION_PERMISSION_ERROR_TEXT, Constant.PERSISTENT_NOTIFICATION_ERROR_COLOR, locationServicesSettingsIntent);
    }
  }

  public static Notification getForegroundServiceNotification(Context context) {
    backgroundGeofencingNotification = BackgroundGeofencingDB.getNotification(context);
    backgroundGeofencingNotification.createNotificationChannel(context);
    Notification notification = backgroundGeofencingNotification.getNotification(context);
    boolean isBackgroundLocationPermissionGranted = OkHi.isBackgroundLocationPermissionGranted(context);
    if (!isBackgroundLocationPermissionGranted) {
      Intent locationServicesSettingsIntent = getNotificationIntent(context, IntentSettings.APP_SETTINGS);
      backgroundGeofencingNotification.setTitle(Constant.PERSISTENT_NOTIFICATION_GENERIC_ERROR_TITLE);
      backgroundGeofencingNotification.setText(Constant.PERSISTENT_NOTIFICATION_LOCATION_PERMISSION_ERROR_TEXT);
      notification = backgroundGeofencingNotification.getNotification(context, Constant.PERSISTENT_NOTIFICATION_ERROR_COLOR, locationServicesSettingsIntent);
    }
    return notification;
  }

  public static int getForegroundServiceNotificationId(Context context) {
    if (backgroundGeofencingNotification == null) {
      backgroundGeofencingNotification = BackgroundGeofencingDB.getNotification(context);
    }
    return backgroundGeofencingNotification.getNotificationId();
  }

  private static Intent getNotificationIntent(Context context, IntentSettings settings) {
    boolean isLocationServicesEnabled = OkHi.isLocationServicesEnabled(context);
    boolean isConnected = BackgroundGeofenceUtil.isNetworkAvailable(context);
    boolean isLocationPermissionGranted = OkHi.isLocationPermissionGranted(context);
    boolean canLaunchWebView = OkHiWebViewActivity.canLaunchWebView(context);
    Intent locationServicesSettingsIntent;
    if (isConnected && canLaunchWebView) {
      boolean isBackgroundLocationPermissionGranted = OkHi.isBackgroundLocationPermissionGranted(context);
      locationServicesSettingsIntent = new Intent(context, OkHiWebViewActivity.class);
      locationServicesSettingsIntent.putExtra("locationPermissionLevel", isBackgroundLocationPermissionGranted ? "always" : isLocationPermissionGranted ? "whenInUse" : "denied");
      locationServicesSettingsIntent.putExtra("locationServicesAvailable", isLocationServicesEnabled);
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
