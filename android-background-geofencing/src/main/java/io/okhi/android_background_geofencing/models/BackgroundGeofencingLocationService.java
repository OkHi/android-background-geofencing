package io.okhi.android_background_geofencing.models;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;

public class BackgroundGeofencingLocationService {

    private Context context;
    private Activity activity;
    private RequestHandler requestHandler;

    BackgroundGeofencingLocationService(Context context, Activity activity, RequestHandler requestHandler) {
        this.context = context;
        this.activity = activity;
        this.requestHandler = requestHandler;
    }



    public static boolean isLocationServicesEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }



    public void requestLocationPermission(String rationaleTitle, String rationaleMessage) {
        if (!BackgroundGeofencingPermissionService.isLocationPermissionGranted(context)) {
            final String[] permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            };
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // build an alert dialog to show rationale to the user
                new AlertDialog.Builder(context)
                        .setTitle(rationaleTitle)
                        .setMessage(rationaleMessage)
                        .setPositiveButton(Constant.PERMISSION_DIALOG_POSITIVE_BUTTON_TEXT, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(activity, permissions, Constant.LOCATION_PERMISSION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton(Constant.PERMISSION_DIALOG_NEGATIVE_BUTTON_TEXT, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestHandler.onError();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                requestHandler.onError();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                requestHandler.onError();
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, permissions, Constant.LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            requestHandler.onSuccess();
        }
    }
}
