package io.okhi.android_background_geofencing.models;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import io.okhi.android_background_geofencing.singletons.LocationSingleton;

public class NotificationStatusUpdate {
    NotificationManager mNotificationManager;
    Context context;
    Intent myIntent;
    String title;
    String text;

    LocationSingleton ls = LocationSingleton.getInstance();

    public NotificationStatusUpdate(Context context){
        this.context = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    // Toggle for location on or off
    public void gpsLocationTurnedOff(){
        myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        title = "Turn on GPS";
        text = "Please turn on GPS to continue with the verification";
        BackgroundGeofencingNotification notification = backgroundGeofencingNotification();
        mNotificationManager.notify(Constant.PERSISTENT_NOTIFICATION_ID, notification.getNotification(context, true, false));
        ls.IS_NOTIFICATION_DISPLAYED = true;
    }

    // Request for precise location
    public void requestForPreciseLocation(){
        myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        title = "Enable Precise Location";
        text = "Precise Location permission required to verify your address";
        BackgroundGeofencingNotification notification = backgroundGeofencingNotification();
        mNotificationManager.notify(Constant.PERSISTENT_NOTIFICATION_ID, notification.getNotification(context, true, false));
        ls.IS_NOTIFICATION_DISPLAYED = true;
    }

    // Request for always location
    public void requestForAlwaysLocation(){
        myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        title = "Please allow All the time Permission";
        text = "Allow Always permission to get verified swiftly";
        BackgroundGeofencingNotification notification = backgroundGeofencingNotification();

        mNotificationManager.notify(Constant.PERSISTENT_NOTIFICATION_ID, notification.getNotification(context, true, false));
        ls.IS_NOTIFICATION_DISPLAYED = true;
    }

    // Request for location permission
    public void requestForLocationPermission(){
        myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        title = "Enable Location permission";
        text = "Location permission required to verify your address";
        BackgroundGeofencingNotification notification = backgroundGeofencingNotification();
        mNotificationManager.notify(Constant.PERSISTENT_NOTIFICATION_ID, notification.getNotification(context, true, false));
        ls.IS_NOTIFICATION_DISPLAYED = true;
    }

    // Reset notification to original state
    public void resetNotification(){
        String packageName = context.getPackageName();
        myIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        title = "Yooooooo";
        text = "Don't mind us";
        BackgroundGeofencingNotification notification = backgroundGeofencingNotification();
        mNotificationManager.notify(Constant.PERSISTENT_NOTIFICATION_ID, notification.getNotification(context, false, false));
        ls.IS_NOTIFICATION_DISPLAYED = true;
    }

    private BackgroundGeofencingNotification backgroundGeofencingNotification(){
        int importance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            importance = NotificationManager.IMPORTANCE_HIGH;
        } else {
            importance = 3;
        }

        return new BackgroundGeofencingNotification(
                title,
                text,
                Constant.PERSISTENT_NOTIFICATION_CHANNEL_ID,
                "OkHi Channel",
                "My channel description",
                importance,
                Constant.PERSISTENT_NOTIFICATION_ID,
                456,
                myIntent
        );

    }
}
