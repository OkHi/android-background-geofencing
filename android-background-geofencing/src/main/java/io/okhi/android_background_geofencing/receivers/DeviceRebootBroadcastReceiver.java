package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceRebootBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "DeviceReboot";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Device restart detected");
    }
}
