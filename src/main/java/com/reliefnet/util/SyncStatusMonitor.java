package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.network.CloudSyncManager;
import com.reliefnet.network.NetworkConfig;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.sql.ResultSet;

/**
 * SyncStatusMonitor - Provides comprehensive monitoring and status reporting for data synchronization
 */
public class SyncStatusMonitor {
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DatabaseManager dbManager;
    
    public SyncStatusMonitor() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.dbManager = DatabaseManager.getInstance();
    }
    
    /**
     * Generate a comprehensive sync status report
     */
    public void generateSyncReport() {
        System.out.println("\n=== RELIEFNET SYNC STATUS REPORT ===");
        System.out.println("Generated: " + new java.util.Date());
        System.out.println("=====================================\n");
        
        // Local database status
        printLocalDataStatus();
        
        // Firebase connectivity
        printFirebaseConnectivity();
        
        // Firebase data counts
        printFirebaseDataCounts();
        
        // Sync status by data type
        printSyncStatusSummary();
        
        // Data consistency check
        printDataConsistencyCheck();
        
        System.out.println("=== END OF REPORT ===\n");
    }
    
    private void printLocalDataStatus() {
        System.out.println("? LOCAL DATABASE STATUS:");
        try {
            // Users
            String userCount = getSingleValue("SELECT COUNT(*) FROM users");
            String pendingUsers = getSingleValue("SELECT COUNT(*) FROM users WHERE sync_status = 'PENDING' OR sync_status IS NULL");
            System.out.println("  Users: " + userCount + " total, " + pendingUsers + " pending sync");
            
            // Messages
            String messageCount = getSingleValue("SELECT COUNT(*) FROM messages");
            String pendingMessages = getSingleValue("SELECT COUNT(*) FROM messages WHERE sync_status = 'PENDING' OR sync_status IS NULL");
            System.out.println("  Messages: " + messageCount + " total, " + pendingMessages + " pending sync");
            
            // Emergency Requests
            String emergencyCount = getSingleValue("SELECT COUNT(*) FROM emergency_requests");
            String pendingEmergencies = getSingleValue("SELECT COUNT(*) FROM emergency_requests WHERE sync_status = 'PENDING' OR sync_status IS NULL");
            System.out.println("  Emergencies: " + emergencyCount + " total, " + pendingEmergencies + " pending sync");
            
            // Resources
            String resourceCount = getSingleValue("SELECT COUNT(*) FROM resources");
            String pendingResources = getSingleValue("SELECT COUNT(*) FROM resources WHERE sync_status = 'PENDING' OR sync_status IS NULL");
            System.out.println("  Resources: " + resourceCount + " total, " + pendingResources + " pending sync");
            
        } catch (Exception e) {
            System.err.println("  ✗ Error reading local database: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void printFirebaseConnectivity() {
        System.out.println("? FIREBASE CONNECTIVITY:");
        try {
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/users.json")
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("  ✓ Firebase connection: SUCCESS");
                    System.out.println("  ✓ Response code: " + response.code());
                } else {
                    System.out.println("  ✗ Firebase connection: FAILED");
                    System.out.println("  ✗ Response code: " + response.code());
                }
            }
        } catch (Exception e) {
            System.out.println("  ✗ Firebase connection: ERROR - " + e.getMessage());
        }
        System.out.println();
    }
    
    private void printFirebaseDataCounts() {
        System.out.println("? FIREBASE DATA COUNTS:");
        
        // Users
        int firebaseUserCount = getFirebaseCount("/users.json");
        System.out.println("  Users in Firebase: " + firebaseUserCount);
        
        // Messages  
        int firebaseMessageCount = getFirebaseCount("/messages.json");
        System.out.println("  Messages in Firebase: " + firebaseMessageCount);
        
        // Emergencies
        int firebaseEmergencyCount = getFirebaseCount("/emergencies.json");
        System.out.println("  Emergencies in Firebase: " + firebaseEmergencyCount);
        
        // Resources
        int firebaseResourceCount = getFirebaseCount("/resources.json");
        System.out.println("  Resources in Firebase: " + firebaseResourceCount);
        
        System.out.println();
    }
    
    private void printSyncStatusSummary() {
        System.out.println("? SYNC STATUS SUMMARY:");
        try {
            // Overall sync health
            String totalRecords = getSingleValue(
                "SELECT (SELECT COUNT(*) FROM users) + " +
                "(SELECT COUNT(*) FROM messages) + " +
                "(SELECT COUNT(*) FROM emergency_requests) + " +
                "(SELECT COUNT(*) FROM resources)"
            );
            
            String pendingRecords = getSingleValue(
                "SELECT (SELECT COUNT(*) FROM users WHERE sync_status = 'PENDING' OR sync_status IS NULL) + " +
                "(SELECT COUNT(*) FROM messages WHERE sync_status = 'PENDING' OR sync_status IS NULL) + " +
                "(SELECT COUNT(*) FROM emergency_requests WHERE sync_status = 'PENDING' OR sync_status IS NULL) + " +
                "(SELECT COUNT(*) FROM resources WHERE sync_status = 'PENDING' OR sync_status IS NULL)"
            );
            
            int total = Integer.parseInt(totalRecords);
            int pending = Integer.parseInt(pendingRecords);
            int synced = total - pending;
            double syncPercentage = total > 0 ? (synced * 100.0 / total) : 100.0;
            
            System.out.println("  Total records: " + total);
            System.out.println("  Synced records: " + synced);
            System.out.println("  Pending records: " + pending);
            System.out.println("  Sync completion: " + String.format("%.1f", syncPercentage) + "%");
            
            if (syncPercentage >= 95) {
                System.out.println("  ✓ Sync Status: EXCELLENT");
            } else if (syncPercentage >= 80) {
                System.out.println("  ? Sync Status: GOOD");
            } else if (syncPercentage >= 50) {
                System.out.println("  ⚠ Sync Status: NEEDS ATTENTION");
            } else {
                System.out.println("  ✗ Sync Status: CRITICAL");
            }
            
        } catch (Exception e) {
            System.err.println("  ✗ Error calculating sync status: " + e.getMessage());
        }
        System.out.println();
    }
    
    private void printDataConsistencyCheck() {
        System.out.println("? DATA CONSISTENCY CHECK:");
        try {
            String localUserCount = getSingleValue("SELECT COUNT(*) FROM users");
            int firebaseUserCount = getFirebaseCount("/users.json");
            
            System.out.println("  Local users: " + localUserCount);
            System.out.println("  Firebase users: " + firebaseUserCount);
            
            if (Integer.parseInt(localUserCount) == firebaseUserCount) {
                System.out.println("  ✓ User data consistency: PERFECT MATCH");
            } else {
                System.out.println("  ⚠ User data consistency: MISMATCH DETECTED");
                System.out.println("    Difference: " + Math.abs(Integer.parseInt(localUserCount) - firebaseUserCount) + " records");
            }
            
        } catch (Exception e) {
            System.err.println("  ✗ Error checking data consistency: " + e.getMessage());
        }
        System.out.println();
    }
    
    private String getSingleValue(String sql) throws Exception {
        try (ResultSet rs = dbManager.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
            return "0";
        }
    }
    
    private int getFirebaseCount(String endpoint) {
        try {
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + endpoint)
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    
                    if (jsonData.trim().equals("null") || jsonData.trim().isEmpty()) {
                        return 0;
                    }
                    
                    JsonNode data = objectMapper.readTree(jsonData);
                    if (data.isObject()) {
                        return data.size();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Firebase count for " + endpoint + ": " + e.getMessage());
        }
        return -1; // Indicates error
    }
    
    /**
     * Run a quick health check
     */
    public boolean isSystemHealthy() {
        try {
            // Check database connection
            dbManager.executeQuery("SELECT 1");
            
            // Check Firebase connection
            Request request = new Request.Builder()
                .url(NetworkConfig.API_BASE_URL + "/users.json")
                .build();
                
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Main method for standalone execution
     */
    public static void main(String[] args) {
        SyncStatusMonitor monitor = new SyncStatusMonitor();
        monitor.generateSyncReport();
    }
}
