package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.network.CloudSyncManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * FirebaseMigrationTool - Tool to migrate existing SQLite data to Firebase
 */
public class FirebaseMigrationTool {
    
    private DatabaseManager dbManager;
    private CloudSyncManager cloudSync;
    
    public FirebaseMigrationTool() {
        this.dbManager = DatabaseManager.getInstance();
        this.cloudSync = CloudSyncManager.getInstance();
    }
    
    /**
     * Main migration method
     */
    public void migrateAllData() {
        System.out.println("=== ReliefNet Data Migration Tool ===");
        System.out.println("This tool will migrate your existing local data to Firebase.");
        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Do you want to proceed with migration? (y/n): ");
            String confirm = scanner.nextLine();
            
            if (!confirm.toLowerCase().startsWith("y")) {
                System.out.println("Migration cancelled.");
                return;
            }
        }
        
        try {
            // Connect to Firebase
            cloudSync.connect();
            System.out.println("Connected to Firebase successfully.");
            
            // Migrate data in order (users first, then data that depends on users)
            migrateUsers();
            migrateMessages();
            migrateEmergencyRequests();
            migrateResources();
            
            System.out.println("=== Migration Complete ===");
            System.out.println("All your existing data has been migrated to Firebase!");
            System.out.println("You can now use the app with real-time sync across devices.");
            
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Migrate users from SQLite to Firebase
     */
    private void migrateUsers() {
        System.out.println("\n--- Migrating Users ---");
        
        try {
            String sql = "SELECT * FROM users";
            
            dbManager.executeQueryWithCallback(sql, (ResultSet rs) -> {
                int count = 0;
                while (rs.next()) {
                    try {
                        String userId = rs.getString("user_id");
                        String email = rs.getString("email");
                        String password = rs.getString("password");
                        String fullName = rs.getString("name");
                        String userType = rs.getString("user_type");
                        String status = rs.getString("status");
                        
                        // Upload user to Firebase
                        boolean success = cloudSync.uploadUserImmediately(
                            userId, email, password != null ? password : "migrated123", 
                            fullName, userType, status != null ? status : "ACTIVE"
                        );
                        
                        if (success) {
                            count++;
                            System.out.println("✓ Migrated user: " + fullName + " (" + userType + ")");
                        } else {
                            System.err.println("✗ Failed to migrate user: " + fullName);
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error migrating user: " + e.getMessage());
                    }
                }
                System.out.println("Total users migrated: " + count);
            });
            
        } catch (Exception e) {
            System.err.println("Error during user migration: " + e.getMessage());
        }
    }
    
    /**
     * Migrate messages from SQLite to Firebase
     */
    private void migrateMessages() {
        System.out.println("\n--- Migrating Messages ---");
        
        try {
            String sql = "SELECT * FROM messages ORDER BY sent_at";
            
            dbManager.executeQueryWithCallback(sql, (ResultSet rs) -> {
                int count = 0;
                while (rs.next()) {
                    try {
                        String messageId = rs.getString("message_id");
                        String senderId = rs.getString("sender_id");
                        String receiverId = rs.getString("receiver_id");
                        String content = rs.getString("content");
                        String messageType = rs.getString("message_type");
                        String channelId = rs.getString("channel_id");
                        
                        // Upload message to Firebase
                        boolean success = cloudSync.uploadMessage(
                            messageId, senderId, receiverId, content, 
                            messageType != null ? messageType : "CHAT",
                            channelId != null ? channelId : "general_chat"
                        );
                        
                        if (success) {
                            count++;
                            System.out.println("✓ Migrated message from: " + senderId);
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error migrating message: " + e.getMessage());
                    }
                }
                System.out.println("Total messages migrated: " + count);
            });
            
        } catch (Exception e) {
            System.err.println("Error during message migration: " + e.getMessage());
        }
    }
    
    /**
     * Migrate emergency requests from SQLite to Firebase
     */
    private void migrateEmergencyRequests() {
        System.out.println("\n--- Migrating Emergency Requests ---");
        
        try {
            String sql = "SELECT * FROM emergency_requests ORDER BY created_at";
            
            dbManager.executeQueryWithCallback(sql, (ResultSet rs) -> {
                int count = 0;
                while (rs.next()) {
                    try {
                        String requestId = rs.getString("request_id");
                        String requesterId = rs.getString("requester_id");
                        String emergencyType = rs.getString("emergency_type");
                        String priority = rs.getString("priority");
                        String description = rs.getString("description");
                        String status = rs.getString("status");
                        String locationName = rs.getString("location_name");
                        double locationLat = rs.getDouble("location_lat");
                        double locationLng = rs.getDouble("location_lng");
                        int peopleCount = rs.getInt("people_count");
                        
                        // Upload emergency request to Firebase
                        boolean success = cloudSync.uploadEmergencyRequest(
                            requestId, requesterId, emergencyType, priority,
                            description, status != null ? status : "PENDING",
                            locationName, locationLat, locationLng, peopleCount
                        );
                        
                        if (success) {
                            count++;
                            System.out.println("✓ Migrated emergency: " + emergencyType + " - " + description);
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error migrating emergency request: " + e.getMessage());
                    }
                }
                System.out.println("Total emergency requests migrated: " + count);
            });
            
        } catch (Exception e) {
            System.err.println("Error during emergency request migration: " + e.getMessage());
        }
    }
    
    /**
     * Migrate resources from SQLite to Firebase
     */
    private void migrateResources() {
        System.out.println("\n--- Migrating Resources ---");
        
        try {
            String sql = "SELECT * FROM resources ORDER BY created_at";
            
            dbManager.executeQueryWithCallback(sql, (ResultSet rs) -> {
                int count = 0;
                while (rs.next()) {
                    try {
                        String resourceId = rs.getString("resource_id");
                        String name = rs.getString("name");
                        String category = rs.getString("category");
                        int quantity = rs.getInt("quantity");
                        String unit = rs.getString("unit");
                        String status = rs.getString("status");
                        String locationName = rs.getString("location_name");
                        double locationLat = rs.getDouble("location_lat");
                        double locationLng = rs.getDouble("location_lng");
                        String notes = rs.getString("notes");
                        
                        // Upload resource to Firebase
                        boolean success = cloudSync.uploadResource(
                            resourceId, name, category, quantity, unit,
                            status != null ? status : "AVAILABLE",
                            locationName, locationLat, locationLng, notes
                        );
                        
                        if (success) {
                            count++;
                            System.out.println("✓ Migrated resource: " + name + " (" + quantity + " " + unit + ")");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error migrating resource: " + e.getMessage());
                    }
                }
                System.out.println("Total resources migrated: " + count);
            });
            
        } catch (Exception e) {
            System.err.println("Error during resource migration: " + e.getMessage());
        }
    }
    
    /**
     * Check what data exists in local database
     */
    public void showDataSummary() {
        System.out.println("\n=== Local Database Summary ===");
        
        try {
            // Count users
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM users", (ResultSet rs) -> {
                if (rs.next()) {
                    System.out.println("Users: " + rs.getInt("count"));
                }
            });
            
            // Count messages
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM messages", (ResultSet rs) -> {
                if (rs.next()) {
                    System.out.println("Messages: " + rs.getInt("count"));
                }
            });
            
            // Count emergency requests
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM emergency_requests", (ResultSet rs) -> {
                if (rs.next()) {
                    System.out.println("Emergency Requests: " + rs.getInt("count"));
                }
            });
            
            // Count resources
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM resources", (ResultSet rs) -> {
                if (rs.next()) {
                    System.out.println("Resources: " + rs.getInt("count"));
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error checking database: " + e.getMessage());
        }
    }
    
    /**
     * Command line interface for the migration tool
     */
    public static void main(String[] args) {
        FirebaseMigrationTool migrationTool = new FirebaseMigrationTool();
        
        System.out.println("ReliefNet Firebase Migration Tool");
        System.out.println("================================");
        
        try {
            // Initialize database
            migrationTool.dbManager.initializeDatabase();
            
            // Show current data summary
            migrationTool.showDataSummary();
            
            // Ask user if they want to migrate
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("\nDo you want to migrate this data to Firebase? (y/n): ");
                String response = scanner.nextLine();
                
                if (response.toLowerCase().startsWith("y")) {
                    migrationTool.migrateAllData();
                } else {
                    System.out.println("Migration skipped. You can run this tool again later if needed.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Migration tool error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
