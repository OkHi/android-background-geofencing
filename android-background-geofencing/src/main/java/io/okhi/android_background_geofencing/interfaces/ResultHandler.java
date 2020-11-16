package io.okhi.android_background_geofencing.interfaces;

import io.okhi.android_core.models.OkHiException;

public interface ResultHandler<T> {
    void onSuccess(T result);

    void onError(OkHiException exception);
}
