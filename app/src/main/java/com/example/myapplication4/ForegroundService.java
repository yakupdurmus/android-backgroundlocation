package com.example.myapplication4;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.http.NetworkException;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private boolean isRunning = true;
    private Thread backgroundThread;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        Log.e("TAG", "onStartCommand worked");


        startBackgroundThread();

        return START_STICKY;
    }

    private void startBackgroundThread() {
        backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                requestLocationUpdates();
            }
        });
        backgroundThread.start();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location lastLocation = null;
                if (locationResult == null) {
                    Log.e("TAG", "Location result null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.e("Location Update", "Lat: " + location.getLatitude() + "," + location.getLongitude());
                    lastLocation = location;
                }

                if (lastLocation != null) {
                    sendLocationData(lastLocation);
                }
            }
        };
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ForegroundService", "Location permission not granted");
            return;
        }

        int fiveMinutes = 300000;
        int twentySeconds = 5000;
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, twentySeconds)
                .setMinUpdateIntervalMillis(twentySeconds)
                .setMaxUpdateDelayMillis(fiveMinutes)
                .build();


        isRunning = true;
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Log.e("TAG", "notification builder");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Tracking your location")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy", "running");
        isRunning = false;
        if (backgroundThread != null) {
            backgroundThread.interrupt();
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

  private void sendLocationData(Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("text/plain");

                RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("name", "yakup")
                        .addFormDataPart("email", "yakuppdurmus@justlife.com")
                        .addFormDataPart("message", "Lat:" + location.getLatitude() + " Long" + location.getLongitude())
                        .build();

                Request request = new Request.Builder()
                        .url("https://www.yakupdurmus.com/YOUR_WEB_SERVICE.php")
                        .method("POST", body)
                        .build();

                try {

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {

                        Log.d("Response", "Success: " + response.body().string());
                    } else {

                        Log.e("Error", "Request failed: " + response.code());
                    }
                } catch (Exception e) {

                    Log.e("Error", "Request error: " + e.toString() + " " + e.getMessage());
                }
            }
        }).start();
    }


}