package io.okhi.android_background_geofencing.models;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.Serializable;
import java.util.Objects;

import io.okhi.android_background_geofencing.R;

public class BackgroundGeofencingNotification implements Serializable {
    private String title;
    private String text;
    private int channelImportance;
    private String channelId;
    private String channelName;
    private String channelDescription;
    private int notificationId;
    private int notificationRequestCode;
    private Intent intent;

    BackgroundGeofencingNotification() {}

    public BackgroundGeofencingNotification(
            @NonNull String title,
            @NonNull String text,
            @NonNull String channelId,
            @NonNull String channelName,
            @NonNull String channelDescription,
            int channelImportance
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.channelImportance = channelImportance;
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
            int notificationId,
            int notificationRequestCode,
            Intent intent
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.channelImportance = channelImportance;
        this.notificationId = notificationId;
        this.notificationRequestCode = notificationRequestCode;
        this.intent = intent;
    }

    public Notification getNotification(Context context, Boolean isError, Boolean isProgressing) {
        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationRequestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );

        }else {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationRequestCode,
                    intent,
                    0
            );
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(text);

        try {
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            int icon = bundle.getInt(Constant.FOREGROUND_NOTIFICATION_ICON_META_KEY);
            int color = bundle.getInt(Constant.FOREGROUND_NOTIFICATION_COLOR_META_KEY);

            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), icon);
            if (icon != 0) {
                builder.setSmallIcon(icon);

            } else {
                builder.setSmallIcon(R.drawable.ic_person_pin);
            }
            if (color != 0) {
                builder.setColor(context.getResources().getColor(color));
            }

            if(isError){
                builder.setColor(Color.argb(255, 255, 0, 0))
                        .setColorized(true)
                        .setLargeIcon(largeIcon);
            }

            if(isProgressing){
                builder.setColor(Color.argb(255, 255,165,0))
                        .setColorized(true)
                        .setLargeIcon(largeIcon);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.build();
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

