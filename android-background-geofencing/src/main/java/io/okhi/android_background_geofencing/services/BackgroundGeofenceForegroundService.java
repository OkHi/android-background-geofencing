package io.okhi.android_background_geofencing.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceDeviceMeta;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSource;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_background_geofencing.receivers.BackgroundGeofenceLocationServicesReceiver;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocationService;

public class BackgroundGeofenceForegroundService extends Service {
    private static String TAG = "BGFS";

    private PowerManager.WakeLock wakeLock;

    private Handler handler = new Handler();

    private Runnable runnable;

    private boolean foregroundWorkStarted;

    private  boolean isWithForegroundService = false;

    // watch location
    private LocationRequest watchLocationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback watchLocationCallback;
    private static Lock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();
    private BackgroundGeofencingWebHook webHook;
    private BackgroundGeofenceLocationServicesReceiver receiver;
    private HashMap<String, BackgroundGeofenceTransition> transitionTracker = new HashMap<>();
    private BackgroundGeofencingLocationService backgroundGeofencingLocationService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        BackgroundGeofencingNotification backgroundGeofencingNotification = BackgroundGeofencingDB.getNotification(getApplicationContext());
        backgroundGeofencingNotification.createNotificationChannel(getApplicationContext());
        BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(getApplicationContext());
        isWithForegroundService = setting != null && setting.isWithForegroundService();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constant.FOREGROUND_SERVICE_WAKE_LOCK_TAG);
        webHook = BackgroundGeofencingDB.getWebHook(getApplicationContext());
        startForeground(backgroundGeofencingNotification.getNotificationId(), backgroundGeofencingNotification.getNotification(getApplicationContext()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                IntentFilter intentFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
                receiver = new BackgroundGeofenceLocationServicesReceiver();
                registerReceiver(receiver, intentFilter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (webHook == null) {
            webHook = BackgroundGeofencingDB.getWebHook(getApplicationContext());
        }
        if (webHook == null) {
            foregroundWorkStarted = false;
            stopService(true);
        } else {
            if (intent != null && intent.hasExtra(Constant.FOREGROUND_SERVICE_ACTION) && Objects.equals(intent.getStringExtra(Constant.FOREGROUND_SERVICE_ACTION), Constant.FOREGROUND_SERVICE_STOP)) {
                foregroundWorkStarted = false;
                stopService(true);
            }
            if (intent != null && intent.hasExtra(Constant.FOREGROUND_SERVICE_ACTION) && Objects.equals(intent.getStringExtra(Constant.FOREGROUND_SERVICE_ACTION), Constant.FOREGROUND_SERVICE_GEOFENCE_EVENT)) {
                if (intent.hasExtra(Constant.FOREGROUND_SERVICE_TRANSITION_SIGNATURE)) {
                    uploadGeofenceTransition(intent.getStringExtra(Constant.FOREGROUND_SERVICE_TRANSITION_SIGNATURE));
                } else {
                    restartFailedGeofences();
                }
            }
            if (isWithForegroundService && !foregroundWorkStarted) {
                foregroundWorkStarted = true;
                startForegroundPingService();
                startForegroundLocationWatch();
            }
        }
        return isWithForegroundService ? START_STICKY : START_NOT_STICKY;
    }

    private void manageDeviceWake(boolean wake) {
        if (wake && !wakeLock.isHeld()) {
            try {
                lock.lock();
                wakeLock.acquire();
            } catch (Exception e) {
                /// ignore
            } finally {
                lock.unlock();
            }
        } else {
            try {
                lock.lock();
                wakeLock.release();
            } catch (Exception e) {
                /// ignore
            } finally {
                lock.unlock();
            }
        }
    }

    private void uploadGeofenceTransition(String transitionSignature) {
        if (transitionSignature == null) return;
        manageDeviceWake(true);
        final BackgroundGeofenceTransition transition = BackgroundGeofencingDB.getTransitionFromSignature(getApplicationContext(), transitionSignature);
        if (transition == null) return;
        BackgroundGeofencingDB.removeGeofenceTransition(transition, getApplicationContext());
        transition.asyncUpload(getApplicationContext(), webHook, new ResultHandler<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                handleServiceStop();
                manageDeviceWake(false);
            }
            @Override
            public void onError(BackgroundGeofencingException exception) {
                transition.save(getApplicationContext());
                BackgroundGeofenceTransition.scheduleAsyncUploadTransition(getApplicationContext());
                handleServiceStop();
                manageDeviceWake(false);
            }
        });
    }

    // TODO: refactor this to something cleaner
    private void restartFailedGeofences() {
        try {
            ArrayList<BackgroundGeofence> failedGeofences = new ArrayList<>();
            if (BackgroundGeofenceUtil.canRestartGeofences(getApplicationContext())) {
                ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getGeofences(getApplicationContext(), BackgroundGeofenceSource.NATIVE_GEOFENCE);
                for (BackgroundGeofence geofence : geofences) {
                    if (geofence.isFailing()) {
                        failedGeofences.add(geofence);
                    }
                }
                if (!failedGeofences.isEmpty()) {
                    BackgroundGeofenceRestartWorker.restartGeofences(failedGeofences, getApplicationContext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startForegroundPingService() {
        runnable = new Runnable() {
            @Override
            public void run() {
                /* do what you need to do */
                manageDeviceWake(true);
                BackgroundGeofenceDeviceMeta.asyncUpload(getApplicationContext());
                backgroundGeofencingLocationService = new BackgroundGeofencingLocationService();
                backgroundGeofencingLocationService.fetchCurrentLocation(getApplicationContext(), new ResultHandler<Location>() {
                    @Override
                    public void onSuccess(Location result) {
                        generateUploadGeofenceTransitions(result, Constant.FOREGROUND_SERVICE_PING_GEOFENCE_SOURCE);
                        manageDeviceWake(false);
                    }
                    @Override
                    public void onError(BackgroundGeofencingException exception) {
                        exception.printStackTrace();
                        manageDeviceWake(false);
                    }
                });
                /* and here comes the "trick" */
                handler.postDelayed(this, Constant.FOREGROUND_SERVICE_PING_INTERVAL);
            }
        };
        handler.postDelayed(runnable, Constant.FOREGROUND_SERVICE_PING_DELAY);
    }

    private void startForegroundLocationWatch() {
        createLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        watchLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    BackgroundGeofenceDeviceMeta.asyncUpload(getApplicationContext());
                    handleOnLocationResult(locationResult.getLastLocation());
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(watchLocationRequest, watchLocationCallback, null);
    }

    private void handleOnLocationResult(final Location location) {
        // TODO: refactor 100 to a constant
        if (location.hasAccuracy() && location.getAccuracy() < 100) {
            generateUploadGeofenceTransitions(location, Constant.FOREGROUND_SERVICE_WATCH_GEOFENCE_SOURCE);
        } else {
            new BackgroundGeofencingLocationService().fetchCurrentLocation(getApplicationContext(), new ResultHandler<Location>() {
                @Override
                public void onSuccess(Location result) {
                    if (result.getAccuracy() < location.getAccuracy()) {
                        generateUploadGeofenceTransitions(result, Constant.FOREGROUND_SERVICE_WATCH_GEOFENCE_SOURCE);
                    } else {
                        generateUploadGeofenceTransitions(location, Constant.FOREGROUND_SERVICE_WATCH_GEOFENCE_SOURCE);
                    }
                }
                @Override
                public void onError(BackgroundGeofencingException exception) {
                    exception.printStackTrace();
                    generateUploadGeofenceTransitions(location, Constant.FOREGROUND_SERVICE_WATCH_GEOFENCE_SOURCE);
                }
            });
        }
    }

    private void generateUploadGeofenceTransitions(Location location, String source) {
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getGeofences(getApplicationContext(), BackgroundGeofenceSource.FOREGROUND_WATCH);
        ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofenceTransition.generateTransitions(
                source,
                location,
                geofences,
                false,
                getApplicationContext()
        );
        for (final BackgroundGeofenceTransition transition: transitions) {
            if (!BackgroundGeofencingDB.isWithinTimeThreshold(transition, getApplicationContext())) {
                BackgroundGeofencingDB.removeGeofenceTransition(transition, getApplicationContext());
            } else {
                transition.asyncUpload(getApplicationContext(), webHook, new ResultHandler<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) { }
                    @Override
                    public void onError(BackgroundGeofencingException exception) {
                        BackgroundGeofenceTransition.scheduleAsyncUploadTransition(getApplicationContext());
                    }
                });
            }
        }
    }

    private void createLocationRequest() {
        watchLocationRequest = new LocationRequest();
        watchLocationRequest.setInterval(Constant.FOREGROUND_SERVICE_LOCATION_UPDATE_INTERVAL);
        watchLocationRequest.setFastestInterval(Constant.FOREGROUND_SERVICE_LOCATION_UPDATE_INTERVAL / 2);
        watchLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        watchLocationRequest.setSmallestDisplacement(Constant.FOREGROUND_SERVICE_LOCATION_DISPLACEMENT);
    }

    private void stopService(boolean forceStop) {
        if (!isWithForegroundService || forceStop) {
           runCleanUp();
           stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        runCleanUp();
        unregisterReceiver(receiver);
    }

    private void runCleanUp() {
        BackgroundGeofenceUtil.log(getApplicationContext(), TAG, "Running clean up..");
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (fusedLocationProviderClient != null && watchLocationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(watchLocationCallback);
        }
        foregroundWorkStarted = false;
        BackgroundGeofenceUtil.log(getApplicationContext(), TAG, "Clean up done");
    }

    private void handleServiceStop() {
        if (isWithForegroundService) {
            boolean hasGeofences = !BackgroundGeofencingDB.getGeofences(getApplicationContext(), BackgroundGeofenceSource.FOREGROUND_PING).isEmpty() || !BackgroundGeofencingDB.getGeofences(getApplicationContext(), BackgroundGeofenceSource.FOREGROUND_WATCH).isEmpty();
            if (hasGeofences) return;
        }
        BackgroundGeofencing.stopForegroundService(getApplicationContext());
    }
}
