package io.okhi.android_background_geofencing.models;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;
import java.util.HashMap;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_core.OkHi;

public class BackgroundGeofenceAppOpen {

    private static HashMap<String, BackgroundGeofenceTransition> transitionTracker = new HashMap<>();

    public BackgroundGeofenceAppOpen () {}

    public static void transmitAppOpenEvent (final Context context, final BackgroundGeofencingWebHook webHook, BackgroundGeofence geofence) {
        final ArrayList<BackgroundGeofence> geofences = new ArrayList<>();
        geofences.add(geofence);
        new BackgroundGeofencingLocationService().fetchCurrentLocation(context, new ResultHandler<Location>() {
            @Override
            public void onSuccess(Location result) {
                transmitAppOpenEvent(context, result, webHook, geofences);
            }
            @Override
            public void onError(BackgroundGeofencingException exception) {
                exception.printStackTrace();
            }
        });
    }

    public static void transmitAppOpenEvent (final Context context, final BackgroundGeofencingWebHook webHook) {
        if (!BackgroundGeofenceUtil.isAppOnForeground(context)) return;
        new BackgroundGeofencingLocationService().fetchCurrentLocation(context, new ResultHandler<Location>() {
            @Override
            public void onSuccess(Location result) {
                ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getGeofences(context, BackgroundGeofenceSource.APP_OPEN);
                transmitAppOpenEvent(context, result, webHook, geofences);
            }
            @Override
            public void onError(BackgroundGeofencingException exception) {
                exception.printStackTrace();
            }
        });
    }

    private static void transmitAppOpenEvent (final Context context, Location location, BackgroundGeofencingWebHook webHook, ArrayList<BackgroundGeofence> geofences) {
        for (BackgroundGeofence geofence: geofences) {
            ArrayList<String> ids = new ArrayList<>();
            ids.add(geofence.getId());
            final BackgroundGeofenceTransition transition = new BackgroundGeofenceTransition.Builder(ids)
                .setLocationDate(location.getTime())
                .setGeoPointProvider(location.getProvider())
                .setLat(location.getLatitude())
                .setLon(location.getLongitude())
                .setGpsAccuracy(location.getAccuracy())
                .setTransitionEvent(BackgroundGeofenceUtil.isEnter(location,geofence) ? "enter" : "exit")
                .setGeoPointSource("appOpen")
                .setInstalledAppsList(OkHi.getInstalledApps(context))
                .build();
            if (isWithinTimeThreshold(transition)) {
                transition.asyncUpload(context, webHook, new ResultHandler<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {

                    }
                    @Override
                    public void onError(BackgroundGeofencingException exception) {
                        transition.save(context);
                    }
                });
            }
        }
    }

    // TODO: refactor this to own class
    private static boolean isWithinTimeThreshold(BackgroundGeofenceTransition transition) {
        if (transitionTracker.containsKey(transition.getGeoPointSource())) {
            BackgroundGeofenceTransition lastTransition = transitionTracker.get(transition.getGeoPointSource());
            if (lastTransition.getStringIds().equals(transition.getStringIds()) && lastTransition.getTransitionEvent().equals(transition.getTransitionEvent()) && transition.getTransitionDate() - lastTransition.getTransitionDate() < 60000) {
                return false;
            }
        }
        transitionTracker.put(transition.getGeoPointSource(), transition);
        return true;
    }
}
