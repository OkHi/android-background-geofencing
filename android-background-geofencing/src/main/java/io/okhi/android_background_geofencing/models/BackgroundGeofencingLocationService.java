package io.okhi.android_background_geofencing.models;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import io.okhi.android_background_geofencing.activity.OkHiWebViewActivity;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;

// TODO: kiano move this to OkHiLocationService class
public class BackgroundGeofencingLocationService {
    private LocationRequest watchLocationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback watchLocationCallback;
    private Location location;
    private ResultHandler<Location> handler;
    private Context context;
    private Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            if (location != null) {
                handleOnLocationSuccess();
            }
        }
    };
    private boolean handlerCalled = false;

    public BackgroundGeofencingLocationService () {}

    private void createLocationRequest() {
        watchLocationRequest = new LocationRequest();
        watchLocationRequest.setInterval(Constant.FOREGROUND_SERVICE_LOCATION_UPDATE_INTERVAL);
        watchLocationRequest.setFastestInterval(Constant.FOREGROUND_SERVICE_LOCATION_UPDATE_INTERVAL / 2);
        watchLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        watchLocationRequest.setSmallestDisplacement(Constant.FOREGROUND_SERVICE_LOCATION_DISPLACEMENT);
    }

    private void startForegroundLocationWatch() {
        createLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        watchLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    handleOnLocationResult(locationResult.getLastLocation());
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(watchLocationRequest, watchLocationCallback, Looper.getMainLooper());
    }

    private void handleOnLocationResult(Location location) {
        this.location = location;
        if (this.handler != null && location.getAccuracy() < 100) {
            handleOnLocationSuccess();
        }
    }

    private void handleOnLocationSuccess () {
        timer.cancel();
        fusedLocationProviderClient.removeLocationUpdates(watchLocationCallback);
        if (!handlerCalled) {
            handlerCalled = true;
            handler.onSuccess(location);
        }
    }

    public void fetchCurrentLocation (Context context, final ResultHandler<Location> handler) {
        if (!OkHiLocationService.isLocationServicesEnabled(context)) {
            handler.onError(new BackgroundGeofencingException(BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE, "Location services are unavailable"));
            return;
        }
        if (BackgroundGeofenceUtil.isAppOnForeground(context) && !OkHiPermissionService.isLocationPermissionGranted(context)) {
            handler.onError(new BackgroundGeofencingException(BackgroundGeofencingException.PERMISSION_DENIED_CODE, "Location permission not granted"));
            return;
        }
        if (!BackgroundGeofenceUtil.isAppOnForeground(context) && !OkHiPermissionService.isBackgroundLocationPermissionGranted(context)) {
            handler.onError(new BackgroundGeofencingException(BackgroundGeofencingException.PERMISSION_DENIED_CODE, "Location permission not granted"));
            return;
        }
        this.context = context;
        this.handler = handler;
        timer.scheduleAtFixedRate(task, 20000, 20000);
        ContextCompat.getMainExecutor(context).execute(new Runnable() {
            @Override
            public void run() {
                startForegroundLocationWatch();
            }
        });
    }

    public static void checkLocationPermissions(Context context){

        HashMap<String, Boolean> locationState = BackgroundGeofenceUtil.locationPermissionState(context);
        boolean ACCESS_COARSE_LOCATION = Boolean.TRUE.equals(locationState.get("ACCESS_COARSE_LOCATION"));
        boolean ACCESS_FINE_LOCATION = Boolean.TRUE.equals(locationState.get("ACCESS_FINE_LOCATION"));
        boolean ACCESS_BACKGROUND_LOCATION = Boolean.TRUE.equals(locationState.get("ACCESS_BACKGROUND_LOCATION"));

        Intent myIntent;
        int color = Color.argb(255, 255, 0, 0);

        myIntent = new Intent(context, OkHiWebViewActivity.class);

        // Background & General location permission
        myIntent.putExtra("locationPermissionLevel", ACCESS_BACKGROUND_LOCATION ? "always" : ACCESS_COARSE_LOCATION ? "whenInUse" : "denied");

        // Precise || estimate location permission
        myIntent.putExtra("locationPrecisionLevel", ACCESS_FINE_LOCATION ? "precise" : "approximate");

        Boolean isNotified = false;
        if(BackgroundGeofencingDB.getPermissionNotified(context) != null){
            isNotified = BackgroundGeofencingDB.getPermissionNotified(context);
        }

        if(isNotified && !locationState.containsValue(false)){
            // Reset only if formerly updated
            BackgroundGeofencingNotification.resetNotification(context);
            BackgroundGeofencingDB.setPermissionNotified(context, false);
        } else if(!isNotified){

            // Location permission Required
            if(!ACCESS_COARSE_LOCATION){
                BackgroundGeofencingNotification.updateNotification(
                        context,
                        "Enable Location permission",
                        "Location permission required to verify your address",
                        color,
                        myIntent
                );
                BackgroundGeofencingDB.setPermissionNotified(context, true);
            } else if(!ACCESS_BACKGROUND_LOCATION){
                // Location Permission Always Required
                BackgroundGeofencingNotification.updateNotification(
                        context,
                        "Please allow \"All The Time\" Permission",
                        "Allow Always permission to get verified swiftly",
                        color,
                        myIntent
                );
                BackgroundGeofencingDB.setPermissionNotified(context, true);
            }else if(!ACCESS_FINE_LOCATION){
                // Location Precise permission Required
                BackgroundGeofencingNotification.updateNotification(
                        context,
                        "Enable Precise Location",
                        "Precise Location permission required to verify your address",
                        color,
                        myIntent
                );
                BackgroundGeofencingDB.setPermissionNotified(context, true);
            }
        }
    }
}
