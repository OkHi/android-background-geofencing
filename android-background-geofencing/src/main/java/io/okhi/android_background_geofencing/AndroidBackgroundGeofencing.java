package io.okhi.android_background_geofencing;


import android.content.Context;

import io.okhi.android_background_geofencing.models.LocationService;

public class AndroidBackgroundGeofencing {
    public static boolean isLocationPermissionGranted(Context context) {
        return LocationService.isLocationPermissionGranted(context);
    }

    public static boolean isLocationServicesEnabled(Context context) {
        return LocationService.isLocationServicesEnabled(context);
    }
}
