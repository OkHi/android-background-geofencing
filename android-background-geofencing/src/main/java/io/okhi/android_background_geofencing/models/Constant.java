package io.okhi.android_background_geofencing.models;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;

import java.util.concurrent.TimeUnit;

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
    public static final String DB_WEBHOOK_CONFIGURATION_KEY = "WEBHOOK_CONFIGURATION_KEY";
    public static final String DB_BACKGROUND_GEOFENCE_STRUCTURE_VERSION = "v1:";
    public static final String DB_BACKGROUND_GEOFENCE_PREFIX_KEY = "BACKGROUND_GEOFENCE:" + DB_BACKGROUND_GEOFENCE_STRUCTURE_VERSION;

    // geofence defaults
    public static final long DEFAULT_GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE;
    public static final int DEFAULT_GEOFENCE_RADIUS = 300;
    public static final int DEFAULT_GEOFENCE_NOTIFICATION_RESPONSIVENESS = 300000;
    public static final int DEFAULT_GEOFENCE_LOITERING_DELAY = 1800000;
    public static final boolean DEFAULT_GEOFENCE_REGISTER_ON_DEVICE_RESTART = true;
    public static final int DEFAULT_GEOFENCE_TRANSITION_TYPES = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL;
    public static final int DEFAULT_GEOFENCE_INITIAL_TRIGGER_TRANSITION_TYPES = GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT | GeofencingRequest.INITIAL_TRIGGER_DWELL;

    // jobs
    public static final int GEOFENCE_TRANSITION_JOB_ID = 5;
    public static final String GEOFENCE_TRANSITION_EVENT_JSON_PAYLOAD = "GEOFENCE_TRANSITION_EVENT_JSON_PAYLOAD";
    public static final String GEOFENCE_TRANSITION_UPLOAD_REQUEST = "GEOFENCE_TRANSITION_UPLOAD_REQUEST";

    // TODO: change backoff delay to 45min
    public static int GEOFENCE_UPLOAD_WORK_MAX_ATTEMPTS = 10;

    public static int GEOFENCE_UPLOAD_WORK_BACK_OFF_DELAY = 16;
    public static TimeUnit  GEOFENCE_UPLOAD_WORK_BACK_OFF_DELAY_TIME_UNIT = TimeUnit.MINUTES;

    // TODO: change initial delay to 10min
    public static final long GEOFENCE_UPLOAD_WORK_INITIAL_DELAY = 1;
    public static final TimeUnit GEOFENCE_UPLOAD_WORK_INITIAL_DELAY_TIME_UNIT = TimeUnit.MINUTES;

}
