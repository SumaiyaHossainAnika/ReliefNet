package com.reliefnet.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resource model class for managing relief resources with JavaFX Properties
 */
public class Resource {
    
    public enum ResourceCategory {
        FOOD("Food & Nutrition", "🍽️"),
        WATER("Water & Sanitation", "💧"),
        MEDICAL("Medical Supplies", "🏥"),
        SHELTER("Shelter & Housing", "🏠"),
        CLOTHING("Clothing & Textiles", "👕"),
        TOOLS("Tools & Equipment", "🔧"),
        COMMUNICATION("Communication Equipment", "📱"),
        ENERGY("Energy & Fuel", "⚡");
        
        private final String displayName;
        private final String icon;
        
        ResourceCategory(String displayName, String icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
    }
    
    public enum ResourceStatus {
        AVAILABLE("Available", "#28a745"),
        ALLOCATED("Allocated", "#ffc107"),
        DELIVERED("Delivered", "#17a2b8"),
        EXPIRED("Expired", "#dc3545"),
        DAMAGED("Damaged", "#6c757d");
        
        private final String displayName;
        private final String color;
        
        ResourceStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
    
    // JavaFX Properties for TableView binding
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final StringProperty unit = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty location = new SimpleStringProperty();
    
    // Additional fields
    private String resourceId;
    private ResourceCategory categoryEnum;
    private double locationLat;
    private double locationLng;
    private ResourceStatus statusEnum;
    private LocalDate expiryDate;
    private String allocatedTo;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Resource() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Resource(String name, String category, int quantity, String unit, String status, String location) {
        this();
        setName(name);
        setCategory(category);
        setQuantity(quantity);
        setUnit(unit);
        setStatus(status);
        setLocation(location);
    }
    
    public Resource(String resourceId, String name, ResourceCategory category, int quantity, String unit) {
        this();
        this.resourceId = resourceId;
        setName(name);
        setCategory(category.getDisplayName());
        this.categoryEnum = category;
        setQuantity(quantity);
        setUnit(unit);
        setStatus("Available");
        this.statusEnum = ResourceStatus.AVAILABLE;
    }
    
    // JavaFX Property getters
    public StringProperty nameProperty() { return name; }
    public StringProperty categoryProperty() { return category; }
    public IntegerProperty quantityProperty() { return quantity; }
    public StringProperty unitProperty() { return unit; }
    public StringProperty statusProperty() { return status; }
    public StringProperty locationProperty() { return location; }
    
    // Getters and Setters
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name != null ? name : ""); }
    
    public String getCategory() { return category.get(); }
    public void setCategory(String category) { this.category.set(category != null ? category : ""); }
    
    public ResourceCategory getCategoryEnum() { return categoryEnum; }
    public void setCategoryEnum(ResourceCategory categoryEnum) { 
        this.categoryEnum = categoryEnum;
        setCategory(categoryEnum.getDisplayName());
    }
    
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int quantity) { 
        this.quantity.set(quantity);
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getUnit() { return unit.get(); }
    public void setUnit(String unit) { this.unit.set(unit != null ? unit : ""); }
    
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { 
        this.status.set(status != null ? status : "");
        this.updatedAt = LocalDateTime.now();
    }
    
    public ResourceStatus getStatusEnum() { return statusEnum; }
    public void setStatusEnum(ResourceStatus statusEnum) { 
        this.statusEnum = statusEnum;
        setStatus(statusEnum.getDisplayName());
    }
    
    public String getLocation() { return location.get(); }
    public void setLocation(String location) { this.location.set(location != null ? location : ""); }
    
    public double getLocationLat() { return locationLat; }
    public void setLocationLat(double locationLat) { this.locationLat = locationLat; }
    
    public double getLocationLng() { return locationLng; }
    public void setLocationLng(double locationLng) { this.locationLng = locationLng; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getAllocatedTo() { return allocatedTo; }
    public void setAllocatedTo(String allocatedTo) { 
        this.allocatedTo = allocatedTo;
        if (allocatedTo != null) {
            setStatus("Allocated");
            this.statusEnum = ResourceStatus.ALLOCATED;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    public boolean isAvailable() {
        return "Available".equals(getStatus()) && !isExpired();
    }
    
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
    
    public boolean isExpiringSoon() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(7));
    }
    
    public String getDisplayQuantity() {
        return getQuantity() + " " + getUnit();
    }
    
    public String getExpiryStatus() {
        if (expiryDate == null) return "No expiry";
        
        LocalDate now = LocalDate.now();
        if (expiryDate.isBefore(now)) {
            return "Expired";
        } else if (expiryDate.isBefore(now.plusDays(7))) {
            return "Expires soon";
        } else {
            return "Valid until " + expiryDate;
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
    
    public void reduceQuantity(int amount) {
        if (amount <= 0) return;
        
        int newQuantity = Math.max(0, getQuantity() - amount);
        setQuantity(newQuantity);
        
        // Update status if quantity reaches zero
        if (newQuantity == 0 && "Available".equals(getStatus())) {
            setStatus("Allocated");
            this.statusEnum = ResourceStatus.ALLOCATED;
        }
    }
    
    public void addQuantity(int amount) {
        if (amount <= 0) return;
        
        int newQuantity = getQuantity() + amount;
        setQuantity(newQuantity);
        
        // Update status back to available if it was allocated due to zero quantity
        if (newQuantity > 0 && "Allocated".equals(getStatus()) && this.allocatedTo == null) {
            setStatus("Available");
            this.statusEnum = ResourceStatus.AVAILABLE;
        }
    }
    
    @Override
    public String toString() {
        return "Resource{" +
                "resourceId='" + resourceId + '\'' +
                ", name='" + getName() + '\'' +
                ", category=" + getCategory() +
                ", quantity=" + getQuantity() +
                ", unit='" + getUnit() + '\'' +
                ", status=" + getStatus() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Resource resource = (Resource) obj;
        return resourceId != null ? resourceId.equals(resource.resourceId) : resource.resourceId == null;
    }
    
    @Override
    public int hashCode() {
        return resourceId != null ? resourceId.hashCode() : 0;
    }
}