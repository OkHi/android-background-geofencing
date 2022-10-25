package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.services.BackgroundGeofencingNotificationService;
import io.okhi.android_core.OkHi;

public class BackgroundGeofenceLocationServicesReceiver extends BroadcastReceiver {
  private static String ACTION_MATCH = "android.location.PROVIDERS_CHANGED";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().matches(ACTION_MATCH) && BackgroundGeofencing.isForegroundServiceRunning(context.getApplicationContext())) {
      if (!OkHi.isLocationServicesEnabled(context)) {
        BackgroundGeofencingNotificationService.notifyEnableLocationServices(context);
      } else {
        BackgroundGeofencingNotification.resetNotification(context);
      }
    }
  }
}
