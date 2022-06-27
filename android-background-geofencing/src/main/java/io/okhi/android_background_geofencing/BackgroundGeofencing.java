package io.okhi.android_background_geofencing;

import static io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil.scheduleServiceRestarts;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.work.Operation;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceAppOpen;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceDeviceMeta;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSource;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.receivers.UserPresentBroadcastReceiver;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundService;

public class BackgroundGeofencing {


  private final int PHONE_CODE = 1001;
  private final int DEVICE_ADMIN_CODE = 1002;
  private final int DEVICE_PROTECTED_APPS = 1003;

  private final Activity activity;

  public BackgroundGeofencing(Activity activity) {
    this.activity = activity;
  }

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
    registerComponents(context);
    registerReceivers(context);
    if (!allGeofences.isEmpty()) {
      new BackgroundGeofenceDeviceMeta(context, allGeofences).asyncUpload();
      BackgroundGeofenceAppOpen.transmitAppOpenEvent(context, webHook);
      if (setting != null && setting.isWithForegroundService()) {
        try {
          startForegroundService(context);
        } catch (BackgroundGeofencingException e) {
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

  public static void startForegroundService (Context context) throws BackgroundGeofencingException {
    boolean hasGeofences = !BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_PING).isEmpty() || !BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_WATCH).isEmpty();
    boolean isBackgroundLocationPermissionGranted = BackgroundGeofenceUtil.isBackgroundLocationPermissionGranted(context);
    boolean isGooglePlayServicesAvailable = BackgroundGeofenceUtil.isGooglePlayServicesAvailable(context);
    boolean isLocationServicesEnabled = BackgroundGeofenceUtil.isLocationServicesEnabled(context);
    boolean isNotificationAvailable = BackgroundGeofencingDB.getNotification(context) != null;
    scheduleServiceRestarts(context);
    if (isForegroundServiceRunning(context)) {
      return;
    }
    if (!hasGeofences || !isBackgroundLocationPermissionGranted || !isGooglePlayServicesAvailable || !isLocationServicesEnabled || !isNotificationAvailable) {
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
    ContextCompat.startForegroundService(context, serviceIntent);
    BackgroundGeofenceUtil.scheduleForegroundRestartWorker(context, 1, TimeUnit.HOURS);
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

  public static void registerComponents(Context context){

    ComponentName userPresent = new ComponentName(context, UserPresentBroadcastReceiver.class);

    PackageManager pm = context.getPackageManager();
    pm.setComponentEnabledSetting(
            userPresent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
    );

  }

  public static void registerReceivers(Context context){

    BroadcastReceiver br = new UserPresentBroadcastReceiver();

    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_USER_PRESENT);

    if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ){
      filter.addAction(Intent.ACTION_USER_UNLOCKED);
    }
    filter.addAction(Intent.ACTION_DREAMING_STOPPED);
    filter.addAction(Intent.ACTION_POWER_CONNECTED);
    filter.addAction(Intent.ACTION_SCREEN_ON);

    context.registerReceiver(br, filter);

  }

  public void requestAutoLoad(){

    String manufacturer = Build.MANUFACTURER;
    Intent intent = new Intent();
    // Transsion Group
    if (
            manufacturer.toLowerCase().contains("infinix") ||
            manufacturer.toLowerCase().contains("tecno") ||
            manufacturer.toLowerCase().contains("itel")
    ){
      ComponentName component = new ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity");
      intent.setComponent(component);
      activity.startActivityForResult(intent, PHONE_CODE);
    }

    // XIAOMI (Redmi)
    if (manufacturer.toLowerCase().contains("xiaomi")){
      ComponentName component = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
      intent.setComponent(component);
      activity.startActivityForResult(intent, PHONE_CODE);
    }

    // OPPO
    if (manufacturer.toLowerCase().contains("oppo")){
      ComponentName component = new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity");
      intent.setClassName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity");
      intent.setComponent(component);
      activity.startActivityForResult(intent, PHONE_CODE);
    }

    // VIVO
    if (manufacturer.toLowerCase().contains("vivo")){
      ComponentName component = new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity");
      intent.setComponent(component);
      activity.startActivityForResult(intent, PHONE_CODE);
    }

    // Letv
    if (manufacturer.toLowerCase().contains("letv")){
      ComponentName component = new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity");
      intent.setComponent(component);
      activity.startActivityForResult(intent, PHONE_CODE);
    }

  }

  public void requestAdminAccess(Context context){

    DevicePolicyManager mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName mAdminName = new ComponentName(context, BackgroundGeofenceForegroundService.class);

    try {
      if (!mDPM.isAdminActive(mAdminName)) {
        try {
          Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
          intent.putExtra( DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                  "Click on Activate button to secure your application."
          );
          ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings");
          intent.setComponent(componentName);
          activity.startActivityForResult(intent, DEVICE_ADMIN_CODE);

        } catch (Exception e) {
          e.printStackTrace();
          Log.e("Permissions error", e.toString());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      Log.e("RequestAdminAccess", e.toString());
    }
  }

  public void requestAppProtection(){

    String manufacturer = Build.MANUFACTURER;

      try {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra( DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Add Okhi to Protected Apps for the app to work efficiently"
        );
        // Transsion Group
        if ( manufacturer.toLowerCase().contains("infinix") ||
             manufacturer.toLowerCase().contains("tecno") ||
             manufacturer.toLowerCase().contains("itel")
        ){
          ComponentName componentName = new ComponentName("com.transsion.phonemaster", "com.cyin.himgr.widget.activity.MainSettingGpActivity");
          intent.setComponent(componentName);
        }
        activity.startActivityForResult(intent, DEVICE_PROTECTED_APPS);

      } catch (Exception e) {
        e.printStackTrace();
        Log.e("Permissions error", e.toString());
      }
  }

  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

    switch(requestCode) {
      case PHONE_CODE: {
        Log.d("On Result", "--------------------> PHONE_CODE response Code " + resultCode);
      }
      case DEVICE_ADMIN_CODE: {

        Log.d("On Result", "--------------------> DEVICE_ADMIN_CODE response Code " + resultCode);
      }
      case DEVICE_PROTECTED_APPS: {

        Log.d("On Result", "--------------------> DEVICE_PROTECTED_APPS response Code " + resultCode);
      }
    }

  }

}
