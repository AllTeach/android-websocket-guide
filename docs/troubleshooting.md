# Troubleshooting Guide

## Common Problems and Solutions

### Connection Issues

#### Problem: "Connection Refused"
**Error:** `java.net.ConnectException: Connection refused`

**Causes:**
1. Server is not running
2. Wrong IP address or port
3. Firewall blocking connection
4. Using wrong URL format

**Solutions:**
1. Verify server is running: `netstat -an | grep 8765`
2. Check IP address:
   - Emulator: Use `ws://10.0.2.2:8765` (NOT localhost)
   - Real device: Use computer's local IP like `ws://192.168.1.100:8765`
3. Disable firewall temporarily to test
4. Ensure URL starts with `ws://` or `wss://`

#### Problem: "Network on Main Thread"
**Error:** `android.os.NetworkOnMainThreadException`

**Cause:** Attempting network operations directly on the main thread

**Solution:**
The Java-WebSocket library handles threading automatically. Make sure you're using:
```java
webSocketClient.connect();  // This is non-blocking
```
NOT manual Socket operations on main thread.

### UI Update Issues

#### Problem: "CalledFromWrongThreadException"
**Error:** `android.view.ViewRootImpl$CalledFromWrongThreadException`

**Cause:** Updating UI from background thread

**Solution:**
Always use Handler to post UI updates:
```java
@Override
public void onMessage(String message) {
    mainHandler.post(() -> {
        textView.setText(message);
    });
}
```

### Message Issues

#### Problem: Messages not appearing in UI

**Debug Steps:**
1. Check if message is being sent (add log in sendMessage)
2. Check if message is received by server (check server logs)
3. Check if onMessage is called (add log)
4. Check if UI update is happening (add log in Handler.post)

**Common Cause:** Forgetting to post to main thread

#### Problem: JSON Parse Errors
**Error:** `com.google.gson.JsonSyntaxException`

**Cause:** Server sending non-JSON data or wrong format

**Solution:**
Wrap JSON parsing in try-catch:
```java
@Override
public void onMessage(String message) {
    try {
        WebSocketMessage msg = gson.fromJson(message, WebSocketMessage.class);
        // Process
    } catch (JsonSyntaxException e) {
        Log.e(TAG, "Invalid JSON: " + message, e);
    }
}
```

### Server Issues

#### Problem: Server crashes on client disconnect

**Cause:** Not handling disconnections properly

**Solution:**
Use try-catch in Python server:
```python
try:
    async for message in websocket:
        # Handle message
except websockets.exceptions.ConnectionClosed:
    pass  # Normal disconnection
finally:
    await self.unregister_client(websocket)
```

### Permission Issues

#### Problem: App crashes immediately
**Error:** `SecurityException: Permission denied`

**Cause:** Missing INTERNET permission

**Solution:**
Add to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Build Issues

#### Problem: "Cannot resolve symbol WebSocketClient"

**Cause:** Missing dependency

**Solution:**
Add to app/build.gradle:
```gradle
dependencies {
    implementation 'org.java-websocket:Java-WebSocket:1.5.3'
}
```

Then click "Sync Now"

## Debugging Checklist

When things aren't working:

### Server Side
- [ ] Server is running (`python websocket_server.py`)
- [ ] No errors in server console
- [ ] Server is listening on correct port
- [ ] Firewall allows connections

### Android Side
- [ ] INTERNET permission in manifest
- [ ] Dependencies synced
- [ ] Correct server URL (check IP and port)
- [ ] Handler created with main looper
- [ ] All UI updates posted to Handler
- [ ] Checking Logcat for errors

### Network
- [ ] Computer and device on same WiFi
- [ ] Can ping server from device
- [ ] Firewall not blocking port 8765
- [ ] Using correct URL format (ws:// not http://)

## Getting Help

If you're still stuck:

1. **Check Logcat** - Most errors show up here
2. **Check server logs** - See what server receives
3. **Test with HTML client** - Isolate if it's Android-specific
4. **Simplify** - Remove features until basic connection works
5. **Compare with working code** - Check against repository examples

## Quick Reference

### Server Commands
```bash
# Start server
python websocket_server.py

# Check if port is in use
netstat -an | grep 8765
lsof -i :8765

# Kill process on port (if needed)
kill -9 $(lsof -t -i:8765)
```

### Android Logcat Filters
```bash
# View all logs for your app
adb logcat | grep "com.example.websocketclient"

# View only WebSocket logs
adb logcat | grep "WebSocketManager"

# View only errors
adb logcat *:E
```

### Testing URLs
- Emulator: `ws://10.0.2.2:8765`
- Same machine: `ws://localhost:8765`
- Local network: `ws://192.168.1.100:8765` (use your IP)
- Public server: `ws://yourserver.com:8765`

---

**Still having issues?** Review the documentation:
- [WebSocket Basics](01-websocket-basics.md)
- [Python Server Setup](02-python-server.md)
- [Android Client](03-android-client.md)
- [Threading Guide](04-threading.md)
- [Testing Guide](05-testing.md)