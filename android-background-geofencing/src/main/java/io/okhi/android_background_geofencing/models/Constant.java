package io.okhi.android_background_geofencing.models;

import com.esotericsoftware.kryo.Kryo;

public class Constant {

    // request codes
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    public static final int ENABLE_LOCATION_SERVICES_REQUEST_CODE = 2;
    public static final int OPEN_LOCATION_SERVICES_SETTINGS_REQUEST_CODE = 3;
    public static final int ENABLE_GOOGLE_PLAY_SERVICES_REQUEST_CODE = 4;

    // timers and delays
    public static final long SERVICE_WAIT_DELAY = 2000;
    public static final long DEFAULT_WEBHOOK_TIMEOUT = 10000;

    // dialogs
    public static final String PERMISSION_DIALOG_POSITIVE_BUTTON_TEXT = "GRANT";
    public static final String PERMISSION_DIALOG_NEGATIVE_BUTTON_TEXT = "CANCEL";

    // db
    public static final String DB_NAME_VERSION = "v1";
    public static final String DB_NAME = "BACKGROUND_GEOFENCING_DB:" + Constant.DB_NAME_VERSION;
    public static final String DB_WEBHOOK_CONFIGURATION_KEY = "DB_WEBHOOK_CONFIGURATION_KEY";
}
