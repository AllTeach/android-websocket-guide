package com.example.websocketclient;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * WebSocket Manager
 * =================
 * 
 * Manages WebSocket connection with the server.
 * 
 * Threading Model:
 * ----------------
 * - WebSocketClient runs on a BACKGROUND THREAD (created by library)
 * - All callbacks (onOpen, onMessage, etc.) execute on BACKGROUND THREAD
 * - UI updates MUST be posted to the MAIN/UI THREAD using Handler
 * 
 * Usage:
 * ------
 * WebSocketManager manager = new WebSocketManager(serverUrl);
 * manager.setListener(new WebSocketListener() {
 *     @Override
 *     public void onMessageReceived(String message) {
 *         // This runs on UI thread - safe to update UI
 *         textView.setText(message);
 *     }
 * });
 * manager.connect();
 */
public class WebSocketManager {
    
    private static final String TAG = "WebSocketManager";
    
    private WebSocketClient webSocketClient;
    private String serverUrl;
    private WebSocketListener listener;
    
    // Handler for posting to main/UI thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private final Gson gson = new Gson();
    private boolean isConnected = false;
    
    /**
     * Listener interface for WebSocket events
     * All callbacks are delivered on the MAIN/UI THREAD
     */
    public interface WebSocketListener {
        void onConnected();
        void onMessageReceived(String message);
        void onDisconnected(String reason);
        void onError(String error);
    }
    
    public WebSocketManager(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }
    
    /**
     * Connect to WebSocket server
     * Non-blocking - spawns background thread
     */
    public void connect() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            Log.w(TAG, "Already connected");
            return;
        }
        
        try {
            URI serverUri = new URI(serverUrl);
            
            webSocketClient = new WebSocketClient(serverUri) {
                
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i(TAG, "WebSocket opened");
                    isConnected = true;
                    
                    // Post to UI thread
                    if (listener != null) {
                        mainHandler.post(() -> listener.onConnected());
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "Message received: " + message);
                    
                    // Post to UI thread
                    if (listener != null) {
                        mainHandler.post(() -> listener.onMessageReceived(message));
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "WebSocket closed: " + reason);
                    isConnected = false;
                    
                    String closeReason = remote ? "Server closed connection: " + reason 
                                                 : "Client closed connection";
                    
                    // Post to UI thread
                    if (listener != null) {
                        mainHandler.post(() -> listener.onDisconnected(closeReason));
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                    isConnected = false;
                    
                    String errorMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                    
                    // Post to UI thread
                    if (listener != null) {
                        mainHandler.post(() -> listener.onError(errorMsg));
                    }
                }
            };
            
            // Start connection on background thread
            webSocketClient.connect();
            Log.i(TAG, "Connecting to " + serverUrl);
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid server URL", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onError("Invalid server URL: " + e.getMessage()));
            }
        }
    }
    
    /**
     * Disconnect from WebSocket server
     */
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
            isConnected = false;
            Log.i(TAG, "Disconnected");
        }
    }
    
    /**
     * Send a text message to the server
     */
    public void sendMessage(String content, String sender) {
        if (!isConnected || webSocketClient == null || !webSocketClient.isOpen()) {
            Log.w(TAG, "Cannot send message: Not connected");
            if (listener != null) {
                mainHandler.post(() -> listener.onError("Not connected to server"));
            }
            return;
        }
        
        // Create JSON message
        JsonObject message = new JsonObject();
        message.addProperty("content", content);
        message.addProperty("sender", sender);
        
        String jsonMessage = gson.toJson(message);
        
        // Send message (non-blocking)
        webSocketClient.send(jsonMessage);
        Log.d(TAG, "Message sent: " + jsonMessage);
    }
    
    public boolean isConnected() {
        return isConnected && webSocketClient != null && webSocketClient.isOpen();
    }
    
    public String getServerUrl() {
        return serverUrl;
    }
}