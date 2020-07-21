package io.okhi.android_background_geofencing.models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.receivers.BackgroundGeofenceBroadcastReceiver;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceRestartWorker;

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

    BackgroundGeofence() {}

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
    }

    public static class BackgroundGeofenceBuilder {
        private String id;
        private double lat;
        private double lng;
        private float radius = Constant.DEFAULT_GEOFENCE_RADIUS;
        private long expirationTimestamp = Constant.DEFAULT_GEOFENCE_EXPIRATION;
        private long expiration = Constant.DEFAULT_GEOFENCE_EXPIRATION;
        private int notificationResponsiveness = Constant.DEFAULT_GEOFENCE_NOTIFICATION_RESPONSIVENESS;
        private int loiteringDelay = Constant.DEFAULT_GEOFENCE_LOITERING_DELAY;
        private boolean registerOnDeviceRestart = Constant.DEFAULT_GEOFENCE_REGISTER_ON_DEVICE_RESTART;
        private int transitionTypes = Constant.DEFAULT_GEOFENCE_TRANSITION_TYPES;
        private int initialTriggerTransitionTypes = Constant.DEFAULT_GEOFENCE_INITIAL_TRIGGER_TRANSITION_TYPES;

        public BackgroundGeofenceBuilder(String id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }

        public BackgroundGeofenceBuilder setRadius(float radius){
            this.radius = radius;
            return this;
        }

        public BackgroundGeofenceBuilder setExpiration(long expiration){
            this.expiration = expiration;
            this.expirationTimestamp = System.currentTimeMillis() + expiration;
            return this;
        }

        public BackgroundGeofenceBuilder setNotificationResponsiveness(int notificationResponsiveness){
            this.notificationResponsiveness = notificationResponsiveness;
            return this;
        }

        public BackgroundGeofenceBuilder setLoiteringDelay(int loiteringDelay){
            this.loiteringDelay = loiteringDelay;
            return this;
        }

        public BackgroundGeofenceBuilder setConfiguration(boolean registerOnDeviceRestart){
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

        public BackgroundGeofence build(){
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
        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private void save(Context context) {
        BackgroundGeofencingDB.saveBackgroundGeofence(this, context);
    }

    private void start(final boolean silently, final Context context, final RequestHandler requestHandler) {
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
                requestHandler.onError();
                e.printStackTrace();
            }
        });
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
        for(Geofence geofence: geofences) {
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
                        BackoffPolicy.EXPONENTIAL,
                        Constant.GEOFENCE_RESTART_WORK_BACKOFF_DELAY,
                        Constant.GEOFENCE_RESTART_WORK_BACKOFF_DELAY_TIME_UNIT
                )
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(Constant.GEOFENCE_RESTART_WORK_NAME, ExistingWorkPolicy.REPLACE, failedGeofencesRestartWork);
    }

    public boolean hasExpired() {
        return expirationTimestamp > 0 && System.currentTimeMillis() > expirationTimestamp;
    }
}