package com.reliefnet.network;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.util.DataSyncManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MeshNetworkManager - Handles peer-to-peer mesh networking when internet is unavailable
 * Creates a local mesh network between ReliefNet devices within range
 */
public class MeshNetworkManager {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MeshPeer> connectedPeers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private String localNodeId;
    
    public MeshNetworkManager() {
        this.localNodeId = generateNodeId();
    }
    
    /**
     * Start the mesh network
     */
    public void start() {
        if (isRunning) {
            return;
        }
        
        try {
            serverSocket = new ServerSocket(NetworkConfig.MESH_PORT);
            isRunning = true;
            
            // Start accepting connections
            threadPool.execute(this::acceptConnections);
            
            // Start peer discovery
            threadPool.execute(this::discoverPeers);
            
            System.out.println("Mesh network started on port " + NetworkConfig.MESH_PORT + 
                             " with node ID: " + localNodeId);
            
        } catch (IOException e) {
            System.err.println("Error starting mesh network: " + e.getMessage());
            isRunning = false;
        }
    }
    
    /**
     * Stop the mesh network
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Disconnect from all peers
            for (MeshPeer peer : connectedPeers.values()) {
                peer.disconnect();
            }
            connectedPeers.clear();
            
            threadPool.shutdown();
            System.out.println("Mesh network stopped");
            
        } catch (IOException e) {
            System.err.println("Error stopping mesh network: " + e.getMessage());
        }
    }
    
    /**
     * Accept incoming connections from other nodes
     */
    private void acceptConnections() {
        while (isRunning && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                
                System.out.println("Mesh peer connected: " + clientAddress);
                
                // Create peer connection
                MeshPeer peer = new MeshPeer(clientSocket);
                threadPool.execute(peer);
                
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting mesh connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Discover and connect to nearby peers
     */
    private void discoverPeers() {
        while (isRunning) {
            try {
                // Broadcast discovery message on local network
                broadcastDiscovery();
                
                // Wait before next discovery attempt
                Thread.sleep(30000); // 30 seconds
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error during peer discovery: " + e.getMessage());
            }
        }
    }
    
    /**
     * Broadcast discovery message to find nearby ReliefNet nodes
     */
    private void broadcastDiscovery() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            
            String discoveryMessage = NetworkConfig.PEER_BROADCAST_MESSAGE + ":" + localNodeId + ":" + NetworkConfig.MESH_PORT;
            byte[] buffer = discoveryMessage.getBytes();
            
            // Broadcast to common network ranges
            String[] broadcastAddresses = {
                "192.168.1.255",
                "192.168.0.255", 
                "10.0.0.255",
                "172.16.255.255"
            };
            
            for (String broadcast : broadcastAddresses) {
                try {
                    InetAddress address = InetAddress.getByName(broadcast);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, NetworkConfig.PEER_DISCOVERY_PORT);
                    socket.send(packet);
                } catch (Exception e) {
                    // Ignore individual broadcast failures
                }
            }
            
            socket.close();
            
        } catch (Exception e) {
            System.err.println("Error broadcasting discovery: " + e.getMessage());
        }
    }
    
    /**
     * Connect to a discovered peer
     */
    public void connectToPeer(String peerAddress, int peerPort) {
        if (connectedPeers.containsKey(peerAddress)) {
            return; // Already connected
        }
        
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(peerAddress, peerPort), NetworkConfig.CONNECTION_TIMEOUT_MS);
            
            MeshPeer peer = new MeshPeer(socket);
            threadPool.execute(peer);
            
            System.out.println("Connected to mesh peer: " + peerAddress + ":" + peerPort);
            
        } catch (IOException e) {
            System.err.println("Error connecting to peer " + peerAddress + ": " + e.getMessage());
        }
    }
    
    /**
     * Send message through mesh network
     */
    public boolean sendMessage(String messageId, String content, String channelId) {
        if (connectedPeers.isEmpty()) {
            return false;
        }
        
        try {
            MeshMessage meshMessage = new MeshMessage();
            meshMessage.type = "MESSAGE";
            meshMessage.messageId = messageId;
            meshMessage.content = content;
            meshMessage.channelId = channelId;
            meshMessage.sourceNodeId = localNodeId;
            meshMessage.timestamp = System.currentTimeMillis();
            
            String jsonMessage = objectMapper.writeValueAsString(meshMessage);
            
            // Send to all connected peers
            for (MeshPeer peer : connectedPeers.values()) {
                peer.sendMessage(jsonMessage);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error sending mesh message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform mesh network sync
     */
    public void performSync() {
        if (connectedPeers.isEmpty()) {
            return;
        }
        
        try {
            // Send sync request to all peers
            MeshMessage syncRequest = new MeshMessage();
            syncRequest.type = "SYNC_REQUEST";
            syncRequest.sourceNodeId = localNodeId;
            syncRequest.timestamp = System.currentTimeMillis();
            
            String jsonMessage = objectMapper.writeValueAsString(syncRequest);
            
            for (MeshPeer peer : connectedPeers.values()) {
                peer.sendMessage(jsonMessage);
            }
            
        } catch (Exception e) {
            System.err.println("Error performing mesh sync: " + e.getMessage());
        }
    }
    
    private String generateNodeId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return "RELIEF_" + hostname + "_" + System.currentTimeMillis() % 10000;
        } catch (Exception e) {
            return "RELIEF_UNKNOWN_" + System.currentTimeMillis() % 10000;
        }
    }
    
    /**
     * Handle incoming mesh message
     */
    private void handleMeshMessage(String jsonMessage, MeshPeer fromPeer) {
        try {
            MeshMessage message = objectMapper.readValue(jsonMessage, MeshMessage.class);
            
            // Prevent message loops
            if (localNodeId.equals(message.sourceNodeId)) {
                return;
            }
            
            switch (message.type) {
                case "MESSAGE":
                    handleMessageSync(message);
                    break;
                case "SYNC_REQUEST":
                    handleSyncRequest(fromPeer);
                    break;
                case "SYNC_RESPONSE":
                    handleSyncResponse(message);
                    break;
                case "EMERGENCY":
                    handleEmergencySync(message);
                    break;
            }
            
            // Forward message to other peers (flooding algorithm)
            forwardMessage(jsonMessage, fromPeer);
            
        } catch (Exception e) {
            System.err.println("Error handling mesh message: " + e.getMessage());
        }
    }
    
    private void handleMessageSync(MeshMessage message) {
        try {
            // Store message in local database
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String insertSQL = "INSERT OR IGNORE INTO messages " +
                "(message_id, sender_id, content, message_type, channel_id, sent_at, sync_status) " +
                "VALUES (?, ?, ?, 'CHAT', ?, datetime('now'), 'SYNCED')";
            
            int rowsAffected = dbManager.executeUpdate(insertSQL, 
                message.messageId, message.sourceNodeId, message.content, message.channelId);
            
            if (rowsAffected > 0) {
                System.out.println("Synced mesh message: " + message.messageId);
                DataSyncManager.getInstance().notifyCommunicationDataChanged();
            }
            
        } catch (Exception e) {
            System.err.println("Error handling message sync: " + e.getMessage());
        }
    }
    
    private void handleSyncRequest(MeshPeer fromPeer) {
        try {
            // Send recent data to requesting peer
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Get recent messages
            String recentMessagesSQL = "SELECT * FROM messages WHERE sent_at >= datetime('now', '-1 hour') ORDER BY sent_at DESC LIMIT 20";
            try (java.sql.ResultSet rs = dbManager.executeQuery(recentMessagesSQL)) {
                while (rs.next()) {
                    MeshMessage syncMessage = new MeshMessage();
                    syncMessage.type = "SYNC_RESPONSE";
                    syncMessage.messageId = rs.getString("message_id");
                    syncMessage.content = rs.getString("content");
                    syncMessage.channelId = rs.getString("channel_id");
                    syncMessage.sourceNodeId = localNodeId;
                    syncMessage.timestamp = rs.getTimestamp("sent_at").getTime();
                    
                    String jsonMessage = objectMapper.writeValueAsString(syncMessage);
                    fromPeer.sendMessage(jsonMessage);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error handling sync request: " + e.getMessage());
        }
    }
    
    private void handleSyncResponse(MeshMessage message) {
        // Same as handleMessageSync for now
        handleMessageSync(message);
    }
    
    private void handleEmergencySync(MeshMessage message) {
        try {
            // Handle emergency request synchronization
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String insertSQL = "INSERT OR IGNORE INTO emergency_requests " +
                "(request_id, requester_id, emergency_type, priority, location_lat, location_lng, " +
                "description, status, created_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), 'SYNCED')";
            
            // Note: This would need the full emergency data in the message
            dbManager.executeUpdate(insertSQL, message.messageId, message.sourceNodeId,
                "UNKNOWN", "HIGH", 0.0, 0.0, message.content, "PENDING");
            
            DataSyncManager.getInstance().notifyEmergencyDataChanged();
            
        } catch (Exception e) {
            System.err.println("Error handling emergency sync: " + e.getMessage());
        }
    }
    
    private void forwardMessage(String jsonMessage, MeshPeer excludePeer) {
        // Forward to all peers except the one we received it from
        for (MeshPeer peer : connectedPeers.values()) {
            if (peer != excludePeer && peer.isConnected()) {
                peer.sendMessage(jsonMessage);
            }
        }
    }
    
    public int getConnectedPeerCount() {
        return connectedPeers.size();
    }
    
    /**
     * Represents a connection to a mesh peer
     */
    private class MeshPeer implements Runnable {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final String peerAddress;
        private boolean connected = true;
        
        public MeshPeer(Socket socket) throws IOException {
            this.socket = socket;
            this.peerAddress = socket.getInetAddress().getHostAddress();
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            
            connectedPeers.put(peerAddress, this);
        }
        
        @Override
        public void run() {
            try {
                String message;
                while (connected && (message = reader.readLine()) != null) {
                    handleMeshMessage(message, this);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Error reading from mesh peer " + peerAddress + ": " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }
        
        public void sendMessage(String message) {
            if (connected && writer != null) {
                writer.println(message);
            }
        }
        
        public void disconnect() {
            connected = false;
            connectedPeers.remove(peerAddress);
            
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing peer connection: " + e.getMessage());
            }
            
            System.out.println("Mesh peer disconnected: " + peerAddress);
        }
        
        public boolean isConnected() {
            return connected && !socket.isClosed();
        }
    }
    
    /**
     * Message structure for mesh network communication
     */
    private static class MeshMessage {
        public String type;
        public String messageId;
        public String content;
        public String channelId;
        public String sourceNodeId;
        public long timestamp;
        
        // Default constructor for Jackson
        public MeshMessage() {}
    }
}
