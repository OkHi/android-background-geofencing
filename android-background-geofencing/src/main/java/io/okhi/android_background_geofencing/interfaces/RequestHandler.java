package io.okhi.android_background_geofencing.interfaces;

import io.okhi.android_core.models.OkHiException;

public interface RequestHandler {
    void onSuccess();

    void onError(OkHiException exception);
}
