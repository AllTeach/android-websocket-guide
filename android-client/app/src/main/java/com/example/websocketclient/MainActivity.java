package com.example.websocketclient;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    
    private EditText serverUrlInput;
    private Button connectButton;
    private Button disconnectButton;
    private TextView statusText;
    private TextView messagesText;
    private ScrollView messagesScrollView;
    private EditText messageInput;
    private Button sendButton;
    
    private WebSocketManager webSocketManager;
    private Gson gson = new Gson();
    
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        serverUrlInput = findViewById(R.id.serverUrlInput);
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        statusText = findViewById(R.id.statusText);
        messagesText = findViewById(R.id.messagesText);
        messagesScrollView = findViewById(R.id.messagesScrollView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        
        // Set default server URL - UPDATE THIS WITH YOUR IP
        serverUrlInput.setText("ws://192.168.1.100:8765");
    }
    
    private void setupListeners() {
        connectButton.setOnClickListener(v -> connectToServer());
        disconnectButton.setOnClickListener(v -> disconnectFromServer());
        sendButton.setOnClickListener(v -> sendMessage());
        
        // Send on Enter key
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    private void connectToServer() {
        String serverUrl = serverUrlInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(serverUrl)) {
            appendMessage("[ERROR] Please enter server URL");
            return;
        }
        
        if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
            appendMessage("[ERROR] URL must start with ws:// or wss://");
            return;
        }
        
        appendMessage("[INFO] Connecting to " + serverUrl + "...");
        
        webSocketManager = new WebSocketManager(serverUrl);
        webSocketManager.setListener(new WebSocketManager.WebSocketListener() {
            @Override
            public void onConnected() {
                statusText.setText("Connected");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                sendButton.setEnabled(true);
                serverUrlInput.setEnabled(false);
                appendMessage("[INFO] Connected to server");
            }
            
            @Override
            public void onMessageReceived(String message) {
                try {
                    WebSocketMessage wsMessage = gson.fromJson(message, WebSocketMessage.class);
                    
                    if ("message".equals(wsMessage.getType())) {
                        appendMessage(String.format("[%s] %s: %s",
                                formatTimestamp(wsMessage.getTimestamp()),
                                wsMessage.getSender(),
                                wsMessage.getContent()));
                    } else if ("system".equals(wsMessage.getType())) {
                        appendMessage(String.format("[SYSTEM] %s", wsMessage.getMessage()));
                    } else if ("error".equals(wsMessage.getType())) {
                        appendMessage(String.format("[ERROR] %s", wsMessage.getMessage()));
                    }
                } catch (Exception e) {
                    appendMessage("[ERROR] Failed to parse message: " + e.getMessage());
                }
            }
            
            @Override
            public void onDisconnected(String reason) {
                statusText.setText("Disconnected");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                sendButton.setEnabled(false);
                serverUrlInput.setEnabled(true);
                appendMessage("[INFO] Disconnected: " + reason);
            }
            
            @Override
            public void onError(String error) {
                appendMessage("[ERROR] " + error);
            }
        });
        
        webSocketManager.connect();
    }
    
    private void disconnectFromServer() {
        if (webSocketManager != null) {
            webSocketManager.disconnect();
            webSocketManager = null;
        }
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(message)) {
            return;
        }
        
        if (webSocketManager == null || !webSocketManager.isConnected()) {
            appendMessage("[ERROR] Not connected to server");
            return;
        }
        
        webSocketManager.sendMessage(message, "AndroidUser");
        messageInput.setText("");
    }
    
    private void appendMessage(String message) {
        String currentText = messagesText.getText().toString();
        String newText = currentText + "\n" + message;
        messagesText.setText(newText);
        
        // Auto-scroll to bottom
        messagesScrollView.post(() -> messagesScrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private String formatTimestamp(String isoTimestamp) {
        try {
            // Parse ISO format and convert to simple time
            return isoTimestamp.substring(11, 19);
        } catch (Exception e) {
            return timeFormat.format(new Date());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
    }
}