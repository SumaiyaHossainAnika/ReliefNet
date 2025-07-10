package com.reliefnet.network;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.util.DataSyncManager;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * WebSocketSyncManager - Handles real-time synchronization via WebSocket connections
 * Supports both server mode (for authorities) and client mode (for volunteers/survivors)
 */
public class WebSocketSyncManager {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketServer server;
    private WebSocketClient client;
    private final Map<WebSocket, String> connectedClients = new ConcurrentHashMap<>();
    private boolean isServerMode = false;
    private boolean isConnected = false;
    
    public WebSocketSyncManager() {
        // Constructor
    }
    
    /**
     * Start local WebSocket server (for authorities)
     */
    public void startLocalServer() {
        if (isServerMode) {
            return;
        }
        
        try {
            InetSocketAddress address = new InetSocketAddress(NetworkConfig.WEBSOCKET_PORT);
            server = new ReliefNetWebSocketServer(address);
            server.start();
            isServerMode = true;
            System.out.println("Started WebSocket server on port " + NetworkConfig.WEBSOCKET_PORT);
        } catch (Exception e) {
            System.err.println("Error starting WebSocket server: " + e.getMessage());
        }
    }
    
    /**
     * Connect to local WebSocket server (for volunteers/survivors)
     */
    public void connectToLocalServer(String serverAddress) {
        if (client != null && client.isOpen()) {
            return;
        }
        
        try {
            String serverUrl = "ws://" + serverAddress + ":" + NetworkConfig.WEBSOCKET_PORT + NetworkConfig.WEBSOCKET_PATH;
            URI serverUri = URI.create(serverUrl);
            client = new ReliefNetWebSocketClient(serverUri);
            client.connect();
            System.out.println("Connecting to WebSocket server: " + serverUrl);
        } catch (Exception e) {
            System.err.println("Error connecting to WebSocket server: " + e.getMessage());
        }
    }
    
