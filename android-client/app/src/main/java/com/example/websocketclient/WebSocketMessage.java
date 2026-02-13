package com.example.websocketclient;

public class WebSocketMessage {
    
    private String type;
    private String content;
    private String sender;
    private String timestamp;
    private String message;
    private Integer client_count;
    
    public WebSocketMessage() {
    }
    
    public WebSocketMessage(String type, String content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Integer getClientCount() {
        return client_count;
    }
    
    public void setClientCount(Integer client_count) {
        this.client_count = client_count;
    }
    
    @Override
    public String toString() {
        if ("system".equals(type)) {
            return "[SYSTEM] " + message;
        } else if ("message".equals(type)) {
            return sender + ": " + content;
        } else if ("error".equals(type)) {
            return "[ERROR] " + message;
        }
        return content != null ? content : message;
    }
}