package io.okhi.android_background_geofencing.models;

import android.content.Context;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.okhi.android_background_geofencing.database.BackgroundGeofencingDB;
import okhttp3.Headers;

@SuppressWarnings("rawtypes")
public class BackgroundGeofencingWebHook implements Serializable {
    private String url;
    private long timeout = Constant.DEFAULT_WEBHOOK_TIMEOUT;
    private HashMap<String, String> headers;
    private JSONObject meta;
    private WebHookType webhookType = WebHookType.GEOFENCE;
    private String webHookRequest = WebHookRequest.POST.name();

    BackgroundGeofencingWebHook() {
    }

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

    public BackgroundGeofencingWebHook(String url, int timeout, HashMap<String, String> headers, JSONObject meta) {
        this.url = url;
        this.timeout = timeout;
        this.headers = headers;
        this.meta = meta;
    }

    public BackgroundGeofencingWebHook(String url, int timeout, HashMap<String, String> headers, JSONObject meta, WebHookType webhookType) {
        this.url = url;
        this.timeout = timeout;
        this.headers = headers;
        this.meta = meta;
        this.webhookType = webhookType;
    }

    public BackgroundGeofencingWebHook(String url, int timeout, HashMap<String, String> headers, JSONObject meta, WebHookType webhookType, WebHookRequest webHookRequest) {
        this.url = url;
        this.timeout = timeout;
        this.headers = headers;
        this.meta = meta;
        this.webhookType = webhookType;
        this.webHookRequest = webHookRequest.name();
    }

    public void save(Context context) {
        BackgroundGeofencingDB.saveWebHook(this, context);
    }

    public Headers getHeaders() {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null) {
            for (Map.Entry<String, String> stringStringEntry : headers.entrySet()) {
                String key = (String) ((Map.Entry) stringStringEntry).getKey();
                String value = (String) ((Map.Entry) stringStringEntry).getValue();
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

    public JSONObject getMeta() {
        return meta;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getUrl() {
        return url;
    }

    public String getUrl(String geofenceId) {
        if (url.contains("${id}")) {
            return url.replace("${id}", geofenceId);
        }
        return url;
    }

    public WebHookType getWebhookType() {
        return webhookType;
    }

    public WebHookRequest getWebHookRequest() {
        if (webHookRequest.equals(WebHookRequest.PATCH)) {
            return WebHookRequest.PATCH;
        }
        if (webHookRequest.equals(WebHookRequest.DELETE)) {
            return WebHookRequest.DELETE;
        }
        if (webHookRequest.equals(WebHookRequest.PUT)) {
            return WebHookRequest.PUT;
        }
        return WebHookRequest.POST;
    }
}
