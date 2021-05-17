package io.okhi.android_background_geofencing.models;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.receivers.BackgroundGeofenceBroadcastReceiver;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceRestartWorker;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiCoreUtil;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static io.okhi.android_background_geofencing.models.BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE;

public class BackgroundGeofence implements Serializable {

    private String id;
    private double lat;
    private double lng;
    private float radius;
    private long expiration;
    private long expirationTimestamp;
    private int notificationResponsiveness;
    private int loiteringDelay;
    private int transitionTypes;
    private int initialTriggerTransitionTypes;
    private long registrationTimestamp;
    private boolean isFailing = false;

    public static int TRANSITION_ENTER = Geofence.GEOFENCE_TRANSITION_ENTER;
    public static int TRANSITION_EXIT = Geofence.GEOFENCE_TRANSITION_EXIT;
    public static int TRANSITION_DWELL = Geofence.GEOFENCE_TRANSITION_DWELL;

    public static int INITIAL_TRIGGER_ENTER = GeofencingRequest.INITIAL_TRIGGER_ENTER;
    public static int INITIAL_TRIGGER_EXIT = GeofencingRequest.INITIAL_TRIGGER_EXIT;
    public static int INITIAL_TRIGGER_DWELL = GeofencingRequest.INITIAL_TRIGGER_DWELL;

    private boolean withNativeGeofenceTracking = true;
    private boolean withForegroundPingTracking = true;
    private boolean withForegroundWatchTracking = true;
    private boolean withAppOpenTracking = true;

    BackgroundGeofence() {
    }

    private BackgroundGeofence(BackgroundGeofenceBuilder builder) {
        this.id = builder.id;
        this.lat = builder.lat;
        this.lng = builder.lng;
        this.radius = builder.radius;
        this.expiration = builder.expiration;
        this.expirationTimestamp = builder.expirationTimestamp;
        this.notificationResponsiveness = builder.notificationResponsiveness;
        this.loiteringDelay = builder.loiteringDelay;
        this.registrationTimestamp = System.currentTimeMillis();
        this.transitionTypes = builder.transitionTypes;
        this.initialTriggerTransitionTypes = builder.initialTriggerTransitionTypes;
        this.withNativeGeofenceTracking = builder.withNativeGeofenceTracking;
        this.withForegroundPingTracking = builder.withForegroundPingTracking;
        this.withForegroundWatchTracking = builder.withForegroundWatchTracking;
        this.withAppOpenTracking = builder.withAppOpenTracking;
    }

    public static class BackgroundGeofenceBuilder {
        private final String id;
        private final double lat;
        private final double lng;
        private float radius = Constant.DEFAULT_GEOFENCE_RADIUS;
        private long expirationTimestamp = Constant.DEFAULT_GEOFENCE_EXPIRATION;
        private long expiration = Constant.DEFAULT_GEOFENCE_EXPIRATION;
        private int notificationResponsiveness = Constant.DEFAULT_GEOFENCE_NOTIFICATION_RESPONSIVENESS;
        private int loiteringDelay = Constant.DEFAULT_GEOFENCE_LOITERING_DELAY;
        private boolean registerOnDeviceRestart = Constant.DEFAULT_GEOFENCE_REGISTER_ON_DEVICE_RESTART;
        private int transitionTypes = Constant.DEFAULT_GEOFENCE_TRANSITION_TYPES;
        private int initialTriggerTransitionTypes = Constant.DEFAULT_GEOFENCE_INITIAL_TRIGGER_TRANSITION_TYPES;
        private boolean withNativeGeofenceTracking = true;
        private boolean withForegroundPingTracking = true;
        private boolean withForegroundWatchTracking = true;
        private boolean withAppOpenTracking = true;

        public BackgroundGeofenceBuilder(String id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }

        public BackgroundGeofenceBuilder setRadius(float radius) {
            this.radius = radius;
            return this;
        }

        public BackgroundGeofenceBuilder setExpiration(long expiration) {
            if (expiration > 0) {
                this.expiration = expiration;
                this.expirationTimestamp = System.currentTimeMillis() + expiration;
            }
            return this;
        }

