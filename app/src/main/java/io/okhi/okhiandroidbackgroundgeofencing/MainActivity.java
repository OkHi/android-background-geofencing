package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
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
        BackgroundGeofencing.init(this);
        final Button button = findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permissionService.requestLocationPermission("We need permission", "Pretty please", new RequestHandler() {
                    @Override
                    public void onSuccess() {
                        startGeofence();
                    }

                    @Override
                    public void onError(BackgroundGeofencingException e) {
                    }
                });
            }
        });
        permissionService = new BackgroundGeofencingPermissionService(this);
        locationService = new BackgroundGeofencingLocationService(this);
        playService = new BackgroundGeofencingPlayService(this);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        JSONObject meta = new JSONObject();
        try {
            meta.put("meta", "critic");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webHook = new BackgroundGeofencingWebHook("https://3eddcfcf8f83.ngrok.io/transition", 10000, headers, meta);
        webHook.save(this);
    }

    private void startGeofence() {
        BackgroundGeofence backgroundGeofence = new BackgroundGeofence.BackgroundGeofenceBuilder("kianohome", -1.3146948, 36.8359575)
                .setNotificationResponsiveness(300000)
                .setLoiteringDelay(60000)
                .build();
        backgroundGeofence.start(getApplicationContext(), new RequestHandler() {
            @Override
            public void onSuccess() {
                Log.v("KIANO", "🕺");
            }

            @Override
            public void onError(BackgroundGeofencingException e) {
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
