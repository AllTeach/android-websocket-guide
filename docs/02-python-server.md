# Part 2: Python WebSocket Server Deep Dive

## Introduction

In this section, we'll build a complete WebSocket server using Python's `asyncio` and the `websockets` library. This server will handle multiple concurrent connections and broadcast messages to all connected clients.

## Why Python and asyncio?

### Benefits of Python for WebSocket Servers

1. **Simple, readable syntax** - Easy to understand and maintain
2. **Rich ecosystem** - Many libraries available
3. **asyncio** - Native async/await support (Python 3.7+)
4. **Cross-platform** - Runs on Windows, macOS, Linux

### Why asyncio?

**asyncio** is Python's built-in library for writing concurrent code using async/await syntax.

**Key Concept**: asyncio uses **cooperative multitasking** on a **single thread**.

```python
# Traditional multi-threading (multiple threads)
Thread 1: Handle Client A
Thread 2: Handle Client B
Thread 3: Handle Client C
# OS manages switching between threads

# asyncio (single thread)
Main Thread:
  - Handle Client A (yields when waiting)
  - Handle Client B (yields when waiting)
  - Handle Client C (yields when waiting)
# Program manages switching between tasks
```

## Installation

### Install Python 3.8+

Check your Python version:
```bash
python3 --version
# Should be 3.8 or higher
```

### Create Virtual Environment (Recommended)

```bash
# Create virtual environment
python3 -m venv venv

# Activate it
# On macOS/Linux:
source venv/bin/activate

# On Windows:
venv\Scripts\activate
```

### Install websockets Library

```bash
pip install websockets
```

Or use the requirements.txt:
```bash
pip install -r requirements.txt
```

## Server Architecture

```
┌─────────────────────────────────────────┐
│         AsyncIO Event Loop              │
│         (Single Thread)                 │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────┐  ┌──────────┐           │
│  │ Client 1 │  │ Client 2 │  ...      │
│  │Coroutine │  │Coroutine │           │
│  └────┬─────┘  └────┬─────┘           │
│       │             │                  │
│       └──────┬──────┘                  │
│              │                         │
│     ┌────────▼────────┐               │
│     │  connected_     │               │
│     │  clients: Set   │               │
│     └─────────────────┘               │
│                                         │
└─────────────────────────────────────────┘
```

## Code Walkthrough

### 1. Imports and Setup

```python
import asyncio
import websockets
import json
import logging
from datetime import datetime
from typing import Set

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)
```

**What's happening:**
- `asyncio`: Event loop and coroutines
- `websockets`: WebSocket protocol implementation
- `json`: Parse and create JSON messages
- `logging`: Track events and debug
- `datetime`: Timestamp messages
- `typing.Set`: Type hints for better code

### 2. Connected Clients Tracking

```python
connected_clients: Set[websockets.WebSocketServerProtocol] = set()
```

**Why a Set?**
- Fast add/remove operations: O(1)
- No duplicates automatically
- Easy to iterate over all clients

**Thread Safety:**
- Safe in asyncio (single thread)
- No locks needed
- All operations happen in event loop

### 3. WebSocketServer Class

```python
class WebSocketServer:
    def __init__(self, host: str = "0.0.0.0", port: int = 8765):
        self.host = host
        self.port = port
        self.connected_clients = connected_clients
```

