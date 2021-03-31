package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.RequestHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_core.OkHi;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;

public class MainActivity extends AppCompatActivity {

    OkHi okHi;
    BackgroundGeofencingWebHook webHook;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        okHi = new OkHi(this);

        BackgroundGeofencingNotification notification = new BackgroundGeofencingNotification(
                "Hi Kiano",
                "Don't mind us",
                "OkHi_Channel_id",
                "OkHi Channel",
                "My channel description",
                NotificationManager.IMPORTANCE_HIGH,
            123,
            456
        );

        BackgroundGeofencing.init(this, notification);

        final Button button = findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okHi.requestBackgroundLocationPermission("We need permission", "Pretty please", new OkHiRequestHandler<Boolean>() {
                    @Override
                    public void onResult(Boolean result) {
                        Log.v("OKHI", result + "..Hmm");
                        if (result) startGeofence();
                    }

                    @Override
                    public void onError(OkHiException e) {
                    }
                });
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        JSONObject meta = new JSONObject();
        try {
            meta.put("meta", "critic");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webHook = new BackgroundGeofencingWebHook("https://jsondataserver.okhi.io/data", 10000, headers, meta);
        webHook.save(this);
    }

    private void startGeofence() {
        BackgroundGeofence homeGeofence = new BackgroundGeofence.BackgroundGeofenceBuilder("home1", -1.314593 , 36.836299)
                .setNotificationResponsiveness(5)
                .setLoiteringDelay(60000)
                .setInitialTriggerTransitionTypes(0)
                .build();
//        BackgroundGeofence workGeofence = new BackgroundGeofence.BackgroundGeofenceBuilder("work2", -1.313339237582541, 36.842414181487776)
//                .setNotificationResponsiveness(5)
//                .setLoiteringDelay(60000)
//                .setInitialTriggerTransitionTypes(0)
//                .build();
//        BackgroundGeofence homeGeofence1 = new BackgroundGeofence.BackgroundGeofenceBuilder("home3", -1.3148501, 36.8363831)
//                .setNotificationResponsiveness(5)
//                .setLoiteringDelay(60000)
//                .setInitialTriggerTransitionTypes(0)
//                .build();
//        BackgroundGeofence workGeofence2 = new BackgroundGeofence.BackgroundGeofenceBuilder("work4", -1.313339237582541, 36.842414181487776)
//                .setNotificationResponsiveness(5)
//                .setLoiteringDelay(60000)
//                .setInitialTriggerTransitionTypes(0)
//                .build();
//        BackgroundGeofence homeGeofence3 = new BackgroundGeofence.BackgroundGeofenceBuilder("home5", -1.3148501, 36.8363831)
//                .setNotificationResponsiveness(5)
//                .setLoiteringDelay(60000)
//                .setInitialTriggerTransitionTypes(0)
//                .build();
//        BackgroundGeofence workGeofence4 = new BackgroundGeofence.BackgroundGeofenceBuilder("work6", -1.313339237582541, 36.842414181487776)
//                .setNotificationResponsiveness(5)
//                .setLoiteringDelay(60000)
//                .setInitialTriggerTransitionTypes(0)
//                .build();
        final BackgroundGeofence[] geofences = {homeGeofence};
//        final BackgroundGeofence[] geofences = {homeGeofence, workGeofence, homeGeofence1, workGeofence2, homeGeofence3, workGeofence4};
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (BackgroundGeofence geofence: geofences) {
                    geofence.start(getApplicationContext(), new RequestHandler() {
                        @Override
                        public void onSuccess() {
                            Log.v("MainActivity", "Work geofence started");
//                            BackgroundGeofencing.startForegroundService(getApplicationContext());
                        }

                        @Override
                        public void onError(BackgroundGeofencingException exception) {
                            exception.printStackTrace();
                            Log.v("MainActivity", exception.getMessage());
                        }
                    });
                }
            }
        }).start();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                for (BackgroundGeofence geofence: geofences) {
//                    geofence.start(getApplicationContext(), new RequestHandler() {
//                        @Override
//                        public void onSuccess() {
//                            Log.v("MainActivity", "Work geofence started");
//                        }
//
//                        @Override
//                        public void onError(BackgroundGeofencingException exception) {
//
//                        }
//                    });
//                }
//            }
//        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                BackgroundGeofencingDB.getAllGeofences(getApplicationContext());
                Log.v("Thread", "gotten access");
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        okHi.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        okHi.onActivityResult(requestCode, resultCode, data);
    }

    public void isServiceRunning (View view) {
        showMessage("Service running: " + BackgroundGeofencing.isForegroundServiceRunning(getApplicationContext()));
    }

    public void startService (View view) {
        try {
            BackgroundGeofencing.startForegroundService(getApplicationContext());
        } catch (BackgroundGeofencingException e) {
            e.printStackTrace();
        }
    }

    public void stopGeofence(View view) {
        BackgroundGeofence.stop(getApplicationContext(), "home1");
    }

    public void stopService (View v) {
        BackgroundGeofencing.stopForegroundService(getApplicationContext());
    }

    private void showMessage(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
