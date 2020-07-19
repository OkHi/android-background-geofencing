package io.okhi.android_background_geofencing.models;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;

public class BackgroundGeofencingPermissionService {
    private Activity activity;
    private RequestHandler requestHandler;

    public BackgroundGeofencingPermissionService(Activity activity) {
        this.activity = activity;
    }

    public static boolean isLocationPermissionGranted(Context context) {
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermission(String rationaleTitle, String rationaleMessage, final RequestHandler requestHandler) {
        if (isLocationPermissionGranted(activity)) {
            requestHandler.onSuccess();
            return;
        }

        this.requestHandler = requestHandler;

        final String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        };

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(activity)
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
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity, permissions, Constant.LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    public void onRequestPermissionsResult(int requestCode, String [] permissions, int [] grantResults) {
        if (requestCode == Constant.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestHandler.onSuccess();
            } else {
                requestHandler.onError();
            }
        }
    }
}
