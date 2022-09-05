package io.okhi.android_background_geofencing.database;

import android.content.Context;
import android.content.SharedPreferences;

import io.okhi.android_background_geofencing.models.Constant;

public class DataStore {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    DataStore(Context context){
        this.prefs = context.getSharedPreferences(Constant.PREFERENCE_NAME, Context.MODE_PRIVATE);
        this.editor = this.prefs.edit();
    }

    public String readString(String key){
        return prefs.getString(key, "");
    }
    public int readInt(String key){
        return prefs.getInt(key, 0);
    }
    public long readLong(String key){ return prefs.getLong(key, 0L);}
    public float readFloat(String key){
        return prefs.getFloat(key, 0f);
    }
    public boolean readBool(String key){
        return prefs.getBoolean(key, false);
    }

    // Save values
    public boolean saveEntry(String key, Object value){
        try{
            if (value instanceof String) editor.putString(key, (String) value).apply();
            if (value instanceof Integer) editor.putInt(key, (Integer) value).apply();
            if (value instanceof Float) editor.putFloat(key, (Float) value).apply();
            if (value instanceof Long) editor.putLong(key, (Long) value).apply();
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value).apply();
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
