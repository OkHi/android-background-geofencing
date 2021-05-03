package io.okhi.android_background_geofencing.models;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;

public class BackgroundGeofenceDeviceMeta {
  private String deviceId;
  private String deviceManufacturer = Build.MANUFACTURER;
  private String deviceModel = Build.MODEL;
  private String deviceName = Build.PRODUCT;
  private int deviceBatteryLevel = -1;

  private String deviceOsName = "Android";
  private String deviceOsVersion = Build.VERSION.RELEASE;

  private String networkCarrierName;
  private String networkSimOperatorName;
  private boolean networkCellular = false;
  private boolean networkWifi = false;
  private String netWorkSSID;
  private ArrayList<HashMap<String, String>> permissions = new ArrayList<>();

  public BackgroundGeofenceDeviceMeta(Context context) {
    BackgroundGeofencingDB.saveDeviceId(context);
    boolean isBackgroundLocationPermissionGranted = OkHiPermissionService.isBackgroundLocationPermissionGranted(context);
    boolean isLocationPermissionGranted = OkHiPermissionService.isLocationPermissionGranted(context);
    boolean isLocationServicesEnabled = OkHiLocationService.isLocationServicesEnabled(context);
    this.deviceId = BackgroundGeofencingDB.getDeviceId(context);
    BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    this.networkCarrierName = manager.getNetworkOperatorName();
    this.networkSimOperatorName = manager.getSimOperatorName();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this.deviceBatteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100;
    }
    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    this.networkWifi = wifiNetworkInfo.isConnected();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      this.networkCellular = manager.isDataEnabled();
    }
    if (isLocationServicesEnabled && isLocationPermissionGranted && this.networkWifi) {
      WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      WifiInfo info = wifiManager.getConnectionInfo();
      this.netWorkSSID = info.getSSID();
    }
    HashMap<String,String> locationPermission = new HashMap<>();
    locationPermission.put("type", "location");
    locationPermission.put("status", isLocationPermissionGranted ? "granted" : "denied");
    if (isLocationPermissionGranted) {
      locationPermission.put("level", isBackgroundLocationPermissionGranted ? "always" : "whenInUse");
    }
    permissions.add(locationPermission);
  }

  public String toJSON() {
    try {
      JSONObject payload = new JSONObject();
      JSONObject deviceInformation = new JSONObject();
      JSONObject osInformation = new JSONObject();
      JSONObject networkInformation = new JSONObject();
      JSONArray permissionsInformation = new JSONArray(this.permissions);

      deviceInformation.put("id", this.deviceId);
      deviceInformation.put("manufacturer", this.deviceManufacturer);
      deviceInformation.put("model", this.deviceModel);
      deviceInformation.put("name", this.deviceName);
      deviceInformation.put("batteryLevel", this.deviceBatteryLevel);

      osInformation.put("name", this.deviceOsName);
      osInformation.put("version", this.deviceOsVersion);

      networkInformation.put("carrier", this.networkCarrierName);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        networkInformation.put("cellular", this.networkCellular);
      }
      networkInformation.put("wifi", this.networkWifi);
      if (this.netWorkSSID != null) {
        networkInformation.put("wifiSSID", this.netWorkSSID);
      }

      payload.put("device", deviceInformation);
      payload.put("os", osInformation);
      payload.put("network", networkInformation);
      payload.put("permissions", permissionsInformation);

      return payload.toString();

    } catch (Exception error) {
      return null;
    }
  }
}
