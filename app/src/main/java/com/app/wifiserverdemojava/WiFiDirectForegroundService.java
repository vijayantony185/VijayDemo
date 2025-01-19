package com.app.wifiserverdemojava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class WiFiDirectForegroundService extends Service {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private final IntentFilter intentFilter = new IntentFilter();

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Wi-Fi P2P components
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager != null ? manager.initialize(this, getMainLooper(), null) : null;

        // Add intent filters for Wi-Fi Direct actions
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Register the BroadcastReceiver
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, null); // Pass null for activity
        registerReceiver(receiver, intentFilter);

        // Start the foreground service with a notification
        startForeground(1, createNotification());
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel(
                "WiFiDirectService",
                "Wi-Fi Direct Service",
                NotificationManager.IMPORTANCE_LOW
        );
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        return new Notification.Builder(this, "WiFiDirectService")
                .setContentTitle("Wi-Fi Direct Service")
                .setContentText("Listening for Wi-Fi Direct events")
                .setSmallIcon(R.drawable.focus_foreground_listerner)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Ensures the service restarts if terminated
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used
    }
}
