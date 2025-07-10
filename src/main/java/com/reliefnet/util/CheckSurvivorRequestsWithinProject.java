package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import java.sql.*;

public class CheckSurvivorRequestsWithinProject {
    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            System.out.println("=== Checking for survivor-generated requests ===");
              // Check emergency_requests table for survivor submissions
            String sql = "SELECT request_id, emergency_type, description, status, assigned_volunteer, submitted_by, created_at FROM emergency_requests WHERE submitted_by IS NOT NULL AND submitted_by != ''";
            
            try (ResultSet rs = dbManager.executeQuery(sql)) {
                
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("Request ID: %s%n", rs.getString("request_id"));
                    System.out.printf("  Type: %s%n", rs.getString("emergency_type"));
                    System.out.printf("  Status: %s%n", rs.getString("status"));
                    System.out.printf("  Assigned Volunteer: %s%n", rs.getString("assigned_volunteer"));
                    System.out.printf("  Submitted by: %s%n", rs.getString("submitted_by"));
                    System.out.printf("  Created: %s%n", rs.getString("created_at"));
                    System.out.printf("  Description: %s%n", rs.getString("description"));
                    System.out.println("  ---");
                }
                
                if (!found) {
                    System.out.println("No survivor-generated requests found.");                    System.out.println("Creating a test survivor request...");
                    
                    // Create a test survivor request using executeUpdate
                    String insertSql = "INSERT INTO emergency_requests (request_id, emergency_type, description, status, priority, location, submitted_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    String requestId = "TEST_SURVIVOR_REQ_" + System.currentTimeMillis();
                    int result = dbManager.executeUpdate(insertSql, 
                        requestId,
                        "Medical",
                        "Need immediate medical assistance - survivor generated request for testing",
                        "PENDING",
                        "HIGH",
                        "Test Location",
                        "survivor_test_user",
                        new java.util.Date().toString()
                    );
                    System.out.println("Test survivor request created. Rows affected: " + result);
                }
            }
              // Also check volunteer_assignments
            System.out.println("\n=== Checking volunteer assignments ===");
            String vaSql = "SELECT * FROM volunteer_assignments ORDER BY created_at DESC LIMIT 10";
            try (ResultSet rs = dbManager.executeQuery(vaSql)) {
                
                while (rs.next()) {
                    System.out.printf("Assignment ID: %s | Volunteer: %s | Request: %s | Type: %s | Status: %s%n",
                            rs.getString("assignment_id"),
                            rs.getString("volunteer_id"),
                            rs.getString("request_id"),
                            rs.getString("assignment_type"),
                            rs.getString("status")
                    );
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
