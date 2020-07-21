package io.okhi.android_background_geofencing.models;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackgroundGeofenceTransition implements Serializable {

    private ArrayList<String> ids;
    private long transitionDate;
    private String geoPointProvider;
    private double lat;
    private double lon;
    private double gpsAccuracy;
    private String transitionEvent;
    private String geoPointSource;
    private String deviceOSName;
    private String deviceOSVersion;
    private String deviceManufacturer;
    private String deviceModel;
    private HashMap<String, Double> geoPoint;

    private static String TAG = "GeofenceTransition";

    BackgroundGeofenceTransition() {}

    private BackgroundGeofenceTransition(BackgroundGeofenceTransitionBuilder builder) {
        ids = builder.ids;
        transitionDate = builder.transitionDate;
        geoPointProvider = builder.geoPointProvider;
        lat = builder.lat;
        lon = builder.lon;
        geoPoint = new HashMap<>();
        geoPoint.put("lat", lat);
        geoPoint.put("lon", lon);
        gpsAccuracy = builder.gpsAccuracy;
        transitionEvent = builder.transitionEvent;
        geoPointSource = builder.geoPointSource;
        deviceOSName = builder.deviceOSName;
        deviceOSVersion = builder.deviceOSVersion;
        deviceManufacturer = builder.deviceManufacturer;
        deviceModel = builder.deviceModel;
    }

    public void save(Context context) {
        BackgroundGeofencingDB.saveGeofenceTransitionEvent(this, context);
    }

    public static class BackgroundGeofenceTransitionBuilder {
        private ArrayList<String> ids = new ArrayList<>();
        private long transitionDate;
        private String geoPointProvider;
        private double lat;
        private double lon;
        private double gpsAccuracy;
        private String transitionEvent;
        private String geoPointSource;
        private String deviceOSName;
        private String deviceOSVersion;
        private String deviceManufacturer;
        private String deviceModel;
        private final HashMap<Integer, String> GeofenceTransitionEventNameMap = generateGeofenceTransitionHashMap();
        private HashMap<Integer, String> generateGeofenceTransitionHashMap() {
            HashMap<Integer, String> map = new HashMap<>();
            map.put(Geofence.GEOFENCE_TRANSITION_EXIT, "exit");
            map.put(Geofence.GEOFENCE_TRANSITION_DWELL, "dwell");
            map.put(Geofence.GEOFENCE_TRANSITION_ENTER, "enter");
            return map;
        }

        public BackgroundGeofenceTransitionBuilder(GeofencingEvent geofencingEvent) {
            // get ids
            for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                ids.add(geofence.getRequestId());
            }
            // triggering location
            Location location = geofencingEvent.getTriggeringLocation();
            // get transition date
            transitionDate = location.getTime();
            // get gps provider
            geoPointProvider = location.getProvider();
            // get actual geopoint
            lat = location.getLatitude();
            lon = location.getLongitude();
            // get accuracy
            gpsAccuracy = location.getAccuracy();
            // get transition event name
            transitionEvent = GeofenceTransitionEventNameMap.get(geofencingEvent.getGeofenceTransition());
            // set source as geofence
            geoPointSource = "geofence";
            // get device information
            deviceOSName = "android";
            deviceOSVersion = Build.VERSION.RELEASE;
            deviceManufacturer = Build.MANUFACTURER;
            deviceModel = Build.MODEL;
        }

        public BackgroundGeofenceTransition build(){
            return new BackgroundGeofenceTransition(this);
        }
    }

    public String toJSONString() throws JSONException {
        return toJSONObject().toString();
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject payload = new JSONObject();
        JSONArray transits = new JSONArray();
        JSONObject transit = new JSONObject();

        // build transit
        transit.put("ids", new JSONArray(ids));
        transit.put("transition_date", transitionDate);
        transit.put("geopoint_provider", geoPointProvider);
        transit.put("geo_point", new JSONObject(geoPoint));
        transit.put("gps_accuracy", gpsAccuracy);
        transit.put("transition_event", transitionEvent);
        transit.put("geo_point_source", geoPointSource);
        transit.put("device_os_name", deviceOSName);
        transit.put("device_os_version", deviceOSVersion);
        transit.put("device_manufacturer", deviceManufacturer);
        transit.put("device_model", deviceModel);

        // build transits
        transits.put(transit);

        // build payload
        payload.put("transits", transits);

        return payload;
    }

    public boolean syncUpload(BackgroundGeofencingWebHook webHook) throws JSONException, IOException {
        JSONObject meta = webHook.getMeta();
        OkHttpClient client = getHttpClient(webHook);
        JSONObject payload = toJSONObject();
        if (meta != null) {
            payload.put("meta", meta);
        }
        RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(webHook.getUrl())
                .headers(webHook.getHeaders())
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        return response.isSuccessful();
    }

    private OkHttpClient getHttpClient(BackgroundGeofencingWebHook webHook) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS).build();
        return client;
    }

    public String getTransitionEvent() {
        return transitionEvent;
    }

    public String getGeoPointSource() {
        return geoPointSource;
    }

    public String getGeoPointProvider() {
        return geoPointProvider;
    }

    public String getDeviceOSVersion() {
        return deviceOSVersion;
    }

    public String getDeviceOSName() {
        return deviceOSName;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public long getTransitionDate() {
        return transitionDate;
    }

    public HashMap<String, Double> getGeoPoint() {
        return geoPoint;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public ArrayList<String> getIds() {
        return ids;
    }

    public double getGpsAccuracy() {
        return gpsAccuracy;
    }

    public static void scheduleGeofenceTransitionUploadWork(GeofencingEvent geofencingEvent, Context context) {
        BackgroundGeofenceTransition transition = new BackgroundGeofenceTransition.BackgroundGeofenceTransitionBuilder(geofencingEvent).build();
        Log.v(TAG, "Received a " + transition.getTransitionEvent() + "geofence event");
        transition.save(context);
        OneTimeWorkRequest geofenceTransitionUploadWorkRequest = new OneTimeWorkRequest.Builder(BackgroundGeofenceTransitionUploadWorker.class)
                .setConstraints(Constant.GEOFENCE_WORK_MANAGER_CONSTRAINTS)
                .addTag(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_TAG)
                .setInitialDelay(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY, Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY_TIME_UNIT)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY_TIME_UNIT
                )
                .build();
        WorkManager.getInstance(context).enqueue(geofenceTransitionUploadWorkRequest);
        Log.v(TAG, "Geofence transition upload enqueued successfully");
    }
}
