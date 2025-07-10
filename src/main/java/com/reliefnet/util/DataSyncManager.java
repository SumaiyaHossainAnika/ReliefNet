package com.reliefnet.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DataSyncManager - Manages real-time data synchronization across all views and users
 * When any user makes changes, all other views are notified to refresh
 */
public class DataSyncManager {
    
    private static DataSyncManager instance;
    private final List<DataChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    private DataSyncManager() {}
    
    public static synchronized DataSyncManager getInstance() {
        if (instance == null) {
            instance = new DataSyncManager();
        }
        return instance;
    }
    
    /**
     * Interface for components that want to be notified of data changes
     */
    public interface DataChangeListener {
        void onResourceDataChanged();
        void onEmergencyDataChanged();
        void onUserDataChanged();
        void onDashboardDataChanged();
        void onVolunteerDataChanged();
        void onCommunicationDataChanged();
        void onSettingsDataChanged();
    }
    
    /**
     * Register a listener to receive data change notifications
     */
    public void addListener(DataChangeListener listener) {
        listeners.add(listener);
        System.out.println("DataSyncManager: Added listener, total: " + listeners.size());
    }
    
    /**
     * Remove a listener
     */
    public void removeListener(DataChangeListener listener) {
        listeners.remove(listener);
        System.out.println("DataSyncManager: Removed listener, total: " + listeners.size());
    }
    
    /**
     * Notify all listeners that resource data has changed
     */
    public void notifyResourceDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of resource data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onResourceDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of resource change: " + e.getMessage());
            }
        }
        // Also notify dashboard since resources affect dashboard metrics
        notifyDashboardDataChanged();
    }
    
    /**
     * Notify all listeners that emergency data has changed
     */
    public void notifyEmergencyDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of emergency data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onEmergencyDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of emergency change: " + e.getMessage());
            }
        }
        // Also notify dashboard since emergencies affect dashboard metrics
        notifyDashboardDataChanged();
    }
    
    /**
     * Notify all listeners that user data has changed
     */
    public void notifyUserDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of user data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onUserDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of user change: " + e.getMessage());
            }
        }
        // Also notify dashboard since users affect dashboard metrics
        notifyDashboardDataChanged();
    }
    
    /**
     * Notify all listeners that dashboard should refresh
     */
    public void notifyDashboardDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of dashboard data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onDashboardDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of dashboard change: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners that volunteer data has changed
     */
    public void notifyVolunteerDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of volunteer data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onVolunteerDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of volunteer change: " + e.getMessage());
            }
        }
        // Also notify dashboard since volunteers affect dashboard metrics
        notifyDashboardDataChanged();
    }
    
    /**
     * Notify all listeners that communication data has changed
     */
    public void notifyCommunicationDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of communication data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onCommunicationDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of communication change: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners that settings have changed
     */
    public void notifySettingsDataChanged() {
        System.out.println("DataSyncManager: Notifying " + listeners.size() + " listeners of settings data change");
        for (DataChangeListener listener : listeners) {
            try {
                listener.onSettingsDataChanged();
            } catch (Exception e) {
                System.err.println("Error notifying listener of settings change: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners that ALL data should be refreshed (system-wide refresh)
     */
    public void notifyAllDataChanged() {
        System.out.println("DataSyncManager: Triggering SYSTEM-WIDE data refresh for " + listeners.size() + " listeners");
        
        // Notify each data type to ensure complete refresh
        notifyResourceDataChanged();
        notifyEmergencyDataChanged();
        notifyUserDataChanged();
        notifyVolunteerDataChanged();
        notifyCommunicationDataChanged();
        notifySettingsDataChanged();
        notifyDashboardDataChanged();
        
        System.out.println("DataSyncManager: System-wide refresh complete");
    }
}
