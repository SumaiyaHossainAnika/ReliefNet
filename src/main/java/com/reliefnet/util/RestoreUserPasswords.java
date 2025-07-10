package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import java.sql.ResultSet;
import java.util.*;

public class RestoreUserPasswords {
    
    // Known passwords for users who should have them (from previous sessions)
    private static final Map<String, String> KNOWN_PASSWORDS = new HashMap<>();
    
    static {
        KNOWN_PASSWORDS.put("afi", "afifa123");
        KNOWN_PASSWORDS.put("ani", "anika123");
        KNOWN_PASSWORDS.put("anik", "anik123");
        KNOWN_PASSWORDS.put("anita", "anita123");
        KNOWN_PASSWORDS.put("atif", "atif123");
        KNOWN_PASSWORDS.put("karim", "karim123");
        KNOWN_PASSWORDS.put("omi", "omi123");
        KNOWN_PASSWORDS.put("rah", "rahim123");
        KNOWN_PASSWORDS.put("rid", "ridhi123");
        KNOWN_PASSWORDS.put("riya", "riya123");
        KNOWN_PASSWORDS.put("sum", "sumaiya123");
        KNOWN_PASSWORDS.put("test_authority", "admin123");
        KNOWN_PASSWORDS.put("test_survivor", "survivor123");
        KNOWN_PASSWORDS.put("test_volunteer_approved", "volunteer123");
        KNOWN_PASSWORDS.put("test_volunteer_location", "location123");
    }

    public static void main(String[] args) {
        try {
            System.out.println("=== Restoring User Passwords ===");
            
            // Initialize database
            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.initializeDatabase();
            
            // Check current users and restore passwords
            String selectSql = "SELECT user_id, email, name, password FROM users WHERE password IS NULL OR password = ''";
            
            dbManager.executeQueryWithCallback(selectSql, (rs) -> {
                try {
                    List<String> usersToRestore = new ArrayList<>();
                    List<String> usersFromOtherDevices = new ArrayList<>();
                    
                    while (rs.next()) {
                        String userId = rs.getString("user_id");
                        String email = rs.getString("email");
                        String name = rs.getString("name");
                        
                        if (KNOWN_PASSWORDS.containsKey(userId)) {
                            usersToRestore.add(userId);
                            System.out.println("Will restore password for: " + userId + " (" + name + ")");
                        } else {
                            usersFromOtherDevices.add(userId);
                            System.out.println("User from other device (needs first-time setup): " + userId + " (" + name + ")");
                        }
                    }
                    
                    // Restore known passwords
                    System.out.println("\nRestoring passwords for known users...");
                    for (String userId : usersToRestore) {
                        String password = KNOWN_PASSWORDS.get(userId);
                        String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
                        
                        try {
                            int updated = dbManager.executeUpdate(updateSql, password, userId);
                            if (updated > 0) {
                                System.out.println("✓ Restored password for: " + userId);
                            } else {
                                System.out.println("✗ Failed to restore password for: " + userId);
                            }
                        } catch (Exception e) {
                            System.out.println("✗ Error restoring password for " + userId + ": " + e.getMessage());
                        }
                    }
                    
                    // For users from other devices, set a special flag
                    System.out.println("\nMarking users from other devices for first-time setup...");
                    for (String userId : usersFromOtherDevices) {
                        String updateSql = "UPDATE users SET password = 'NEEDS_FIRST_TIME_SETUP' WHERE user_id = ?";
                        
                        try {
                            int updated = dbManager.executeUpdate(updateSql, userId);
                            if (updated > 0) {
                                System.out.println("✓ Marked for first-time setup: " + userId);
                            }
                        } catch (Exception e) {
                            System.out.println("✗ Error marking user " + userId + ": " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            
            // Show final status
            System.out.println("\n=== Final User Status ===");
            String finalSql = "SELECT user_id, email, name, password FROM users ORDER BY user_id";
            
            dbManager.executeQueryWithCallback(finalSql, (rs) -> {
                try {
                    int total = 0;
                    int withPassword = 0;
                    int needsSetup = 0;
                    
                    while (rs.next()) {
                        total++;
                        String userId = rs.getString("user_id");
                        String email = rs.getString("email");
                        String name = rs.getString("name");
                        String password = rs.getString("password");
                        
                        String status;
                        if (password == null || password.trim().isEmpty()) {
                            status = "NO PASSWORD";
                        } else if ("NEEDS_FIRST_TIME_SETUP".equals(password)) {
                            status = "NEEDS SETUP";
                            needsSetup++;
                        } else {
                            status = "HAS PASSWORD";
                            withPassword++;
                        }
                        
                        System.out.printf("  %s | %s | %s | %s%n", userId, email, name, status);
                    }
                    
                    System.out.printf("\nSummary: %d total, %d with password, %d need first-time setup%n", 
                        total, withPassword, needsSetup);
                    
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
