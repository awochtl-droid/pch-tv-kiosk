package com.pch.kiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Launches the kiosk app automatically when the Fire TV boots up.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
        }
    }
}
