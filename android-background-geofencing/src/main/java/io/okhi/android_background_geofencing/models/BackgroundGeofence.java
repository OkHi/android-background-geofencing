package io.okhi.android_background_geofencing.models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.Serializable;
import java.util.ArrayList;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.receivers.BackgroundGeofenceBroadcastReceiver;

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
    private boolean registerOnDeviceRestart;

    private PendingIntent geofencePendingIntent;
    private boolean failing = false;

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
        this.registerOnDeviceRestart = builder.registerOnDeviceRestart;
        this.transitionTypes = builder.transitionTypes;
        this.initialTriggerTransitionTypes = builder.initialTriggerTransitionTypes;
    }

    public static class BackgroundGeofenceBuilder {
        private String id;
        private double lat;
        private double lng;
        private long expirationTimestamp;
        private float radius = Constant.DEFAULT_GEOFENCE_RADIUS;
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
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, BackgroundGeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
}
