package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.network.CloudSyncManager;
import java.sql.ResultSet;

public class CheckFirebaseSync {
    public static void main(String[] args) {
        try {
            System.out.println("=== Firebase Sync Status Check ===");
            
            // Initialize database
            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.initializeDatabase();
            
            // Check local users using SQL
            System.out.println("\nLocal Database Users:");
            String sql = "SELECT user_id, email, name, password, sync_status FROM users ORDER BY user_id";
            
            int localCount = 0;
            int withPassword = 0;
            
            dbManager.executeQueryWithCallback(sql, (rs) -> {
                try {
                    int count = 0;
                    int passCount = 0;
                    while (rs.next()) {
                        count++;
                        String userId = rs.getString("user_id");
                        String email = rs.getString("email");
                        String name = rs.getString("name");
                        String password = rs.getString("password");
                        String syncStatus = rs.getString("sync_status");
                        
                        boolean hasPassword = (password != null && !password.trim().isEmpty());
                        if (hasPassword) {
                            passCount++;
                        }
                        
                        System.out.printf("  %s | %s | %s | Pass: %s | Sync: %s%n", 
                            userId, email, name, hasPassword ? "YES" : "NO", syncStatus);
                    }
                    System.out.printf("\nLocal Summary: %d total users, %d with passwords%n", count, passCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            // Check Firebase sync
            System.out.println("\nAttempting Firebase sync...");
            CloudSyncManager syncManager = CloudSyncManager.getInstance();
            
            // Try to download users from Firebase
            syncManager.downloadAllUsers();
            
            // Check again after sync
            System.out.println("\nAfter Firebase sync - Local Database Users:");
            dbManager.executeQueryWithCallback(sql, (rs) -> {
                try {
                    int count = 0;
                    int passCount = 0;
                    while (rs.next()) {
                        count++;
                        String userId = rs.getString("user_id");
                        String email = rs.getString("email");
                        String name = rs.getString("name");
                        String password = rs.getString("password");
                        String syncStatus = rs.getString("sync_status");
                        
                        boolean hasPassword = (password != null && !password.trim().isEmpty());
                        if (hasPassword) {
                            passCount++;
                        }
                        
                        System.out.printf("  %s | %s | %s | Pass: %s | Sync: %s%n", 
                            userId, email, name, hasPassword ? "YES" : "NO", syncStatus);
                    }
                    System.out.printf("\nAfter Sync Summary: %d total users, %d with passwords%n", count, passCount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
