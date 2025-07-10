package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;

public class DatabaseCleanup {
    public static void main(String[] args) {
        System.out.println("=== Cleaning up Database ===");
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // 1. Remove SOS entries from emergency_requests table
            System.out.println("1. Removing SOS entries from emergency_requests table...");
            String deleteSOSFromEmergency = "DELETE FROM emergency_requests WHERE description LIKE '%SOS%' OR requester_id = 'SOS_USER'";
            int sosDeleted = dbManager.executeUpdate(deleteSOSFromEmergency);
            System.out.println("   Removed " + sosDeleted + " SOS entries from emergency_requests");
            
            // 2. Clear all test messages from messages table
            System.out.println("2. Clearing test messages from messages table...");
            String clearMessages = "DELETE FROM messages";
            int messagesDeleted = dbManager.executeUpdate(clearMessages);
            System.out.println("   Removed " + messagesDeleted + " test messages");
            
            // 3. Check if sos_alerts table exists and show its structure
            System.out.println("3. Checking sos_alerts table...");
            String checkSOSTable = "SELECT name FROM sqlite_master WHERE type='table' AND name='sos_alerts'";
            try (java.sql.ResultSet rs = dbManager.executeQuery(checkSOSTable)) {
                if (rs.next()) {
                    System.out.println("   ✓ sos_alerts table exists");
                    
                    // Show table structure
                    String tableInfoSql = "PRAGMA table_info(sos_alerts)";
                    System.out.println("   SOS alerts table structure:");
                    try (java.sql.ResultSet tableInfo = dbManager.executeQuery(tableInfoSql)) {
                        while (tableInfo.next()) {
                            String columnName = tableInfo.getString("name");
                            String columnType = tableInfo.getString("type");
                            System.out.println("     - " + columnName + " (" + columnType + ")");
                        }
                    }
                } else {
                    System.err.println("   ✗ sos_alerts table does NOT exist!");
                }
            }
            
            // 4. Count remaining records
            System.out.println("4. Final record counts:");
            
            String countEmergencyRequests = "SELECT COUNT(*) as count FROM emergency_requests";
            try (java.sql.ResultSet rs = dbManager.executeQuery(countEmergencyRequests)) {
                if (rs.next()) {
                    System.out.println("   Emergency requests: " + rs.getInt("count"));
                }
            }
            
            String countMessages = "SELECT COUNT(*) as count FROM messages";
            try (java.sql.ResultSet rs = dbManager.executeQuery(countMessages)) {
                if (rs.next()) {
                    System.out.println("   Messages: " + rs.getInt("count"));
                }
            }
            
            String countSOSAlerts = "SELECT COUNT(*) as count FROM sos_alerts";
            try (java.sql.ResultSet rs = dbManager.executeQuery(countSOSAlerts)) {
                if (rs.next()) {
                    System.out.println("   SOS alerts: " + rs.getInt("count"));
                }
            }
            
            System.out.println("✓ Database cleanup completed successfully!");
            
        } catch (Exception e) {
            System.err.println("✗ Database cleanup failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== End Database Cleanup ===");
    }
}
