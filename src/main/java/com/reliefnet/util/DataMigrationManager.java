package com.reliefnet.util;

import com.reliefnet.database.DatabaseManager;
import com.reliefnet.network.CloudSyncManager;
import com.reliefnet.model.User;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * DataMigrationManager - Handles bidirectional sync between SQLite and Firebase
 * Ensures no data is lost and both databases stay in sync
 */
public class DataMigrationManager {
    
    private static DataMigrationManager instance;
    private final DatabaseManager dbManager;
    private final CloudSyncManager cloudSync;
    private boolean migrationCompleted = false;
    
    private DataMigrationManager() {
        this.dbManager = DatabaseManager.getInstance();
        this.cloudSync = CloudSyncManager.getInstance();
    }
    
    public static DataMigrationManager getInstance() {
        if (instance == null) {
            instance = new DataMigrationManager();
        }
        return instance;
    }
    
    /**
     * Performs complete bidirectional migration and sync
     */
    public CompletableFuture<Boolean> performFullMigration() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🚀 Starting full data migration and sync...");
                
                // Step 1: Connect to cloud service
                cloudSync.connect();
                
                // Step 2: Migrate existing SQLite data to Firebase using existing sync
                System.out.println("📤 Starting sync to upload local data to Firebase...");
                cloudSync.performSync();
                
                // Step 3: Set up continuous bidirectional sync
                setupBidirectionalSync();
                
                migrationCompleted = true;
                System.out.println("✅ Full migration and sync completed successfully!");
                return true;
                
            } catch (Exception e) {
                System.err.println("❌ Migration failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Setup continuous bidirectional sync
     */
    private void setupBidirectionalSync() {
        System.out.println("🔄 Setting up bidirectional sync...");
        
        // Set up periodic sync every 30 seconds
        Timer syncTimer = new Timer(true);
        syncTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (migrationCompleted) {
                    performIncrementalSync();
                }
            }
        }, 30000, 30000); // Start after 30 seconds, repeat every 30 seconds
        
        System.out.println("✅ Bidirectional sync enabled - syncing every 30 seconds");
    }
    
    /**
     * Perform incremental sync to keep databases in sync
     */
    private void performIncrementalSync() {
        try {
            System.out.println("🔄 Performing incremental sync...");
            
            // Use existing CloudSyncManager sync functionality
            cloudSync.performSync();
            
            System.out.println("✅ Incremental sync completed");
            
        } catch (Exception e) {
            System.err.println("❌ Incremental sync error: " + e.getMessage());
        }
    }
    
    /**
     * Check if migration has been completed
     */
    public boolean isMigrationCompleted() {
        return migrationCompleted;
    }
    
    /**
     * Run migration in background task
     */
    public void runMigrationInBackground() {
        Task<Boolean> migrationTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return performFullMigration().get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    System.out.println("🎉 Data migration completed successfully!");
                    System.out.println("📡 Your existing data has been uploaded to Firebase");
                    System.out.println("🔄 Continuous sync is now active - data will sync every 30 seconds");
                    System.out.println("✨ Firebase acts as if it was there from the beginning!");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    System.err.println("💥 Data migration failed!");
                    if (getException() != null) {
                        getException().printStackTrace();
                    }
                });
            }
        };
        
        Thread migrationThread = new Thread(migrationTask);
        migrationThread.setDaemon(true);
        migrationThread.setName("DataMigration");
        migrationThread.start();
    }
    
    /**
     * Get migration status for UI display
     */
    public String getMigrationStatus() {
        if (migrationCompleted) {
            return "✅ Migration Complete - Bidirectional sync active";
        } else {
            return "🔄 Migration in progress...";
        }
    }
    
    /**
     * Force a manual sync
     */
    public void forceSync() {
        if (migrationCompleted) {
            CompletableFuture.runAsync(() -> {
                System.out.println("🔄 Manual sync triggered...");
                cloudSync.performSync();
                System.out.println("✅ Manual sync completed");
            });
        } else {
            System.out.println("⏳ Migration not completed yet. Cannot perform manual sync.");
        }
    }
}
