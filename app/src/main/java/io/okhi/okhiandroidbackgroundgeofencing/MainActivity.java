package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingPermissionService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingPlayService;

public class MainActivity extends AppCompatActivity {

    BackgroundGeofencingPermissionService permissionService;

    BackgroundGeofencingLocationService locationService;

    BackgroundGeofencingPlayService playService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionService = new BackgroundGeofencingPermissionService(this);
        locationService = new BackgroundGeofencingLocationService(this);
        playService = new BackgroundGeofencingPlayService(this);
        if (!BackgroundGeofencingPlayService.isGooglePlayServicesAvailable(getApplicationContext())) {
            playService.requestEnableGooglePlayServices(new RequestHandler() {
                @Override
                public void onSuccess() {
                    Log.v("KIANO", "Play services enabled");
                }

                @Override
                public void onError() {
                    Log.v("KIANO", "Play services disabled");
                }
            });
        }
//        BackgroundGeofencingLocationService.openLocationServicesSettings(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionService.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        locationService.onActivityResult(requestCode, resultCode, data);
        playService.onActivityResult(requestCode, resultCode, data);
    }
}
