package io.okhi.android_background_geofencing.database;

import android.content.Context;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;

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

    public static void saveWebHook(BackgroundGeofencingWebHook webHook, Context context) {
        save(Constant.DB_WEBHOOK_CONFIGURATION_KEY, webHook, context);
    }

    public static BackgroundGeofencingWebHook getWebHook(Context context) {
        return (BackgroundGeofencingWebHook) get(Constant.DB_WEBHOOK_CONFIGURATION_KEY, BackgroundGeofencingWebHook.class, context);
    }

}
