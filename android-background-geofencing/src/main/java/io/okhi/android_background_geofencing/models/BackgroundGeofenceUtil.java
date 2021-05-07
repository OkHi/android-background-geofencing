package io.okhi.android_background_geofencing.models;

import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.okhi.android_background_geofencing.interfaces.ResultHandler;
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
                Log.v("Util", "Chinese phone detected..");
                return true;
            }
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    public static OkHttpClient getHttpClient(BackgroundGeofencingWebHook webHook) {
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
}
