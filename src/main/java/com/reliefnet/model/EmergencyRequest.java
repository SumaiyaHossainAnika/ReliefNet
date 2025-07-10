package com.reliefnet.model;

import java.time.LocalDateTime;

/**
 * EmergencyRequest model class for handling emergency situations
 */
public class EmergencyRequest {
    
    public enum EmergencyType {
        MEDICAL("Medical Emergency"),
        RESCUE("Rescue Operation"),
        FOOD("Food Shortage"),
        WATER("Water Crisis"),
        SHELTER("Shelter Need");
        
        private final String displayName;
        
        EmergencyType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum Priority {
        CRITICAL("Critical", "#dc3545", 1),
        HIGH("High", "#ffc107", 2),
        MEDIUM("Medium", "#17a2b8", 3),
        LOW("Low", "#28a745", 4);
        
        private final String displayName;
        private final String color;
        private final int level;
        
        Priority(String displayName, String color, int level) {
            this.displayName = displayName;
            this.color = color;
            this.level = level;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
        public int getLevel() { return level; }
    }
    
    public enum RequestStatus {
        PENDING("Pending"),
        ASSIGNED("Assigned"),
        IN_PROGRESS("In Progress"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        RequestStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private String requestId;
    private String requesterId;
    private EmergencyType emergencyType;
    private Priority priority;
    private double locationLat;
    private double locationLng;
    private String locationName;
    private String description;
    private int peopleCount;
    private RequestStatus status;
    private String assignedVolunteer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public EmergencyRequest() {}
    
    public EmergencyRequest(String requestId, String requesterId, EmergencyType emergencyType, 
                           Priority priority, double locationLat, double locationLng, String description) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.emergencyType = emergencyType;
        this.priority = priority;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.description = description;
        this.status = RequestStatus.PENDING;
        this.peopleCount = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    
    public EmergencyType getEmergencyType() { return emergencyType; }
    public void setEmergencyType(EmergencyType emergencyType) { this.emergencyType = emergencyType; }
    
    // Alias method for compatibility
    public EmergencyType getType() { return emergencyType; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double locationLat) { this.locationLat = locationLat; }
    
    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double locationLng) { this.locationLng = locationLng; }
    
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getPeopleCount() { return peopleCount; }
    public void setPeopleCount(int peopleCount) { this.peopleCount = peopleCount; }
    
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getAssignedVolunteer() { return assignedVolunteer; }
    public void setAssignedVolunteer(String assignedVolunteer) { 
        this.assignedVolunteer = assignedVolunteer;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    public boolean isActive() {
        return status == RequestStatus.PENDING || status == RequestStatus.ASSIGNED || status == RequestStatus.IN_PROGRESS;
    }
    
    public boolean isCritical() {
        return priority == Priority.CRITICAL;
    }
    
    public boolean isUrgent() {
        return priority == Priority.CRITICAL || priority == Priority.HIGH;
    }
      public String getTimeElapsed() {
        if (createdAt == null) return "Unknown";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (minutes < 1440) { // 24 hours
            long hours = minutes / 60;
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else {
            long days = minutes / 1440;
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
    }
    
    public double getDistanceFrom(double lat, double lng) {
        // Haversine formula for distance calculation
        double R = 6371; // Earth's radius in kilometers
        
        double dLat = Math.toRadians(lat - this.locationLat);
        double dLng = Math.toRadians(lng - this.locationLng);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(this.locationLat)) * Math.cos(Math.toRadians(lat)) *
                   Math.sin(dLng/2) * Math.sin(dLng/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    @Override
    public String toString() {
        return "EmergencyRequest{" +
                "requestId='" + requestId + '\'' +
                ", emergencyType=" + emergencyType +
                ", priority=" + priority +
                ", status=" + status +
                ", peopleCount=" + peopleCount +
                ", locationName='" + locationName + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EmergencyRequest that = (EmergencyRequest) obj;
        return requestId != null ? requestId.equals(that.requestId) : that.requestId == null;
    }
    
    @Override
    public int hashCode() {
        return requestId != null ? requestId.hashCode() : 0;
    }
}
