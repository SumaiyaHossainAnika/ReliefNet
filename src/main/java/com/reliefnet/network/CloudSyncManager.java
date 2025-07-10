package com.reliefnet.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.util.DataSyncManager;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * CloudSyncManager - Handles synchronization with cloud serv                                // Insert if not exists
                                String insertSQL = "INSERT OR IGNORE INTO messages " +
                                    "(message_id, sender_id, content, message_type, channel_id, sent_at, sync_status) " +
                                    "VALUES (?, ?, ?, 'CHAT', ?, datetime('now'), 'SYNCED')";
                                int rowsAffected = dbManager.executeUpdate(insertSQL, messageId, senderId, content, channelId);
                                
                                // If a new message was inserted, notify the UI
                                if (rowsAffected > 0) {
                                    System.out.println("New message synced from Firebase: " + messageId);
                                    // Notify UI to refresh messages
                                    DataSyncManager.getInstance().notifyCommunicationDataChanged();
                                }(Firebase/Custom API)
 */
public class CloudSyncManager {
    
    private static CloudSyncManager instance;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private boolean isConnected = false;
    
    private CloudSyncManager() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(NetworkConfig.CONNECTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public static synchronized CloudSyncManager getInstance() {
        if (instance == null) {
            instance = new CloudSyncManager();
        }
        return instance;
    }
    
    public void connect() {
        try {
            // Test connection to cloud service
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/health")
                .build();
                
            Response response = httpClient.newCall(request).execute();
            isConnected = response.isSuccessful();
            
            if (isConnected) {
                System.out.println("Connected to cloud sync service");
            } else {
                System.err.println("Failed to connect to cloud sync service");
            }
            
        } catch (Exception e) {
            System.err.println("Error connecting to cloud sync: " + e.getMessage());
            isConnected = false;
        }
    }
    
    public void disconnect() {
        isConnected = false;
        System.out.println("Disconnected from cloud sync service");
    }
    
    public void performSync() {
        if (!isConnected) {
            return;
        }
        
        try {
            // Sync messages
            syncMessages();
            
            // Sync emergency requests
            syncEmergencyRequests();
            
            // Sync user data
            syncUserData();
            
            // Sync resources
            syncResources();
            
            System.out.println("Cloud sync completed successfully");
            
        } catch (Exception e) {
            System.err.println("Error during cloud sync: " + e.getMessage());
        }
    }
    
    private void syncMessages() throws Exception {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Upload pending messages
        String selectPendingSQL = "SELECT * FROM messages WHERE sync_status = 'PENDING' OR sync_status IS NULL";
        try (java.sql.ResultSet rs = dbManager.executeQuery(selectPendingSQL)) {
            while (rs.next()) {
                String messageId = rs.getString("message_id");
                String content = rs.getString("content");
                String channelId = rs.getString("channel_id");
                String senderId = rs.getString("sender_id");
                
                // Upload message to cloud
                if (uploadMessage(messageId, senderId, content, channelId)) {
                    // Mark as synced
                    String updateSQL = "UPDATE messages SET sync_status = 'SYNCED' WHERE message_id = ?";
                    dbManager.executeUpdate(updateSQL, messageId);
                }
            }
        }
        
        // Download new messages
        downloadNewMessages();
    }
    
    private void syncEmergencyRequests() throws Exception {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Upload pending emergency requests
        String selectPendingSQL = "SELECT * FROM emergency_requests WHERE sync_status = 'PENDING' OR sync_status IS NULL";
        try (java.sql.ResultSet rs = dbManager.executeQuery(selectPendingSQL)) {
            while (rs.next()) {
                String requestId = rs.getString("request_id");
                // Upload emergency request data
                if (uploadEmergencyRequest(rs)) {
                    // Mark as synced
                    String updateSQL = "UPDATE emergency_requests SET sync_status = 'SYNCED' WHERE request_id = ?";
                    dbManager.executeUpdate(updateSQL, requestId);
                }
            }
        }
        
        // Download new emergency requests
        downloadNewEmergencyRequests();
    }
    
    private void syncUserData() throws Exception {
        // Sync user status and location updates
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        String selectPendingSQL = "SELECT * FROM users WHERE sync_status = 'PENDING' OR sync_status IS NULL";
        try (java.sql.ResultSet rs = dbManager.executeQuery(selectPendingSQL)) {
            while (rs.next()) {
                String userId = rs.getString("user_id");
                if (uploadUserData(rs)) {
                    String updateSQL = "UPDATE users SET sync_status = 'SYNCED' WHERE user_id = ?";
                    dbManager.executeUpdate(updateSQL, userId);
                }
            }
        }
        
        // Download new users
        downloadNewUsers();
    }
    
    private void syncResources() throws Exception {
        // Similar sync pattern for resources
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        String selectPendingSQL = "SELECT * FROM resources WHERE sync_status = 'PENDING' OR sync_status IS NULL";
        try (java.sql.ResultSet rs = dbManager.executeQuery(selectPendingSQL)) {
            while (rs.next()) {
                String resourceId = rs.getString("resource_id");
                if (uploadResource(rs)) {
                    String updateSQL = "UPDATE resources SET sync_status = 'SYNCED' WHERE resource_id = ?";
                    dbManager.executeUpdate(updateSQL, resourceId);
                }
            }
        }
        
        // Download new resources
        downloadNewResources();
    }
    
    private boolean uploadMessage(String messageId, String senderId, String content, String channelId) {
        try {
            // Create JSON payload
            String json = objectMapper.writeValueAsString(new Object() {
                public final String id = messageId;
                public final String sender = senderId;
                public final String message = content;
                public final String channel = channelId;
                public final long timestamp = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.MESSAGES_ENDPOINT)
                .post(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading message: " + e.getMessage());
            return false;
        }
    }
    
    private boolean uploadEmergencyRequest(java.sql.ResultSet rs) {
        try {
            // Create emergency request JSON from ResultSet
            String json = objectMapper.writeValueAsString(new Object() {
                public String request_id = rs.getString("request_id");
                public String requester_id = rs.getString("requester_id");
                public String emergency_type = rs.getString("emergency_type");
                public String priority = rs.getString("priority");
                public double location_lat = rs.getDouble("location_lat");
                public double location_lng = rs.getDouble("location_lng");
                public String description = rs.getString("description");
                public String status = rs.getString("status");
                public long timestamp = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.EMERGENCIES_ENDPOINT)
                .post(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading emergency request: " + e.getMessage());
            return false;
        }
    }
    
    private boolean uploadUserData(java.sql.ResultSet rs) {
        try {
            String userId = rs.getString("user_id");
            String name = rs.getString("name");
            
            // Upload user data with complete information, handling null values
            String json = objectMapper.writeValueAsString(new Object() {
                public String user_id = rs.getString("user_id");
                public String name = rs.getString("name");
                public String email = getStringOrDefault(rs, "email", "");
                public String user_type = getStringOrDefault(rs, "user_type", "SURVIVOR");
                public String status = getStringOrDefault(rs, "status", "ACTIVE");
                public double location_lat = getDoubleOrDefault(rs, "location_lat", 0.0);
                public double location_lng = getDoubleOrDefault(rs, "location_lng", 0.0);
                public String location_name = getStringOrDefault(rs, "location_name", "");
                public long timestamp = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/users.json")
                .post(body)  // Use POST to add to collection
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("✓ User synced to cloud: " + name);
                    return true;
                } else {
                    System.err.println("✗ Failed to sync user " + name + ": " + response.code());
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    System.err.println("   Response: " + responseBody);
                    return false;
                }
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error uploading user data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Helper methods to handle null values gracefully
    private String getStringOrDefault(java.sql.ResultSet rs, String columnName, String defaultValue) {
        try {
            String value = rs.getString(columnName);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private double getDoubleOrDefault(java.sql.ResultSet rs, String columnName, double defaultValue) {
        try {
            return rs.getDouble(columnName);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private boolean uploadResource(java.sql.ResultSet rs) {
        try {
            String json = objectMapper.writeValueAsString(new Object() {
                public String resource_id = rs.getString("resource_id");
                public String name = rs.getString("name");
                public String category = rs.getString("category");
                public int quantity = rs.getInt("quantity");
                public String status = rs.getString("status");
                public double location_lat = rs.getDouble("location_lat");
                public double location_lng = rs.getDouble("location_lng");
                public long timestamp = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.RESOURCES_ENDPOINT)
                .post(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading resource: " + e.getMessage());
            return false;
        }
    }
    
    private void downloadNewMessages() {
        try {
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.MESSAGES_ENDPOINT)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    
                    // Check if response is valid JSON
                    if (jsonData.trim().startsWith("<")) {
                        System.out.println("? Skipping message download - Firebase returned HTML (likely empty collection)");
                        return;
                    }
                    
                    JsonNode messages = objectMapper.readTree(jsonData);
                    
                    // Firebase returns an object with keys as IDs, not an array
                    if (messages.isObject()) {
                        DatabaseManager dbManager = DatabaseManager.getInstance();
                        
                        messages.fields().forEachRemaining(entry -> {
                            try {
                                JsonNode message = entry.getValue();
                                String messageId = message.get("id").asText();
                                String senderId = message.get("sender").asText();
                                String content = message.get("message").asText();
                                String channelId = message.get("channel").asText();
                                
                                // Insert if not exists
                                String insertSQL = "INSERT OR IGNORE INTO messages " +
                                    "(message_id, sender_id, content, message_type, channel_id, sent_at, sync_status) " +
                                    "VALUES (?, ?, ?, 'CHAT', ?, datetime('now'), 'SYNCED')";
                                int rowsAffected = dbManager.executeUpdate(insertSQL, messageId, senderId, content, channelId);
                                
                                // Notify UI if new message was inserted
                                if (rowsAffected > 0) {
                                    com.reliefnet.util.DataSyncManager.getInstance().notifyCommunicationDataChanged();
                                    System.out.println("New message synced from cloud: " + messageId);
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing message: " + e.getMessage());
                            }
                        });
                    }
                } else {
                    System.out.println("? No new messages to download (response: " + response.code() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error downloading messages: " + e.getMessage());
            // Don't print stack trace for expected JSON parsing errors
            if (!e.getMessage().contains("Unexpected character")) {
                e.printStackTrace();
            }
        }
    }
    
    private void downloadNewEmergencyRequests() {
        try {
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.EMERGENCIES_ENDPOINT)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    
                    // Check if response is valid JSON
                    if (jsonData.trim().startsWith("<")) {
                        System.out.println("? Skipping emergency download - Firebase returned HTML (likely empty collection)");
                        return;
                    }
                    
                    JsonNode emergencies = objectMapper.readTree(jsonData);
                    
                    // Firebase returns an object with keys as IDs, not an array
                    if (emergencies.isObject()) {
                        DatabaseManager dbManager = DatabaseManager.getInstance();
                        
                        emergencies.fields().forEachRemaining(entry -> {
                            try {
                                JsonNode emergency = entry.getValue();
                                String requestId = emergency.get("request_id").asText();
                                
                                // Check if already exists
                                String checkSQL = "SELECT COUNT(*) FROM emergency_requests WHERE request_id = ?";
                                try (java.sql.ResultSet rs = dbManager.executeQuery(checkSQL, requestId)) {
                                    if (rs.next() && rs.getInt(1) == 0) {
                                        // Insert new emergency request
                                        String insertSQL = "INSERT INTO emergency_requests " +
                                            "(request_id, requester_id, emergency_type, description, priority, status, " +
                                            "location_lat, location_lng, created_at, sync_status) " +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), 'SYNCED')";
                                        
                                        dbManager.executeUpdate(insertSQL,
                                            requestId,
                                            emergency.get("requester_id").asText(),
                                            emergency.get("emergency_type").asText(),
                                            emergency.get("description").asText(),
                                            emergency.get("priority").asText(),
                                            emergency.get("status").asText(),
                                            emergency.get("location_lat").asDouble(),
                                            emergency.get("location_lng").asDouble()
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing emergency request: " + e.getMessage());
                            }
                        });
                    }
                } else {
                    System.out.println("? No new emergencies to download (response: " + response.code() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error downloading emergency requests: " + e.getMessage());
            // Don't print stack trace for expected JSON parsing errors
            if (!e.getMessage().contains("Unexpected character")) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean sendMessage(String messageId, String content, String channelId) {
        // For real-time messaging through cloud - backward compatibility
        return sendMessage(messageId, "UNKNOWN_SENDER", content, channelId);
    }
    
    public boolean sendMessage(String messageId, String senderId, String content, String channelId) {
        // For real-time messaging through cloud with sender information
        return uploadMessage(messageId, senderId, content, channelId);
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * REAL-TIME IMMEDIATE SYNC METHODS
     * These trigger instant sync when data changes happen
     */
    
    public void syncNewEmergencyImmediately(String emergencyId) {
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String sql = "SELECT * FROM emergency_requests WHERE request_id = ?";
                try (java.sql.ResultSet rs = dbManager.executeQuery(sql, emergencyId)) {
                    if (rs.next()) {
                        if (uploadEmergencyRequest(rs)) {
                            String updateSQL = "UPDATE emergency_requests SET sync_status = 'SYNCED' WHERE request_id = ?";
                            dbManager.executeUpdate(updateSQL, emergencyId);
                            System.out.println("? Emergency synced immediately: " + emergencyId);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in immediate emergency sync: " + e.getMessage());
            }
        });
    }
    
    public void syncNewResourceImmediately(String resourceId) {
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String sql = "SELECT * FROM resources WHERE resource_id = ?";
                try (java.sql.ResultSet rs = dbManager.executeQuery(sql, resourceId)) {
                    if (rs.next()) {
                        if (uploadResource(rs)) {
                            String updateSQL = "UPDATE resources SET sync_status = 'SYNCED' WHERE resource_id = ?";
                            dbManager.executeUpdate(updateSQL, resourceId);
                            System.out.println("? Resource synced immediately: " + resourceId);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in immediate resource sync: " + e.getMessage());
            }
        });
    }
    
    public void syncNewUserImmediately(String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String sql = "SELECT * FROM users WHERE user_id = ?";
                try (java.sql.ResultSet rs = dbManager.executeQuery(sql, userId)) {
                    if (rs.next()) {
                        if (uploadUserData(rs)) {
                            String updateSQL = "UPDATE users SET sync_status = 'SYNCED' WHERE user_id = ?";
                            dbManager.executeUpdate(updateSQL, userId);
                            System.out.println("? User synced immediately: " + userId);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in immediate user sync: " + e.getMessage());
            }
        });
    }
    
    public void syncMessageImmediately(String messageId) {
        CompletableFuture.runAsync(() -> {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String sql = "SELECT * FROM messages WHERE message_id = ?";
                try (java.sql.ResultSet rs = dbManager.executeQuery(sql, messageId)) {
                    if (rs.next()) {
                        String senderId = rs.getString("sender_id");
                        String content = rs.getString("content");
                        String channelId = rs.getString("channel_id");
                        
                        if (uploadMessage(messageId, senderId, content, channelId)) {
                            String updateSQL = "UPDATE messages SET sync_status = 'SYNCED' WHERE message_id = ?";
                            dbManager.executeUpdate(updateSQL, messageId);
                            System.out.println("? Message synced immediately: " + messageId);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in immediate message sync: " + e.getMessage());
            }
        });
    }
    
    /**
     * Download and sync user data from cloud for cross-device authentication
     */
    public void downloadAllUsers() {
        try {
            // Download all users from Firebase
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.USERS_ENDPOINT)
                .get()
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    JsonNode usersNode = objectMapper.readTree(jsonData);
                    
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    
                    // Process each user from Firebase
                    if (usersNode != null && usersNode.isObject()) {
                        usersNode.fields().forEachRemaining(entry -> {
                            try {
                                JsonNode userData = entry.getValue();
                                
                                String userId = userData.get("user_id") != null ? userData.get("user_id").asText() : null;
                                String email = userData.get("email") != null ? userData.get("email").asText() : null;
                                String password = userData.get("password") != null ? userData.get("password").asText() : null;
                                String fullName = userData.get("name") != null ? userData.get("name").asText() : null;
                                String userType = userData.get("user_type") != null ? userData.get("user_type").asText() : null;
                                String status = userData.get("status") != null ? userData.get("status").asText() : null;
                                
                                // Validate required fields
                                if (userId == null || email == null) {
                                    System.err.println("Skipping user with missing required fields (user_id or email)");
                                    return;
                                }
                                
                                // Check if user already exists locally
                                String checkSQL = "SELECT COUNT(*) FROM users WHERE user_id = ?";
                                try (java.sql.ResultSet rs = dbManager.executeQuery(checkSQL, userId)) {
                                    if (rs.next() && rs.getInt(1) == 0) {
                                        // User doesn't exist locally, add them
                                        String insertSQL = "INSERT INTO users (user_id, email, password, name, user_type, status, sync_status) VALUES (?, ?, ?, ?, ?, ?, 'SYNCED')";
                                        dbManager.executeUpdate(insertSQL, userId, email, password, fullName, userType, status);
                                        System.out.println("Synced user from cloud: " + fullName);
                                    } else {
                                        // User exists, update their info (preserve password for local authentication)
                                        String updateSQL = "UPDATE users SET email = ?, name = ?, user_type = ?, status = ?, sync_status = 'SYNCED' WHERE user_id = ? AND (password IS NULL OR password = '')";
                                        int updatedRows = dbManager.executeUpdate(updateSQL, email, fullName, userType, status, userId);
                                        
                                        // If no rows were updated (password exists), don't overwrite the user
                                        if (updatedRows == 0) {
                                            System.out.println("Preserved local user with password: " + fullName);
                                        } else {
                                            System.out.println("Updated user without password: " + fullName);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing user: " + e.getMessage());
                                // Continue processing other users instead of stopping
                            }
                        });
                    }
                    
                    System.out.println("User sync completed - all users from other devices are now available");
                }
            }
        } catch (Exception e) {
            System.err.println("Error downloading users: " + e.getMessage());
        }
    }
    
    /**
     * Immediately upload a new user for real-time cross-device availability
     */
    public boolean uploadUserImmediately(String userId, String email, String password, String fullName, String userType, String status) {
        try {
            // Create user object for Firebase
            final String userIdFinal = userId;
            final String emailFinal = email;
            final String passwordFinal = password;
            final String fullNameFinal = fullName;
            final String userTypeFinal = userType;
            final String statusFinal = status;
            
            String json = objectMapper.writeValueAsString(new Object() {
                public final String user_id = userIdFinal;
                public final String email = emailFinal;
                public final String password = passwordFinal;
                public final String name = fullNameFinal;  // Changed from full_name to name
                public final String user_type = userTypeFinal;
                public final String status = statusFinal;
                public final long created_at = System.currentTimeMillis();
            });
            
            // Upload to Firebase as part of users collection
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/users.json")
                .post(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("User uploaded to cloud immediately: " + fullName);
                    return true;
                } else {
                    System.err.println("Failed to upload user to cloud: " + response.code());
                    return false;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading user immediately: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Upload a message to Firebase
     */
    public boolean uploadMessage(String messageId, String senderId, String receiverId, String content, String messageType, String channelId) {
        try {
            final String messageIdFinal = messageId;
            final String senderIdFinal = senderId;
            final String receiverIdFinal = receiverId;
            final String contentFinal = content;
            final String messageTypeFinal = messageType;
            final String channelIdFinal = channelId;
            
            String json = objectMapper.writeValueAsString(new Object() {
                public final String message_id = messageIdFinal;
                public final String sender_id = senderIdFinal;
                public final String receiver_id = receiverIdFinal;
                public final String content = contentFinal;
                public final String message_type = messageTypeFinal;
                public final String channel_id = channelIdFinal;
                public final long sent_at = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/messages/" + messageId + ".json")
                .put(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Upload an emergency request to Firebase
     */
    public boolean uploadEmergencyRequest(String requestId, String requesterId, String emergencyType, String priority, 
                                        String description, String status, String locationName, double locationLat, 
                                        double locationLng, int peopleCount) {
        try {
            final String requestIdFinal = requestId;
            final String requesterIdFinal = requesterId;
            final String emergencyTypeFinal = emergencyType;
            final String priorityFinal = priority;
            final String descriptionFinal = description;
            final String statusFinal = status;
            final String locationNameFinal = locationName;
            final double locationLatFinal = locationLat;
            final double locationLngFinal = locationLng;
            final int peopleCountFinal = peopleCount;
            
            String json = objectMapper.writeValueAsString(new Object() {
                public final String request_id = requestIdFinal;
                public final String requester_id = requesterIdFinal;
                public final String emergency_type = emergencyTypeFinal;
                public final String priority = priorityFinal;
                public final String description = descriptionFinal;
                public final String status = statusFinal;
                public final String location_name = locationNameFinal;
                public final double location_lat = locationLatFinal;
                public final double location_lng = locationLngFinal;
                public final int people_count = peopleCountFinal;
                public final long created_at = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/emergencies/" + requestId + ".json")
                .put(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading emergency request: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Upload a resource to Firebase
     */
    public boolean uploadResource(String resourceId, String name, String category, int quantity, String unit,
                                String status, String locationName, double locationLat, double locationLng, String notes) {
        try {
            final String resourceIdFinal = resourceId;
            final String nameFinal = name;
            final String categoryFinal = category;
            final int quantityFinal = quantity;
            final String unitFinal = unit;
            final String statusFinal = status;
            final String locationNameFinal = locationName;
            final double locationLatFinal = locationLat;
            final double locationLngFinal = locationLng;
            final String notesFinal = notes;
            
            String json = objectMapper.writeValueAsString(new Object() {
                public final String resource_id = resourceIdFinal;
                public final String name = nameFinal;
                public final String category = categoryFinal;
                public final int quantity = quantityFinal;
                public final String unit = unitFinal;
                public final String status = statusFinal;
                public final String location_name = locationNameFinal;
                public final double location_lat = locationLatFinal;
                public final double location_lng = locationLngFinal;
                public final String notes = notesFinal;
                public final long created_at = System.currentTimeMillis();
            });
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/resources/" + resourceId + ".json")
                .put(body)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading resource: " + e.getMessage());
            return false;
        }
    }
    
    private void downloadNewUsers() {
        try {
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.USERS_ENDPOINT)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    
                    // Check if response is valid JSON
                    if (jsonData.trim().startsWith("<")) {
                        System.out.println("? Skipping user download - Firebase returned HTML (likely empty collection)");
                        return;
                    }
                    
                    JsonNode users = objectMapper.readTree(jsonData);
                    
                    // Firebase returns an object with keys as IDs, not an array
                    if (users.isObject()) {
                        DatabaseManager dbManager = DatabaseManager.getInstance();
                        
                        users.fields().forEachRemaining(entry -> {
                            try {
                                JsonNode user = entry.getValue();
                                String userId = user.get("user_id").asText();
                                
                                // Check if user already exists
                                String checkSQL = "SELECT COUNT(*) FROM users WHERE user_id = ?";
                                try (java.sql.ResultSet rs = dbManager.executeQuery(checkSQL, userId)) {
                                    if (rs.next() && rs.getInt(1) == 0) {
                                        // Insert new user
                                        String insertSQL = "INSERT INTO users " +
                                            "(user_id, name, email, user_type, status, location_name, " +
                                            "location_lat, location_lng, timestamp, sync_status) " +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED')";
                                        
                                        dbManager.executeUpdate(insertSQL,
                                            userId,
                                            user.get("name").asText(),
                                            user.get("email").asText(),
                                            user.get("user_type").asText(),
                                            user.get("status").asText(),
                                            user.has("location_name") ? user.get("location_name").asText() : "",
                                            user.has("location_lat") ? user.get("location_lat").asDouble() : 0.0,
                                            user.has("location_lng") ? user.get("location_lng").asDouble() : 0.0,
                                            user.get("timestamp").asLong()
                                        );
                                        System.out.println("? Downloaded new user: " + user.get("name").asText());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing user: " + e.getMessage());
                            }
                        });
                    }
                } else {
                    System.out.println("? No new users to download (response: " + response.code() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error downloading users: " + e.getMessage());
            if (!e.getMessage().contains("Unexpected character")) {
                e.printStackTrace();
            }
        }
    }
    
    private void downloadNewResources() {
        try {
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + NetworkConfig.RESOURCES_ENDPOINT)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    
                    // Check if response is valid JSON
                    if (jsonData.trim().startsWith("<")) {
                        System.out.println("? Skipping resource download - Firebase returned HTML (likely empty collection)");
                        return;
                    }
                    
                    JsonNode resources = objectMapper.readTree(jsonData);
                    
                    // Firebase returns an object with keys as IDs, not an array
                    if (resources.isObject()) {
                        DatabaseManager dbManager = DatabaseManager.getInstance();
                        
                        resources.fields().forEachRemaining(entry -> {
                            try {
                                JsonNode resource = entry.getValue();
                                String resourceId = resource.get("resource_id").asText();
                                
                                // Check if resource already exists
                                String checkSQL = "SELECT COUNT(*) FROM resources WHERE resource_id = ?";
                                try (java.sql.ResultSet rs = dbManager.executeQuery(checkSQL, resourceId)) {
                                    if (rs.next() && rs.getInt(1) == 0) {
                                        // Insert new resource
                                        String insertSQL = "INSERT INTO resources " +
                                            "(resource_id, name, category, quantity, status, " +
                                            "location_lat, location_lng, timestamp, sync_status) " +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED')";
                                        
                                        dbManager.executeUpdate(insertSQL,
                                            resourceId,
                                            resource.get("name").asText(),
                                            resource.get("category").asText(),
                                            resource.get("quantity").asInt(),
                                            resource.get("status").asText(),
                                            resource.has("location_lat") ? resource.get("location_lat").asDouble() : 0.0,
                                            resource.has("location_lng") ? resource.get("location_lng").asDouble() : 0.0,
                                            resource.get("timestamp").asLong()
                                        );
                                        System.out.println("? Downloaded new resource: " + resource.get("name").asText());
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing resource: " + e.getMessage());
                            }
                        });
                    }
                } else {
                    System.out.println("? No new resources to download (response: " + response.code() + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Error downloading resources: " + e.getMessage());
            if (!e.getMessage().contains("Unexpected character")) {
                e.printStackTrace();
            }
        }
    }
}
