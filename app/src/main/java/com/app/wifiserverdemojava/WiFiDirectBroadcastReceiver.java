package com.app.wifiserverdemojava;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final Activity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Activity activity) {
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Log.d("WDBroadcastReceiver", "Action " + action);

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("WDBroadcastReceiver", "Wi-Fi P2P state enabled.");
                } else {
                    Log.d("WDBroadcastReceiver", "Wi-Fi P2P state disabled.");
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                if (manager != null) {
                    manager.requestPeers(channel, peers -> {

                    });
                }
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                if (manager != null) {
                    manager.requestConnectionInfo(channel, info -> {
                        if (info.groupFormed) {
                            Log.d("WDBroadcastReceiver", "Wi-Fi P2P group already formed.");
                            ServerManager.startServer();
                        } else {
                            Log.d("WDBroadcastReceiver", "Wi-Fi P2P group not formed. Attempting to create a group.");
                            manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    ServerManager.startServer();
                                    Log.d("Server", "Wi-Fi Direct group created successfully.");
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.e("Server", "Failed to create group: " + reason);
                                }
                            });
                        }
                    });
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // Handle this device's Wi-Fi state change if needed.
                break;

            default:
                Log.d("WDBroadcastReceiver", "Unhandled action: " + action);
                break;
        }
    }
}
