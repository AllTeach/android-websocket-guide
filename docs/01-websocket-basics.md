# Part 1: WebSocket Basics

## What is WebSocket?

WebSocket is a **communication protocol** that provides **full-duplex** (two-way) communication between a client and server over a single, persistent TCP connection.

### Traditional HTTP vs WebSocket

#### HTTP Request-Response Model
```
Client: "Hey server, give me data"
Server: "Here's your data"
Connection closes.

Client: "Hey server, any new data?"
Server: "Here's new data"
Connection closes.

(Repeats constantly - inefficient!)
```

#### WebSocket Persistent Connection
```
Client: "Connect to me via WebSocket"
Server: "Connection established"

[Connection stays open]

Server: "Here's new data" (anytime)
Client: "Here's my data" (anytime)
Server: "More data for you"

(Both can send anytime - efficient!)
```

### Key Differences

| Feature | HTTP | WebSocket |
|---------|------|-----------|
| **Direction** | One-way (request-response) | Two-way (bidirectional) |
| **Connection** | Short-lived | Persistent |
| **Overhead** | High (headers every request) | Low (one handshake) |
| **Real-time** | Polling required | Native support |
| **Use case** | Loading web pages | Chat, gaming, live updates |

## WS vs WSS

### WS (WebSocket)
- Protocol: `ws://`
- **Unencrypted** connection
- Like HTTP
- Use for: Local development, testing

```
ws://localhost:8765
ws://192.168.1.100:8765
```

### WSS (WebSocket Secure)
- Protocol: `wss://`
- **Encrypted** with TLS/SSL
- Like HTTPS
- Use for: Production, public internet

```
wss://api.example.com/socket
```

**⚠️ Always use WSS in production!**

## WebSocket Lifecycle

### 1. Handshake (HTTP Upgrade)

```http
Client Request:
GET /chat HTTP/1.1
Host: server.example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13

Server Response:
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

### 2. Open Connection

```
✅ WebSocket connection established
Now both sides can send messages freely
```

### 3. Message Exchange

**Text Messages:**
```json
{"type": "message", "content": "Hello!"}
```

**Binary Messages:**
```
[Binary data like images, files]
```

**Control Messages:**
- **Ping**: "Are you still there?"
- **Pong**: "Yes, I'm here!"
- **Close**: "Goodbye"

### 4. Connection Close

Either side can initiate:

```
Client or Server: "Close connection"
Other side: "Acknowledged, closing"
Connection closed.
```

## Frame Structure

WebSocket messages are sent as **frames**:

```
┌─────────────────────────────────┐
│ FIN | Opcode | Mask | Length    │  Header
├─────────────────────────────────┤
│ Masking Key (if masked)         │
├─────────────────────────────────┤
│ Payload Data                    │  Your actual message
└─────────────────────────────────┘
```

**Opcode Types:**
- `0x1`: Text frame (UTF-8)
- `0x2`: Binary frame
- `0x8`: Close
- `0x9`: Ping
- `0xA`: Pong

## When to Use WebSocket

### ✅ Good Use Cases

1. **Chat Applications**
   - Real-time message delivery
   - Typing indicators
   - Online status

2. **Live Feeds**
   - Social media updates
   - News tickers
   - Stock prices

3. **Collaborative Tools**
   - Google Docs-like editing
   - Shared whiteboards
   - Multi-user games

4. **IoT & Sensors**
   - Real-time sensor data
   - Device control
   - Live monitoring

5. **Gaming**
   - Multiplayer games
   - Live scores
   - Player positions

### ❌ When NOT to Use WebSocket

1. **Simple REST APIs**
   - Fetching user profile
   - CRUD operations
   - One-time data requests
   → Use HTTP/REST

2. **File Downloads**
   - Large file transfers
   - Progress tracking (can use HTTP)
   → Use HTTP with range requests

3. **Public APIs**
   - Rate limiting needed
   - Caching important
   → Use HTTP/REST

## WebSocket vs Alternatives

### Server-Sent Events (SSE)
```
WebSocket: Two-way
SSE: One-way (server → client only)

Use SSE when:
- Only server pushes data
- Simpler implementation needed
- HTTP/2 available
```

### Long Polling
```
Client: "Any updates?"
Server: [Waits... waits...] "Here's an update"
Client: "Any more updates?"
[Repeat]

Problems:
- High latency
- Server resource waste
- Not truly real-time
```

### WebRTC
```
WebSocket: Server-mediated
WebRTC: Peer-to-peer

Use WebRTC when:
- Video/audio streaming
- Direct client-to-client
- Low latency critical
```

## Security Considerations

### 1. Origin Checking
```python
# Server should verify origin
if request.origin not in ALLOWED_ORIGINS:
    reject_connection()
```

### 2. Authentication
```javascript
// Send token on connect
const ws = new WebSocket('ws://server.com?token=abc123');
```

### 3. Input Validation
```python
# Always validate messages
try:
    data = json.loads(message)
    validate(data)
except:
    send_error()
```

### 4. Rate Limiting
```python
# Prevent spam
if message_count > MAX_PER_MINUTE:
    disconnect_client()
```

## Real-World Examples

### Example 1: WhatsApp Web
```
- Uses WebSocket for message delivery
- Encrypted (WSS)
- Reconnects automatically
- Shows "connecting..." when connection drops
```

### Example 2: Stock Trading Apps
```
- Real-time price updates via WebSocket
- Thousands of messages per second
- Low latency critical
```

### Example 3: Multiplayer Games
```
- Player position updates
- Game state synchronization
- Sub-100ms latency needed
```

## Summary

✅ **WebSocket** = Real-time, two-way communication
✅ **Persistent** connection (no repeated handshakes)
✅ **Low overhead** (minimal headers after handshake)
✅ **Best for**: Chat, live feeds, gaming, IoT

🔒 **Always use WSS in production**
🔒 **Validate all input**
🔒 **Implement authentication**

## Next Steps

Now that you understand WebSocket basics, let's build a server!

→ [Part 2: Python WebSocket Server](02-python-server.md)

## Quiz

Test your understanding:

1. What's the main difference between HTTP and WebSocket?
2. When should you use WSS instead of WS?
3. Can the server send messages to the client without a request?
4. Name 3 good use cases for WebSocket.
5. What happens during the WebSocket handshake?

**Answers:**
1. HTTP is request-response (one-way), WebSocket is bidirectional with persistent connection
2. WSS should be used in production for encrypted communication over public networks
3. Yes! WebSocket allows server to push messages anytime without client request
4. Chat apps, live feeds, collaborative tools, IoT sensors, multiplayer games
5. Client sends HTTP Upgrade request, server responds with 101 Switching Protocols, connection established