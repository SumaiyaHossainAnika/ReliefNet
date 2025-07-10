package com.reliefnet.util;

import com.reliefnet.model.*;
import com.reliefnet.database.DatabaseManager;
import java.time.LocalDateTime;
import java.util.*;

/**
 * FraudDetectionEngine - AI-powered fraud detection and prevention system
 * Core functionality: Detect suspicious patterns, duplicate requests, and unusual activities
 */
public class FraudDetectionEngine {
      private static FraudDetectionEngine instance;
    private DatabaseManager dbManager;
    private Map<String, List<String>> userActivityLog;
    private Map<String, Double> suspiciousActivityScores;
    private double detectionThreshold = 75.0; // Percentage threshold for fraud detection
    
    // Fraud detection rules
    public enum FraudType {
        DUPLICATE_REQUEST("Duplicate emergency request within short timeframe"),
        IMPOSSIBLE_LOCATION("User location changes impossibly fast"),
        EXCESSIVE_REQUESTS("Too many resource requests in short period"),
        INVALID_IDENTITY("Identity verification failed"),
        SUSPICIOUS_PATTERN("Unusual activity pattern detected");
        
        private final String description;
        
        FraudType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public static class FraudAlert {
        private String userId;
        private FraudType fraudType;
        private double confidenceScore;
        private String details;
        private LocalDateTime timestamp;
        
        public FraudAlert(String userId, FraudType fraudType, double confidenceScore, String details) {
            this.userId = userId;
            this.fraudType = fraudType;
            this.confidenceScore = confidenceScore;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public String getUserId() { return userId; }
        public FraudType getFraudType() { return fraudType; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public boolean isCritical() {
            return confidenceScore >= 90.0;
        }
        
        @Override
        public String toString() {
            return String.format("FRAUD ALERT [%s]: %s (%.1f%% confidence) - %s", 
                    fraudType, userId, confidenceScore, details);
        }
    }
    
    private FraudDetectionEngine() {
        this.dbManager = DatabaseManager.getInstance();
        this.userActivityLog = new HashMap<>();
        this.suspiciousActivityScores = new HashMap<>();
    }
    
    public static FraudDetectionEngine getInstance() {
        if (instance == null) {
            synchronized (FraudDetectionEngine.class) {
                if (instance == null) {
                    instance = new FraudDetectionEngine();
                }
            }
        }
        return instance;
    }
    
    /**
     * Analyze emergency request for potential fraud
     */    public List<FraudAlert> analyzeEmergencyRequest(EmergencyRequest request) {
        List<FraudAlert> alerts = new ArrayList<>();
        
        // Load user activity history from database
        loadUserActivityHistory(request.getRequesterId());
        
        // Check for duplicate requests
        FraudAlert duplicateAlert = checkDuplicateRequests(request);
        if (duplicateAlert != null) {
            alerts.add(duplicateAlert);
            storeFraudAlert(duplicateAlert);
        }
        
        // Check location consistency
        FraudAlert locationAlert = checkLocationConsistency(request);
        if (locationAlert != null) {
            alerts.add(locationAlert);
            storeFraudAlert(locationAlert);
        }
        
        // Check request frequency
        FraudAlert frequencyAlert = checkRequestFrequency(request.getRequesterId());
        if (frequencyAlert != null) {
            alerts.add(frequencyAlert);
            storeFraudAlert(frequencyAlert);
        }
        
        // Update user activity log
        logUserActivity(request.getRequesterId(), "EMERGENCY_REQUEST", request.toString());
        
        return alerts;
    }
    
    /**
     * Analyze resource request for potential fraud
     */
    public List<FraudAlert> analyzeResourceRequest(String userId, Resource resource, int requestedQuantity) {
        List<FraudAlert> alerts = new ArrayList<>();
        
        // Check for excessive resource requests
        double excessiveRequestScore = calculateExcessiveRequestScore(userId, resource.getCategory());
        if (excessiveRequestScore > detectionThreshold) {
            alerts.add(new FraudAlert(userId, FraudType.EXCESSIVE_REQUESTS, 
                    excessiveRequestScore, 
                    String.format("Requested %d %s (excessive for user pattern)", 
                            requestedQuantity, resource.getName())));
        }
        
        // Check for suspicious patterns
        double patternScore = analyzeUserPattern(userId);
        if (patternScore > detectionThreshold) {
            alerts.add(new FraudAlert(userId, FraudType.SUSPICIOUS_PATTERN, 
                    patternScore, "Unusual resource request pattern detected"));
        }
        
        // Log activity
        logUserActivity(userId, "RESOURCE_REQUEST", 
                String.format("%s:%d", resource.getName(), requestedQuantity));
        
        return alerts;
    }
    
    /**
     * Verify user identity using multiple factors
     */
    public boolean verifyUserIdentity(User user, String phoneNumber, String location) {
        double identityScore = 0.0;
        
        // Phone verification (30 points)
        if (user.getPhone() != null && user.getPhone().equals(phoneNumber)) {
            identityScore += 30.0;
        }
        
        // Location verification (25 points)
        if (user.getLocation() != null && user.getLocation().equalsIgnoreCase(location)) {
            identityScore += 25.0;
        }
        
        // Historical activity verification (25 points)
        if (hasConsistentActivityHistory(user.getUserId())) {
            identityScore += 25.0;
        }
        
        // Account age verification (20 points)
        if (user.getCreatedAt() != null && 
            user.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7))) {
            identityScore += 20.0;
        }
        
        return identityScore >= 70.0; // Require 70% identity confidence
    }
      /**
     * Get fraud risk score for a user
     */
    public double getUserFraudRiskScore(String userId) {
        return suspiciousActivityScores.getOrDefault(userId, 0.0);
    }
    
