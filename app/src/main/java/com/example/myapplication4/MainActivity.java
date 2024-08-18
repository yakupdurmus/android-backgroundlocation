package com.example.myapplication4;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.BackoffPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;


import android.content.pm.PackageManager;
import android.os.Build;

import android.view.View;


import androidx.activity.EdgeToEdge;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {

    public Activity activity;
    Button requestPermissionButton, backgroundLocationButton;
    private final String[] foregroundLocationPermission =
            {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
    private final String[] backgroundLocationPermission =
            {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };

    private PermissionManager permissionManager;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backgroundLocationButton = findViewById(R.id.getBackgrounLocationButton);
        requestPermissionButton = findViewById(R.id.locationRequestButton);

        permissionManager = PermissionManager.getInstance(this);
        locationManager = LocationManager.getInstance(this);

        requestPermissionButton.setOnClickListener(v -> {
            if (!permissionManager.checkPermissions(foregroundLocationPermission)) {
                permissionManager.askPermissions(MainActivity.this, foregroundLocationPermission, 100);
            }
        });

        backgroundLocationButton.setOnClickListener(v -> {
            if (!permissionManager.checkPermissions(backgroundLocationPermission)) {
                permissionManager.askPermissions(MainActivity.this, backgroundLocationPermission, 200);
            } else {
                if (locationManager.isLocationEnabled()) {
                    startLocationWork();
                } else {
                    locationManager.createLocationRequest();
                    Toast.makeText(MainActivity.this, "Location service not enabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionManager.handlePermissionResult(MainActivity.this, 100, permissions, grantResults)) {
            Log.e("TAG", "1");

            if (locationManager.isLocationEnabled()) {
                startLocationWork();
            } else {
                Log.e("TAG","onRequestPermissionsResult else");
                //locationManager.createLocationResult();
                Toast.makeText(MainActivity.this, "Location service is not enabled", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void startLocationWork() {
        OneTimeWorkRequest foregroundWorkRequest = new OneTimeWorkRequest.Builder(LocationWork.class)
                .addTag("LocationWork")
                .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(MainActivity.this).enqueue(foregroundWorkRequest);
    }


}