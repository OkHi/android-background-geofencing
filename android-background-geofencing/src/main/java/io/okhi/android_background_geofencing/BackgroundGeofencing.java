package io.okhi.android_background_geofencing;


import android.content.Context;

import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;

public class BackgroundGeofencing {
    public static void init(Context context) {
        BackgroundGeofence.scheduleGeofenceRestartWork(context, 10, TimeUnit.SECONDS);
        BackgroundGeofenceTransition.scheduleGeofenceTransitionUploadWork(context, 10, TimeUnit.SECONDS);
    }
}