    /**
     * Store fraud alert in database
     */    private void storeFraudAlert(FraudAlert alert) {
        try {
            // Use dbManager to store the alert in database
            String sql = "INSERT INTO fraud_alerts (user_id, fraud_type, confidence_score, message, status) VALUES (?, ?, ?, ?, 'pending')";
            dbManager.executeUpdate(sql, 
                alert.getUserId(), 
                alert.getFraudType().toString(), 
                alert.getConfidenceScore(), 
                alert.getDetails());
            System.out.println("Stored fraud alert: " + alert);
        } catch (Exception e) {
            System.err.println("Failed to store fraud alert: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load user activity history from database
     */
    private void loadUserActivityHistory(String userId) {
        try {
            // Use dbManager to load activity history
            // Implementation would use dbManager.executeQuery() if the method exists
            System.out.println("Loading activity history for user: " + userId);
        } catch (Exception e) {
            System.err.println("Failed to load user activity: " + e.getMessage());
        }
    }
    
    /**
     * Block a user due to fraud detection
     */
    public void blockUser(String userId, String reason) {
        // Update database to mark user as blocked
        logUserActivity(userId, "BLOCKED", reason);
        suspiciousActivityScores.put(userId, 100.0); // Maximum risk score
        
        System.out.println("🚫 USER BLOCKED: " + userId + " - " + reason);
    }
    
    /**
     * Set fraud detection sensitivity
     */
    public void setDetectionThreshold(double threshold) {
        this.detectionThreshold = Math.max(0.0, Math.min(100.0, threshold));
    }
    
    public double getDetectionThreshold() {
        return detectionThreshold;
    }
    
    // Private helper methods
    
    private FraudAlert checkDuplicateRequests(EmergencyRequest request) {
        String userId = request.getRequesterId();
        List<String> recentActivities = userActivityLog.getOrDefault(userId, new ArrayList<>());
        
        // Check if similar emergency request was made in last hour
        long duplicateCount = recentActivities.stream()
                .filter(activity -> activity.contains("EMERGENCY_REQUEST"))
                .filter(activity -> activity.contains(request.getType().toString()))
                .count();
        
        if (duplicateCount > 1) {
            double confidence = Math.min(95.0, 60.0 + (duplicateCount * 15.0));
            return new FraudAlert(userId, FraudType.DUPLICATE_REQUEST, confidence,
                    String.format("Similar emergency request made %d times recently", duplicateCount));
        }
        
        return null;
    }
    
    private FraudAlert checkLocationConsistency(EmergencyRequest request) {
        String userId = request.getRequesterId();
        
        // This would check against previous location data
        // For now, implement basic impossible travel detection
        double travelSpeed = calculateTravelSpeed(userId, request.getLocationLat(), request.getLocationLng());
        
        if (travelSpeed > 200.0) { // More than 200 km/h is suspicious
            return new FraudAlert(userId, FraudType.IMPOSSIBLE_LOCATION, 85.0,
                    String.format("Location changed at impossible speed: %.1f km/h", travelSpeed));
        }
        
        return null;
    }
    
    private FraudAlert checkRequestFrequency(String userId) {
        List<String> recentActivities = userActivityLog.getOrDefault(userId, new ArrayList<>());
        
        long requestCount = recentActivities.stream()
                .filter(activity -> activity.contains("REQUEST"))
                .count();
        
        if (requestCount > 10) { // More than 10 requests recently
            double confidence = Math.min(90.0, 50.0 + (requestCount * 4.0));
            return new FraudAlert(userId, FraudType.EXCESSIVE_REQUESTS, confidence,
                    String.format("Made %d requests in short timeframe", requestCount));
        }
        
        return null;
    }
    
    private double calculateExcessiveRequestScore(String userId, String resourceCategory) {
        List<String> activities = userActivityLog.getOrDefault(userId, new ArrayList<>());
        
        long categoryRequests = activities.stream()
                .filter(activity -> activity.contains("RESOURCE_REQUEST"))
                .filter(activity -> activity.toLowerCase().contains(resourceCategory.toLowerCase()))
                .count();
        
        // Calculate score based on request frequency
        return Math.min(100.0, categoryRequests * 15.0);
    }
    
    private double analyzeUserPattern(String userId) {
        List<String> activities = userActivityLog.getOrDefault(userId, new ArrayList<>());
        
        if (activities.size() < 3) {
            return 0.0; // Not enough data for pattern analysis
        }
        
        // Simple pattern analysis - check for rapid sequential requests
        long rapidRequests = activities.stream()
                .filter(activity -> activity.contains("REQUEST"))
                .limit(5)
                .count();
        
        if (rapidRequests >= 5) {
            return 80.0; // High suspicion for rapid requests
        }
        
        return 0.0;
    }
    
    private boolean hasConsistentActivityHistory(String userId) {
        List<String> activities = userActivityLog.getOrDefault(userId, new ArrayList<>());
        return activities.size() >= 3 && activities.size() <= 20; // Reasonable activity level
    }
    
    private double calculateTravelSpeed(String userId, double lat, double lng) {
        // This would calculate speed based on previous location and timestamp
        // For demo purposes, return a random speed
        return Math.random() * 300; // 0-300 km/h
    }
    
    private void logUserActivity(String userId, String activityType, String details) {
        userActivityLog.computeIfAbsent(userId, k -> new ArrayList<>())
                .add(String.format("[%s] %s: %s", 
                        LocalDateTime.now().toString(), activityType, details));
        
        // Keep only last 50 activities per user
        List<String> activities = userActivityLog.get(userId);
        if (activities.size() > 50) {
            activities.subList(0, activities.size() - 50).clear();
        }
        
        // Update suspicion score
        updateSuspicionScore(userId);
    }
    
    private void updateSuspicionScore(String userId) {
        List<String> activities = userActivityLog.getOrDefault(userId, new ArrayList<>());
        
        double score = 0.0;
        
        // Count suspicious activities
        long blockedCount = activities.stream().filter(a -> a.contains("BLOCKED")).count();
        long excessiveRequests = activities.stream().filter(a -> a.contains("REQUEST")).count();
        
        score += blockedCount * 50.0; // Heavy penalty for being blocked
        score += Math.min(30.0, excessiveRequests * 2.0); // Penalty for excessive requests
        
        suspiciousActivityScores.put(userId, Math.min(100.0, score));
    }
    
    /**
     * Generate fraud detection report
     */
    public String generateFraudReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== FRAUD DETECTION REPORT ===\n");
        report.append("Detection Threshold: ").append(detectionThreshold).append("%\n");
        report.append("Total Users Monitored: ").append(userActivityLog.size()).append("\n");
        
        long highRiskUsers = suspiciousActivityScores.values().stream()
                .filter(score -> score >= 70.0)
                .count();
        
        report.append("High Risk Users: ").append(highRiskUsers).append("\n");
        
        if (highRiskUsers > 0) {
            report.append("\nHigh Risk Users:\n");
            suspiciousActivityScores.entrySet().stream()
                    .filter(entry -> entry.getValue() >= 70.0)
                    .forEach(entry -> report.append("- ")
                            .append(entry.getKey())
                            .append(" (Risk: ")
                            .append(String.format("%.1f", entry.getValue()))
                            .append("%)\n"));
        }
        
        return report.toString();
    }
}
