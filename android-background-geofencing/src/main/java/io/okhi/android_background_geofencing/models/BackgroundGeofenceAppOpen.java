package io.okhi.android_background_geofencing.models;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;

public class BackgroundGeofenceAppOpen {
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

    public static void transmitAppOpenEvent (Context context, Location location, BackgroundGeofencingWebHook webHook, ArrayList<BackgroundGeofence> geofences) {
        for (BackgroundGeofence geofence: geofences) {
            ArrayList<String> ids = new ArrayList<>();
            ids.add(geofence.getId());
            BackgroundGeofenceTransition transition = new BackgroundGeofenceTransition.Builder(ids)
                .setLocationDate(location.getTime())
                .setGeoPointProvider(location.getProvider())
                .setLat(location.getLatitude())
                .setLon(location.getLongitude())
                .setGpsAccuracy(location.getAccuracy())
                .setTransitionEvent(BackgroundGeofenceUtil.isEnter(location,geofence) ? "enter" : "exit")
                .setGeoPointSource("appOpen")
                .build();
            try {
                Boolean result = transition.syncUpload(context, webHook);
                if (!result) {
                    transition.save(context);
                }
            } catch (Exception e) {
                transition.save(context);
            }
        }
    }
}
