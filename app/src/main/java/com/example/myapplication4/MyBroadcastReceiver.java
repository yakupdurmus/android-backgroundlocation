package com.example.myapplication4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class MyBroadcastReceiver extends BroadcastReceiver {


    public static final String PACKAGE = "com.example.myapplication4";
    public static final String ACTION_ACTIVITY_UPDATE = PACKAGE + ".ACTION_ACTIVITY_UPDATE";
    public static final String EXTRA_ACTIVITY_MESSAGE = PACKAGE + ".EXTRA_ACTIVITY_MESSAGE";

    public static final String ACTIVITY_TRANSITION_ACTION = "ACTIVITY_TRANSITION_ACTION";

    public static final String ACTIVITY_RECOGNITION_ACTION = "ACTIVITY_RECOGNITION_ACTION";


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.e("TAG", "Received action: " + action);
        String message = "";


        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.e("TAG", "boot completed");
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.e("TAG", "foreground service started");
                context.startForegroundService(serviceIntent);
            } else {
                Log.e("TAG", "foreground service not started");
            }
        }

        if (action.equals(ACTIVITY_TRANSITION_ACTION)) {
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    message += "Transition: " + getTransitionType(event.getTransitionType()) +
                            " for activity: " + getActivityString(event.getActivityType()) + "\n";
                }
            }
        } else if (action.equals(ACTIVITY_RECOGNITION_ACTION)) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                DetectedActivity mostProbableActivity = result.getMostProbableActivity();
                message = "Activity: " + getActivityString(mostProbableActivity.getType()) +
                        " Confidence: " + mostProbableActivity.getConfidence() + "%";


            }
        }

        Log.e("TAG", "Broadcast message " + message);
        if (!message.equals("")) {
            Intent localIntent = new Intent(ACTION_ACTIVITY_UPDATE);
            localIntent.putExtra(EXTRA_ACTIVITY_MESSAGE, message);
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        }
    }

    private String getTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

    private String getActivityString(int detectedActivityType) {
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.WALKING:
                return "Walking";
            default:
                return "Unknown";
        }
    }
}
