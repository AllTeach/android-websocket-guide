# Part 4: Threading & Concurrency Deep Dive

## Introduction

Understanding threading is **critical** for Android WebSocket development. This section explains:
- How threads work in Android
- Why we need background threads for network operations
- How to safely communicate between threads
- Common threading pitfalls and solutions

## Android Threading Model

### The Two-Thread Rule

```
┌────────────────────────────────────────┐
│         MAIN/UI THREAD                 │
│  - Created when app starts             │
│  - Handles ALL UI updates              │
│  - Handles user interactions           │
│  - CANNOT do long operations           │
│  - CANNOT do network I/O               │
│  - Also called "UI Thread"             │
└────────────────────────────────────────┘
              │
              │ Must communicate via Handler
              │
┌────────────────────────────────────────┐
│       BACKGROUND THREADS               │
│  - Created as needed                   │
│  - Handle network operations           │
│  - Handle file I/O                     │
│  - Handle database operations          │
│  - CANNOT update UI directly           │
└────────────────────────────────────────┘
```

### Why This Separation?

**Main Thread Must Be Responsive:**
- Android requires UI updates within 16ms (60 FPS)
- If main thread blocked, UI freezes ("App Not Responding")
- User clicks don't register
- App appears broken

**Network Operations Are Slow:**
- Can take seconds to complete
- May fail or timeout
- Would freeze UI if run on main thread

## The ANR (Application Not Responding) Dialog

```
┌──────────────────────────────────────┐
│  ⚠️  App Name isn't responding       │
│                                      │
│  Do you want to close it?            │
│                                      │
│  [ Wait ]  [ Close ]                 │
└──────────────────────────────────────┘
```

**This happens when:**
- Main thread blocked for >5 seconds
- Most common cause: Network on main thread

## WebSocket Threading Model

### How Java-WebSocket Library Works

```java
// You call this on MAIN THREAD
webSocketClient.connect();
```

**What happens behind the scenes:**

```
MAIN THREAD:
  1. Your code calls connect()
  2. Library creates NEW THREAD
  3. Returns immediately (non-blocking)
  
BACKGROUND THREAD (created by library):
  4. Opens TCP socket
  5. Performs handshake
  6. Maintains connection
  7. Sends/receives data
  8. Calls your callbacks (onOpen, onMessage, etc.)
```

### Critical Insight: Callbacks Run on Background Thread

```java
webSocketClient = new WebSocketClient(serverUri) {
    @Override
    public void onMessage(String message) {
        // ⚠️ THIS RUNS ON BACKGROUND THREAD!
        // ❌ CANNOT update UI directly here
        
        // This will crash:
        textView.setText(message);  // CalledFromWrongThreadException
    }
};
```

## Handler: The Thread Bridge

### What is a Handler?

A **Handler** is Android's mechanism for posting tasks to a specific thread's message queue.

```
┌─────────────────────────────────────┐
│  BACKGROUND THREAD                  │
│  onMessage() called                 │
│       │                             │
│       └─> mainHandler.post(...)     │
└─────────────┬───────────────────────┘
              │
              │ Message posted to queue
              ▼
┌─────────────────────────────────────┐
│  MAIN THREAD                        │
│  Message Queue:                     │
│  [ Task 1 ]                         │
│  [ Task 2 ]                         │
│  [ Your Runnable ] ← Executed here  │
│       │                             │
│       └─> textView.setText(...)     │
│            ✅ SAFE! On main thread  │
└─────────────────────────────────────┘
```

## Summary

### Key Rules

1. **Main Thread:**
   - ✅ Update UI
   - ✅ Handle user input
   - ❌ Network operations
   - ❌ Long computations

2. **Background Thread:**
   - ✅ Network operations
   - ✅ File I/O
   - ✅ Database queries
   - ❌ Update UI directly

3. **Use Handler to bridge threads:**
   ```java
   mainHandler.post(() -> {
       // UI updates here
   });
   ```

→ [Part 5: Testing & Debugging](05-testing.md)