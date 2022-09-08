package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.singletons.LocationSingleton;

public class GPSLocationReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {

        LocationSingleton ls = LocationSingleton.getInstance();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
                if(!ls.IS_ERROR_DISPLAYED ){
                    int color = Color.argb(255, 255, 0, 0);
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    BackgroundGeofencingNotification.updatePersistentNotification(context, "Turn on GPS",  "Please turn on GPS to continue with the verification", color, myIntent);;
                }
                ls.IS_ERROR_DISPLAYED = true;
            }
            else{
                if(ls.IS_ERROR_DISPLAYED){
                    ls.IS_ERROR_DISPLAYED = false;
                    BackgroundGeofencingNotification.resetNotification(context);
                }
            }
        }
    }
}
