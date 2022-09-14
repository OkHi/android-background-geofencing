package io.okhi.android_background_geofencing.interfaces;

import android.webkit.JavascriptInterface;

import io.okhi.android_background_geofencing.activity.OkHiWebViewActivity;

public class WebAppInterface {
    OkHiWebViewActivity mContext;
    public WebAppInterface(OkHiWebViewActivity c) {
        mContext = c;
    }

    @JavascriptInterface
    public void receiveMessage(String results) {
        mContext.receiveMessage(results);
    }
}

