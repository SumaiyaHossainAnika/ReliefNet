package com.reliefnet.network;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.model.User;
import java.net.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * NetworkManager - Core networking component that manages all network operations
 * Handles online/offline detection, connection switching, and sync coordination
 */
public class NetworkManager {
    
    private static NetworkManager instance;
    private NetworkConfig.NetworkMode currentMode;
    private NetworkConfig.NetworkRole currentRole;
    
    // Network components
    private CloudSyncManager cloudSync;
    private MeshNetworkManager meshNetwork;
    private PeerDiscoveryManager peerDiscovery;
    private WebSocketSyncManager webSocketSync;
    
    // Connection monitoring
    private ScheduledExecutorService networkMonitor;
    private boolean isOnline = false;
    private boolean hasInternetAccess = false;
    
    // Event listeners
    private final List<NetworkStatusListener> statusListeners = new ArrayList<>();
    
    private User currentUser;
    
    private NetworkManager() {
        this.currentMode = NetworkConfig.NetworkMode.OFFLINE_STANDALONE;
        this.networkMonitor = Executors.newScheduledThreadPool(2);
        initializeNetworkComponents();
        startNetworkMonitoring();
    }
    
    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    /**
     * Initialize the network manager with current user
     */
    public void initialize(User user) {
        this.currentUser = user;
        
        // Determine network role based on user type
        if (user.getUserType() == User.UserType.AUTHORITY) {
            this.currentRole = NetworkConfig.NetworkRole.AUTHORITY_SERVER;
        } else if (user.getUserType() == User.UserType.VOLUNTEER) {
            this.currentRole = NetworkConfig.NetworkRole.VOLUNTEER_CLIENT;
        } else {
            this.currentRole = NetworkConfig.NetworkRole.SURVIVOR_CLIENT;
        }
        
        System.out.println("NetworkManager initialized for user: " + user.getFullName() + " as " + currentRole);
        updateNetworkMode();
    }
    
