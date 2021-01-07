package io.okhi.android_background_geofencing.models;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
    private int notificationRequestCode;

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
        this.notificationRequestCode = 2;
    }

    public BackgroundGeofencingNotification(
            @NonNull String title,
            @NonNull String text,
            @NonNull String channelId,
            @NonNull String channelName,
            @NonNull String channelDescription,
            int channelImportance,
            int icon,
            int notificationId,
            int notificationRequestCode
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.channelImportance = channelImportance;
        this.icon = icon;
        this.notificationId = notificationId;
        this.notificationRequestCode = notificationRequestCode;
    }

    public Notification getNotification(Context context) {
        String packageName = context.getPackageName();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationRequestCode, intent, 0);
        return new NotificationCompat.Builder(context, channelId)
                .setContentIntent(pendingIntent)
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

    public int getNotificationRequestCode() {
        return notificationRequestCode;
    }
}
