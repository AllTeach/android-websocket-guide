"""
WebSocket Server with Broadcasting
===================================

This server:
- Accepts multiple client connections
- Broadcasts messages from any client to all connected clients
- Handles connection/disconnection gracefully
- Runs on asyncio (single-threaded cooperative multitasking)

Architecture:
- Each client connection runs as a coroutine
- Shared 'connected_clients' set tracks all active connections
- Broadcasting iterates through all clients and sends messages
"""

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

# Global set to track all connected clients
# This is thread-safe in asyncio because it runs on a single thread
connected_clients: Set[websockets.WebSocketServerProtocol] = set()


class WebSocketServer:
    """
    WebSocket Server Manager
    
    Handles:
    - Client connections
    - Message broadcasting
    - Connection lifecycle
    """
    
    def __init__(self, host: str = "0.0.0.0", port: int = 8765):
        """
        Initialize server configuration
        
        Args:
            host: Server host (0.0.0.0 = all interfaces)
            port: Server port
        """
        self.host = host
        self.port = port
        self.connected_clients = connected_clients
    
    async def register_client(self, websocket: websockets.WebSocketServerProtocol):
        """
        Register a new client connection
        
        Args:
            websocket: The WebSocket connection object
        """
        self.connected_clients.add(websocket)
        client_count = len(self.connected_clients)
        logger.info(f"Client connected. Total clients: {client_count}")
        
        # Send welcome message to the new client
        welcome_msg = {
            "type": "system",
            "message": "Connected to server",
            "timestamp": datetime.now().isoformat(),
            "client_count": client_count
        }
        await websocket.send(json.dumps(welcome_msg))
        
        # Notify all other clients
        await self.broadcast({
            "type": "system",
            "message": f"New client joined. Total: {client_count}",
            "timestamp": datetime.now().isoformat()
        }, exclude=websocket)
    
    async def unregister_client(self, websocket: websockets.WebSocketServerProtocol):
        """
        Unregister a disconnected client
        
        Args:
            websocket: The WebSocket connection object
        """
        self.connected_clients.discard(websocket)
        client_count = len(self.connected_clients)
        logger.info(f"Client disconnected. Total clients: {client_count}")
        
        # Notify remaining clients
        await self.broadcast({
            "type": "system",
            "message": f"Client left. Total: {client_count}",
            "timestamp": datetime.now().isoformat()
        })
    
    async def broadcast(self, message: dict, exclude: websockets.WebSocketServerProtocol = None):
        """
        Broadcast a message to all connected clients
        
        Args:
            message: Dictionary to send (will be JSON encoded)
            exclude: Optional websocket to exclude from broadcast
        
        Technical Details:
        - Uses asyncio.gather() for concurrent sending
        - Handles disconnections gracefully
        - Non-blocking operation
        """
        if not self.connected_clients:
            logger.warning("No clients connected for broadcast")
            return
        
        # Convert message to JSON
        message_json = json.dumps(message)
        
        # Create list of send coroutines
        # We filter out the excluded client if specified
        send_tasks = []
        for client in self.connected_clients:
            if client != exclude:
                send_tasks.append(self.send_to_client(client, message_json))
        
        # Send to all clients concurrently
        if send_tasks:
            # gather() runs all coroutines concurrently
            # return_exceptions=True prevents one failure from stopping others
            results = await asyncio.gather(*send_tasks, return_exceptions=True)
            
            # Log any errors
            for i, result in enumerate(results):
                if isinstance(result, Exception):
                    logger.error(f"Broadcast error: {result}")
    
    async def send_to_client(self, websocket: websockets.WebSocketServerProtocol, message: str):
        """
        Send a message to a specific client with error handling
        
        Args:
            websocket: Target client connection
            message: JSON string to send
        """
        try:
            await websocket.send(message)
        except websockets.exceptions.ConnectionClosed:
            logger.warning("Attempted to send to closed connection")
            # Remove from connected clients
            self.connected_clients.discard(websocket)
        except Exception as e:
            logger.error(f"Error sending to client: {e}")
    
    async def handle_client(self, websocket: websockets.WebSocketServerProtocol, path: str):
        """
        Handle individual client connection
        
        This is the main handler coroutine for each connected client.
        It runs for the lifetime of the connection.
        
        Args:
            websocket: The client's WebSocket connection
            path: The connection path (e.g., "/")
        
        Flow:
        1. Register client
        2. Listen for messages in loop
        3. Broadcast received messages
        4. Unregister on disconnect
        """
        # Register the client
        await self.register_client(websocket)
        
        try:
            # Keep listening for messages until connection closes
            async for message in websocket:
                logger.info(f"Received message: {message}")
                
                try:
                    # Parse incoming message
                    data = json.loads(message)
                    
                    # Create broadcast message with metadata
                    broadcast_msg = {
                        "type": "message",
                        "content": data.get("content", ""),
                        "sender": data.get("sender", "anonymous"),
                        "timestamp": datetime.now().isoformat()
                    }
                    
                    # Broadcast to all clients (including sender)
                    await self.broadcast(broadcast_msg)
                    
                except json.JSONDecodeError:
                    logger.error(f"Invalid JSON received: {message}")
                    error_msg = {
                        "type": "error",
                        "message": "Invalid message format"
                    }
                    await websocket.send(json.dumps(error_msg))
                except Exception as e:
                    logger.error(f"Error processing message: {e}")
        
        except websockets.exceptions.ConnectionClosed:
            logger.info("Client connection closed normally")
        except Exception as e:
            logger.error(f"Error in client handler: {e}")
        finally:
            # Always unregister client on disconnect
            await self.unregister_client(websocket)
    
    async def start(self):
        """
        Start the WebSocket server
        
        Technical Details:
        - Creates a server socket
        - Listens for connections
        - Spawns handle_client coroutine for each connection
        - Runs until interrupted
        """
        logger.info(f"Starting WebSocket server on {self.host}:{self.port}")
        
        # Start server
        # serve() creates a server and handles accepting connections
        async with websockets.serve(
            self.handle_client,
            self.host,
            self.port,
            # Optional: configure ping/pong for connection keepalive
            ping_interval=30,  # Send ping every 30 seconds
            ping_timeout=10    # Wait 10 seconds for pong
        ):
            logger.info(f"Server running on ws://{self.host}:{self.port}")
            # Keep server running
            await asyncio.Future()  # Run forever


async def main():
    """
    Main entry point
    """
    server = WebSocketServer(host="0.0.0.0", port=8765)
    await server.start()


if __name__ == "__main__":
    try:
        # Run the async main function
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Server stopped by user")
    except Exception as e:
        logger.error(f"Server error: {e}")