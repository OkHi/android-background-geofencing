package io.okhi.android_background_geofencing.models;

import android.content.Context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import okhttp3.Headers;

public class BackgroundGeofencingWebHook implements Serializable {
    private String url;
    private long timeout = Constant.DEFAULT_WEBHOOK_TIMEOUT;
    private HashMap<String, String> headers;
    private HashMap<String, Object> meta;

    BackgroundGeofencingWebHook() {}

    public BackgroundGeofencingWebHook(String url) {
        this.url = url;
    }

    public BackgroundGeofencingWebHook(String url, int timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    public BackgroundGeofencingWebHook(String url, int timeout, HashMap<String, String> headers) {
        this.url = url;
        this.timeout = timeout;
        this.headers = headers;
    }

    public BackgroundGeofencingWebHook(String url, int timeout, HashMap<String, String> headers, HashMap<String, Object> meta) {
        this.url = url;
        this.timeout = timeout;
        this.headers = headers;
        this.meta = meta;
    }

    public void save(Context context) {
        BackgroundGeofencingDB.saveWebHook(this, context);
    }

    public Headers getHeaders() {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null) {
            Iterator hIterator = headers.entrySet().iterator();
            while (hIterator.hasNext()) {
                Map.Entry mapElement = (Map.Entry)hIterator.next();
                String key = (String) mapElement.getKey();
                String value = (String) mapElement.getValue();
                if (key.toLowerCase().equals("content-type")) {
                    headers.remove(key);
                } else {
                    headerBuilder.add(key, value);
                }
            }
        }
        headerBuilder.add("Content-Type", "application/json");
        return headerBuilder.build();
    }

    public HashMap<String, Object> getMeta() {
        return meta;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getUrl() {
        return url;
    }
}
