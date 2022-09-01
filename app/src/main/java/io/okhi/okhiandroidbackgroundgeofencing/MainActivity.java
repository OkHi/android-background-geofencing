package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.models.WebHookRequest;
import io.okhi.android_background_geofencing.models.WebHookType;
import io.okhi.android_core.OkHi;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;

public class MainActivity extends AppCompatActivity {

    OkHi okHi;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        okHi = new OkHi(this);
        context = this;

        String packageName = context.getPackageName();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        BackgroundGeofencingNotification notification = new BackgroundGeofencingNotification(
                "Yooooooo",
                "Don't mind us",
                Constant.PERSISTENT_NOTIFICATION_CHANNEL_ID,
                "OkHi Channel",
                "My channel description",
                NotificationManager.IMPORTANCE_HIGH,
                Constant.PERSISTENT_NOTIFICATION_ID,
            456,
                intent
        );

        BackgroundGeofencing.init(this, notification);

        final Button button = findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okHi.requestBackgroundLocationPermission("We need permission", "Pretty please", new OkHiRequestHandler<Boolean>() {
                    @Override
                    public void onResult(Boolean result) {
                        if (result) startGeofence();
                    }

                    @Override
                    public void onError(OkHiException e) {
                    }
                });
            }
        });

        final Button phone_master = findViewById(R.id.phone_master);
        phone_master.setOnClickListener((v) -> {
            final String PACKAGE_NAME = "com.transsion.phonemaster";

            if(BackgroundGeofencing.isPackageInstalled(PACKAGE_NAME, this)){
                Toast.makeText(context, "Package Found", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, "Package Not available", Toast.LENGTH_LONG).show();
            }
        });

        final Button protected_settings = findViewById(R.id.protected_settings);
        protected_settings.setOnClickListener((v) -> {
            final String PACKAGE_NAME = "com.transsion.phonemaster";
            final String PROTECTED_APPS_CLASS_NAME = "com.cyin.himgr.widget.activity.MainSettingGpActivity";

            Intent myIntent = new Intent();
            myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Add the application to Protected Apps to enable verification");
            ComponentName componentName = new ComponentName(PACKAGE_NAME, PROTECTED_APPS_CLASS_NAME);
            myIntent.setComponent(componentName);
            try {
                startActivityForResult(myIntent, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        final Button update_progress = findViewById(R.id.update_progress);
        update_progress.setOnClickListener((v) -> {
            BackgroundGeofencingNotification notice = new BackgroundGeofencingNotification(
                    "Verifying...",
                    "Verification In Progress",
                    Constant.PERSISTENT_NOTIFICATION_CHANNEL_ID,
                    "OkHi Channel",
                    "My channel description",
                    NotificationManager.IMPORTANCE_HIGH,
                    Constant.PERSISTENT_NOTIFICATION_ID,
                    456,
                    intent
            );

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(Constant.PERSISTENT_NOTIFICATION_ID, notice.getNotification(context, false,true));

        });

        final Button location_request = findViewById(R.id.location_request);
        location_request.setOnClickListener((v) -> {
            HashMap<String, Boolean> locationState = BackgroundGeofenceUtil.locationPermissionState(this);
            Boolean ACCESS_COARSE_LOCATION = locationState.get("ACCESS_COARSE_LOCATION");
            Boolean ACCESS_FINE_LOCATION = locationState.get("ACCESS_FINE_LOCATION");
            Boolean ACCESS_BACKGROUND_LOCATION = locationState.get("ACCESS_BACKGROUND_LOCATION");

            Log.d("ACCESS_COARSE_LOCATION", "The Value is: " + ACCESS_COARSE_LOCATION);
            Log.d("ACCESS_FINE_LOCATION", "The Value is: " + ACCESS_FINE_LOCATION);
            Log.d("ACCESS_BACKGROUND", "The Value is: " + ACCESS_BACKGROUND_LOCATION);
            Log.d("Contains false", "The Value is : " + locationState.containsValue(false));

            BackgroundGeofencingLocationService.checkLocationPermissions(this);
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
            "https://1b11-41-90-187-107.ngrok.io/transits",
            10000,
            headers,
            null,
            WebHookType.GEOFENCE,
            WebHookRequest.POST
        );
        geofenceWebHook.save(this);
        BackgroundGeofencingWebHook deviceMetaWebHook = new BackgroundGeofencingWebHook(
            "https://1b11-41-90-187-107.ngrok.io/device-meta",
            10000,
            headers,
            null,
            WebHookType.DEVICE_PING,
            WebHookRequest.POST
        );
        deviceMetaWebHook.save(this);
        BackgroundGeofencingWebHook stopVerificationWebHook = new BackgroundGeofencingWebHook(
            "https://1b11-41-90-187-107.ngrok.io/stop/verification",
            10000,
            headers,
            null,
            WebHookType.STOP,
            WebHookRequest.PATCH
        );
        stopVerificationWebHook.save(this);
    }

    private void startGeofence() {
        okHi.requestBackgroundLocationPermission("Hi", "There", new OkHiRequestHandler<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                BackgroundGeofence homeGeofence = new BackgroundGeofence.BackgroundGeofenceBuilder("home1", -1.4618082,37.0146066)
                    .setNotificationResponsiveness(5)
                    .setLoiteringDelay(60000)
                    .setInitialTriggerTransitionTypes(0)
                    .build();
                final BackgroundGeofence[] geofences = {homeGeofence};
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

            @Override
            public void onError(OkHiException exception) {

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
        } catch (BackgroundGeofencingException e) {
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
}
