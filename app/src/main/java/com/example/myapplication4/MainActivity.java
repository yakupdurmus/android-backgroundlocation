package com.example.myapplication4;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {

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

        //Location buttons
        backgroundLocationButton = findViewById(R.id.getBackgrounLocationButton);
        requestPermissionButton = findViewById(R.id.locationRequestButton);

        permissionManager = PermissionManager.getInstance(this);

        //First should be access location permission for foreground
        requestPermissionButton.setOnClickListener(v -> {
            if (!permissionManager.checkPermissions(foregroundLocationPermission)) {
                permissionManager.askPermissions(MainActivity.this, foregroundLocationPermission, 100);
            }
        });

        //Second should be access location permission fore background (allow always)
        backgroundLocationButton.setOnClickListener(v -> {
            if (!permissionManager.checkPermissions(backgroundLocationPermission)) {
                permissionManager.askPermissions(MainActivity.this, backgroundLocationPermission, 200);
            } else {
                if (permissionManager.isLocationEnabled()) {
                    Log.e("TAG","background location permission enabled");
                    startLocationWork();
                } else {
                    permissionManager.createLocationRequest();
                    Toast.makeText(MainActivity.this, "Location service not enabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionManager.handlePermissionResult(MainActivity.this, 100, permissions, grantResults)) {

            if (permissionManager.isLocationEnabled()) {
                Log.e("TAG","onRequestPermissionResult isLocationEnabled true");
                startLocationWork();
            } else {
                Log.e("TAG","onRequestPermissionResult isLocationEnabled false");
                //locationManager.createLocationResult();
                Toast.makeText(MainActivity.this, "Location service is not enabled", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void startLocationWork() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !foregroundServiceRunning()) {
            startForegroundService(serviceIntent);
        }else {
            Log.e("TAG","Main activity error");
        }
    }

        public  boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service:activityManager.getRunningServices(Integer.MAX_VALUE)){
            if(ForegroundService.class.getName().equals(service.service.getClassName())){
                return  true;
            }
        }

        return false;
    }

}