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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackgroundGeofenceDeviceMeta {
  private String deviceId;
  private String deviceManufacturer = Build.MANUFACTURER;
  private String deviceModel = Build.MODEL;
  private String deviceName = Build.PRODUCT;
  private double deviceBatteryLevel = -1;

  private String deviceOsName = "Android";
  private String deviceOsVersion = Build.VERSION.RELEASE;

  private String networkCarrierName;
  private String networkSimOperatorName;
  private boolean networkCellular = false;
  private boolean networkWifi = false;
  private String netWorkSSID;
  private ArrayList<HashMap<String, String>> permissions = new ArrayList<>();

  private String timezone = TimeZone.getDefault().getID();
  private ArrayList<String> geofenceIds = new ArrayList<>();
  private Context context;

  public BackgroundGeofenceDeviceMeta(Context context, ArrayList<BackgroundGeofence> geofences) {
    this.context = context;
    BackgroundGeofencingDB.saveDeviceId(this.context);

    boolean isBackgroundLocationPermissionGranted = OkHiPermissionService.isBackgroundLocationPermissionGranted(this.context);
    boolean isLocationPermissionGranted = OkHiPermissionService.isLocationPermissionGranted(this.context);
    boolean isLocationServicesEnabled = OkHiLocationService.isLocationServicesEnabled(this.context);
    this.deviceId = BackgroundGeofencingDB.getDeviceId(this.context);
    BatteryManager bm = (BatteryManager) this.context.getSystemService(Context.BATTERY_SERVICE);
    TelephonyManager manager = (TelephonyManager)this.context.getSystemService(Context.TELEPHONY_SERVICE);
    this.networkCarrierName = manager.getNetworkOperatorName();
    this.networkSimOperatorName = manager.getSimOperatorName();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      this.deviceBatteryLevel = (double) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100.0;
    }
    ConnectivityManager connManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo wifiNetworkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    this.networkWifi = wifiNetworkInfo.isConnected();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      this.networkCellular = manager.isDataEnabled();
    }
    if (isLocationServicesEnabled && isLocationPermissionGranted && this.networkWifi) {
      WifiManager wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
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
    for(BackgroundGeofence geofence: geofences) {
      this.geofenceIds.add(geofence.getId());
    }
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
      payload.put("timezone", timezone);
      payload.put("device", deviceInformation);
      payload.put("os", osInformation);
      payload.put("network", networkInformation);
      payload.put("permissions", permissionsInformation);
      payload.put("location_ids", new JSONArray(this.geofenceIds));
      return payload.toString();
    } catch (Exception error) {
      return null;
    }
  }

  public void syncUpload() {
    BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(this.context, WebHookType.DEVICE_PING);
    if (webHook == null) return;
    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), this.toJSON());
    Request.Builder requestBuild = new Request.Builder();
    requestBuild.url(webHook.getUrl());
    requestBuild.headers(webHook.getHeaders());
    if (webHook.getWebHookRequest() == WebHookRequest.POST) {
      requestBuild.post(requestBody);
    }
    if (webHook.getWebHookRequest() == WebHookRequest.PATCH) {
      requestBuild.patch(requestBody);
    }
    if (webHook.getWebHookRequest() == WebHookRequest.DELETE) {
      requestBuild.delete(requestBody);
    }
    if (webHook.getWebHookRequest() == WebHookRequest.PUT) {
      requestBuild.put(requestBody);
    }
    Request request = requestBuild.build();
    OkHttpClient client = BackgroundGeofenceUtil.getHttpClient(webHook);
    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        e.printStackTrace();
      }
      @Override
      public void onResponse(Call call, Response response) throws IOException {
        response.close();
      }
    });
  }
}
