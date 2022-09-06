package io.okhi.android_background_geofencing.database;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import io.okhi.android_background_geofencing.models.Constant;
import io.okhi.android_core.models.OkHiException;

public class DataStore {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public DataStore(Context context){
        this.prefs = context.getSharedPreferences(Constant.PREFERENCE_NAME, Context.MODE_PRIVATE);
        this.editor = this.prefs.edit();
    }

    public String readString(String key){
        return prefs.getString(key, "");
    }
    public int readInt(String key){
        return prefs.getInt(key, 0);
    }
    public boolean readBool(String key){
        return prefs.getBoolean(key, false);
    }

    // Save values
    public boolean saveEntry(String key, Object value) {
        try{
            if (value instanceof String) editor.putString(key, (String) value).apply();
            if (value instanceof Integer) editor.putInt(key, (Integer) value).apply();
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value).apply();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
