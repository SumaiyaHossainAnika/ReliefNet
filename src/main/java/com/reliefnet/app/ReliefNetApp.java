package com.reliefnet.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.concurrent.Task;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.util.MemoryManager;
import com.reliefnet.util.DataMigrationManager;
import com.reliefnet.view.AuthenticationView;

/**
 * ReliefNet - Disaster Relief System for Bangladesh
 * Main Application Class with Splash Screen
 */
public class ReliefNetApp extends Application {
    
    private static final String APP_TITLE = "ReliefNet - Disaster Relief System";

    @Override
    public void start(Stage primaryStage) {
        // Initialize memory manager first
        MemoryManager memoryManager = MemoryManager.getInstance();
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down ReliefNet...");
            memoryManager.shutdown();
            DatabaseManager.getInstance().closeConnection();
        }));
        
        // Initialize database
        try {
            DatabaseManager.getInstance().initializeDatabase();
            System.out.println("Database initialized successfully");
            
            // Start data migration in background to sync existing data with Firebase
            startDataMigration();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            showErrorDialog("Database Error", e);
            return;
        }
        
        // Print initial memory usage
        memoryManager.printMemoryUsage();
        
        // Show splash screen first, then authentication
        showSplashScreen(primaryStage, () -> showAuthenticationScreen(primaryStage));
    }
      private void showSplashScreen(Stage primaryStage, Runnable onComplete) {
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        
        // Create splash screen layout
        VBox splashRoot = createSplashScreen();
        Scene splashScene = new Scene(splashRoot, 800, 600);
        
        // Apply theme
        ThemeManager.applyTheme(splashScene);
        
        splashStage.setScene(splashScene);
        splashStage.setTitle(APP_TITLE);
        
        // Set splash screen icon
        try {
            splashStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
        } catch (Exception e) {
            System.out.println("Could not load splash screen icon: " + e.getMessage());
        }
        
        splashStage.centerOnScreen();
        splashStage.show();
        
        // Simple timer - show splash for 2.5 seconds then close
        Task<Void> splashTimer = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(2500); // 2.5 seconds
                return null;
            }
        };
        
        splashTimer.setOnSucceeded(e -> {
            splashStage.close();
            onComplete.run();
        });
        
        Thread timerThread = new Thread(splashTimer);
        timerThread.setDaemon(true);
        timerThread.start();
    }private VBox createSplashScreen() {
        // Try to load splash.png as the full splash screen
        try {
            Image splashImage = new Image(getClass().getResourceAsStream("/images/splash.png"));
            if (splashImage != null && !splashImage.isError()) {
                System.out.println("Successfully loaded splash.png");
                
                // Create ImageView that fills the entire screen
                ImageView splashImageView = new ImageView(splashImage);
                splashImageView.setFitWidth(800);
                splashImageView.setFitHeight(600);
                splashImageView.setPreserveRatio(false); // Fill entire screen
                splashImageView.setSmooth(true);
                
                // Create container for the image
                VBox container = new VBox();
                container.setAlignment(Pos.CENTER);
                container.setPrefSize(800, 600);
                container.getChildren().add(splashImageView);
                
                return container;
            }
        } catch (Exception e) {
            System.out.println("Could not load splash.png: " + e.getMessage());
        }
        
        // Fallback: Simple gradient background if splash.png fails
        VBox fallback = new VBox();
        fallback.setAlignment(Pos.CENTER);
        fallback.setPrefSize(800, 600);
        fallback.setStyle("-fx-background: linear-gradient(to bottom, #E8F4FD 0%, #B8D4F1 50%, #1e3c72 100%);");
        
        Text fallbackText = new Text("Loading ReliefNet...");
        fallbackText.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        fallbackText.setFill(Color.web("#1B2951"));
        
        fallback.getChildren().add(fallbackText);
        return fallback;
    }
    
    private void showAuthenticationScreen(Stage primaryStage) {
        try {
            AuthenticationView authView = new AuthenticationView(primaryStage);
            Scene authScene = authView.createAuthenticationScene();
            
            primaryStage.setScene(authScene);
            primaryStage.setTitle(APP_TITLE + " - Login");
            primaryStage.setMaximized(false);
            primaryStage.centerOnScreen();
            primaryStage.show();
              // Set application icon
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/logo.png")));
            } catch (Exception e) {
                System.out.println("Could not load application icon: " + e.getMessage());
            }
              } catch (Exception e) {
            showErrorDialog("Failed to start authentication", e);
        }
    }
    
    private void showErrorDialog(String title, Throwable exception) {
        Platform.runLater(() -> {
            // Create simple error dialog
            Stage errorStage = new Stage();
            VBox errorRoot = new VBox(20);
            errorRoot.setAlignment(Pos.CENTER);
            errorRoot.setPadding(new javafx.geometry.Insets(30));
            errorRoot.setStyle("-fx-background-color: #f8f9fa;");
            
            Text errorTitle = new Text(title);
            errorTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            errorTitle.setFill(Color.web("#dc3545"));
            
            Text errorMessage = new Text(exception != null ? exception.getMessage() : "Unknown error occurred");
            errorMessage.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            errorMessage.setFill(Color.web("#6c757d"));
            
            errorRoot.getChildren().addAll(errorTitle, errorMessage);
            
            Scene errorScene = new Scene(errorRoot, 400, 200);
            errorStage.setScene(errorScene);
            errorStage.setTitle("ReliefNet Error");
            errorStage.show();
        });
    }
    
    @Override
    public void stop() {
        // Cleanup resources
        DatabaseManager.getInstance().closeConnection();
        Platform.exit();
    }
      public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start data migration and bidirectional sync
     */
    private void startDataMigration() {
        System.out.println("🚀 Starting data migration and bidirectional sync...");
        
        // Run migration in background
        DataMigrationManager.getInstance().runMigrationInBackground();
        
        System.out.println("📡 Migration started - your existing data will be synced to Firebase");
        System.out.println("🔄 After migration, real-time bidirectional sync will be active");
    }
}
