package io.okhi.android_background_geofencing.models;

import androidx.annotation.Nullable;

public class BackgroundGeofencingException extends Exception {
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
