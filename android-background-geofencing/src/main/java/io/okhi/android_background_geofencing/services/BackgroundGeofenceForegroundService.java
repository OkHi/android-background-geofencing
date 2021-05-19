package io.okhi.android_background_geofencing.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSource;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingLocationService;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.Constant;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(backgroundGeofencingNotification.getNotificationId(), backgroundGeofencingNotification.getNotification(getApplicationContext()));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(Constant.FOREGROUND_SERVICE_ACTION) && Objects.equals(intent.getStringExtra(Constant.FOREGROUND_SERVICE_ACTION), Constant.FOREGROUND_SERVICE_STOP)) {
            foregroundWorkStarted = false;
            stopService(true);
        }
        if (intent != null && intent.hasExtra(Constant.FOREGROUND_SERVICE_ACTION) && Objects.equals(intent.getStringExtra(Constant.FOREGROUND_SERVICE_ACTION), Constant.FOREGROUND_SERVICE_GEOFENCE_EVENT)) {
            startGeofenceTransitionWork(true);
        }
        if (isWithForegroundService && !foregroundWorkStarted) {
            foregroundWorkStarted = true;
            startForegroundPingService();
            startForegroundLocationWatch();
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

    private void startGeofenceTransitionWork(final boolean restartGeofences) {
        try {
            manageDeviceWake(true);
            boolean result = BackgroundGeofenceTransitionUploadWorker.uploadTransitions(getApplicationContext());
            if (!result) {
                BackgroundGeofenceTransition.scheduleGeofenceTransitionUploadWork(getApplicationContext());
            }
            if (restartGeofences) {
                restartFailedGeofences();
            }
            Log.v(TAG, "Transition work complete.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manageDeviceWake(false);
            stopService(false);
        }
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
                OkHiLocationService.getCurrentLocation(getApplicationContext(), new OkHiRequestHandler<Location>() {
                    @Override
                    public void onResult(Location location) {
                        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getGeofences(getApplicationContext(), BackgroundGeofenceSource.FOREGROUND_PING);
                        ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofenceTransition.generateTransitions(
                                Constant.FOREGROUND_SERVICE_PING_GEOFENCE_SOURCE,
                                location,
                                geofences,
                                false,
                                getApplicationContext()
                        );
                        for (BackgroundGeofenceTransition transition : transitions) {
                            transition.save(getApplicationContext());
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                startGeofenceTransitionWork(false);
                            }
                        }).start();
                        manageDeviceWake(false);
                    }
                    @Override
                    public void onError(OkHiException exception) {
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
            generateGeofenceTransitions(location);
        } else {
            new BackgroundGeofencingLocationService().fetchCurrentLocation(getApplicationContext(), new ResultHandler<Location>() {
                @Override
                public void onSuccess(Location result) {
                    generateGeofenceTransitions(result);
                }
                @Override
                public void onError(BackgroundGeofencingException exception) {
                    exception.printStackTrace();
                    generateGeofenceTransitions(location);
                }
            });
        }
    }

    private void generateGeofenceTransitions(Location location) {
        ArrayList<BackgroundGeofence> geofences = BackgroundGeofencingDB.getGeofences(getApplicationContext(), BackgroundGeofenceSource.FOREGROUND_WATCH);
        ArrayList<BackgroundGeofenceTransition> transitions = BackgroundGeofenceTransition.generateTransitions(
                Constant.FOREGROUND_SERVICE_WATCH_GEOFENCE_SOURCE,
                location,
                geofences,
                false,
                getApplicationContext()
        );
        for (BackgroundGeofenceTransition transition : transitions) {
            transition.save(getApplicationContext());
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                startGeofenceTransitionWork(false);
            }
        }).start();
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
        BackgroundGeofenceSetting setting = BackgroundGeofencingDB.getBackgroundGeofenceSetting(getApplicationContext());
        if (setting != null && setting.isWithForegroundService()) {
            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "Attempting to restart foreground service");
                            try {
                                BackgroundGeofencing.startForegroundService(getApplicationContext());
                            } catch (BackgroundGeofencingException e) {
                                e.printStackTrace();
                            }
                            handler.removeCallbacks(this);
                        }
                    };
                    handler.postDelayed(runnable, 60000);
                    Log.v(TAG, "Restart scheduled in the next 1min");
                }
            }).start();
        }
    }

    private void runCleanUp() {
        Log.v(TAG, "Running clean up..");
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (fusedLocationProviderClient != null && watchLocationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(watchLocationCallback);
        }
        foregroundWorkStarted = false;
        Log.v(TAG, "Clean up done");
    }
}
