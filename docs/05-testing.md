# Part 5: Testing & Debugging

## Introduction

Testing WebSocket applications requires special techniques because you're dealing with:
- Asynchronous operations
- Network connectivity
- Real-time communication
- Threading complexities

This guide covers testing strategies and debugging tools.

## Testing Strategies

### 1. Manual Testing with Test Client

The quickest way to test is using the HTML test client or command-line tools.

#### HTML Test Client

```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket Test Client</title>
</head>
<body>
    <h1>WebSocket Test Client</h1>
    <div id="status">Disconnected</div>
    <input id="serverUrl" value="ws://localhost:8765" />
    <button onclick="connect()">Connect</button>
    <button onclick="disconnect()">Disconnect</button>
    <br><br>
    <input id="messageInput" placeholder="Type message..." />
    <button onclick="sendMessage()">Send</button>
    <div id="messages"></div>

    <script>
        let ws = null;

        function connect() {
            const url = document.getElementById('serverUrl').value;
            ws = new WebSocket(url);

            ws.onopen = () => {
                document.getElementById('status').textContent = 'Connected';
                document.getElementById('status').style.color = 'green';
            };

            ws.onmessage = (event) => {
                const div = document.getElementById('messages');
                div.innerHTML += '<p>' + event.data + '</p>';
            };

            ws.onclose = () => {
                document.getElementById('status').textContent = 'Disconnected';
                document.getElementById('status').style.color = 'red';
            };

            ws.onerror = (error) => {
                console.error('WebSocket error:', error);
            };
        }

        function disconnect() {
            if (ws) {
                ws.close();
            }
        }

        function sendMessage() {
            const input = document.getElementById('messageInput');
            const message = {
                content: input.value,
                sender: 'WebClient'
            };
            ws.send(JSON.stringify(message));
            input.value = '';
        }
    </script>
</body>
</html>
```

## Debugging Techniques

### 1. Android Logcat

#### Viewing Logs in Android Studio

1. Open **Logcat** tab (bottom of screen)
2. Select your device/emulator
3. Filter by package: `com.example.websocketclient`
4. Filter by tag: `WebSocketManager`

#### Log Levels

```java
Log.v(TAG, "Verbose - detailed");
Log.d(TAG, "Debug - debugging");
Log.i(TAG, "Info - informational");
Log.w(TAG, "Warning - potential issue");
Log.e(TAG, "Error - something wrong");
```

## Common Issues and Solutions

### Issue 1: Connection Refused

**Symptoms:**
```
E/WebSocketManager: WebSocket error: java.net.ConnectException: Connection refused
```

**Solutions:**

1. **Server not running** - Check if server is running with `netstat -an | grep 8765`
2. **Wrong IP address** - Emulator: Use `10.0.2.2` not `localhost`
3. **Firewall blocking** - Add exception for port 8765

### Issue 2: CalledFromWrongThreadException

**Error:**
```
android.view.ViewRootImpl$CalledFromWrongThreadException
```

**Solution:**
```java
// Always use Handler for UI updates
mainHandler.post(() -> {
    textView.setText(message);
});
```

## Testing Checklist

### Server Testing
- [ ] Server starts without errors
- [ ] Server accepts connections
- [ ] Server receives messages
- [ ] Server broadcasts to all clients
- [ ] Server handles disconnections gracefully

### Android App Testing
- [ ] App connects to server
- [ ] Connection status updates correctly
- [ ] Can send messages
- [ ] Can receive messages
- [ ] Messages display correctly
- [ ] Can disconnect cleanly
- [ ] Handles connection errors

---

**Key Takeaways:**
✅ Test with multiple clients
✅ Use Logcat for Android debugging
✅ Monitor server logs for issues
✅ Check network connectivity
✅ Handle all error cases gracefully