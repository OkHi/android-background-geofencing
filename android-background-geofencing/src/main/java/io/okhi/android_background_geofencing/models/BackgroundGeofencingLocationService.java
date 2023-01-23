package io.okhi.android_background_geofencing.models;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Timer;
import java.util.TimerTask;

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
}
