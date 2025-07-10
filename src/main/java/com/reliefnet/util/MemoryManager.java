package com.reliefnet.util;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Memory Management Utility for ReliefNet
 * Helps prevent memory leaks and manages JavaFX collections
 */
public class MemoryManager {
    
    private static MemoryManager instance;
    private ScheduledExecutorService memoryCleanupExecutor;
    private List<WeakReference<ObservableList<?>>> observableLists;
    
    private MemoryManager() {
        this.observableLists = new ArrayList<>();
        startMemoryCleanupTask();
    }
    
    public static synchronized MemoryManager getInstance() {
        if (instance == null) {
            instance = new MemoryManager();
        }
        return instance;
    }
    
    /**
     * Register an ObservableList for memory management
     */
    public <T> void registerObservableList(ObservableList<T> list) {
        observableLists.add(new WeakReference<>(list));
    }
    
    /**
     * Clear all registered ObservableLists to free memory
     */
    public void clearAllObservableLists() {
        Platform.runLater(() -> {
            observableLists.removeIf(ref -> {
                ObservableList<?> list = ref.get();
                if (list != null) {
                    list.clear();
                    return false;
                } else {
                    return true; // Remove dead references
                }
            });
        });
    }
    
    /**
     * Safely clear a specific ObservableList
     */
    public static <T> void clearObservableList(ObservableList<T> list) {
        if (list != null && Platform.isFxApplicationThread()) {
            list.clear();
        } else if (list != null) {
            Platform.runLater(() -> list.clear());
        }
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    public void forceGarbageCollection() {
        new Thread(() -> {
            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // runFinalization() is deprecated, just use gc()
        }).start();
    }
    
    /**
     * Get current memory usage information
     */
    public void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        System.out.println("=== Memory Usage ===");
        System.out.printf("Used: %.2f MB\n", usedMemory / 1024.0 / 1024.0);
        System.out.printf("Free: %.2f MB\n", freeMemory / 1024.0 / 1024.0);
        System.out.printf("Total: %.2f MB\n", totalMemory / 1024.0 / 1024.0);
        System.out.printf("Max: %.2f MB\n", maxMemory / 1024.0 / 1024.0);
        System.out.printf("Usage: %.1f%%\n", (usedMemory * 100.0) / maxMemory);
        System.out.println("===================");
    }
    
    /**
     * Check if memory usage is critical (above 80%)
     */
    public boolean isMemoryUsageCritical() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        return (usedMemory * 100.0 / maxMemory) > 80.0;
    }
    
    /**
     * Start a background task to monitor and clean memory
     */
    private void startMemoryCleanupTask() {
        memoryCleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "MemoryCleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        // Check memory every 30 seconds
        memoryCleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                if (isMemoryUsageCritical()) {
                    System.out.println("Memory usage critical - performing cleanup...");
                    clearAllObservableLists();
                    forceGarbageCollection();
                }
                
                // Print memory usage every 5 minutes in debug mode
                if (System.getProperty("reliefnet.debug") != null) {
                    printMemoryUsage();
                }
            } catch (Exception e) {
                System.err.println("Error in memory cleanup: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Shutdown the memory manager
     */
    public void shutdown() {
        if (memoryCleanupExecutor != null && !memoryCleanupExecutor.isShutdown()) {
            memoryCleanupExecutor.shutdown();
            try {
                if (!memoryCleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    memoryCleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                memoryCleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
