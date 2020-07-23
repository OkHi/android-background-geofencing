package io.okhi.android_background_geofencing.models;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import io.okhi.android_background_geofencing.interfaces.RequestHandler;

public class BackgroundGeofencingPlayService {
    private RequestHandler requestHandler;
    private GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
    private Activity activity;
    private BackgroundGeofencingException exception = new BackgroundGeofencingException(BackgroundGeofencingException.SERVICE_UNAVAILABLE_CODE, "Google play services is currently unavailable");

    public BackgroundGeofencingPlayService(Activity activity) {
        this.activity = activity;
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        return result == ConnectionResult.SUCCESS;
    }

    public void requestEnableGooglePlayServices(final RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        int result = googleAPI.isGooglePlayServicesAvailable(activity.getApplicationContext());
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                Dialog dialog = googleAPI.getErrorDialog(activity,result,Constant.ENABLE_GOOGLE_PLAY_SERVICES_REQUEST_CODE);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        requestHandler.onError(exception);
                    }
                });
                dialog.show();
            } else {
                // device not supported
                requestHandler.onError(exception);
            }
        } else {
            requestHandler.onSuccess();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constant.ENABLE_GOOGLE_PLAY_SERVICES_REQUEST_CODE) {
            Handler handle = new Handler();
            handle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isGooglePlayServicesAvailable(activity)) {
                        requestHandler.onSuccess();
                    } else {
                        requestHandler.onError(exception);
                    }
                }
            }, Constant.SERVICE_WAIT_DELAY);
        }
    }
}