    private void initializeNetworkComponents() {
        try {
            cloudSync = CloudSyncManager.getInstance();
            meshNetwork = new MeshNetworkManager();
            peerDiscovery = new PeerDiscoveryManager();
            webSocketSync = new WebSocketSyncManager();
            
            System.out.println("Network components initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing network components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startNetworkMonitoring() {
        // Check network status every 5 seconds for better responsiveness
        networkMonitor.scheduleAtFixedRate(this::checkNetworkStatus, 0, 5, TimeUnit.SECONDS);
        
        // Perform sync operations every 30 seconds when online
        networkMonitor.scheduleAtFixedRate(this::performPeriodicSync, 30, 30, TimeUnit.SECONDS);
    }
    
    private void checkNetworkStatus() {
        try {
            boolean wasOnline = isOnline;
            boolean hadInternet = hasInternetAccess;
            
            // Check local network connectivity
            isOnline = isNetworkAvailable();
            
            // Check internet connectivity
            hasInternetAccess = isInternetAvailable();
            
            // Update network mode based on connectivity
            NetworkConfig.NetworkMode previousMode = currentMode;
            updateNetworkMode();
            
            // Notify listeners if status changed
            if (wasOnline != isOnline || hadInternet != hasInternetAccess || previousMode != currentMode) {
                notifyNetworkStatusChange();
            }
            
        } catch (Exception e) {
            System.err.println("Error checking network status: " + e.getMessage());
        }
    }
    
    private boolean isNetworkAvailable() {
        try {
            // Try to connect to local network
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("8.8.8.8", 53), NetworkConfig.CONNECTION_TIMEOUT_MS);
            socket.close();
            return true;
        } catch (IOException e) {
            // Try local network test
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                return localHost.isReachable(NetworkConfig.CONNECTION_TIMEOUT_MS);
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    private boolean isInternetAvailable() {
        try {
            // Use a simple and fast connectivity test
            // Test Google's public DNS server (8.8.8.8) on port 53 (DNS)
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("8.8.8.8", 53), 3000); // 3 second timeout
                return true;
            }
        } catch (Exception e) {
            // Fallback: try connecting to Cloudflare DNS
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("1.1.1.1", 53), 3000);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
    
    private void updateNetworkMode() {
        NetworkConfig.NetworkMode newMode;
        
        if (hasInternetAccess) {
            newMode = NetworkConfig.NetworkMode.ONLINE_CLOUD;
        } else {
            // Check for nearby ReliefNet devices
            if (peerDiscovery.hasNearbyPeers()) {
                newMode = NetworkConfig.NetworkMode.OFFLINE_MESH;
            } else {
                newMode = NetworkConfig.NetworkMode.OFFLINE_STANDALONE;
            }
        }
        
        if (newMode != currentMode) {
            NetworkConfig.NetworkMode oldMode = currentMode;
            currentMode = newMode;
            handleModeTransition(oldMode, newMode);
        }
    }
    
    private void handleModeTransition(NetworkConfig.NetworkMode oldMode, NetworkConfig.NetworkMode newMode) {
        System.out.println("Network mode changed: " + oldMode + " -> " + newMode);
        
        switch (newMode) {
            case ONLINE_CLOUD:
                enableCloudSync();
                disableMeshNetwork();
                break;
                
            case ONLINE_LOCAL:
                // For the simplified 3-state model, treat ONLINE_LOCAL the same as ONLINE_CLOUD
                enableCloudSync();
                disableMeshNetwork();
                break;
                
            case OFFLINE_MESH:
                disableCloudSync();
                enableMeshNetwork();
                break;
                
            case OFFLINE_STANDALONE:
                disableCloudSync();
                disableMeshNetwork();
                break;
        }
        
        notifyNetworkStatusChange();
    }
    
    private void enableCloudSync() {
        try {
            cloudSync.connect();
            webSocketSync.connect();
            System.out.println("Cloud sync enabled");
        } catch (Exception e) {
            System.err.println("Error enabling cloud sync: " + e.getMessage());
        }
    }
    
    private void enableMeshNetwork() {
        try {
            meshNetwork.start();
            peerDiscovery.startDiscovery();
            System.out.println("Mesh network enabled");
        } catch (Exception e) {
            System.err.println("Error enabling mesh network: " + e.getMessage());
        }
    }
    
    private void disableCloudSync() {
        try {
            if (cloudSync != null) {
                cloudSync.disconnect();
            }
            System.out.println("Cloud sync disabled");
        } catch (Exception e) {
            System.err.println("Error disabling cloud sync: " + e.getMessage());
        }
    }
    
    private void disableMeshNetwork() {
        try {
            if (meshNetwork != null) {
                meshNetwork.stop();
            }
            System.out.println("Mesh network disabled");
        } catch (Exception e) {
            System.err.println("Error disabling mesh network: " + e.getMessage());
        }
    }
    
    private void performPeriodicSync() {
        if (currentMode == NetworkConfig.NetworkMode.ONLINE_CLOUD) {
            cloudSync.performSync();
        } else if (currentMode == NetworkConfig.NetworkMode.ONLINE_LOCAL) {
            webSocketSync.performSync();
        } else if (currentMode == NetworkConfig.NetworkMode.OFFLINE_MESH) {
            meshNetwork.performSync();
        }
        // No sync needed for OFFLINE_STANDALONE
    }
    
    private void notifyNetworkStatusChange() {
        for (NetworkStatusListener listener : statusListeners) {
            try {
                listener.onNetworkStatusChanged(currentMode, isOnline, hasInternetAccess);
            } catch (Exception e) {
                System.err.println("Error notifying network status listener: " + e.getMessage());
            }
        }
    }
    
    // Public API methods
    
    public NetworkConfig.NetworkMode getCurrentMode() {
        return currentMode;
    }
    
    public NetworkConfig.NetworkRole getCurrentRole() {
        return currentRole;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public boolean hasInternetAccess() {
        return hasInternetAccess;
    }
    
    public void addNetworkStatusListener(NetworkStatusListener listener) {
        statusListeners.add(listener);
    }
    
    public void removeNetworkStatusListener(NetworkStatusListener listener) {
        statusListeners.remove(listener);
    }
    
    /**
     * Manually trigger a sync operation
     */
    public void triggerSync() {
        performPeriodicSync();
    }
    
    /**
     * Send a message (will be routed based on current network mode)
     */
    public boolean sendMessage(String messageId, String content, String channelId) {
        try {
            switch (currentMode) {
                case ONLINE_CLOUD:
                    return cloudSync.sendMessage(messageId, content, channelId);
                case ONLINE_LOCAL:
                    return webSocketSync.sendMessage(messageId, content, channelId);
                case OFFLINE_MESH:
                    return meshNetwork.sendMessage(messageId, content, channelId);
                default:
                    // Store locally for later sync
                    return storeForLaterSync(messageId, content, channelId);
            }
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send a message with sender information (will be routed based on current network mode)
     */
    public boolean sendMessage(String messageId, String senderId, String content, String channelId) {
        try {
            switch (currentMode) {
                case ONLINE_CLOUD:
                    return cloudSync.sendMessage(messageId, senderId, content, channelId);
                case ONLINE_LOCAL:
                    return webSocketSync.sendMessage(messageId, content, channelId);
                case OFFLINE_MESH:
                    return meshNetwork.sendMessage(messageId, content, channelId);
                default:
                    // Store locally for later sync
                    return storeForLaterSync(messageId, content, channelId);
            }
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            return false;
        }
    }

    private boolean storeForLaterSync(String messageId, String content, String channelId) {
        try {
            // Store in local database with sync flag
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "INSERT INTO messages (message_id, sender_id, content, message_type, " +
                        "sent_at, channel_id, sync_status) VALUES (?, ?, ?, 'CHAT', datetime('now'), ?, 'PENDING')";
            
            String senderId = currentUser != null ? currentUser.getUserId() : "UNKNOWN";
            dbManager.executeUpdate(sql, messageId, senderId, content, channelId);
            
            System.out.println("Message stored for later sync: " + messageId);
            return true;
        } catch (Exception e) {
            System.err.println("Error storing message for sync: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get network status summary for UI display
     */
    public String getNetworkStatusSummary() {
        // Special handling for Authority users - they only show Online/Disconnected
        if (currentUser != null && currentUser.getUserType() == User.UserType.AUTHORITY) {
            if (hasInternetAccess) {
                return "🌐 Online - Ready to serve other devices";
            } else {
                return "📴 Disconnected - No internet connection available";
            }
        }
        
        // Special handling for Volunteer users - Online/Mesh/Disconnected
        if (currentUser != null && currentUser.getUserType() == User.UserType.VOLUNTEER) {
            if (hasInternetAccess) {
                return "🌐 Online - Ready to serve as local server";
            } else if (peerDiscovery.hasNearbyPeers()) {
                return "📱 Mesh Network - Connected to " + peerDiscovery.getPeerCount() + " devices";
            } else {
                return "📴 Disconnected - No network available";
            }
        }
        
        // Special handling for Survivor users - Online/Mesh/Disconnected (no local server mode)
        if (currentUser != null && currentUser.getUserType() == User.UserType.SURVIVOR) {
            if (hasInternetAccess) {
                return "� Online - Internet connection available";
            } else if (peerDiscovery.hasNearbyPeers()) {
                return "📱 Mesh Network - Connected to " + peerDiscovery.getPeerCount() + " devices";
            } else {
                return "📴 Disconnected - No network available";
            }
        }
        
        // Fallback - should not happen with proper user types
        if (hasInternetAccess) {
            return "🌐 Online - Internet connection available";
        } else if (peerDiscovery.hasNearbyPeers()) {
            return "📱 Mesh Network - Connected to " + peerDiscovery.getPeerCount() + " devices";
        } else {
            return "📴 Disconnected - No network available";
        }
    }
    
    public void shutdown() {
        try {
            if (networkMonitor != null) {
                networkMonitor.shutdown();
            }
            disableCloudSync();
            disableMeshNetwork();
            System.out.println("NetworkManager shutdown complete");
        } catch (Exception e) {
            System.err.println("Error during NetworkManager shutdown: " + e.getMessage());
        }
    }
    
    // Network status listener interface
    public interface NetworkStatusListener {
        void onNetworkStatusChanged(NetworkConfig.NetworkMode mode, boolean isOnline, boolean hasInternet);
    }
}