        public BackgroundGeofenceBuilder setNotificationResponsiveness(int notificationResponsiveness) {
            this.notificationResponsiveness = notificationResponsiveness;
            return this;
        }

        public BackgroundGeofenceBuilder setLoiteringDelay(int loiteringDelay) {
            this.loiteringDelay = loiteringDelay;
            return this;
        }

        public BackgroundGeofenceBuilder setConfiguration(boolean registerOnDeviceRestart) {
            this.registerOnDeviceRestart = registerOnDeviceRestart;
            return this;
        }

        public BackgroundGeofenceBuilder setTransitionTypes(int transitionTypes) {
            this.transitionTypes = transitionTypes;
            return this;
        }

        public BackgroundGeofenceBuilder setInitialTriggerTransitionTypes(int initialTriggerTransitionTypes) {
            this.initialTriggerTransitionTypes = initialTriggerTransitionTypes;
            return this;
        }

        public BackgroundGeofenceBuilder setWithNativeGeofenceTracking(boolean withNativeGeofenceTracking) {
            this.withNativeGeofenceTracking = withNativeGeofenceTracking;
            return this;
        }

        public BackgroundGeofenceBuilder setWithForegroundWatchTracking(boolean withForegroundWatchTracking) {
            this.withForegroundWatchTracking = withForegroundWatchTracking;
            return this;
        }

        public BackgroundGeofenceBuilder setWithForegroundPingTracking(boolean withForegroundPingTracking) {
            this.withForegroundPingTracking = withForegroundPingTracking;
            return this;
        }

        public BackgroundGeofenceBuilder setWithAppOpenTracking(boolean withAppOpenTracking) {
            this.withAppOpenTracking = withAppOpenTracking;
            return this;
        }

        public BackgroundGeofence build() {
            return new BackgroundGeofence(this);
        }
    }

    private GeofencingRequest getGeofencingRequest(boolean silently, ArrayList<Geofence> geofenceList) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.addGeofences(geofenceList);
        if (silently) {
            builder.setInitialTrigger(0);
            return builder.build();
        }
        builder.setInitialTrigger(initialTriggerTransitionTypes);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(Context context) {
        Intent intent = new Intent(context, BackgroundGeofenceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void save(Context context) {
        BackgroundGeofencingDB.saveBackgroundGeofence(this, context);
    }

    @SuppressLint("MissingPermission")
    private void start(final boolean silently, final Context context, final RequestHandler requestHandler) {
        boolean isLocationServicesEnabled = BackgroundGeofenceUtil.isLocationServicesEnabled(context);
        boolean isBackgroundLocationPermissionGranted = BackgroundGeofenceUtil.isBackgroundLocationPermissionGranted(context);
        boolean isGooglePlayServicesAvailable = BackgroundGeofenceUtil.isGooglePlayServicesAvailable(context);

        if (!isLocationServicesEnabled) {
            requestHandler.onError(new BackgroundGeofencingException(SERVICE_UNAVAILABLE_CODE, "Location services are unavailable"));
            return;
        }

        if (!isGooglePlayServicesAvailable) {
            requestHandler.onError(new BackgroundGeofencingException(SERVICE_UNAVAILABLE_CODE, "Google play services are unavailable"));
            return;
        }

        if (!isBackgroundLocationPermissionGranted) {
            requestHandler.onError(new BackgroundGeofencingException(BackgroundGeofencingException.PERMISSION_DENIED_CODE, "Location permissions aren't granted"));
            return;
        }

        if (this.isWithNativeGeofenceTracking()) {
            GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
            ArrayList<Geofence> geofenceList = new ArrayList<>();
            Geofence geofence = new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(expiration)
                .setTransitionTypes(transitionTypes)
                .setLoiteringDelay(loiteringDelay)
                .setNotificationResponsiveness(notificationResponsiveness)
                .build();
            geofenceList.add(geofence);
            geofencingClient.addGeofences(getGeofencingRequest(silently, geofenceList), getGeofencePendingIntent(context)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    if (!silently) {
                        save(context);
                    }
                    requestHandler.onSuccess();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    requestHandler.onError(new BackgroundGeofencingException(BackgroundGeofencingException.UNKNOWN_EXCEPTION, e.getMessage()));
                    e.printStackTrace();
                }
            });
        } else {
            save(context);
            requestHandler.onSuccess();
        }

        if (this.isWithAppOpenTracking() && !silently) {
            triggerInitialAppOpen(context);
        }
    }

