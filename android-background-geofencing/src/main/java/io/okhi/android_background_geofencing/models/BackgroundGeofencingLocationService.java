package io.okhi.android_background_geofencing.models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;

public class BackgroundGeofencingLocationService {

    private Context context;
    private Activity activity;
    private RequestHandler requestHandler;
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest = buildLocationSettingsRequest();
    private BackgroundGeofencingException exception = new BackgroundGeofencingException(BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE, "Location services are currently unavailable");

    public BackgroundGeofencingLocationService(Activity activity) {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.settingsClient = LocationServices.getSettingsClient(context);
    }

    public static boolean isLocationServicesEnabled(Context context) {
        int locationMode = 0;
        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    public static void openLocationServicesSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, Constant.OPEN_LOCATION_SERVICES_SETTINGS_REQUEST_CODE, new Bundle());
    }

    private LocationSettingsRequest buildLocationSettingsRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        return builder.build();
    }

    private void onLocationSettingsResponse(Task<LocationSettingsResponse> task) {
        try {
            LocationSettingsResponse response = task.getResult(ApiException.class);
        } catch (ApiException e) {
            switch (e.getStatusCode()) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(activity, Constant.ENABLE_LOCATION_SERVICES_REQUEST_CODE);
                    } catch (Exception error) {
                        requestHandler.onError(exception);
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void requestEnableLocationServices(RequestHandler handler) {
        if (isLocationServicesEnabled(context)) {
            handler.onSuccess();
            return;
        }
        this.requestHandler = handler;
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                onLocationSettingsResponse(task);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Handler handler = new Handler();
        if (requestCode == Constant.ENABLE_LOCATION_SERVICES_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        requestHandler.onSuccess();
                    }
                }, Constant.SERVICE_WAIT_DELAY);
            } else {
                requestHandler.onError(exception);
            }
        }
    }

    public static void getCurrentLocation(Context context, final ResultHandler<Location> handler) {
        boolean isLocationPermissionGranted = BackgroundGeofencingPermissionService.isLocationPermissionGranted(context);
        final BackgroundGeofencingException exception = new BackgroundGeofencingException(BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE, "Last location is unavailable");
        if (!isLocationPermissionGranted) {
            handler.onError(exception);
        } else {
            final FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        handler.onError(exception);
                        return;
                    }
                    handler.onSuccess(locationResult.getLastLocation());
                    client.removeLocationUpdates(this);
                }
            };
            client.requestLocationUpdates(getLocationRequest(), locationCallback, Looper.getMainLooper());
        }
    }

    private static LocationRequest getLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setExpirationDuration(Constant.LOCATION_REQUEST_EXPIRATION_DURATION);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }
}
