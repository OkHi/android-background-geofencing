package io.okhi.android_background_geofencing.models;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingWorkPolicy;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceTransitionUploadWorker;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

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
    private long locationDate;
    private HashMap<String, Double> geoPoint;
    private String uuid = UUID.randomUUID().toString();
    private static String TAG = "GeofenceTransition";

    BackgroundGeofenceTransition() {
    }

    private BackgroundGeofenceTransition(Builder builder) {
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
        locationDate = builder.locationDate;
    }

    public void save(Context context) {
        BackgroundGeofencingDB.saveGeofenceTransitionEvent(this, context);
    }

    public static class Builder {
        private ArrayList<String> ids = new ArrayList<>();
        private long transitionDate;
        private String geoPointProvider;
        private double lat;
        private double lon;
        private double gpsAccuracy;
        private String transitionEvent;
        private String geoPointSource;
        private String deviceOSName = "android";
        private String deviceOSVersion = Build.VERSION.RELEASE;
        private String deviceManufacturer = Build.MANUFACTURER;
        private String deviceModel = Build.MODEL;
        private final HashMap<Integer, String> GeofenceTransitionEventNameMap = generateGeofenceTransitionHashMap();
        private long locationDate;

        private HashMap<Integer, String> generateGeofenceTransitionHashMap() {
            HashMap<Integer, String> map = new HashMap<>();
            map.put(Geofence.GEOFENCE_TRANSITION_EXIT, "exit");
            map.put(Geofence.GEOFENCE_TRANSITION_DWELL, "dwell");
            map.put(Geofence.GEOFENCE_TRANSITION_ENTER, "enter");
            return map;
        }

        public Builder(GeofencingEvent geofencingEvent) {
            // get ids
            for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
                ids.add(geofence.getRequestId());
            }
            // triggering location
            Location location = geofencingEvent.getTriggeringLocation();
            // get transition date
            locationDate = location.getTime();
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
        }

        Builder(ArrayList<String> ids) {
            this.ids = ids;
        }

        Builder setTransitionDate(long transitionDate) {
            this.transitionDate = transitionDate;
            return this;
        }

        Builder setLat(double lat) {
            this.lat = lat;
            return this;
        }

        Builder setLon(double lon) {
            this.lon = lon;
            return this;
        }

        Builder setGpsAccuracy(float gpsAccuracy) {
            this.gpsAccuracy = gpsAccuracy;
            return this;
        }

        Builder setTransitionEvent(@NonNull String transitionEvent) {
            this.transitionEvent = transitionEvent;
            return this;
        }

        Builder setGeoPointSource(@NonNull String geoPointSource) {
            this.geoPointSource = geoPointSource;
            return this;
        }

        Builder setGeoPointProvider(@NonNull String geoPointProvider) {
            this.geoPointProvider = geoPointProvider;
            return this;
        }

        Builder setLocationDate(long date) {
            this.locationDate = date;
            return this;
        }

        public BackgroundGeofenceTransition build() {
            // TODO: validate if all necessary fields aren't null
            transitionDate = System.currentTimeMillis();
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
        transit.put("location_date", locationDate);
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
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), payload.toString());
        Request request = new Request.Builder()
                .url(webHook.getUrl())
                .headers(webHook.getHeaders())
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return true;
        } else {
            response.close();
            return false;
        }
    }

    private OkHttpClient getHttpClient(BackgroundGeofencingWebHook webHook) {
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                .supportsTlsExtensions(true)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,
                        CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                        CipherSuite.TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA)
                .build();
        return new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(spec))
                .connectTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS).build();
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

    public String getUUID() {
        return uuid;
    }

    public static void scheduleGeofenceTransitionUploadWork(Context context, int duration, TimeUnit unit) {
        schedule(context, duration, unit);
    }

    private static void schedule(Context context, long duration, TimeUnit unit) {
        OneTimeWorkRequest geofenceTransitionUploadWorkRequest = new OneTimeWorkRequest.Builder(BackgroundGeofenceTransitionUploadWorker.class)
                .setConstraints(Constant.GEOFENCE_WORK_MANAGER_CONSTRAINTS)
                .addTag(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_TAG)
                .setInitialDelay(duration, unit)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY,
                        Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY_TIME_UNIT
                )
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_NAME, ExistingWorkPolicy.REPLACE, geofenceTransitionUploadWorkRequest);
    }

    public static void scheduleGeofenceTransitionUploadWork(Context context) {
        schedule(context, Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY, Constant.GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY_TIME_UNIT);
    }

    public static ArrayList<BackgroundGeofenceTransition> generateTransitions(String geoPointSource, Location location, ArrayList<BackgroundGeofence> geofences, boolean withDwell, Context context) {
        ArrayList<String> enterIds = new ArrayList<>();
        ArrayList<String> exitIds = new ArrayList<>();
        ArrayList<BackgroundGeofenceTransition> transitions = new ArrayList<>();
        for (BackgroundGeofence geofence : geofences) {
            if (isEnter(location, geofence)) {
                enterIds.add(geofence.getId());
            } else {
                exitIds.add(geofence.getId());
            }
        }
        if (!enterIds.isEmpty()) {
            BackgroundGeofenceTransition enterTransition = new Builder(enterIds)
                    .setLocationDate(location.getTime())
                    .setGeoPointProvider(location.getProvider())
                    .setLat(location.getLatitude())
                    .setLon(location.getLongitude())
                    .setGpsAccuracy(location.getAccuracy())
                    .setTransitionEvent("enter")
                    .setGeoPointSource(geoPointSource)
                    .build();
            transitions.add(enterTransition);
        }
        if (!exitIds.isEmpty()) {
            BackgroundGeofenceTransition enterTransition = new Builder(exitIds)
                    .setLocationDate(location.getTime())
                    .setGeoPointProvider(location.getProvider())
                    .setLat(location.getLatitude())
                    .setLon(location.getLongitude())
                    .setGpsAccuracy(location.getAccuracy())
                    .setTransitionEvent("exit")
                    .setGeoPointSource(geoPointSource)
                    .build();
            transitions.add(enterTransition);
        }
        return transitions;
    }

    private static void removeGeofenceEnterTimestamp(BackgroundGeofence geofence, Context context) {
        BackgroundGeofencingDB.removeGeofenceEnterTimestamp(geofence, context);
    }

    private static void saveGeofenceEnterTimestamp(BackgroundGeofence geofence, Context context) {
        BackgroundGeofencingDB.saveGeofenceEnterTimestamp(geofence, context);
    }

    private static boolean isEnter(Location location, BackgroundGeofence geofence) {
        double distance = distance(location.getLatitude(), geofence.getLat(), location.getLongitude(), geofence.getLng(), 0.0, 0.0);
        return distance < geofence.getRadius();
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @returns Distance in Meters
     */
    private static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }
}
