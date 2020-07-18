package io.okhi.android_background_geofencing;

import android.content.Context;
import android.widget.Toast;

public class AndroidBackgroundGeofencing {
    public static void ping (Context context) {
        Toast.makeText(context, "PONG", Toast.LENGTH_LONG).show();
    }
}
