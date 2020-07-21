package io.okhi.android_background_geofencing.database;

import android.content.Context;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;

import java.util.ArrayList;

import io.okhi.android_background_geofencing.BackgroundGeofencing;
import io.okhi.android_background_geofencing.models.BackgroundGeofence;
import io.okhi.android_background_geofencing.models.BackgroundGeofenceTransition;
import io.okhi.android_background_geofencing.models.BackgroundGeofencingWebHook;
import io.okhi.android_background_geofencing.models.Constant;

public class BackgroundGeofencingDB {

    private static String TAG = "BackgroundGeofencingDB";

    private static void save(String key, Object object, Context context) {
        try {
            DB db = DBFactory.open(context, Constant.DB_NAME);
            db.put(key, object);
            db.close();
            Log.v(TAG, "Successfully saved: " + key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object get(String key, Class objectClass, Context context) {
        try {
            DB db = DBFactory.open(context, Constant.DB_NAME);
            Object value = db.get(key, objectClass);
            db.close();
            Log.v(TAG, "Successfully got: " + key);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String[] getKeys(String prefix, Context context) {
        try {
            DB db = DBFactory.open(context, Constant.DB_NAME);
            String[] key = db.findKeys(prefix);
            db.close();
            return key;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void remove(String key, Context context) {
        try {
            DB db = DBFactory.open(context, Constant.DB_NAME);
            db.del(key);
            db.close();
            Log.v(TAG, "Successfully removed: " + key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveWebHook(BackgroundGeofencingWebHook webHook, Context context) {
        save(Constant.DB_WEBHOOK_CONFIGURATION_KEY, webHook, context);
    }

    public static BackgroundGeofencingWebHook getWebHook(Context context) {
        return (BackgroundGeofencingWebHook) get(Constant.DB_WEBHOOK_CONFIGURATION_KEY, BackgroundGeofencingWebHook.class, context);
    }

    public static void saveBackgroundGeofence(BackgroundGeofence geofence, Context context) {
        save(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY + geofence.getId(), geofence, context);
    }

    public static void saveGeofenceTransitionEvent(BackgroundGeofenceTransition transition, Context context) {
        String key = Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY + transition.getTransitionDate();
        BackgroundGeofenceTransition existingTransition = (BackgroundGeofenceTransition) get(key, BackgroundGeofenceTransition.class, context);
        if (existingTransition == null) {
            save(key, transition, context);
        }
    }

    public static ArrayList<BackgroundGeofenceTransition> getAllGeofenceTransitions(Context context) {
        ArrayList<BackgroundGeofenceTransition> transitions = new ArrayList<>();
        String[] keys = getKeys(Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY, context);
        if (keys != null) {
            for(String key: keys) {
                BackgroundGeofenceTransition transition = (BackgroundGeofenceTransition) get(key, BackgroundGeofenceTransition.class, context);
                transitions.add(transition);
            }
        }
        return transitions;
    }

    public static void removeGeofenceTransition(BackgroundGeofenceTransition transition, Context context) {
        String key = Constant.DB_BACKGROUND_GEOFENCE_TRANSITION_PREFIX_KEY + transition.getTransitionDate();
        remove(key, context);
    }

    public static BackgroundGeofence getBackgroundGeofence(String geofenceId, Context context) {
        return (BackgroundGeofence) get(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY + geofenceId, BackgroundGeofence.class, context);
    }

    public static ArrayList<BackgroundGeofence> getAllGeofences(Context context) {
        ArrayList<BackgroundGeofence> geofences = new ArrayList<>();
        String[] keys = getKeys(Constant.DB_BACKGROUND_GEOFENCE_PREFIX_KEY, context);
        if (keys != null) {
            for (String key: keys) {
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
        for(BackgroundGeofence geofence: geofences) {
            if (geofence.isFailing()) {
                failingGeofences.add(geofence);
            }
        }
        return failingGeofences;
    }

    // TODO: edge case we don't get a single geofence event, need to track registration date within geofences
    public static long getLastGeofenceTransitionEventTimestamp(Context context) {
        long timestamp = -1;
        ArrayList<BackgroundGeofenceTransition> transitions = getAllGeofenceTransitions(context);
        for (BackgroundGeofenceTransition transition: transitions) {
            if (timestamp < transition.getTransitionDate()) {
                timestamp = transition.getTransitionDate();
            }
        }
        return timestamp;
    }
}
