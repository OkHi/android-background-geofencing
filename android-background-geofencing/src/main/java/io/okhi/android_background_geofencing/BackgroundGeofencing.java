package io.okhi.android_background_geofencing;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.core.content.ContextCompat;
import androidx.work.Operation;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceAppOpen;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceDeviceMeta;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSource;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundService;

public class BackgroundGeofencing {

  public static void init(final Context context, final BackgroundGeofencingNotification notification) {
    Operation operation = WorkManager.getInstance(context).cancelAllWork();
    operation.getResult().addListener(new Runnable() {
      @Override
      public void run() {
        BackgroundGeofencing.startUpSequence(context, notification);
      }
    }, new Executor() {
      @Override
      public void execute(Runnable command) {
        command.run();
      }
    });
  }

  private static void startUpSequence(final Context context, BackgroundGeofencingNotification notification) {
    BackgroundGeofencingDB.saveNotification(notification, context);
    BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(context);
    BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(context);
    ArrayList<BackgroundGeofence> allGeofences = BackgroundGeofencingDB.getAllGeofences(context);
    boolean isAppOnForeground = BackgroundGeofenceUtil.isAppOnForeground(context);
    boolean canRestartGeofences  = BackgroundGeofenceUtil.canRestartGeofences(context);
    if (!allGeofences.isEmpty()) {
      new BackgroundGeofenceDeviceMeta(context, allGeofences).asyncUpload();
      BackgroundGeofenceAppOpen.transmitAppOpenEvent(context, webHook);
      if (setting != null && setting.isWithForegroundService()) {
        try {
          startForegroundService(context);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (isAppOnForeground && webHook != null && canRestartGeofences) {
        performBackgroundWork(context);
      }
    }
  }

  public static void performBackgroundWork(Context context) {
    BackgroundGeofenceTransition.asyncUploadAllTransitions(context);
  }

  public static void stopForegroundService (Context context) {
    if (!isForegroundServiceRunning(context)) {
      return;
    }
    BackgroundGeofencingDB.saveSetting(new BackgroundGeofenceSetting.Builder().setWithForegroundService(false).build(), context);
    Intent serviceIntent = new Intent(context, BackgroundGeofenceForegroundService.class);
    serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_STOP);
    ContextCompat.startForegroundService(context, serviceIntent);
    BackgroundGeofenceUtil.cancelForegroundRestartWorker(context);
  }

  public static void startForegroundService (Context context) throws Exception {
    boolean hasGeofences = !BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_PING).isEmpty() || !BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_WATCH).isEmpty();
    boolean isBackgroundLocationPermissionGranted = BackgroundGeofenceUtil.isBackgroundLocationPermissionGranted(context);
    boolean isGooglePlayServicesAvailable = BackgroundGeofenceUtil.isGooglePlayServicesAvailable(context);
    boolean isLocationServicesEnabled = BackgroundGeofenceUtil.isLocationServicesEnabled(context);
    boolean isNotificationAvailable = BackgroundGeofencingDB.getNotification(context) != null;
    if (isForegroundServiceRunning(context)) {
      return;
    }
    if ( !hasGeofences || !isBackgroundLocationPermissionGranted || !isGooglePlayServicesAvailable || !isLocationServicesEnabled || !isNotificationAvailable) {
      String message = !hasGeofences ? "No saved viable foreground locations" :
          !isBackgroundLocationPermissionGranted ? "Background location permission not granted" :
              !isGooglePlayServicesAvailable ? "Google play services are currently unavailable" :
                  !isNotificationAvailable ? "Notification configuration unavailable" :
                      "Location services are unavailable" ;
      throw new BackgroundGeofencingException(BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE, message);
    }
    BackgroundGeofencingDB.saveSetting(new BackgroundGeofenceSetting.Builder().setWithForegroundService(true).build(), context);
    Intent serviceIntent = new Intent(context, BackgroundGeofenceForegroundService.class);
    serviceIntent.putExtra(Constant.FOREGROUND_SERVICE_ACTION, Constant.FOREGROUND_SERVICE_START_STICKY);

    try {
      ContextCompat.startForegroundService(context, serviceIntent);
      BackgroundGeofenceUtil.scheduleForegroundRestartWorker(context, 1, TimeUnit.HOURS);
    } catch (Exception e) {
      throw new BackgroundGeofencingException(
              BackgroundGeofencingException.UNKNOWN_EXCEPTION,
              "Could not start background service"
      );
    }
  }

  public static boolean isForegroundServiceRunning (Context context) {
    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (BackgroundGeofenceForegroundService.class.getName().equals(service.service.getClassName())) {
        if (service.foreground || service.started) {
          return true;
        }

      }
    }
    return false;
  }

  public static void triggerGeofenceEvents(Context context, String source, String geoPointProviderSuffix) {
    BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(context);
    if (webHook == null) return;
    BackgroundGeofencingLocationService service = new BackgroundGeofencingLocationService();
    service.fetchCurrentLocation(context, new ResultHandler<Location>() {
      @Override
      public void onSuccess(Location location) {
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(context);
        ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofenceTransition.generateTransitions(
          source == null ? Constant.FOREGROUND_SERVICE_PING_GEOFENCE_SOURCE : source,
          geoPointProviderSuffix,
          location,
          geofences,
          false,
          context
        );
        for(BackgroundGeofenceTransition transition: transitions) {
          transition.asyncUpload(context, webHook, false, new ResultHandler<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {

            }
            @Override
            public void onError(BackgroundGeofencingException exception) {
              exception.printStackTrace();
            }
          });
        }
      }
      @Override
      public void onError(BackgroundGeofencingException exception) {
        exception.printStackTrace();
      }
    });
  }
}
