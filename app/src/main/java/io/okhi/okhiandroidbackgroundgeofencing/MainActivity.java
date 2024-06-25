package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
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
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.WebHookRequest;
import io.okhi.android_background_geofencing.models.WebHookType;
import io.okhi.android_core.OkHi;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkPreference;

public class MainActivity extends AppCompatActivity {

    OkHi okHi;
    Boolean withBackground = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        okHi = new OkHi(this);

        BackgroundGeofencingNotification notification = new BackgroundGeofencingNotification(
                "Yooooooo",
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
                if(withBackground){
                    okHi.requestBackgroundLocationPermission("We need permission", "Pretty please", new OkHiRequestHandler<Boolean>() {
                        @Override
                        public void onResult(Boolean result) {
                            if (result) startGeofence();
                        }

                        @Override
                        public void onError(OkHiException e) {
                        }
                    });
                } else {
                    startGeofence();
                }
            }
        });
        final Button local_notification = findViewById(R.id.local_notification);
        local_notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackgroundGeofencingNotification notification = new BackgroundGeofencingNotification(
                        "Yo Low",
                        "Am locally triggered",
                        "OkHi_Channel_id",
                        "OkHi Channel",
                        "My channel description",
                        NotificationManager.IMPORTANCE_HIGH,
                        456
                );
                /**try {
                    BackgroundGeofencingNotification.launchLocalNotification(notification, Color.RED,MainActivity.this);

                } catch (OkHiException e) {
                    e.printStackTrace();
                } */
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
        BackgroundGeofencingWebHook geofenceWebHook = new BackgroundGeofencingWebHook(
            "https://jsondataserver.okhi.io/data",
            10000,
            headers,
            null,
            WebHookType.GEOFENCE,
            WebHookRequest.POST
        );
        geofenceWebHook.save(this);
        BackgroundGeofencingWebHook deviceMetaWebHook = new BackgroundGeofencingWebHook(
            "https://jsondataserver.okhi.io/data",
            10000,
            headers,
            null,
            WebHookType.DEVICE_PING,
            WebHookRequest.POST
        );
        deviceMetaWebHook.save(this);
        BackgroundGeofencingWebHook stopVerificationWebHook = new BackgroundGeofencingWebHook(
            "https://jsondataserver.okhi.io/data",
            10000,
            headers,
            null,
            WebHookType.STOP,
            WebHookRequest.POST
        );
        stopVerificationWebHook.save(this);
    }

    private void startGeofence() {
        okHi.requestLocationPermission(new OkHiRequestHandler<Boolean>(){
            @Override
            public void onResult(Boolean aBoolean) {
                if(withBackground){
                    okHi.requestBackgroundLocationPermission("Hi", "There", new OkHiRequestHandler<Boolean>() {
                        @Override
                        public void onResult(Boolean result) {
                            createGeofence();
                        }

                        @Override
                        public void onError(OkHiException e) {
                            assert e.getMessage() != null;
                            Log.v("MainActivity", e.getMessage());
                        }
                    });
                } else {
                    createGeofence();
                }
            }

            @Override
            public void onError(OkHiException e) {
                assert e.getMessage() != null;
                Log.v("MainActivity", e.getMessage());
            }
        });

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

//        final BackgroundGeofence[] geofences = {homeGeofence, workGeofence, homeGeofence1, workGeofence2, homeGeofence3, workGeofence4};
        


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

    private void createGeofence(){
        BackgroundGeofence homeGeofence = new BackgroundGeofence.BackgroundGeofenceBuilder("home1", -1.314611, 36.836299)
                .setNotificationResponsiveness(5)
                .setLoiteringDelay(60000)
                .setInitialTriggerTransitionTypes(0)
                .build();
        final BackgroundGeofence[] geofences = {homeGeofence};
        for (BackgroundGeofence geofence: geofences) {
            geofence.start(getApplicationContext(), withBackground, new RequestHandler() {
                @Override
                public void onSuccess() {
                    Log.v("MainActivity", "Work geofence started");
//                            BackgroundGeofencing.startForegroundService(getApplicationContext());
                }

                @Override
                public void onError(BackgroundGeofencingException exception) {
                    exception.printStackTrace();
                    assert exception.getMessage() != null;
                    Log.v("MainActivity", exception.getMessage());
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopGeofence(View view) {
        BackgroundGeofence.stop(getApplicationContext(), "home1", new ResultHandler<String>() {
            @Override
            public void onSuccess(String result) {
                showMessage("Stopped: " + result);
            }

            @Override
            public void onError(BackgroundGeofencingException exception) {
                showMessage("Something went wrong: " + exception.getCode() + "\n" + exception.getMessage());
            }
        });
    }

    public void fetchIds(View v){
        
    }

    public void deleteAddressIds(View v){
//        BackgroundGeofence.delete(this, "home3");
    }

    public void stopService (View v) {
        BackgroundGeofencing.stopForegroundService(getApplicationContext());
    }

    private void showMessage(final String s) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void handleTriggerEvents(View v) {
        BackgroundGeofencing.triggerGeofenceEvents(getApplicationContext(), null, null);
    }
}
