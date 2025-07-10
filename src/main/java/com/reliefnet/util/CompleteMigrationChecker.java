package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive migration checker for all data types
 */
public class CompleteMigrationChecker {
    
    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            System.out.println("=== COMPLETE MIGRATION STATUS CHECK ===");
            System.out.println();
            
            // Check Users
            AtomicInteger userCount = new AtomicInteger(0);
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM users", rs -> {
                if (rs.next()) {
                    userCount.set(rs.getInt("count"));
                }
            });
            System.out.println(String.format("%-20s: %d records", "USERS", userCount.get()));
            
            System.out.println("Sample users:");
            dbManager.executeQueryWithCallback("SELECT user_id, name, email, user_type FROM users LIMIT 5", rs -> {
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("user_id") + " | " + 
                                     rs.getString("name") + " | " + 
                                     rs.getString("email") + " | " + 
                                     rs.getString("user_type"));
                }
            });
            System.out.println();
            
            // Check Messages
            AtomicInteger messageCount = new AtomicInteger(0);
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM messages", rs -> {
                if (rs.next()) {
                    messageCount.set(rs.getInt("count"));
                }
            });
            System.out.println(String.format("%-20s: %d records", "MESSAGES", messageCount.get()));
            
            System.out.println("Sample messages:");
            dbManager.executeQueryWithCallback("SELECT message_id, sender_id, content, timestamp FROM messages LIMIT 3", rs -> {
                while (rs.next()) {
                    String content = rs.getString("content");
                    if (content != null && content.length() > 30) {
                        content = content.substring(0, 30) + "...";
                    }
                    System.out.println("  - " + rs.getString("message_id") + " | " + 
                                     rs.getString("sender_id") + " | " + 
                                     content + " | " + 
                                     rs.getLong("timestamp"));
                }
            });
            System.out.println();
            
            // Check Emergency Requests
            AtomicInteger emergencyCount = new AtomicInteger(0);
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM emergency_requests", rs -> {
                if (rs.next()) {
                    emergencyCount.set(rs.getInt("count"));
                }
            });
            System.out.println(String.format("%-20s: %d records", "EMERGENCY REQUESTS", emergencyCount.get()));
            
            System.out.println("Sample emergencies:");
            dbManager.executeQueryWithCallback("SELECT request_id, user_id, emergency_type, priority, status FROM emergency_requests LIMIT 3", rs -> {
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("request_id") + " | " + 
                                     rs.getString("user_id") + " | " + 
                                     rs.getString("emergency_type") + " | " + 
                                     rs.getString("priority") + " | " + 
                                     rs.getString("status"));
                }
            });
            System.out.println();
            
            // Check Resources
            AtomicInteger resourceCount = new AtomicInteger(0);
            dbManager.executeQueryWithCallback("SELECT COUNT(*) as count FROM resources", rs -> {
                if (rs.next()) {
                    resourceCount.set(rs.getInt("count"));
                }
            });
            System.out.println(String.format("%-20s: %d records", "RESOURCES", resourceCount.get()));
            
            System.out.println("Sample resources:");
            dbManager.executeQueryWithCallback("SELECT resource_id, resource_name, category, quantity, status FROM resources LIMIT 3", rs -> {
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("resource_id") + " | " + 
                                     rs.getString("resource_name") + " | " + 
                                     rs.getString("category") + " | " + 
                                     rs.getInt("quantity") + " | " + 
                                     rs.getString("status"));
                }
            });
            System.out.println();
            
            System.out.println("=== MIGRATION ANALYSIS ===");
            int totalRecords = userCount.get() + messageCount.get() + emergencyCount.get() + resourceCount.get();
            System.out.println("Total records in local database: " + totalRecords);
            System.out.println();
            System.out.println("If only 1 user appeared in Firebase out of this data,");
            System.out.println("then the migration likely failed for ALL data types!");
            System.out.println("This suggests a systematic issue with the migration process.");
            System.out.println();
            System.out.println("Possible causes:");
            System.out.println("1. Firebase authentication/connection issues");
            System.out.println("2. Network connectivity problems during migration");
            System.out.println("3. Silent exceptions in the migration code");
            System.out.println("4. Firebase API rate limits or quotas");
            System.out.println("5. Malformed data that causes upload failures");
            System.out.println("6. Migration only uploaded the first record of each type");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("Error checking migration status: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
