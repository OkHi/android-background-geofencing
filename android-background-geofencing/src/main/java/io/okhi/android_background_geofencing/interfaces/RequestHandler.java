package io.okhi.android_background_geofencing.interfaces;

import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;

public interface RequestHandler {
    void onSuccess();

    void onError(BackgroundGeofencingException exception);
}