    public void start(Context context, final RequestHandler requestHandler) {
        start(false, context, requestHandler);
    }

    public void restart(Context context, final RequestHandler requestHandler) {
        start(true, context, requestHandler);
    }

    public String getId() {
        return id;
    }

    public boolean isFailing() {
        return isFailing;
    }

    public static void setIsFailing(GeofencingEvent event, Boolean isFailing, Context context) {
        List<Geofence> geofences = event.getTriggeringGeofences();
        for (Geofence geofence : geofences) {
            setIsFailing(geofence.getRequestId(), isFailing, context);
        }
    }

    public static void setIsFailing(String geofenceId, Boolean isFailing, Context context) {
        BackgroundGeofence geofence = BackgroundGeofencingDB.getBackgroundGeofence(geofenceId, context);
        if (geofence != null) {
            geofence.setIsFailing(isFailing, context);
        }
    }

    public void setIsFailing(Boolean isFailing, Context context) {
        if (isFailing != this.isFailing) {
            this.isFailing = isFailing;
            save(context);
        }
    }

    public static void scheduleGeofenceRestartWork(Context context, long duration, TimeUnit unit) {
        schedule(context, duration, unit);
    }

    public static void scheduleGeofenceRestartWork(Context context) {
        schedule(context, Constant.GEOFENCE_RESTART_WORK_DELAY, Constant.GEOFENCE_RESTART_WORK_DELAY_TIME_UNIT);
    }

