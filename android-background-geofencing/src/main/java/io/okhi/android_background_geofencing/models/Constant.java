package io.okhi.android_background_geofencing.models;

import androidx.work.Constraints;
import androidx.work.NetworkType;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;

import java.util.concurrent.TimeUnit;

public class Constant {

    // Address SharedPrefs details
    public static final String PREFERENCE_NAME = "OKHI_ADDRESS_DETAILS";

    // Persistent notification
    public static final int BACKGROUND_GEOFENCE_PERSISTENT_NOTIFICATION_ID = 27505;

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
    public static final String DB_WEBHOOK_CONFIGURATION_KEY = "WEBHOOK_CONFIGURATION_KEY:";
    public static final String DB_BACKGROUND_GEOFENCE_PREFIX_KEY = "BACKGROUND_GEOFENCE:";
    public static final String DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY = "BACKGROUND_GEOFENCE_TRANSITION:";
    public static final String DB_BACKGROUND_GEOFENCE_LAST_TRANSITION_KEY = "BACKGROUND_GEOFENCE_LAST_TRANSITION";
    public static final String DB_INIT_ENTER_GEOFENCE_PREFIX_KEY = "INIT_ENTER_GEOFENCE:";
    public static final String DB_NOTIFICATION_CONFIGURATION_KEY = "NOTIFICATION_CONFIGURATION_KEY";
    public static final String DB_SETTING_CONFIGURATION_KEY = "SETTING_CONFIGURATION_KEY";
    public static final String DB_TRANSITION_TIME_TRACKER_PREFIX = "DB_TRANSITION_TIME_TRACKER_PREFIX:";
    public static final String DB_SAVED_ADDRESS_DETAILS = "DB_SAVED_ADDRESS_DETAILS";

    // geofence defaults
    public static final long DEFAULT_GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE;
    public static final int DEFAULT_GEOFENCE_RADIUS = 300;
    public static final int DEFAULT_GEOFENCE_NOTIFICATION_RESPONSIVENESS = 300000;
    public static final int DEFAULT_GEOFENCE_LOITERING_DELAY = 1800000;
    public static final boolean DEFAULT_GEOFENCE_REGISTER_ON_DEVICE_RESTART = true;
    public static final int DEFAULT_GEOFENCE_TRANSITION_TYPES = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL;
    public static final int DEFAULT_GEOFENCE_INITIAL_TRIGGER_TRANSITION_TYPES = GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT | GeofencingRequest.INITIAL_TRIGGER_DWELL;

    // work manager constants - init work
    public static final String GEOFENCE_INIT_WORK_NAME = "GEOFENCE_INIT_WORK_NAME";

    // work manager constants - geofence transition upload work
    public static final String GEOFENCE_TRANSITION_UPLOAD_WORK_TAG = "GEOFENCE_TRANSITION_UPLOAD_WORK_TAG";
    public static final String GEOFENCE_TRANSITION_UPLOAD_WORK_NAME = "GEOFENCE_TRANSITION_UPLOAD_WORK_NAME";
    public static final String GEOFENCE_ASYNC_TRANSITION_UPLOAD_WORK_NAME = "GEOFENCE_ASYNC_TRANSITION_UPLOAD_WORK";
    public static final long GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY = 15;
    public static final TimeUnit GEOFENCE_TRANSITION_UPLOAD_WORK_DELAY_TIME_UNIT = TimeUnit.SECONDS;
    public static final long GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY = 1; // TODO: change to 1
    public static final TimeUnit GEOFENCE_TRANSITION_UPLOAD_WORK_BACKOFF_DELAY_TIME_UNIT = TimeUnit.HOURS;
    public static final long GEOFENCE_TRANSITION_TIME_STAMP_THRESHOLD = 12 * 60 * 60 * 1000;

    // work manager constants - geofence restart work
    public static final String GEOFENCE_RESTART_WORK_TAG = "GEOFENCE_RESTART_WORK_TAG";
    public static final String GEOFENCE_RESTART_WORK_NAME = "GEOFENCE_RESTART_WORK_NAME";
    public static final long GEOFENCE_RESTART_WORK_DELAY = 1; // TODO: change to 1
    public static final TimeUnit GEOFENCE_RESTART_WORK_DELAY_TIME_UNIT = TimeUnit.HOURS; // TODO: change to hour
    public static final long GEOFENCE_RESTART_WORK_BACKOFF_DELAY = 1; // TODO: change to 1
    public static final TimeUnit GEOFENCE_RESTART_WORK_BACKOFF_DELAY_TIME_UNIT = TimeUnit.HOURS; // TODO: change to hour
    public static final String DB_DEVICE_ID_CONFIGURATION_KEY = "DEVICE_ID_CONFIGURATION_KEY";

    // work manager constraints
    public static Constraints GEOFENCE_WORK_MANAGER_CONSTRAINTS = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();

    public static Constraints GEOFENCE_WORK_MANAGER_INIT_CONSTRAINTS = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

    // location updates
    public static final long LOCATION_REQUEST_EXPIRATION_DURATION = 10000;

    // init
    public static final String APP_OPEN_GEOFENCE_TRANSITION_SOURCE_NAME = "appOpen";

    // foreground service
    public static final String FOREGROUND_SERVICE_ACTION = "action";
    public static final String FOREGROUND_SERVICE_TRANSITION_SIGNATURE = "FOREGROUND_SERVICE_TRANSITION_SIGNATURE";
    public static final String FOREGROUND_SERVICE_GEOFENCE_EVENT = "geofence_event";
    public static final String FOREGROUND_SERVICE_START_STICKY = "start_sticky";
    public static final String FOREGROUND_SERVICE_WAKE_LOCK_TAG = "BackgroundGeofenceForegroundService::WakeLock";
    public static final long FOREGROUND_SERVICE_PING_INTERVAL = 60 * 60 * 1000; // change to 60 * 60 * 1000 || 10000
    public static final long FOREGROUND_SERVICE_PING_DELAY = 0;
    public static final String FOREGROUND_SERVICE_PING_GEOFENCE_SOURCE = "foregroundPing";
    public static final String FOREGROUND_SERVICE_WATCH_GEOFENCE_SOURCE = "foregroundWatch";
    public static final String FOREGROUND_SERVICE_STOP = "foregroundStop";
    public static final String FOREGROUND_SERVICE_UNIQUE_WORK = "foregroundServiceUniqueWork";
    public static final long FOREGROUND_SERVICE_LOCATION_UPDATE_INTERVAL = BackgroundGeofenceUtil.isChineseDevice() ? 300000 :  30 * 60 * 1000 ;
    public static final float FOREGROUND_SERVICE_LOCATION_DISPLACEMENT = BackgroundGeofenceUtil.isChineseDevice() ? 25 : 100;

    public static final String FOREGROUND_NOTIFICATION_ICON_META_KEY = "io.okhi.android_background_geofencing.foreground_notification_icon";
    public static final String FOREGROUND_NOTIFICATION_COLOR_META_KEY = "io.okhi.android_background_geofencing.foreground_notification_color";
}
