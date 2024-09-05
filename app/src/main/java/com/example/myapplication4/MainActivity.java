package com.example.myapplication4;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    Button requestPermissionButton, backgroundLocationButton, getRequestPermissionButton, getActivityRequest;
    TextView activityText;
    private MyBroadcastReceiver receiver;

    private boolean runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    private BroadcastReceiver activityUpdateReceiver;


    private final String[] foregroundLocationPermission =
            {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
    private final String[] backgroundLocationPermission =
            {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };

    private final String[] notificationPermission = {
            Manifest.permission.POST_NOTIFICATIONS
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

        //Notification permission request
        getRequestPermissionButton = findViewById(R.id.getNotificationPermissionButton);

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
                    Log.e("TAG", "background location permission enabled");
                    startLocationWork();
                } else {
                    permissionManager.createLocationRequest();
                    Toast.makeText(MainActivity.this, "Location service not enabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Get notification permission request
        getRequestPermissionButton.setOnClickListener(view -> {
            if (!permissionManager.checkPermissions(notificationPermission)) {
                permissionManager.askPermissions(MainActivity.this, notificationPermission, 300);
            }
        });


        getActivityRequest = findViewById(R.id.getRequestActicity);
        activityText = findViewById(R.id.textView);
        receiver = new MyBroadcastReceiver();
        registerReceiver(receiver, new IntentFilter("ACTIVITY_RECOGNITION_DATA"));


        getActivityRequest.setOnClickListener(view -> {
            String[] activity = {Manifest.permission.ACTIVITY_RECOGNITION};
            String[] foreground = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            String[] background = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};


            boolean perm1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PermissionChecker.PERMISSION_DENIED && runningQOrLater;
            boolean perm2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_DENIED;
            boolean perm3 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_DENIED;
            boolean perm4 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PermissionChecker.PERMISSION_DENIED;


            if (perm2 || perm3) {
                Log.e("tag", "2");
                ActivityCompat.requestPermissions(this, foreground, 200);
            } else if (perm4) {
                Log.e("tag", "3");
                ActivityCompat.requestPermissions(this, background, 300);
            } else if (perm1) {
                Log.e("tag", "1");
                ActivityCompat.requestPermissions(this, activity, 100);
            } else {
                Log.e("TAG", "permission ok");


                requestActivityRecognitionUpdates();
            }

        });

        activityUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MyBroadcastReceiver.ACTION_ACTIVITY_UPDATE)) {
                    String message = intent.getStringExtra(MyBroadcastReceiver.EXTRA_ACTIVITY_MESSAGE);
                    activityText.setText(message);
                }
            }
        };

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionManager.handlePermissionResult(MainActivity.this, 100, permissions, grantResults)) {

            if (permissionManager.isLocationEnabled()) {
                Log.e("TAG", "onRequestPermissionResult isLocationEnabled true");
                startLocationWork();
            } else {
                Log.e("TAG", "onRequestPermissionResult isLocationEnabled false");
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
        } else {
            Log.e("TAG", "Main activity error");
        }
    }

    public boolean foregroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (ForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the local broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                activityUpdateReceiver,
                new IntentFilter(MyBroadcastReceiver.ACTION_ACTIVITY_UPDATE)
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the local broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(activityUpdateReceiver);
    }


    private ActivityTransitionRequest setTransistor() {
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());


        return new ActivityTransitionRequest(transitions);


    }

    private void requestActivityRecognitionUpdates() {
        Intent transitionIntent = new Intent(this, MyBroadcastReceiver.class);
        transitionIntent.setAction("ACTIVITY_TRANSITION_ACTION");
        PendingIntent transitionPendingIntent = PendingIntent.getBroadcast(this, 0, transitionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        Intent recognitionIntent = new Intent(this, MyBroadcastReceiver.class);
        recognitionIntent.setAction("ACTIVITY_RECOGNITION_ACTION");
        PendingIntent recognitionPendingIntent = PendingIntent.getBroadcast(this, 1, recognitionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG", "permission error");
            return;
        }

        ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(setTransistor(), transitionPendingIntent)
                .addOnSuccessListener(result -> Log.e("TEST", "Activity transition update request successful"))
                .addOnFailureListener(e -> Log.e("TEST", "Failed to request activity transition updates", e));

        ActivityRecognition.getClient(this)
                .requestActivityUpdates(5000, recognitionPendingIntent)
                .addOnSuccessListener(result -> Log.e("TEST", "Activity recognition update request successful"))
                .addOnFailureListener(e -> Log.e("TEST", "Failed to request activity recognition updates", e));
    }

}