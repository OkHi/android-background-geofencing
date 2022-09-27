package io.okhi.android_background_geofencing.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.R;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.WebAppInterface;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_core.OkHi;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;
import io.okhi.android_core.models.OkPreference;

public class OkHiWebViewActivity extends AppCompatActivity {
    private WebView webView;
    Context context;
    String webViewLaunchPayload;
    String webViewUrl;
    OkHiPermissionService permissionService;
    OkHiLocationService locationService;
    String LAUNCH_PAYLOAD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        webView = findViewById(R.id.webview);
        context = this;
        permissionService = new OkHiPermissionService(this);
        locationService = new OkHiLocationService(this);
        try {
            LAUNCH_PAYLOAD = fetchStartPayload();
        } catch (OkHiException e) {
            finish();
        }
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
        webView.loadUrl(webViewUrl);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
    }

    private void processBundle(Bundle bundle){
        try {
            ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getAllGeofences(getApplicationContext());
            JSONArray locationIds = new JSONArray();
            for (BackgroundGeofence geofence: geofences) {
                JSONObject location = new JSONObject();
                location.put("id", geofence.getId());
                locationIds.put(location);
            }
            JSONObject launchPayload = new JSONObject(LAUNCH_PAYLOAD);
            launchPayload.put("message", "verification_status");

            JSONObject payload = launchPayload.getJSONObject("payload");
            payload.put("locations", locationIds);

            JSONObject context = payload.getJSONObject("context");
            context.put("locationServicesAvailable", OkHi.isLocationServicesEnabled(getApplicationContext()));

            JSONObject permissions = context.getJSONObject("permissions");
            String locationPermissionLevel = OkHi.isBackgroundLocationPermissionGranted(getApplicationContext()) ? "always" : OkHi.isLocationPermissionGranted(getApplicationContext()) ? "whenInUse" : "denied";
            permissions.put("location", locationPermissionLevel);

            context.put("permissions", permissions);
            payload.put("context", context);
            launchPayload.put("payload", payload);
            webViewUrl = launchPayload.getString("url");
            webViewLaunchPayload = launchPayload.toString().replace("\\", "");
            Log.v("WebviewMe", webViewLaunchPayload);
        }
        catch (Exception e){
            e.printStackTrace();
            finish();
        }
    }

    private class OkHiWebViewClient extends WebViewClient {

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

    public void receiveMessage(String result) {
        try {
            JSONObject transmission = new JSONObject(result);
            String message = transmission.optString("message");
            JSONObject payload = transmission.optJSONObject("payload");
            switch (message) {
                case "request_location_permission":
                    handleRequestLocationPermission(payload);
                    break;
                case "request_enable_location_services":
                    handleRequestEnableLocationServices(payload);
                    break;
                case "app_state":
                    handleLaunch();
                    break;
                default:
                    finish();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void handleRequestEnableLocationServices(JSONObject payload) {
        locationService.requestEnableLocationServices(new OkHiRequestHandler<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (result) {
                    finish();
                }
            }
            @Override
            public void onError(OkHiException exception) {
                exception.printStackTrace();
                finish();
            }
        });
    }

    private void handleRequestLocationPermission(JSONObject payload) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void handleLaunch() {
        if (webViewLaunchPayload != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("javascript:receiveAndroidMessage(" + webViewLaunchPayload + ")", null);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionService.onRequestPermissionsResult(requestCode, permissions, grantResults, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        locationService.onActivityResult(requestCode, resultCode, data);
    }

    private String fetchStartPayload() throws OkHiException {
        String storedPayload = OkPreference.getItem("okcollect-launch-payload", getApplicationContext());
        if (storedPayload != null) {
            return storedPayload;
        }
        throw new OkHiException(OkHiException.UNKNOWN_ERROR_CODE, "Unable to launch");
    }

    public static Boolean canLaunchWebView(Context context) {
        try {
            String storedPayload = OkPreference.getItem("okcollect-launch-payload", context);
            return storedPayload != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isLocationServicesEnabled = OkHi.isLocationServicesEnabled(getApplicationContext());
        boolean isBackgroundLocationPermissionGranted = OkHi.isBackgroundLocationPermissionGranted(getApplicationContext());
        if (isLocationServicesEnabled && isBackgroundLocationPermissionGranted) {
            try {
                BackgroundGeofencing.restartForegroundService(getApplicationContext());
                finish();
            } catch (BackgroundGeofencingException e) {
                e.printStackTrace();
            }
        }
    }
}
