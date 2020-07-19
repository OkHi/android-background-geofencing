package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingPermissionService;

public class MainActivity extends AppCompatActivity {

    BackgroundGeofencingPermissionService permissionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionService = new BackgroundGeofencingPermissionService(MainActivity.this, this, new RequestHandler() {
            @Override
            public void onSuccess() {
                Log.v("Kiano", "Permission granted");
            }

            @Override
            public void onError() {
                Log.v("Kiano", "Permission denied");
            }
        });
        permissionService.requestLocationPermission("Permission services required", "We need it pretty please?");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionService.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
