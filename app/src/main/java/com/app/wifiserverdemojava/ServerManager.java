package com.app.wifiserverdemojava;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerManager {
    private static ServerSocket serverSocket;
    private static boolean isServerRunning = false;
    private static final MutableLiveData<String> receivedMessages = new MutableLiveData<>();
    private static Activity activityContext;

    // Set the activity context
    public static void setActivityContext(Activity activity) {
        activityContext = activity;
    }

    // Start the server
    public static void startServer() {
        if (isServerRunning) return;

        isServerRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(8888);
                Log.d("ServerManager", "Server started on port 8888");

                while (isServerRunning) {
                    Socket clientSocket = serverSocket.accept();
                    if (clientSocket != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String message = reader.readLine();
                        reader.close();
                        clientSocket.close();

                        // Post the received message
                        receivedMessages.postValue(message);

                        // new Handler(Looper.getMainLooper()).post(() -> receivedMessages.setValue(message));
                    }
                }
            } catch (Exception e) {
                Log.e("ServerManager", "Server error: " + e.getMessage());
            }
        }).start();
    }

    // Stop the server
    public static void stopServer() {
        isServerRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            Log.e("ServerManager", "Error closing server socket: " + e.getMessage());
        }
        serverSocket = null;
        Log.d("ServerManager", "Server stopped.");
    }

    // Get live data of received messages
    public static LiveData<String> getReceivedMessages() {
        return receivedMessages;
    }

    // Process the command received
    public static void processCommand(String command) {
        if (activityContext == null) {
            Log.d("CommandProcessor", "Activity context is null");
            return;
        }

        KeyEvent keyEvent;
        switch (command.toLowerCase()) {
            case "up":
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
                break;
            case "down":
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case "left":
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case "right":
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case "enter":
                keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
                break;
            default:
                Log.d("CommandProcessor", "Unknown command: " + command);
                return;
        }

        activityContext.dispatchKeyEvent(keyEvent);
    }
}
