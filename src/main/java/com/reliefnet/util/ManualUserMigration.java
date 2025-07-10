package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.network.CloudSyncManager;
import java.sql.*;

public class ManualUserMigration {
    
    public static void main(String[] args) {
        System.out.println("🔄 Starting manual migration of all users...");
        
        // Skip the full app initialization - just run migration
        try {
            // Direct database connection
            Connection conn = DriverManager.getConnection("jdbc:sqlite:reliefnet.db");
            CloudSyncManager cloudSync = CloudSyncManager.getInstance();
            
            // Connect to cloud
            cloudSync.connect();
            System.out.println("✓ Connected to Firebase");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT user_id, email, name, user_type, status, password FROM users");
            
            int total = 0;
            int successful = 0;
            int failed = 0;
            
            while (rs.next()) {
                total++;
                String userId = rs.getString("user_id");
                String email = rs.getString("email");
                String name = rs.getString("name");
                String userType = rs.getString("user_type");
                String status = rs.getString("status");
                String password = rs.getString("password");
                
                System.out.println("\n" + total + ". Migrating: " + name + " (" + userId + ")");
                
                try {
                    boolean success = cloudSync.uploadUserImmediately(
                        userId, 
                        email != null ? email : "", 
                        password != null ? password : "migrated123", 
                        name, 
                        userType, 
                        status != null ? status : "ACTIVE"
                    );
                    
                    if (success) {
                        successful++;
                        System.out.println("   ✓ SUCCESS");
                    } else {
                        failed++;
                        System.out.println("   ✗ FAILED");
                    }
                    
                    // Small delay to avoid rate limiting
                    Thread.sleep(200);
                    
                } catch (Exception e) {
                    failed++;
                    System.out.println("   ✗ ERROR: " + e.getMessage());
                }
            }
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("📊 MIGRATION SUMMARY");
            System.out.println("=".repeat(50));
            System.out.println("Total users: " + total);
            System.out.println("Successful: " + successful);
            System.out.println("Failed: " + failed);
            System.out.println("Success rate: " + String.format("%.1f%%", (successful * 100.0 / total)));
            System.out.println("=".repeat(50));
            
            conn.close();
            
        } catch (Exception e) {
            System.err.println("Error setting up migration: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.exit(0); // Force exit to avoid JavaFX startup
    }
}
