package io.okhi.android_background_geofencing.database;

import android.content.Context;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSetting;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceSource;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceUtil;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingNotification;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_core.models.OkHiCoreUtil;
import io.okhi.android_background_geofencing.models.WebHookType;

// TODO: implement strategy to dump the current db if we bump up the version

@SuppressWarnings("rawtypes")
public class BackgroundGeofencingDB {

    private static String TAG = "BackgroundGeofencingDB";
    private static Lock lock = new ReentrantLock();
    private static Condition condition = lock.newCondition();

    private static void save(String key, Object object, Context context) {
        try {
            lock.lock();
            DB db = DBFactory.open(context, Constant.DB_NAME);
            db.put(key, object);
            db.close();
            BackgroundGeofenceUtil.log(context, TAG, "Successfully saved: " + key);
        } catch (Exception e) {
            e.printStackTrace();
            OkHiCoreUtil.captureException(e);
        } finally {
            lock.unlock();
        }
    }

    private static Object get(String key, Class objectClass, Context context) {
        try {
            lock.lock();
            DB db = DBFactory.open(context, Constant.DB_NAME);
            Object value = db.get(key, objectClass);
            db.close();
            BackgroundGeofenceUtil.log(context, TAG, "Successfully got: " + key);
            return value;
        } catch (Exception e) {
//            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return null;
    }

    private static String[] getKeys(String prefix, Context context) {
        try {
            lock.lock();
            DB db = DBFactory.open(context, Constant.DB_NAME);
            String[] key = db.findKeys(prefix);
            db.close();
            return key;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            lock.unlock();
        }
    }

    private static void remove(String key, Context context) {
        try {
            lock.lock();
            DB db = DBFactory.open(context, Constant.DB_NAME);
            db.del(key);
            db.close();
            BackgroundGeofenceUtil.log(context, TAG, "Successfully removed: " + key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void saveGeofenceEnterTimestamp(BackgroundGeofence geofence, Context context) {
        try {
            lock.lock();
            String key = Constant.DB_INIT_ENTER_GEOFENCE_PREFIX_KEY + geofence.getId();
            DB db = DBFactory.open(context, Constant.DB_NAME);
            db.putLong(key, System.currentTimeMillis());
            db.close();
            BackgroundGeofenceUtil.log(context, TAG, "Successfully saved: " + key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static long getGeofenceEnterTimestamp(BackgroundGeofence geofence, Context context) {
        long timestamp = -1;
        try {
            lock.lock();
            String key = Constant.DB_INIT_ENTER_GEOFENCE_PREFIX_KEY + geofence.getId();
            DB db = DBFactory.open(context, Constant.DB_NAME);
            timestamp = db.getLong(key);
            db.close();
            BackgroundGeofenceUtil.log(context, TAG, "Successfully got: " + key);
        } catch (Exception e) {
//            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return timestamp;
    }

    public static void saveWebHook(BackgroundGeofencingWebHook webHook, Context context) {
        save(Constant.DB_WEBHOOK_CONFIGURATION_KEY+webHook.getWebhookType().name(), webHook, context);
    }

    public static BackgroundGeofencingWebHook getWebHook(Context context) {
        return (BackgroundGeofencingWebHook) get(Constant.DB_WEBHOOK_CONFIGURATION_KEY+ WebHookType.GEOFENCE.name(), BackgroundGeofencingWebHook.class, context);
    }

    public static BackgroundGeofencingWebHook getWebHook(Context context, WebHookType webhookType) {
        return (BackgroundGeofencingWebHook) get(Constant.DB_WEBHOOK_CONFIGURATION_KEY+webhookType.name(), BackgroundGeofencingWebHook.class, context);
    }

    public static void saveBackgroundGeofence(BackgroundGeofence geofence, Context context) {
        save(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY + geofence.getId(), geofence, context);
    }

    public static void saveGeofenceTransitionEvent(BackgroundGeofenceTransition transition, Context context) {
        try {
            String geofenceTransitionKey = Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY + transition.getSignature();
            String lastGeofenceTransition = Constant.DB_BACKGROUND_GEOFENCE_LAST_TRANSITION_KEY;
            BackgroundGeofenceTransition existingTransition = (BackgroundGeofenceTransition) get(geofenceTransitionKey, BackgroundGeofenceTransition.class, context);
            if (existingTransition == null) {
                save(geofenceTransitionKey, transition, context);
                save(lastGeofenceTransition, transition, context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<BackgroundGeofenceTransition> getAllGeofenceTransitions(Context context) {
        ArrayList<BackgroundGeofenceTransition> transitions = new ArrayList<>();
        String[] keys = getKeys(Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY, context);
        if (keys != null) {
            for (String key : keys) {
                BackgroundGeofenceTransition transition = (BackgroundGeofenceTransition) get(key, BackgroundGeofenceTransition.class, context);
                transitions.add(transition);
            }
        }
        return transitions;
    }

    public static void removeGeofenceTransition(BackgroundGeofenceTransition transition, Context context) {
        try {
            String key = Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY + transition.getSignature();
            remove(key, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BackgroundGeofence getBackgroundGeofence(String geofenceId, Context context) {
        return (BackgroundGeofence) get(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY + geofenceId, BackgroundGeofence.class, context);
    }

    public static ArrayList<BackgroundGeofence> getAllGeofences(Context context) {
        ArrayList<BackgroundGeofence> geofences = new ArrayList<>();
        String[] keys = getKeys(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY, context);
        if (keys != null) {
            for (String key : keys) {
                BackgroundGeofence geofence = (BackgroundGeofence) get(key, BackgroundGeofence.class, context);
                if (geofence != null) {
                    if (geofence.hasExpired()) {
                        removeGeofence(geofence.getId(), context);
                    } else {
                        geofences.add(geofence);
                    }
                }
            }
        }
        return geofences;
    }

    private static void removeGeofence(String id, Context context) {
        remove(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY + id, context);
    }

    public static ArrayList<BackgroundGeofence> getAllFailingGeofences(Context context) {
        ArrayList<BackgroundGeofence> geofences = getAllGeofences(context);
        ArrayList<BackgroundGeofence> failingGeofences = getAllGeofences(context);
        for (BackgroundGeofence geofence : geofences) {
            if (geofence.isFailing()) {
                failingGeofences.add(geofence);
            }
        }
        return failingGeofences;
    }

    // TODO: edge case we don't get a single geofence event, need to track registration date within geofences
    public static long getLastGeofenceTransitionEventTimestamp(Context context) {
        BackgroundGeofenceTransition transition = (BackgroundGeofenceTransition) get(Constant.DB_BACKGROUND_GEOFENCE_LAST_TRANSITION_KEY, BackgroundGeofenceTransition.class, context);
        if (transition == null) {
            return -1;
        }
        return transition.getTransitionDate();
    }

    public static void removeLastGeofenceTransitionEvent(Context context) {
        remove(Constant.DB_BACKGROUND_GEOFENCE_LAST_TRANSITION_KEY, context);
    }

    public static void removeBackgroundGeofence(String id, Context context) {
        remove(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY + id, context);
    }

    public static void removeGeofenceEnterTimestamp(BackgroundGeofence geofence, Context context) {
        String key = Constant.DB_INIT_ENTER_GEOFENCE_PREFIX_KEY + geofence.getId();
        remove(key, context);
    }

    public static void removeGeofenceEnterTimestamp(String geofenceId, Context context) {
        String key = Constant.DB_INIT_ENTER_GEOFENCE_PREFIX_KEY + geofenceId;
        remove(key, context);
    }

    public static void saveNotification(BackgroundGeofencingNotification notification, Context context) {
        if (notification != null) {
            String key = Constant.DB_NOTIFICATION_CONFIGURATION_KEY;
            save(key, notification, context);
        }
    }

    public static BackgroundGeofencingNotification getNotification(Context context) {
        String key = Constant.DB_NOTIFICATION_CONFIGURATION_KEY;
        return (BackgroundGeofencingNotification) get(key, BackgroundGeofencingNotification.class, context);
    }

    public static void saveSetting(BackgroundGeofenceSetting setting, Context context) {
        if (setting != null) {
            String key = Constant.DB_SETTING_CONFIGURATION_KEY;
            save(key, setting, context);
        }
    }

    public static BackgroundGeofenceSetting getBackgroundGeofenceSetting(Context context) {
        String key = Constant.DB_SETTING_CONFIGURATION_KEY;
        return (BackgroundGeofenceSetting) get(key, BackgroundGeofenceSetting.class, context);
    }

    public static void saveDeviceId(Context context) {
        String key = Constant.DB_DEVICE_ID_CONFIGURATION_KEY;
        String deviceId = (String) get(key, String.class, context);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            save(key, deviceId, context);
        }
    }

    public static String getDeviceId(Context context) {
        String key = Constant.DB_DEVICE_ID_CONFIGURATION_KEY;
        return (String) get(key, String.class, context);
    }

    public static ArrayList<BackgroundGeofence> getGeofences(Context context, BackgroundGeofenceSource source) {
        ArrayList<BackgroundGeofence> geofences = new ArrayList<>();
        ArrayList<BackgroundGeofence> savedGeofences = BackgroundGeofencingDB.getAllGeofences(context);
        for(BackgroundGeofence geofence: savedGeofences) {
            if (source == BackgroundGeofenceSource.APP_OPEN && geofence.isWithAppOpenTracking()) {
                geofences.add(geofence);
            }
            if (source == BackgroundGeofenceSource.NATIVE_GEOFENCE && geofence.isWithNativeGeofenceTracking()) {
                geofences.add(geofence);
            }
            if (source == BackgroundGeofenceSource.FOREGROUND_PING && geofence.isWithForegroundPingTracking()) {
                geofences.add(geofence);
            }
            if (source == BackgroundGeofenceSource.FOREGROUND_WATCH && geofence.isWithForegroundWatchTracking()) {
                geofences.add(geofence);
            }
        }
        return geofences;
    }

    public static BackgroundGeofenceTransition getTransitionFromSignature(Context context, String transitionSignature) {
        String geofenceTransitionKey = Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY + transitionSignature;
        BackgroundGeofenceTransition existingTransition = (BackgroundGeofenceTransition) get(geofenceTransitionKey, BackgroundGeofenceTransition.class, context);
        return existingTransition;
    }

    public static boolean isWithinTimeThreshold(BackgroundGeofenceTransition transition, Context context) {
        String key = Constant.DB_TRANSITION_TIME_TRACKER_PREFIX + transition.getGeoPointSource() + ":" + transition.getStringIds() + ":" + transition.getTransitionEvent();
        BackgroundGeofenceTransition existingTransition = (BackgroundGeofenceTransition) get(key, BackgroundGeofenceTransition.class, context);
        if (existingTransition == null || transition.getTransitionDate() - existingTransition.getTransitionDate() > 30000) {
            Log.v("BackDB", "Transition within limit:" + key);
            save(key, transition, context);
            return true;
        } else {
            Log.v("BackDB", "Transition is NOT within limit:" + key);
            return false;
        }
    }
}
