package io.okhi.android_background_geofencing.models;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import io.okhi.android_background_geofencing.interfaces.ResultHandler;
import io.okhi.android_background_geofencing.receivers.AlarmBroadcastReceiver;
import io.okhi.android_background_geofencing.services.BackgroundGeofenceForegroundRestartWorker;
import io.okhi.android_core.interfaces.OkHiRequestHandler;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocationService;
import io.okhi.android_core.models.OkHiPermissionService;
import io.okhi.android_core.models.OkHiPlayService;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

public class BackgroundGeofenceUtil {

    private static String env;

    public static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBackgroundLocationPermissionGranted(Context context) {
        return OkHiPermissionService.isBackgroundLocationPermissionGranted(context);
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        return OkHiPlayService.isGooglePlayServicesAvailable(context);
    }

    public static boolean isLocationServicesEnabled(Context context) {
        return OkHiLocationService.isLocationServicesEnabled(context);
    }

    public static boolean canRestartGeofences(Context context) {
        return isBackgroundLocationPermissionGranted(context) && isGooglePlayServicesAvailable(context) && isLocationServicesEnabled(context);
    }

    public static void getCurrentLocation(Context context, final ResultHandler<Location> handler) {
        OkHiLocationService.getCurrentLocation(context, new OkHiRequestHandler<Location>() {
            @Override
            public void onResult(Location result) {
                handler.onSuccess(result);
            }

            @Override
            public void onError(OkHiException exception) {
                handler.onError(new BackgroundGeofencingException(exception.getCode(), exception.getMessage()));
            }
        });
    }

    public static void scheduleForegroundRestartWorker (Context context, int initialDelay, TimeUnit unit) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(BackgroundGeofenceForegroundRestartWorker.class, 1, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, unit)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(Constant.FOREGROUND_SERVICE_UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    public static void cancelForegroundRestartWorker (Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(Constant.FOREGROUND_SERVICE_UNIQUE_WORK);
    }

    public static boolean isChineseDevice() {
        String[] devices = {"infinix", "tecno"};
        for (String device : devices) {
            if (Build.MANUFACTURER.toLowerCase().contains(device)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    public static OkHttpClient getHttpClient(BackgroundGeofencingWebHook webHook) {
        if (webHook == null) {
            return null;
        }
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
            .supportsTlsExtensions(true)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
            .cipherSuites(
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                CipherSuite.TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,
                CipherSuite.TLS_ECDHE_RSA_WITH_RC4_128_SHA,
                CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
                CipherSuite.TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
                CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA)
            .build();
        return new OkHttpClient.Builder()
            .connectionSpecs(Collections.singletonList(spec))
            .connectTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(webHook.getTimeout(), TimeUnit.MILLISECONDS).build();
    }

    public static boolean isLocationPermissionGranted(Context context) {
        return OkHiPermissionService.isLocationPermissionGranted(context);
    }

    public static boolean isEnter(Location location, BackgroundGeofence geofence) {
        double distance = distance(location.getLatitude(), geofence.getLat(), location.getLongitude(), geofence.getLng(), 0.0, 0.0);
        return distance < geofence.getRadius();
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @returns Distance in Meters
     */
    private static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        double height = el1 - el2;
        distance = Math.pow(distance, 2) + Math.pow(height, 2);
        return Math.sqrt(distance);
    }

    private static String fetchEnv (Context context) {
        if (env == null) {
            BackgroundGeofencingWebHook webHook = BackgroundGeofencingDB.getWebHook(context);
            if (webHook != null && webHook.getUrl().contains("https://dev")) {
                env = "dev";
            } else {
                env = "prod";
            }
        }
        return env;
    }

    public static void log(Context context, String tag, String message) {
        try {
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            String debugLevel = bundle.getString("io.okhi.android_background_geofencing.debug_level", "none");
            if (debugLevel.equals("verbose")) {
                Log.v(tag, message);
            }
        } catch (Exception e) {
            // shhh..
        }
    }
}
