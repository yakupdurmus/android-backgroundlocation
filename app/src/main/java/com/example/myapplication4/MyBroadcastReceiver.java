package com.example.myapplication4;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Log.e("TAG","boot completed");
            Intent serviceIntent = new Intent(context,ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.e("TAG","foreground service started");
                context.startForegroundService(serviceIntent);
            }else{
                Log.e("TAG","foreground service not started");
            }
        }else{
            Log.e("TAG","boot not completed");
        }

    }
}
