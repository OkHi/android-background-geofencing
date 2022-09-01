package io.okhi.android_background_geofencing.singletons;

public class LocationSingleton {
    private static LocationSingleton single_instance = null;

    public Boolean IS_ERROR_DISPLAYED = false;
    public Boolean IS_NOTIFICATION_DISPLAYED = false;

    public static LocationSingleton getInstance()
    {
        if (single_instance == null)
            single_instance = new LocationSingleton();

        return single_instance;
    }
}
