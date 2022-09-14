package io.okhi.android_background_geofencing.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import io.okhi.android_background_geofencing.R;
import io.okhi.android_background_geofencing.interfaces.WebAppInterface;

public class OkHiWebViewActivity extends AppCompatActivity {

    private WebView webView;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = findViewById(R.id.webview);
        context = this;
        Bundle bundle = getIntent().getExtras();
        processBundle(bundle);
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(){
        webView.setWebViewClient(new OkHiWebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebContentsDebuggingEnabled(false);
        webView.addJavascriptInterface(new WebAppInterface(OkHiWebViewActivity.this), "Android");
        webView.loadUrl("https://google.com");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
    }

    private void processBundle(Bundle bundle){
        try {
            // Show status page based on bundle passed
            String locationPermissionLevel = bundle.getString("locationPermissionLevel", "denied");
            boolean isLocationServicesEnabled = bundle.getBoolean("locationServicesAvailable", false);
        }
        catch (Exception e){
            finish();
        }
    }

    private static class OkHiWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // TODO Auto-generated method stub
            super.onPageStarted(view, url, favicon);
            view.evaluateJavascript("window.console.log = function () {};", null);
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

        }
    }

    public void toggleGPS() {

        Intent locationServicesSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(locationServicesSettingsIntent);
    }
}
