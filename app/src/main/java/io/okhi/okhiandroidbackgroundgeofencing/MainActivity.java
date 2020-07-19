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

public class MainActivity extends AppCompatActivity {

    BackgroundGeofencingPermissionService permissionService;
    BackgroundGeofencingLocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!BackgroundGeofencingLocationService.isLocationServicesEnabled(getApplicationContext())) {
            locationService = new BackgroundGeofencingLocationService(this);
            locationService.requestEnableLocationServices(new RequestHandler() {
                @Override
                public void onSuccess() {
                    Log.v("Kiano", "Services enabled");
                }

                @Override
                public void onError() {
                    Log.v("Kiano", "Services disabled");
                }
            });
        }

        permissionService = new BackgroundGeofencingPermissionService( this);
        permissionService.requestLocationPermission("Permission services required", "We need it pretty please?", new RequestHandler() {
            @Override
            public void onSuccess() {
                Log.v("Kiano", "Permission granted");
            }

            @Override
            public void onError() {
                Log.v("Kiano", "Permission denied");
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
    }
}
