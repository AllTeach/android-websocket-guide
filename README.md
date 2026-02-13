# 🚀 WebSocket Tutorial: Python Server + Android Client

A comprehensive, student-friendly tutorial on building real-time messaging applications using WebSocket.

## 📖 What You'll Learn

- ✅ WebSocket protocol fundamentals (WS vs WSS)
- ✅ Build a Python WebSocket server with asyncio
- ✅ Create an Android Java client app
- ✅ Handle real-time message broadcasting
- ✅ Master threading in both Python and Android
- ✅ Debug and test WebSocket applications

## 🎯 What You'll Build

A **real-time messaging application** where:
- Multiple Android clients connect to a Python server
- Messages sent by any client are **broadcast to all clients**
- System notifications inform users of connections/disconnections
- Clean separation of concerns with a reusable `WebSocketManager` class

## 📋 Prerequisites

### Required Knowledge
- Basic Python programming
- Java fundamentals
- Android development basics
- Understanding of JSON

### Required Software
- **Python 3.8+** - [Download](https://www.python.org/downloads/)
- **Android Studio** - [Download](https://developer.android.com/studio)
- **Git** - [Download](https://git-scm.com/)

## 🚀 Quick Start (5 Minutes)

### 1️⃣ Clone Repository
```bash
git clone https://github.com/AllTeach/android-websocket-guide.git
cd android-websocket-guide
```

### 2️⃣ Start Python Server
```bash
cd python-server
pip install -r requirements.txt
python websocket_server.py
```

You should see:
```
2026-02-13 10:00:00 - INFO - Server running on ws://0.0.0.0:8765
```

### 3️⃣ Test with Browser
Open `python-server/test_client.html` in your browser to test the server.

### 4️⃣ Run Android App
1. Open `android-client` folder in Android Studio
2. Find your computer's IP address:
   ```bash
   # macOS/Linux
   ifconfig | grep inet
   
   # Windows
   ipconfig
   ```
3. Update `MainActivity.java` line 50 with your IP:
   ```java
   serverUrlInput.setText("ws://YOUR_IP_HERE:8765");
   ```
4. Run on device/emulator (must be on same WiFi network)

## 📚 Tutorial Sections

### [Part 1: WebSocket Basics](docs/01-websocket-basics.md) ⏱️ 15 min
Learn the fundamentals of WebSocket protocol, how it differs from HTTP, and when to use it.

**Topics:**
- What is WebSocket?
- WS vs WSS (encryption)
- WebSocket lifecycle
- Use cases

### [Part 2: Python Server Deep Dive](docs/02-python-server.md) ⏱️ 30 min
Build and understand the Python WebSocket server using asyncio.

**Topics:**
- Installing `websockets` library
- Asyncio event loop explained
- Handling multiple clients
- Broadcasting messages
- Connection management

### [Part 3: Android Client Implementation](docs/03-android-client.md) ⏱️ 40 min
Create the Android app with proper threading and UI updates.

**Topics:**
- Setting up Android project
- WebSocketManager class design
- Threading with Handler
- UI thread vs Background thread
- Receiving and sending messages

### [Part 4: Threading & Concurrency](docs/04-threading.md) ⏱️ 25 min
Master threading concepts in both Python and Android.

**Topics:**
- Python asyncio (cooperative multitasking)
- Android threading model
- Handler and Looper
- Thread-safe UI updates
- Common threading pitfalls

### [Part 5: Testing & Debugging](docs/05-testing.md) ⏱️ 20 min
Learn how to test and debug your WebSocket applications.

**Topics:**
- Testing with browser JavaScript
- Command-line tools (websocat)
- Android debugging with Logcat
- Network traffic inspection
- Common issues and solutions

## 🎓 Exercises

### Exercise 1: Username Validation ⭐☆☆☆☆
**Goal:** Prevent duplicate usernames

**Tasks:**
- [ ] Track connected usernames on server
- [ ] Reject duplicate username connections
- [ ] Send error message to client
- [ ] Show error in Android UI

[View Hints](exercises/ex1-hints.md) | [View Solution](exercises/ex1-solution.md)

### Exercise 2: Typing Indicator ⭐⭐☆☆☆
**Goal:** Show when users are typing

**Tasks:**
- [ ] Send "typing" event to server
- [ ] Broadcast typing status to other clients
- [ ] Show "User is typing..." in Android
- [ ] Clear indicator after 3 seconds

[View Hints](exercises/ex2-hints.md) | [View Solution](exercises/ex2-hints.md)

### Exercise 3: Private Messages ⭐⭐⭐☆☆
**Goal:** Implement direct messaging

**Tasks:**
- [ ] Add user list display in Android
- [ ] Send message with target username
- [ ] Route message to specific client on server
- [ ] Add "Direct Message" UI

[View Hints](exercises/ex3-hints.md) | [View Solution](exercises/ex3-hints.md)

### Exercise 4: Message History ⭐⭐⭐⭐☆
**Goal:** Persist and retrieve messages

**Tasks:**
- [ ] Add SQLite database to server
- [ ] Store all messages with timestamps
- [ ] Send last 50 messages to new clients
- [ ] Display history in Android app

[View Hints](exercises/ex4-hints.md) | [View Solution](exercises/ex4-hints.md)

### Exercise 5: Secure WebSocket (WSS) ⭐⭐⭐⭐⭐
**Goal:** Add SSL/TLS encryption

**Tasks:**
- [ ] Generate SSL certificate
- [ ] Configure server for WSS
- [ ] Update Android client for WSS
- [ ] Handle certificate validation

[View Hints](exercises/ex5-hints.md) | [View Solution](exercises/ex5-hints.md)

## 🐛 Troubleshooting

### Common Issues

#### ❌ "Connection refused" on Android
**Cause:** Wrong IP address or server not running

**Fix:**
```bash
# Verify server is running
netstat -an | grep 8765

# Use computer's actual IP, not localhost/127.0.0.1
ifconfig  # macOS/Linux
ipconfig  # Windows
```

#### ❌ "NetworkOnMainThreadException" in Android
**Cause:** Network operation on UI thread

**Fix:** Always use `Handler.post()` or background thread for network calls. Our `WebSocketManager` already handles this!

#### ❌ Messages not appearing in Android
**Cause:** Not updating UI on main thread

**Fix:** Ensure listener callbacks use `mainHandler.post()` (already implemented in our code)

#### ❌ "cleartext traffic not permitted"
**Cause:** Android blocks unencrypted HTTP/WS by default (Android 9+)

**Fix:** Add to `AndroidManifest.xml`:
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

[View All Troubleshooting Tips](docs/troubleshooting.md)

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────┐
│           Python WebSocket Server            │
│         (asyncio - single thread)            │
│                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Client 1 │  │ Client 2 │  │ Client 3 │  │
│  │Coroutine │  │Coroutine │  │Coroutine │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │             │              │         │
│       └─────────────┼──────────────┘         │
│                     │                        │
│           ┌─────────▼─────────┐             │
│           │  Broadcast Logic  │             │
│           └───────────────────┘             │
└─────────────────────────────────────────────┘
                      │
                      │ WebSocket (TCP)
                      │
        ┌─────────────┼─────────────┐
        │             │             │
   ┌────▼───┐   ┌────▼───┐   ┌────▼───┐
   │Android │   │Android │   │Android │
   │Client 1│   │Client 2│   │Client 3│
   └────────┘   └────────┘   └────────┘
```

## 🎨 Project Structure

```
android-websocket-guide/
├── README.md                          # You are here!
├── python-server/
│   ├── requirements.txt               # Python dependencies
│   ├── websocket_server.py           # Main server code
│   └── test_client.html              # Browser test client
├── android-client/
│   ├── app/
│   │   ├── build.gradle              # Dependencies
│   │   └── src/main/
│   │       ├── java/com/example/websocketclient/
│   │       │   ├── MainActivity.java
│   │       │   ├── WebSocketManager.java
│   │       │   └── WebSocketMessage.java
│   │       ├── res/layout/
│   │       │   └── activity_main.xml
│   │       └── AndroidManifest.xml
├── docs/
│   ├── 01-websocket-basics.md
│   ├── 02-python-server.md
│   ├── 03-android-client.md
│   ├── 04-threading.md
│   ├── 05-testing.md
│   └── troubleshooting.md
├── exercises/
│   ├── ex1-hints.md
│   ├── ex1-solution.md
│   └── ...
└── images/
    ├── banner.png
    └── architecture.png
```

## 💡 Key Concepts Explained

### Python asyncio (Single Thread)
```python
# Cooperative multitasking - NOT multi-threading
async def handle_client(websocket):
    async for message in websocket:  # Yields control while waiting
        await process(message)        # Other clients get CPU time
        await websocket.send(result)  # Non-blocking I/O
```

### Android Threading (Handler Pattern)
```java
// Background thread → UI thread
@Override
public void onMessage(String message) {
    // This runs on BACKGROUND THREAD
    mainHandler.post(() -> {
        // This runs on UI THREAD - safe to update UI!
        textView.setText(message);
    });
}
```

## 📖 Further Reading

- [WebSocket RFC 6455](https://tools.ietf.org/html/rfc6455) - Official specification
- [Python asyncio docs](https://docs.python.org/3/library/asyncio.html) - Deep dive into async
- [Android threading guide](https://developer.android.com/guide/background) - Official Android docs
- [websockets library docs](https://websockets.readthedocs.io/) - Python WebSocket library

## 🤝 Contributing

Found a bug or want to improve the tutorial? Contributions welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/improvement`)
3. Commit your changes (`git commit -am 'Add new exercise'`)
4. Push to the branch (`git push origin feature/improvement`)
5. Open a Pull Request

## 💬 Get Help

- **GitHub Discussions**: [Ask questions](https://github.com/AllTeach/android-websocket-guide/discussions)
- **Issues**: [Report bugs](https://github.com/AllTeach/android-websocket-guide/issues)

## 📜 License

MIT License - Free for educational use

Copyright (c) 2026 AllTeach

## 🌟 Support

If this tutorial helped you, please ⭐ star the repository!

---

**Happy coding! 🚀**

Built with ❤️ for students learning real-time communication