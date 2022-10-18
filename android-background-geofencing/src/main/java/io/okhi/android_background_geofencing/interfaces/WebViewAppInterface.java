package io.okhi.android_background_geofencing.interfaces;

import android.webkit.JavascriptInterface;

import io.okhi.android_background_geofencing.activities.BackgroundGeofencingWebViewActivity;

public class WebViewAppInterface {
  BackgroundGeofencingWebViewActivity mContext;

  public WebViewAppInterface(BackgroundGeofencingWebViewActivity c) {
    mContext = c;
  }

  @JavascriptInterface
  public void receiveMessage(String results) {
    mContext.receiveMessage(results);
  }
}
