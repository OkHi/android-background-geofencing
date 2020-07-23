package io.okhi.android_background_geofencing.models;

import androidx.annotation.Nullable;

public class BackgroundGeofencingException extends Exception {

    public static final String UNKNOWN_EXCEPTION = "unknown_exception";
    public static final String SERVICE_UNAVAILABLE_CODE = "service_unavailable";
    public static final String PERMISSION_DENIED_CODE = "permission_denied";
    
    private String code;
    private String message;

    BackgroundGeofencingException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    @Nullable
    @Override
    public String getMessage() {
        return message;
    }
}
