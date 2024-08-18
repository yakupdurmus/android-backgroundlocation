package com.example.myapplication4;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class PermissionManager {

    private static PermissionManager instance = null;
    private Context context;
    private LocationRequest locationRequest;

    private static final int REQUEST_CHECK_SETTINGS = 1000;

    private PermissionManager() {
    }

    public static PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager();
            instance.init(context);
        }
        return instance;
    }

    private void init(Context context) {
        this.context = context;
    }

    public boolean checkPermissions(String[] permissions) {
        int size = permissions.length;

        // For multiple permission request
        for (int i = 0; i < size; i++) {
            if (ContextCompat.checkSelfPermission(context, permissions[i]) == PermissionChecker.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    void askPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    boolean handlePermissionResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        boolean isAllPermissionsGranted = true;

        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "Permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    isAllPermissionsGranted = false;
                    Toast.makeText(activity, "Permission denied.", Toast.LENGTH_SHORT).show();
                    showPermissionRational(activity, requestCode, permissions, permissions[i]);
                    break;
                }
            }
        } else {
            isAllPermissionsGranted = false;
        }
        return isAllPermissionsGranted;
    }

    private void showPermissionRational(Activity activity, int requestCode, String[] permissions, String deniedPermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, deniedPermission)) {
                showMessageOKCancel("You need to allow access to the permission(s)!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    askPermissions(activity, permissions, requestCode);
                                }
                            }
                        });
            }
            return;
        }
    }

    void showMessageOKCancel(String msg, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(context)
                .setMessage(msg)
                .setPositiveButton("OK", onClickListener)
                .setNegativeButton("Cancel", onClickListener)
                .create()
                .show();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(context);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult((Activity) context, REQUEST_CHECK_SETTINGS);
                    } catch (Exception ex) {

                    }
                }

            }
        });

    }


    public boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {


            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException ex) {
                ex.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {

            locationProviders = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

}
