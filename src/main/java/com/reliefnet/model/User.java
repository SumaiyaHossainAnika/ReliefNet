package com.reliefnet.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User model class representing different types of users in the system with JavaFX Properties
 */
public class User {
    
    public enum UserType {
        AUTHORITY("Authority"),
        VOLUNTEER("Volunteer"), 
        SURVIVOR("Survivor");
        
        private final String displayName;
        
        UserType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum Status {
        ACTIVE("Active"),
        INACTIVE("Inactive"),
        EMERGENCY("Emergency"),
        AVAILABLE("Available"),
        ASSIGNED("Assigned"),
        ON_MISSION("On Mission"),
        OFFLINE("Offline");
        
        private final String displayName;
        
        Status(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // JavaFX Properties for TableView binding
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty skills = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty location = new SimpleStringProperty();
    private final StringProperty experience = new SimpleStringProperty();
    
    // Additional fields
    private String userId;
    private String fullName;
    private String phoneNumber;
    private String additionalInfo;
    private UserType userType;
    private Status statusEnum;
    private double locationLat;
    private double locationLng;
    private String locationName;
    private LocalDateTime registeredAt;
    private LocalDateTime lastLoginAt;
    private String password;
    private List<String> skillsList;
    private int experienceYears;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
    private String notes;
    
    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }
    
    public User(String name, String email, String phone, String skills, String status, String location, String experience) {
        this();
        setName(name);
        setEmail(email);
        setPhone(phone);
        setSkills(skills);
        setStatus(status);
        setLocation(location);
        setExperience(experience);
    }
      public User(String userId, String name, String email, UserType userType) {
        this();
        this.userId = userId;
        this.fullName = name; // Set both fullName field and name property
        setName(name);
        setEmail(email);
        this.userType = userType;
        setStatus("Active");
        this.statusEnum = Status.ACTIVE;
    }
    
    // JavaFX Property getters
    public StringProperty nameProperty() { return name; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty skillsProperty() { return skills; }
    public StringProperty statusProperty() { return status; }
    public StringProperty locationProperty() { return location; }
    public StringProperty experienceProperty() { return experience; }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name != null ? name : ""); }
    
    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email != null ? email : ""); }
    
    public String getPhone() { return phone.get(); }
    public void setPhone(String phone) { this.phone.set(phone != null ? phone : ""); }
    
    public String getSkills() { return skills.get(); }
    public void setSkills(String skills) { this.skills.set(skills != null ? skills : ""); }
    
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { 
        this.status.set(status != null ? status : "");
        this.lastActive = LocalDateTime.now();
    }
    
    public String getLocation() { return location.get(); }
    public void setLocation(String location) { this.location.set(location != null ? location : ""); }
    
    public String getExperience() { return experience.get(); }
    public void setExperience(String experience) { this.experience.set(experience != null ? experience : ""); }
    
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }
    
    public Status getStatusEnum() { return statusEnum; }
    public void setStatusEnum(Status statusEnum) { 
        this.statusEnum = statusEnum;
        setStatus(statusEnum.getDisplayName());
    }
    
    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double locationLat) { this.locationLat = locationLat; }
    
    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double locationLng) { this.locationLng = locationLng; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public List<String> getSkillsList() { return skillsList; }
    public void setSkillsList(List<String> skillsList) { 
        this.skillsList = skillsList;
        if (skillsList != null && !skillsList.isEmpty()) {
            setSkills(String.join(", ", skillsList));
        }
    }
    
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { 
        this.experienceYears = experienceYears;
        setExperience(experienceYears + " years");
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // Additional getters and setters for authentication
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    // Utility methods
    public boolean isAvailable() {
        return "Available".equals(getStatus()) || "Active".equals(getStatus());
    }
    
    public boolean isOnline() {
        return lastActive != null && 
               lastActive.isAfter(LocalDateTime.now().minusMinutes(5));
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
    
    public boolean hasSkill(String skill) {
        return skillsList != null && skillsList.contains(skill);
    }
    
    public void addSkill(String skill) {
        if (skillsList != null && !skillsList.contains(skill)) {
            skillsList.add(skill);
            setSkills(String.join(", ", skillsList));
        }
    }
    
    public void removeSkill(String skill) {
        if (skillsList != null && skillsList.contains(skill)) {
            skillsList.remove(skill);
            setSkills(String.join(", ", skillsList));
        }
    }
    
    public String getDisplayRole() {
        return userType != null ? userType.getDisplayName() : "Unknown";
    }
    
    public boolean canBeAssigned() {
        return userType == UserType.VOLUNTEER && isAvailable();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", userType=" + userType +
                ", status=" + getStatus() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return userId != null ? userId.equals(user.userId) : user.userId == null;
    }
    
    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}