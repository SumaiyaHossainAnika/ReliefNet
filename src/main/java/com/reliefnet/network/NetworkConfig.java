package com.reliefnet.network;

/**
 * NetworkConfig - Configuration settings for ReliefNet networking
 */
public class NetworkConfig {
    
    // Firebase Configuration
    public static final String FIREBASE_PROJECT_ID = "reliefnet-6e513";
    public static final String FIREBASE_DATABASE_URL = "https://reliefnet-6e513-default-rtdb.asia-southeast1.firebasedatabase.app/";
    public static final String FIREBASE_API_KEY = "AIzaSyCKEjgfyEcfIqUTVMa6DPrdmd_uTop8zp4";
    
    // WebSocket Configuration
    public static final int WEBSOCKET_PORT = 8080;
    public static final String WEBSOCKET_PATH = "/reliefnet-sync";
    
    // Mesh Network Configuration
    public static final int MESH_PORT = 8081;
    public static final String MESH_SERVICE_NAME = "_reliefnet._tcp.local.";
    public static final int DISCOVERY_RANGE_METERS = 300; // 300m discovery range
    
    // Sync Settings
    public static final int SYNC_INTERVAL_SECONDS = 30; // Sync every 30 seconds when online
    public static final int HEARTBEAT_INTERVAL_SECONDS = 10; // Heartbeat every 10 seconds
    public static final int CONNECTION_TIMEOUT_MS = 5000; // 5 second timeout
    
    // Offline Network Settings
    public static final int PEER_DISCOVERY_PORT = 8082;
    public static final String PEER_BROADCAST_MESSAGE = "RELIEFNET_DISCOVER";
    public static final int PEER_RESPONSE_TIMEOUT_MS = 3000;
    
    // API Endpoints - Using your Firebase project for real cloud sync
    public static final String API_BASE_URL = "https://reliefnet-6e513-default-rtdb.asia-southeast1.firebasedatabase.app";
    public static final String USERS_ENDPOINT = "/users.json";
    public static final String MESSAGES_ENDPOINT = "/messages.json";
    public static final String EMERGENCIES_ENDPOINT = "/emergencies.json";
    public static final String RESOURCES_ENDPOINT = "/resources.json";
    
    // Network Status
    public enum NetworkMode {
        ONLINE_CLOUD,      // Connected to internet, using cloud sync
        ONLINE_LOCAL,      // Connected to local network only
        OFFLINE_MESH,      // No internet, using mesh network
        OFFLINE_STANDALONE // Completely offline
    }
    
    // User roles for network permissions
    public enum NetworkRole {
        AUTHORITY_SERVER,  // Authority acting as local server
        VOLUNTEER_CLIENT,  // Volunteer connecting to server
        SURVIVOR_CLIENT    // Survivor connecting to server
    }
}
