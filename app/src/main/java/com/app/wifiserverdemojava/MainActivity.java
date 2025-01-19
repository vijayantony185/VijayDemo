package com.app.wifiserverdemojava;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.widget.TextView;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements org.vosk.android.RecognitionListener {

    private TextView tvResponse;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private SpeechRecognizer speechRecognizer;
    private TextView tvAudioToText;
    private static final String TAG = "AudioToText";
    private final IntentFilter intentFilter = new IntentFilter();

    private SpeechService speechService;
    private SpeechStreamService speechStreamService;

    private Model model;

    @SuppressLint({"MissingPermission", "MissingInflatedId"})
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
        tvResponse = findViewById(R.id.tvResponse);
        tvAudioToText = findViewById(R.id.audioToText);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        channel = manager != null ? manager.initialize(this, getMainLooper(), null) : null;
        if (channel != null) {
            receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        }

        if (manager != null) {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("HomeActivity", "Discovering Peers Success");
                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.d("HomeActivity", "Discovering Peers failed " + reasonCode);
                }
            });
        }

        /*new Thread(() -> {
            receiveAudioFromClient(tvResponse);
        }).start();*/

        //receiveAudioNew(tvResponse);

        initModel();
        receiveAudioAndSave(tvResponse);

        // Initialize SpeechRecognizer
       // speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        //initializeSpeechRecognizer();

        // Start Speech Recognition

       /* // Start the foreground service
        Intent serviceIntent = new Intent(this, WiFiDirectForegroundService.class);
        ContextCompat.startForegroundService(this, serviceIntent);*/

         /*ServerManager.startServer();
        ServerManager.setActivityContext(this);
        ServerManager.getReceivedMessages().observe(this, message -> {
            Log.d("MA","--->Received Message "+message);
            tvResponse.setText(message);
            ServerManager.processCommand(message);
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiver != null) {
            registerReceiver(receiver, intentFilter);
        }
        ServerManager.setActivityContext(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            View currentFocusView = getCurrentFocus();
            if (currentFocusView == null) {
                return super.dispatchKeyEvent(event);
            }

            Log.d("Focus", "Current Focus: " + currentFocusView.getId());

            int direction;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    direction = View.FOCUS_UP;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    direction = View.FOCUS_DOWN;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    direction = View.FOCUS_LEFT;
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    direction = View.FOCUS_RIGHT;
                    break;
                case KeyEvent.KEYCODE_ENTER:
                    currentFocusView.performClick();
                    Log.d("Focus", "Enter Key Pressed");
                    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }

            View nextFocusView = currentFocusView.focusSearch(direction);
            if (nextFocusView != null) {
                Log.d("Focus", "Next Focus: " + nextFocusView.getId());
                nextFocusView.setForeground(ContextCompat.getDrawable(this, R.drawable.focus_foreground_listerner));
                nextFocusView.requestFocus();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

      /*  // Stop the foreground service
        Intent serviceIntent = new Intent(this, WiFiDirectForegroundService.class);
        stopService(serviceIntent);*/
    }

    private void receiveAudioFromClient(TextView statusTextView) {
        ServerSocket serverSocket = null;
        try {
        final int sampleRate = 44100;
        final int serverPort = 9999;
        final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        final int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        AudioTrack audioTrack = null;

        serverSocket = new ServerSocket(serverPort);
        Socket clientSocket = serverSocket.accept();
        InputStream inputStream = clientSocket.getInputStream();
        try {
            runOnUiThread(() -> statusTextView.setText("Waiting for connection..."));

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
            );
            audioTrack.play();

            byte[] buffer = new byte[bufferSize];
            runOnUiThread(() -> statusTextView.setText("Streaming audio from client..."));

            while (true) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    throw new Exception("Client disconnected.");
                }
                //audioTrack.write(buffer, 0, bytesRead);
                startSpeechRecognition();
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> statusTextView.setText("Error: " + e.getMessage()));
        } finally {
            try {
                if (audioTrack != null) {
                    audioTrack.stop();
                    audioTrack.release();
                }
                if (inputStream != null) inputStream.close();
                if (clientSocket != null) clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        }catch (Exception e){
            e.printStackTrace();
            runOnUiThread(() -> statusTextView.setText("Server error: " + e.getMessage()));
        } finally {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void receiveAudioNew(TextView statusTextView){
        new Thread(() -> {
            final int sampleRate = 44100;
            final int serverPort = 9999;
            final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            final int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(serverPort); // ServerSocket remains open
                runOnUiThread(() -> statusTextView.setText("Server is ready. Waiting for connections..."));

                while (true) {
                    Socket clientSocket = null;
                    InputStream inputStream = null;
                    AudioTrack audioTrack = null;

                    try {
                        clientSocket = serverSocket.accept(); // Accept a new client
                        inputStream = clientSocket.getInputStream();

                        audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                channelConfig,
                                audioFormat,
                                bufferSize,
                                AudioTrack.MODE_STREAM
                        );
                        audioTrack.play();

                        byte[] buffer = new byte[bufferSize];
                        runOnUiThread(() -> {
                            statusTextView.setText("Streaming audio from client...");
                            startSpeechRecognition(); // Start Speech Recognition
                        });
                        while (true) {
                            int bytesRead = inputStream.read(buffer);
                            if (bytesRead == -1) {
                                throw new Exception("Client disconnected.");
                            }
                            //audioTrack.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> statusTextView.setText("Error: " + e.getMessage()));
                    } finally {
                        // Clean up client-related resources
                        try {
                            if (audioTrack != null) {
                                audioTrack.stop();
                                audioTrack.release();
                            }
                            if (inputStream != null) inputStream.close();
                            if (clientSocket != null) clientSocket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(() -> statusTextView.setText("Ready for a new connection..."));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> statusTextView.setText("Server error: " + e.getMessage()));
            } finally {
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvAudioToText.setText("Ready for speech...");
            }

            @Override
            public void onBeginningOfSpeech() {
                tvAudioToText.setText("Listening...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Handle volume changes if needed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Handle buffer (if raw audio data is needed)
            }

            @Override
            public void onEndOfSpeech() {
                tvAudioToText.setText("Processing speech...");
            }

            @Override
            public void onError(int error) {
                tvAudioToText.setText("Error: " + getErrorText(error));
                Log.e(TAG, "Speech recognition error: " + getErrorText(error));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    tvAudioToText.setText("Recognized: " + recognizedText);
                    Log.d(TAG, "Recognized text: " + recognizedText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Handle partial results
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Handle custom events
            }
        });
    }


    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    private void startSpeechRecognition(String audioFilePath) {
        runOnUiThread(() -> {
            try {
                // Convert PCM to WAV for SpeechRecognizer compatibility
                File wavFile = convertPcmToWav(audioFilePath);
                playWavFile(wavFile.getPath(), tvResponse);

                // Initialize SpeechRecognizer
                SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        tvAudioToText.setText("Ready for speech...");
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        tvAudioToText.setText("Listening...");
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {

                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {

                    }

                    @Override
                    public void onEndOfSpeech() {
                        tvAudioToText.setText("Processing speech...");
                    }

                    @Override
                    public void onError(int error) {
                        tvAudioToText.setText("Error: " + getErrorText(error));
                    }

                    @Override
                    public void onResults(Bundle results) {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            tvAudioToText.setText("Recognized: " + matches.get(0));
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {}

                    @Override
                    public void onEvent(int eventType, Bundle params) {}
                });

                // Start recognizing
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                intent.putExtra("audioUri", Uri.fromFile(wavFile));
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                speechRecognizer.startListening(intent);

            } catch (Exception e) {
                runOnUiThread(() ->  tvAudioToText.setText("Recognition error: " + e.getMessage()));
            }
        });
    }

    private File convertPcmToWav(String pcmFilePath) throws IOException {
        File pcmFile = new File(pcmFilePath);
        File wavFile = new File(pcmFile.getParent(), "streamed_audio.wav");

        try (FileInputStream pcmInputStream = new FileInputStream(pcmFile);
             FileOutputStream wavOutputStream = new FileOutputStream(wavFile)) {

            long pcmSize = pcmFile.length();
            long dataSize = pcmSize + 36;

            // Write WAV header
            wavOutputStream.write(new byte[]{
                    'R', 'I', 'F', 'F', // Chunk ID
                    (byte) (dataSize & 0xff), (byte) ((dataSize >> 8) & 0xff),
                    (byte) ((dataSize >> 16) & 0xff), (byte) ((dataSize >> 24) & 0xff), // Chunk Size
                    'W', 'A', 'V', 'E', // Format
                    'f', 'm', 't', ' ', // Subchunk1 ID
                    16, 0, 0, 0, // Subchunk1 Size
                    1, 0, // Audio Format (1 for PCM)
                    1, 0, // Number of Channels (1 for Mono)
                    (byte) (44100 & 0xff), (byte) ((44100 >> 8) & 0xff),
                    (byte) ((44100 >> 16) & 0xff), (byte) ((44100 >> 24) & 0xff), // Sample Rate
                    (byte) (88200 & 0xff), (byte) ((88200 >> 8) & 0xff),
                    (byte) ((88200 >> 16) & 0xff), (byte) ((88200 >> 24) & 0xff), // Byte Rate
                    2, 0, // Block Align
                    16, 0, // Bits per Sample
                    'd', 'a', 't', 'a', // Subchunk2 ID
                    (byte) (pcmSize & 0xff), (byte) ((pcmSize >> 8) & 0xff),
                    (byte) ((pcmSize >> 16) & 0xff), (byte) ((pcmSize >> 24) & 0xff)  // Subchunk2 Size
            });

            // Write PCM data
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = pcmInputStream.read(buffer)) != -1) {
                wavOutputStream.write(buffer, 0, bytesRead);
            }
        }

        return wavFile;
    }

    private void receiveAudioAndSave(TextView statusTextView) {
        new Thread(() -> {
            final int sampleRate = 44100;
            final int serverPort = 9999;
            final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            final int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(serverPort);
                runOnUiThread(() -> statusTextView.setText("Server is ready. Waiting for connections..."));

                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         InputStream inputStream = clientSocket.getInputStream();
                         FileOutputStream fileOutputStream = new FileOutputStream(new File(getFilesDir(), "streamed_audio.pcm"))) {

                        runOnUiThread(() -> statusTextView.setText("Receiving audio from client..."));

                        byte[] buffer = new byte[bufferSize];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }

                        runOnUiThread(() -> statusTextView.setText("Audio saved. Starting recognition..."));
                       // startSpeechRecognition(new File(getFilesDir(), "streamed_audio.pcm").getAbsolutePath());
                        recognizeFile(new File(getFilesDir(), "streamed_audio.pcm").getAbsolutePath());

                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> statusTextView.setText("Error: " + e.getMessage()));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> statusTextView.setText("Server error: " + e.getMessage()));
            }
        }).start();
    }

    private void playWavFile(String wavFilePath, TextView statusTextView) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(wavFilePath);
            mediaPlayer.prepare(); // Prepare the MediaPlayer
            mediaPlayer.start();   // Start playing
            statusTextView.setText("Playing audio...");

            mediaPlayer.setOnCompletionListener(mp -> {
                statusTextView.setText("Audio playback completed.");
                mediaPlayer.release(); // Release resources after playback
            });
        } catch (IOException e) {
            e.printStackTrace();
            statusTextView.setText("Error playing audio: " + e.getMessage());
        }
    }

    @Override
    public void onResult(String hypothesis) {
        tvAudioToText.append(hypothesis + "\n");
    }

    @Override
    public void onFinalResult(String hypothesis) {
        tvAudioToText.append(hypothesis + "\n");
        Toast.makeText(this, ""+hypothesis,Toast.LENGTH_LONG).show();
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        tvAudioToText.append(hypothesis + "\n");
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {

    }

    private void initModel() {
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }

    private void recognizeFile(String audioFilePath) {
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        } else {
            try {
                // Initialize recognizer

               // Recognizer rec = new Recognizer(model, 16000.f);
                 Recognizer rec = new Recognizer(model, 16000.f, "[\"one zero zero zero one\", " +
                        "\"oh zero one two three four five six seven eight nine\", \"[unk]\"]");
                String result = rec.getFinalResult();
                Log.d("Res","--------------------->"+result);

                // Convert PCM file to WAV
                File wavFile = convertPcmToWav(audioFilePath); // replace with your PCM file path

                // Open the WAV file as an InputStream
                InputStream ais = new FileInputStream(wavFile);

                // Skip the WAV header (44 bytes)
                if (ais.skip(44) != 44) throw new IOException("File too short");

                // Create and start the SpeechStreamService
                speechStreamService = new SpeechStreamService(rec, ais, 16000);
                speechStreamService.start(this);

            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

    private void setErrorState(String message) {
        tvAudioToText.setText(message);
    }
}