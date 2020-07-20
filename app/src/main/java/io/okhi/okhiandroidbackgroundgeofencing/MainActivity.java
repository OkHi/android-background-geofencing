package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingPermissionService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingPlayService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;

public class MainActivity extends AppCompatActivity {

    BackgroundGeofencingPermissionService permissionService;

    BackgroundGeofencingLocationService locationService;

    BackgroundGeofencingPlayService playService;

    BackgroundGeofencingWebHook webHook;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionService = new BackgroundGeofencingPermissionService(this);
        locationService = new BackgroundGeofencingLocationService(this);
        playService = new BackgroundGeofencingPlayService(this);
        webHook = new BackgroundGeofencingWebHook("https://google.com");
        webHook.save(this);
        permissionService.requestLocationPermission("We need permission", "Pretty please", new RequestHandler() {
            @Override
            public void onSuccess() {
                startGeofence();
            }

            @Override
            public void onError() {

            }
        });
    }

    private void startGeofence() {
        BackgroundGeofence backgroundGeofence = new BackgroundGeofence.BackgroundGeofenceBuilder("kiano", -1.313456, 36.9887).build();
        backgroundGeofence.start(getApplicationContext(), new RequestHandler() {
            @Override
            public void onSuccess() {
                Log.v("KIANO", "🕺");
            }

            @Override
            public void onError() {
                Log.v("KIANO", "😭");
            }
        });
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
