package com.pch.kiosk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Foreground service that watches for the kiosk activity and relaunches it
 * if it's not in the foreground. This survives the HOME button and reboots
 * (via BootReceiver starting this service).
 */
public class KioskService extends Service {

    private static final String CHANNEL_ID = "pch_kiosk_channel";
    private static final int CHECK_INTERVAL_MS = 1500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = true;

    private final Runnable checker = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            launchKiosk();
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Pink Champagne TV")
                .setContentText("Guest TV is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        } else {
            notification = new Notification.Builder(this)
                .setContentTitle("Pink Champagne TV")
                .setContentText("Guest TV is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        }
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        handler.removeCallbacks(checker);
        handler.postDelayed(checker, CHECK_INTERVAL_MS);
        return START_STICKY; // restart if killed
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(checker);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void launchKiosk() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "PCH Kiosk", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