    private static void schedule(Context context, long duration, TimeUnit unit) {
        OneTimeWorkRequest failedGeofencesRestartWork = new OneTimeWorkRequest.Builder(BackgroundGeofenceRestartWorker.class)
            .setConstraints(Constant.GEOFENCE_WORK_MANAGER_CONSTRAINTS)
            .addTag(Constant.GEOFENCE_RESTART_WORK_TAG)
            .setInitialDelay(duration, unit)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                Constant.GEOFENCE_RESTART_WORK_BACKOFF_DELAY,
                Constant.GEOFENCE_RESTART_WORK_BACKOFF_DELAY_TIME_UNIT
            )
            .build();
        WorkManager.getInstance(context).enqueueUniqueWork(Constant.GEOFENCE_RESTART_WORK_NAME, ExistingWorkPolicy.REPLACE, failedGeofencesRestartWork);
    }

    public boolean hasExpired() {
        return expirationTimestamp > 0 && System.currentTimeMillis() > expirationTimestamp;
    }

    public static void stop(final Context context, final String id, final ResultHandler<String> handler) {
        BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(context, WebHookType.STOP);
        if (webHook != null) {
            try {
                JSONObject payload = new JSONObject();
                payload.put("state", "stop");
                OkHttpClient client = BackgroundGeofenceUtil.getHttpClient(webHook);
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), payload.toString());
                Request.Builder requestBuild = new Request.Builder();
                requestBuild.url(webHook.getUrl(id));
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
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        handler.onError(new BackgroundGeofencingException(OkHiException.NETWORK_ERROR_CODE, OkHiException.NETWORK_ERROR_MESSAGE));
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            stopGeofences(context, id);
                            handler.onSuccess(id);
                        } else {
                            OkHiException exception = OkHiCoreUtil.generateOkHiException(response);
                            handler.onError(new BackgroundGeofencingException(exception.getCode(), exception.getMessage()));
                        }
                    }
                });
            } catch (Exception e) {
                handler.onError(new BackgroundGeofencingException(BackgroundGeofencingException.UNKNOWN_EXCEPTION, e.getMessage()));
            }
        } else {
            stopGeofences(context, id);
            handler.onSuccess(id);
        }
    }

    private static void stopGeofences(Context context, String id) {
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
        List<String> ids = new ArrayList<>();
        ids.add(id);
        geofencingClient.removeGeofences(ids);
        BackgroundGeofencingDB.removeBackgroundGeofence(id, context);
        BackgroundGeofencingDB.removeGeofenceEnterTimestamp(id, context);
        ArrayList<BackgroundGeofence> foregroundWatchGeofences = BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_WATCH);
        ArrayList<BackgroundGeofence> foregroundPingGeofences = BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_PING);
        if (foregroundWatchGeofences.isEmpty() && foregroundPingGeofences.isEmpty()) {
            BackgroundGeofencing.stopForegroundService(context);
        }
    }

    public static void stop (Context context, BackgroundGeofence geofence) {
        Boolean isAllStop = !geofence.isWithAppOpenTracking() && !geofence.isWithNativeGeofenceTracking() && !geofence.isWithForegroundWatchTracking() && !geofence.isWithForegroundPingTracking();
        if (isAllStop) {
            BackgroundGeofencingDB.removeBackgroundGeofence(geofence.getId(), context);
            BackgroundGeofencingDB.removeGeofenceEnterTimestamp(geofence.getId(), context);
        }
        if (!geofence.isWithAppOpenTracking()) {
            BackgroundGeofencingDB.removeGeofenceEnterTimestamp(geofence.getId(), context);
        }
        if (!geofence.isWithNativeGeofenceTracking()) {
            GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
            List<String> ids = new ArrayList<>();
            ids.add(geofence.getId());
            geofencingClient.removeGeofences(ids);
        }
        ArrayList<BackgroundGeofence> foregroundWatchGeofences = BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_WATCH);
        ArrayList<BackgroundGeofence> foregroundPingGeofences = BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.FOREGROUND_PING);
        if (foregroundWatchGeofences.isEmpty() && foregroundPingGeofences.isEmpty()) {
            BackgroundGeofencing.stopForegroundService(context);
        }
    }

    public float getRadius() {
        return radius;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public int getLoiteringDelay() {
        return loiteringDelay;
    }

    public boolean isWithNativeGeofenceTracking() {
        return withNativeGeofenceTracking;
    }

    public void setWithNativeGeofenceTracking(boolean withNativeGeofenceTracking) {
        this.withNativeGeofenceTracking = withNativeGeofenceTracking;
    }

    public boolean isWithForegroundPingTracking() {
        return withForegroundPingTracking;
    }

    public void setWithForegroundPingTracking(boolean withForegroundPingTracking) {
        this.withForegroundPingTracking = withForegroundPingTracking;
    }

    public boolean isWithForegroundWatchTracking() {
        return withForegroundWatchTracking;
    }

    public void setWithForegroundWatchTracking(boolean withForegroundWatchTracking) {
        this.withForegroundWatchTracking = withForegroundWatchTracking;
    }

    public boolean isWithAppOpenTracking() {
        return withAppOpenTracking;
    }

    public void setWithAppOpenTracking(boolean withAppOpenTracking) {
        this.withAppOpenTracking = withAppOpenTracking;
    }

    private void triggerInitialAppOpen (final Context context) {
        final BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(context);
        if (webHook != null && BackgroundGeofenceUtil.isLocationServicesEnabled(context) && BackgroundGeofenceUtil.isLocationPermissionGranted(context)) {
            final ArrayList<BackgroundGeofence> geofences = new ArrayList<>();
            geofences.add(this);
            BackgroundGeofenceUtil.getCurrentLocation(context, new ResultHandler<Location>() {
                @Override
                public void onSuccess(Location location) {
                    ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofenceTransition.generateTransitions(
                        Constant.APP_OPEN_GEOFENCE_TRANSITION_SOURCE_NAME,
                        location,
                        geofences,
                        false,
                        context
                    );
                    for(final BackgroundGeofenceTransition transition: transitions) {
                        try {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        transition.syncUpload(context, webHook);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                @Override
                public void onError(BackgroundGeofencingException exception) {
                    exception.printStackTrace();
                }
            });
        }
    }
}
