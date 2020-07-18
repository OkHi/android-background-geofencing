package io.okhi.okhiandroidbackgroundgeofencing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import io.okhi.android_background_geofencing.AndroidBackgroundGeofencing;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndroidBackgroundGeofencing.ping(getApplicationContext());
    }
}
