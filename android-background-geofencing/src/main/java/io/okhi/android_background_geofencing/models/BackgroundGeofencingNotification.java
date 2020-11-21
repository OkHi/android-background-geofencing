package io.okhi.android_background_geofencing.models;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.Serializable;
import java.util.Objects;

public class BackgroundGeofencingNotification implements Serializable {
    private String title;
    private String text;
    private int channelImportance;
    private String channelId;
    private String channelName;
    private String channelDescription;
    private int icon = 0;
    private int notificationId;

    BackgroundGeofencingNotification() {}

    public BackgroundGeofencingNotification(
            @NonNull String title,
            @NonNull String text,
            @NonNull String channelId,
            @NonNull String channelName,
            @NonNull String channelDescription,
            int channelImportance,
            int icon
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.channelImportance = channelImportance;
        this.icon = icon;
        this.notificationId = 1;
    }

    public BackgroundGeofencingNotification(
            @NonNull String title,
            @NonNull String text,
            @NonNull String channelId,
            @NonNull String channelName,
            @NonNull String channelDescription,
            int channelImportance,
            int icon,
            int notificationId
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.channelImportance = channelImportance;
        this.icon = icon;
        this.notificationId = notificationId;
    }

    public Notification getNotification(Context context) {
        return new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .build();
    }

    public void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channelId,
                    channelName,
                    channelImportance
            );
            serviceChannel.setDescription(channelDescription);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            Objects.requireNonNull(manager).createNotificationChannel(serviceChannel);
        }
    }

    public int getNotificationId() {
        return notificationId;
    }
}
