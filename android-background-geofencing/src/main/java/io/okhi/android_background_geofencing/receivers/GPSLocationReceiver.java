package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import io.okhi.android_background_geofencing.models.NotificationStatusUpdate;
import io.okhi.android_background_geofencing.singletons.LocationSingleton;

public class GPSLocationReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {

        LocationSingleton ls = LocationSingleton.getInstance();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        NotificationStatusUpdate notificationStatusUpdate = new NotificationStatusUpdate(context);

        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
                if(!ls.IS_ERROR_DISPLAYED ){
                    notificationStatusUpdate.gpsLocationTurnedOff();
                }else{
                    Log.d("GPSLocationReceiver", "Already notified");
                }
                ls.IS_ERROR_DISPLAYED = true;
            }
            else{
                if(ls.IS_ERROR_DISPLAYED){
                    ls.IS_ERROR_DISPLAYED = false;
                    notificationStatusUpdate.resetNotification();
                }
            }
        }
    }
}