    /**
     * Connect to cloud WebSocket service
     */
    public void connect() {
        // For cloud WebSocket connections
        try {
            String cloudUrl = "wss://reliefnet-sync.herokuapp.com/ws"; // Example cloud URL
            URI cloudUri = URI.create(cloudUrl);
            client = new ReliefNetWebSocketClient(cloudUri);
            client.connect();
            System.out.println("Connecting to cloud WebSocket service");
        } catch (Exception e) {
            System.err.println("Error connecting to cloud WebSocket: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect from WebSocket
     */
    public void disconnect() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
            if (server != null) {
                server.stop();
                server = null;
                isServerMode = false;
            }
            isConnected = false;
            System.out.println("WebSocket disconnected");
        } catch (Exception e) {
            System.err.println("Error disconnecting WebSocket: " + e.getMessage());
        }
    }
    
    /**
     * Send message via WebSocket
     */
    public boolean sendMessage(String messageId, String content, String channelId) {
        try {
            SyncMessage syncMessage = new SyncMessage();
            syncMessage.type = "MESSAGE";
            syncMessage.messageId = messageId;
            syncMessage.content = content;
            syncMessage.channelId = channelId;
            syncMessage.timestamp = System.currentTimeMillis();
            
            String jsonMessage = objectMapper.writeValueAsString(syncMessage);
            
            if (isServerMode && server != null) {
                // Broadcast to all connected clients
                for (WebSocket conn : connectedClients.keySet()) {
                    if (conn.isOpen()) {
                        conn.send(jsonMessage);
                    }
                }
                return true;
            } else if (client != null && client.isOpen()) {
                client.send(jsonMessage);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error sending WebSocket message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform sync operation
     */
    public void performSync() {
        if (isServerMode) {
            // Server broadcasts sync data to all clients
            broadcastSyncData();
        } else {
            // Client requests sync from server
            requestSyncFromServer();
        }
    }
    
    private void broadcastSyncData() {
        try {
            // Broadcast recent data changes to all connected clients
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Get recent messages
            String recentMessagesSQL = "SELECT * FROM messages WHERE sent_at >= datetime('now', '-1 hour') ORDER BY sent_at DESC LIMIT 50";
            try (java.sql.ResultSet rs = dbManager.executeQuery(recentMessagesSQL)) {
                while (rs.next()) {
                    SyncMessage syncMessage = new SyncMessage();
                    syncMessage.type = "SYNC_MESSAGE";
                    syncMessage.messageId = rs.getString("message_id");
                    syncMessage.content = rs.getString("content");
                    syncMessage.channelId = rs.getString("channel_id");
                    syncMessage.senderId = rs.getString("sender_id");
                    syncMessage.timestamp = rs.getTimestamp("sent_at").getTime();
                    
                    String jsonMessage = objectMapper.writeValueAsString(syncMessage);
                    
                    for (WebSocket conn : connectedClients.keySet()) {
                        if (conn.isOpen()) {
                            conn.send(jsonMessage);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error broadcasting sync data: " + e.getMessage());
        }
    }
    
    private void requestSyncFromServer() {
        try {
            SyncMessage syncRequest = new SyncMessage();
            syncRequest.type = "SYNC_REQUEST";
            syncRequest.timestamp = System.currentTimeMillis();
            
            String jsonMessage = objectMapper.writeValueAsString(syncRequest);
            
            if (client != null && client.isOpen()) {
                client.send(jsonMessage);
            }
        } catch (Exception e) {
            System.err.println("Error requesting sync from server: " + e.getMessage());
        }
    }
    
    private void handleIncomingMessage(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "MESSAGE":
                case "SYNC_MESSAGE":
                    handleSyncMessage(jsonNode);
                    break;
                case "SYNC_REQUEST":
                    if (isServerMode) {
                        broadcastSyncData();
                    }
                    break;
                case "EMERGENCY":
                    handleEmergencySync(jsonNode);
                    break;
                default:
                    System.out.println("Unknown WebSocket message type: " + type);
            }
            
        } catch (Exception e) {
            System.err.println("Error handling incoming WebSocket message: " + e.getMessage());
        }
    }
    
    private void handleSyncMessage(JsonNode jsonNode) {
        try {
            String messageId = jsonNode.get("messageId").asText();
            String content = jsonNode.get("content").asText();
            String channelId = jsonNode.get("channelId").asText();
            String senderId = jsonNode.has("senderId") ? jsonNode.get("senderId").asText() : "REMOTE_USER";
            
            // Insert message into local database if not already exists
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String insertSQL = "INSERT OR IGNORE INTO messages " +
                "(message_id, sender_id, content, message_type, channel_id, sent_at, sync_status) " +
                "VALUES (?, ?, ?, 'CHAT', ?, datetime('now'), 'SYNCED')";
            
            int rowsAffected = dbManager.executeUpdate(insertSQL, messageId, senderId, content, channelId);
            
            if (rowsAffected > 0) {
                System.out.println("Synced message from remote: " + messageId);
                // Notify UI to refresh
                DataSyncManager.getInstance().notifyCommunicationDataChanged();
            }
            
        } catch (Exception e) {
            System.err.println("Error handling sync message: " + e.getMessage());
        }
    }
    
    private void handleEmergencySync(JsonNode jsonNode) {
        try {
            // Handle emergency request synchronization
            String requestId = jsonNode.get("requestId").asText();
            String emergencyType = jsonNode.get("emergencyType").asText();
            String priority = jsonNode.get("priority").asText();
            
            // Sync emergency data
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String insertSQL = "INSERT OR IGNORE INTO emergency_requests " +
                "(request_id, requester_id, emergency_type, priority, location_lat, location_lng, " +
                "description, status, created_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), 'SYNCED')";
            
            dbManager.executeUpdate(insertSQL, requestId, jsonNode.get("requesterId").asText(),
                emergencyType, priority, jsonNode.get("latitude").asDouble(),
                jsonNode.get("longitude").asDouble(), jsonNode.get("description").asText(),
                jsonNode.get("status").asText());
            
            DataSyncManager.getInstance().notifyEmergencyDataChanged();
            
        } catch (Exception e) {
            System.err.println("Error handling emergency sync: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * WebSocket Server implementation for authorities
     */
    private class ReliefNetWebSocketServer extends WebSocketServer {
        
        public ReliefNetWebSocketServer(InetSocketAddress address) {
            super(address);
        }
        
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            connectedClients.put(conn, conn.getRemoteSocketAddress().getAddress().getHostAddress());
            System.out.println("Client connected: " + conn.getRemoteSocketAddress());
        }
        
        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            connectedClients.remove(conn);
            System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());
        }
        
        @Override
        public void onMessage(WebSocket conn, String message) {
            System.out.println("Received message from client: " + message);
            handleIncomingMessage(message);
        }
        
        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.err.println("WebSocket server error: " + ex.getMessage());
        }
        
        @Override
        public void onStart() {
            System.out.println("WebSocket server started on " + getAddress());
        }
    }
    
    /**
     * WebSocket Client implementation for volunteers/survivors
     */
    private class ReliefNetWebSocketClient extends WebSocketClient {
        
        public ReliefNetWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshake) {
            isConnected = true;
            System.out.println("Connected to WebSocket server");
        }
        
        @Override
        public void onMessage(String message) {
            System.out.println("Received message from server: " + message);
            handleIncomingMessage(message);
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            isConnected = false;
            System.out.println("Disconnected from WebSocket server: " + reason);
        }
        
        @Override
        public void onError(Exception ex) {
            System.err.println("WebSocket client error: " + ex.getMessage());
        }
    }
    
    /**
     * Message structure for WebSocket communication
     */
    private static class SyncMessage {
        public String type;
        public String messageId;
        public String content;
        public String channelId;
        public String senderId;
        public long timestamp;
        
        // For emergency messages
        public String requestId;
        public String emergencyType;
        public String priority;
        public double latitude;
        public double longitude;
        public String description;
        public String status;
        public String requesterId;
    }
}