**Host Options:**
- "0.0.0.0": Listen on all network interfaces (LAN + localhost)
- "127.0.0.1": Only localhost (can't connect from other devices)
- "192.168.1.100": Specific IP address

**Port:**
- `8765`: Common WebSocket development port
- Choose any unused port (1024-65535)

### 4. Client Registration

```python
async def register_client(self, websocket):
    self.connected_clients.add(websocket)
    client_count = len(self.connected_clients)
    
    # Send welcome message
    welcome_msg = {
        "type": "system",
        "message": "Connected to server",
        "timestamp": datetime.now().isoformat(),
        "client_count": client_count
    }
    await websocket.send(json.dumps(welcome_msg))
    
    # Notify others
    await self.broadcast({
        "type": "system",
        "message": f"New client joined. Total: {client_count}",
        "timestamp": datetime.now().isoformat()
    }, exclude=websocket)
```

**Key Points:**
- `await websocket.send()`: Yields control while sending
- `exclude=websocket`: Don't send "you joined" to yourself
- ISO timestamp: Standard format (YYYY-MM-DDTHH:MM:SS.ffffff)

### 5. Broadcasting Messages

```python
async def broadcast(self, message: dict, exclude=None):
    if not self.connected_clients:
        return
    
    message_json = json.dumps(message)
    
    send_tasks = []
    for client in self.connected_clients:
        if client != exclude:
            send_tasks.append(self.send_to_client(client, message_json))
    
    if send_tasks:
        # Send to all clients concurrently
        results = await asyncio.gather(*send_tasks, return_exceptions=True)
        
        # Log errors
        for result in results:
            if isinstance(result, Exception):
                logger.error(f"Broadcast error: {result}")
```

**asyncio.gather() Explained:**

```python
# Without gather (sequential - SLOW)
await send_to_client_1(msg)  # Wait
await send_to_client_2(msg)  # Wait
await send_to_client_3(msg)  # Wait
# Total: 3 x send_time

# With gather (concurrent - FAST)
await asyncio.gather(
    send_to_client_1(msg),
    send_to_client_2(msg),
    send_to_client_3(msg)
)
# Total: ~1 x send_time (all at once)
```

**return_exceptions=True:**
- If one send fails, others continue
- Errors returned as results, not raised
- Critical for robustness

### 6. Handling Individual Clients

```python
async def handle_client(self, websocket, path):
    await self.register_client(websocket)
    
    try:
        async for message in websocket:
            logger.info(f"Received: {message}")
            
            try:
                data = json.loads(message)
                
                broadcast_msg = {
                    "type": "message",
                    "content": data.get("content", ""),
                    "sender": data.get("sender", "anonymous"),
                    "timestamp": datetime.now().isoformat()
                }
                
                await self.broadcast(broadcast_msg)
                
            except json.JSONDecodeError:
                error_msg = {
                    "type": "error",
                    "message": "Invalid message format"
                }
                await websocket.send(json.dumps(error_msg))
    
    except websockets.exceptions.ConnectionClosed:
        logger.info("Client connection closed")
    finally:
        await self.unregister_client(websocket)
```

**async for message in websocket:**
- Loops until connection closes
- Yields control while waiting for messages
- Other clients get CPU time

**Error Handling:**
- `try/except`: Catch specific errors
- `finally`: Always cleanup (even if error)
- Send error messages back to client

### 7. Starting the Server

```python
async def start(self):
    logger.info(f"Starting server on {self.host}:{self.port}")
    
    async with websockets.serve(
        self.handle_client,
        self.host,
        self.port,
        ping_interval=30,
        ping_timeout=10
    ):
        logger.info(f"Server running on ws://{self.host}:{self.port}")
        await asyncio.Future()  # Run forever
```

**websockets.serve():**
- Creates server socket
- Accepts connections
- Spawns `handle_client()` for each connection

**Ping/Pong:**
- `ping_interval=30`: Send ping every 30 seconds
- `ping_timeout=10`: Wait 10 seconds for pong
- Detects dead connections
- Auto-closes if no response

**await asyncio.Future():**
- Never completes
- Keeps server running
- Until Ctrl+C or error

## Running the Server

### Start Server

```bash
cd python-server
python websocket_server.py
```

### Output

```
2026-02-13 10:00:00 - INFO - Starting WebSocket server on 0.0.0.0:8765
2026-02-13 10:00:00 - INFO - Server running on ws://0.0.0.0:8765
```

### Testing

Open `test_client.html` in browser or use:

```bash
# Install websocat
brew install websocat  # macOS
# or cargo install websocat

# Connect
websocat ws://localhost:8765

# Type messages
{"content": "Hello World", "sender": "TestUser"}
```

## Threading Deep Dive

### asyncio is NOT Multi-threading

```python
# This is what happens:

Main Thread (only thread):
  Event Loop:
    - Client 1 coroutine: await websocket.send()  ← yields
    - Client 2 coroutine: await websocket.send()  ← yields
    - Client 3 coroutine: runs while others wait
    - Client 1 resumes when send completes
```

### When Does asyncio Yield?

```python
# Yields control:
await websocket.send()      # Sending data
await websocket.recv()      # Receiving data
async for msg in websocket: # Waiting for messages
await asyncio.sleep()       # Sleeping

# Does NOT yield:
data = json.loads(message)  # CPU work
result = compute_hash(data) # CPU work
clients.add(websocket)      # Memory operation
```

### Blocking Operations Warning

```python
# ❌ BAD - Blocks event loop
import time
time.sleep(5)  # Blocks ALL clients for 5 seconds!

# ✅ GOOD - Yields control
await asyncio.sleep(5)  # Only this coroutine waits

# ❌ BAD - Blocking I/O
with open('file.txt') as f:
    data = f.read()  # Blocks event loop

# ✅ GOOD - Async I/O
import aiofiles
async with aiofiles.open('file.txt') as f:
    data = await f.read()  # Yields control
```

## Message Protocol

### Client → Server

```json
{
  "content": "Hello everyone!",
  "sender": "Alice"
}
```

### Server → Clients (Message)

```json
{
  "type": "message",
  "content": "Hello everyone!",
  "sender": "Alice",
  "timestamp": "2026-02-13T10:30:45.123456"
}
```

### Server → Clients (System)

```json
{
  "type": "system",
  "message": "New client joined. Total: 3",
  "timestamp": "2026-02-13T10:30:45.123456"
}
```

### Server → Client (Error)

```json
{
  "type": "error",
  "message": "Invalid message format"
}
```

## Performance Considerations

### How Many Clients Can It Handle?

**Single asyncio server can handle:**
- **Hundreds** of clients easily
- **Thousands** with optimization
- **Tens of thousands** with careful tuning

**Limitations:**
- Available memory (each connection = ~2-10 KB)
- Network bandwidth
- Message rate (messages/second)

### Scaling Strategies

**Vertical Scaling:**
```python
# Increase resources
- More RAM
- Faster CPU
- Better network
```

**Horizontal Scaling:**
```python
# Multiple servers + load balancer
Load Balancer
├─ Server 1 (1000 clients)
├─ Server 2 (1000 clients)
└─ Server 3 (1000 clients)

# Need Redis/RabbitMQ for cross-server messaging
```

## Security Best Practices

### 1. Origin Validation

```python
async def handle_client(self, websocket, path):
    origin = websocket.request_headers.get('Origin')
    
    if origin not in ['https://yourapp.com', 'https://www.yourapp.com']:
        await websocket.close(1008, "Invalid origin")
        return
```

### 2. Authentication

```python
async def handle_client(self, websocket, path):
    # Get token from query string
    # ws://server.com?token=abc123
    query = urllib.parse.urlparse(path).query
    params = urllib.parse.parse_qs(query)
    token = params.get('token', [None])[0]
    
    if not verify_token(token):
        await websocket.close(1008, "Invalid token")
        return
```

### 3. Rate Limiting

```python
from collections import defaultdict
import time

message_counts = defaultdict(list)

async def handle_client(self, websocket, path):
    client_id = id(websocket)
    
    async for message in websocket:
        now = time.time()
        message_counts[client_id] = [
            t for t in message_counts[client_id] 
            if now - t < 60  # Last 60 seconds
        ]
        
        if len(message_counts[client_id]) > 20:  # Max 20/min
            await websocket.send(json.dumps({
                "type": "error",
                "message": "Rate limit exceeded"
            }))
            continue
        
        message_counts[client_id].append(now)
        # Process message...
```

## Common Issues & Solutions

### Issue 1: Port Already in Use

```bash
# Error: Address already in use

# Solution 1: Kill process using port
lsof -ti:8765 | xargs kill -9

# Solution 2: Use different port
server = WebSocketServer(port=8766)
```

### Issue 2: Slow Broadcasting

```python
# ❌ SLOW - Sequential
for client in clients:
    await client.send(message)

# ✅ FAST - Concurrent
await asyncio.gather(*[
    client.send(message) for client in clients
])
```

### Issue 3: Memory Leak

```python
# ❌ PROBLEM - Clients never removed
connected_clients.add(websocket)
# Connection closes but never removed!

# ✅ SOLUTION - Always cleanup
try:
    # handle connection
finally:
    connected_clients.discard(websocket)
```

## Next Steps

Now that you understand the Python server, let's build the Android client!

→ [Part 3: Android Client Implementation](03-android-client.md)

## Exercises

1. **Add username tracking**: Store usernames for each connected client
2. **Add rooms/channels**: Allow clients to join different chat rooms
3. **Add message history**: Store last 100 messages and send to new clients
4. **Add private messages**: Route messages to specific users

[View Exercise Solutions](../exercises/)

---

**Key Takeaways:**
✅ asyncio = single-threaded cooperative multitasking
✅ Use `await` for I/O operations (yields control)
✅ Use `asyncio.gather()` for concurrent operations
✅ Always cleanup connections in `finally` block
✅ Implement security (origin, auth, rate limiting)