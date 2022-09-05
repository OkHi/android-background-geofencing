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
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

import io.okhi.android_background_geofencing.R;
import io.okhi.android_core.models.OkHiException;

public class BackgroundGeofencingNotification implements Serializable {
    private String title;
    private String text;
    private int channelImportance;
    private String channelId;
    private String channelName;
    private String channelDescription;
    private int notificationId;
    private int notificationRequestCode;

    BackgroundGeofencingNotification() {}

    public BackgroundGeofencingNotification(
            // Local Notifications
            @NonNull String title,
            @NonNull String text,
            @NonNull String channelId,
            @NonNull String channelName,
            @NonNull String channelDescription,
            int notificationRequestCode,
            int channelImportance
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.notificationRequestCode = notificationRequestCode;
        this.channelImportance = channelImportance;
    }

    public BackgroundGeofencingNotification(
            @NonNull String title,
            @NonNull String text,
            @NonNull String channelId,
            @NonNull String channelName,
            @NonNull String channelDescription,
            int channelImportance,
            int notificationId,
            int notificationRequestCode
    ) {
        this.title = title;
        this.text = text;
        this.channelId = channelId;
        this.channelName = channelName;
        this.channelDescription = channelDescription;
        this.channelImportance = channelImportance;
        this.notificationId = notificationId;
        this.notificationRequestCode = notificationRequestCode;
    }

    private NotificationCompat.Builder notificationBuilder(Context context){
        String packageName = context.getPackageName();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
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
            if (icon != 0) {
                builder.setSmallIcon(icon);
            } else {
                builder.setSmallIcon(R.drawable.ic_person_pin);
            }

            if(color != 0){
                builder.setColor(context.getResources().getColor(color));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder;
    }

    public Notification getNotification(Context context) {
        return notificationBuilder(context).build();
    }

    public Notification getNotification(Context context, int customNotificationColor) {
        NotificationCompat.Builder builder = notificationBuilder(context);

        try {
            builder.setColor(customNotificationColor)
                    .setColorized(true);

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

    public static void launchLocalNotification(
            BackgroundGeofencingNotification backgroundGeofencingNotification,
            int notificationColor,
            Context context
    ) throws OkHiException {
        try {
            backgroundGeofencingNotification.createNotificationChannel(context);
            Notification localNotification = backgroundGeofencingNotification.getNotification(context, notificationColor);
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(new Random().nextInt(), localNotification);

        } catch (Exception e){
            e.printStackTrace();
            throw new OkHiException(OkHiException.UNKNOWN_ERROR_CODE, OkHiException.UNKNOWN_ERROR_MESSAGE);
        }
    }
}
