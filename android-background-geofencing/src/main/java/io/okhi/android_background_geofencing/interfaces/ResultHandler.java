package io.okhi.android_background_geofencing.interfaces;

import io.okhi.android_background_geofencing.models.BackgroundGeofencingException;

public interface ResultHandler<T> {
    void onSuccess(T result);
    void onError(BackgroundGeofencingException exception);
}
