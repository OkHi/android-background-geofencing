package io.okhi.android_background_geofencing.interfaces;

import android.webkit.JavascriptInterface;

import io.okhi.android_background_geofencing.activity.OkHiWebActivity;

public class WebAppInterface {
    OkHiWebActivity mContext;
    public WebAppInterface(OkHiWebActivity c) {
        mContext = c;
    }

    @JavascriptInterface
    public void switchGPS() {
        mContext.toggleGPS();
    }
}

