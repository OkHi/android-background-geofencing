package io.okhi.android_background_geofencing.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import io.okhi.android_background_geofencing.BackgroundGeofencing;

class AppConstants{
    public int GPS_REQUEST = 100;
}

public class WebViewActivity extends AppCompatActivity {

    BackgroundGeofencing backgroundGeofencing;
    WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        backgroundGeofencing = new BackgroundGeofencing(this);
        webView.loadUrl("https://gransono.github.io/okhi_web/oktest.html");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new OkHiWebInterface(this, backgroundGeofencing, webView ), "Android");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppConstants constants = new AppConstants();

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == constants.GPS_REQUEST) {
                Toast.makeText(this, "GPS Turned On", Toast.LENGTH_LONG).show();
            }
        }
    }
}

class OkHiWebInterface {
    Context mContext;
    BackgroundGeofencing mBackgroundGeofencing;
    WebView mWebView;

    OkHiWebInterface(Context c, BackgroundGeofencing backgroundGeofencing, WebView webView) {
        mContext = c;
        mBackgroundGeofencing = backgroundGeofencing;
        mWebView = webView;
    }

    @JavascriptInterface
    public void openTecnoProtectedIntent(String pkgName, String className) {
        mBackgroundGeofencing.requestAppProtection(pkgName, className);
    }

    @JavascriptInterface
    public void reload() {
        mWebView.reload();
    }

    @JavascriptInterface
    public void switchGPS(){
        Toast.makeText(mContext, "GPS Turned On Success", Toast.LENGTH_LONG).show();
//        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//        mContext.startActivity(intent);
        new GpsUtils(mContext).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                // isGPS = isGPSEnable;
                Toast.makeText(mContext, "GPS Turned On Success", Toast.LENGTH_LONG).show();
            }
        });
    }
}

class GpsUtils {

    private Context context;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationManager locationManager;
    private LocationRequest locationRequest;

    AppConstants constants = new AppConstants();

    public GpsUtils(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mSettingsClient = LocationServices.getSettingsClient(context);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000);
        locationRequest.setFastestInterval(2 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        mLocationSettingsRequest = builder.build();
//**************************
        builder.setAlwaysShow(true); //this is the key ingredient
        //**************************
    }

    // method for turn on GPS
    public void turnGPSOn(final onGpsListener onGpsListener) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (onGpsListener != null) {
                onGpsListener.gpsStatus(true);
            }
        } else {
            mSettingsClient
                    .checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener((Activity) context, new OnSuccessListener<LocationSettingsResponse>() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
//  GPS is already enable, callback GPS status through listener
                            if (onGpsListener != null) {
                                onGpsListener.gpsStatus(true);
                            }
                        }
                    })
                    .addOnFailureListener((Activity) context, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    try {
                                        // Show the dialog by calling startResolutionForResult(), and check the
                                        // result in onActivityResult().
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult((Activity) context, constants.GPS_REQUEST);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.i("Location Class", "PendingIntent unable to execute request.");
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    String errorMessage = "Location settings are inadequate, and cannot be " +
                                            "fixed here. Fix in Settings.";
                                    Log.e("Location Class", errorMessage);
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    public interface onGpsListener {
        void gpsStatus(boolean isGPSEnable);
    }
}