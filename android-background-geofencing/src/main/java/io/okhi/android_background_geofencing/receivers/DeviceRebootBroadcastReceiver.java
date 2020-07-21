package io.okhi.android_background_geofencing.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceRebootBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "DeviceRebootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Device reboot detected");
        // TODO: for devices that receive this, attempt to restart all geofences
    }
}
