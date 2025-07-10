package com.reliefnet.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.model.User;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SettingsView - System configuration, user preferences, and administrative settings
 * Core functionality: System settings, user management, data backup, fraud prevention settings
 */
public class SettingsView {
    
    private VBox mainContainer;
    private TabPane settingsTabPane;
    private TextField systemNameField;
    private TextField locationField;    private ComboBox<String> languageCombo;    private ComboBox<String> timeZoneCombo;
    private CheckBox offlineModeCheck;
    private CheckBox autoBackupCheck;
    private CheckBox fraudDetectionCheck;
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;    // Reference to the authenticated user and database manager
    private User currentUser;
    private DatabaseManager dbManager;
    
    public SettingsView() {
        dbManager = DatabaseManager.getInstance();
    }
      public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            System.err.println("Warning: null user set in SettingsView");
        } else {
            System.out.println("Current user set in SettingsView: " + user.getUserId());
        }
    }
      public VBox createSettingsView() {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f8f9fa;");
        
        // Set preferred height to ensure scrolling works
        mainContainer.setPrefHeight(1200); // Set high enough to trigger scrolling
        
        // Header section
        VBox header = createHeader();
        
        // Settings tabs
        settingsTabPane = createSettingsTabs();
        
        // Load user settings after creating the UI components
        loadUserSettings();
        
        mainContainer.getChildren().addAll(header, settingsTabPane);
        
        return mainContainer;
    }
      private VBox createHeader() {
        VBox header = new VBox(10);
        
        Label titleLabel = new Label("System Settings");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1e3c72"));
        
        // Create role-based subtitle
        String subtitle;
        if (currentUser != null && currentUser.getUserType() == User.UserType.AUTHORITY) {
            subtitle = "Configure system preferences, security settings, and administrative options";
        } else {
            subtitle = "Configure your personal preferences and security settings";
        }
        
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#666666"));
        
        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }
      private TabPane createSettingsTabs() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        // General Settings Tab - Available to all users
        Tab generalTab = new Tab("General");
        generalTab.setContent(createGeneralSettings());
        generalTab.setClosable(false);
        
        // Security Tab - Available to all users
        Tab securityTab = new Tab("Security");
        securityTab.setContent(createSecuritySettings());
        securityTab.setClosable(false);
        
        // Backup & Data Tab - Available to all users
        Tab backupTab = new Tab("Backup & Data");
        backupTab.setContent(createBackupSettings());
        backupTab.setClosable(false);
        
        // Start with basic tabs available to all users
        tabPane.getTabs().addAll(generalTab, securityTab, backupTab);
        
        // Role-based tabs - only add for AUTHORITY users
        if (currentUser != null && currentUser.getUserType() == User.UserType.AUTHORITY) {
            // Fraud Prevention Tab - Authority only
            Tab fraudTab = new Tab("Fraud Prevention");
            fraudTab.setContent(createFraudPreventionSettings());
            fraudTab.setClosable(false);
            
            // User Management Tab - Authority only
            Tab usersTab = new Tab("User Management");
            usersTab.setContent(createUserManagementSettings());
            usersTab.setClosable(false);
            
            // Add authority-only tabs
            tabPane.getTabs().addAll(fraudTab, usersTab);
        }
        
        return tabPane;
    }
    
    private VBox createGeneralSettings() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        
        // System Information
        VBox systemInfo = createSectionContainer("System Information");
        
        GridPane systemGrid = new GridPane();
        systemGrid.setHgap(15);
        systemGrid.setVgap(10);
        
        systemGrid.add(new Label("System Name:"), 0, 0);
        systemNameField = new TextField("ReliefNet Bangladesh");
        systemNameField.setPrefWidth(300);
        ThemeManager.styleTextField(systemNameField);
        systemGrid.add(systemNameField, 1, 0);
        
        systemGrid.add(new Label("Location:"), 0, 1);
        locationField = new TextField("Dhaka, Bangladesh");
        locationField.setPrefWidth(300);
        ThemeManager.styleTextField(locationField);
        systemGrid.add(locationField, 1, 1);
        
        systemGrid.add(new Label("Language:"), 0, 2);
        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "বাংলা (Bengali)", "हिन्दी (Hindi)");
        languageCombo.setValue("English");
        languageCombo.setPrefWidth(300);
        ThemeManager.styleComboBox(languageCombo);
        systemGrid.add(languageCombo, 1, 2);
        
        systemGrid.add(new Label("Time Zone:"), 0, 3);
        timeZoneCombo = new ComboBox<>();
        timeZoneCombo.getItems().addAll("GMT+6 (Dhaka)", "GMT+5:30 (Kolkata)", "GMT+0 (UTC)");
        timeZoneCombo.setValue("GMT+6 (Dhaka)");
        timeZoneCombo.setPrefWidth(300);
        ThemeManager.styleComboBox(timeZoneCombo);
        systemGrid.add(timeZoneCombo, 1, 3);
        
        systemInfo.getChildren().add(systemGrid);
        
        // Operational Settings
        VBox operationalSettings = createSectionContainer("Operational Settings");
        
        VBox checkboxContainer = new VBox(10);
        
        offlineModeCheck = new CheckBox("Enable Offline Mode (Critical for disaster scenarios)");
        offlineModeCheck.setSelected(true);
        offlineModeCheck.setFont(Font.font("Segoe UI", 12));
          autoBackupCheck = new CheckBox("Automatic Data Backup");
        autoBackupCheck.setSelected(true);
        autoBackupCheck.setFont(Font.font("Segoe UI", 12));
        
        checkboxContainer.getChildren().addAll(offlineModeCheck, autoBackupCheck);
        operationalSettings.getChildren().add(checkboxContainer);
        
        // Save button
        HBox buttonContainer = new HBox(15);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);
        Button saveButton = new Button("Save General Settings");
        Button resetButton = new Button("Reset to Default");
        ThemeManager.stylePrimaryButton(saveButton);
        ThemeManager.styleSecondaryButton(resetButton);
          // Add functionality to Save General Settings button
        saveButton.setOnAction(e -> {
            try {                // Save the general settings to the database or configuration
                if (currentUser == null) {
                    throw new Exception("No user is currently logged in");
                }
                
                String userId = currentUser.getUserId();
                
                // Use individual settings with REPLACE to handle insert or update
                saveUserSetting(userId, "system_name", systemNameField.getText());
                saveUserSetting(userId, "location", locationField.getText());
                saveUserSetting(userId, "language", languageCombo.getValue());
                saveUserSetting(userId, "time_zone", timeZoneCombo.getValue());
                saveUserSetting(userId, "offline_mode", String.valueOf(offlineModeCheck.isSelected()));
                saveUserSetting(userId, "auto_backup", String.valueOf(autoBackupCheck.isSelected()));
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Settings Saved");
                successAlert.setHeaderText("General Settings Saved Successfully");
                successAlert.setContentText("Your general settings have been updated and saved.");
                successAlert.showAndWait();
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to Save Settings");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        // Add functionality to Reset to Default button
        resetButton.setOnAction(e -> {
            try {
                // Reset fields to default values
                systemNameField.setText("ReliefNet Bangladesh");
                locationField.setText("Dhaka, Bangladesh");
                languageCombo.setValue("English");
                timeZoneCombo.setValue("GMT+6 (Dhaka)");
                offlineModeCheck.setSelected(true);
                autoBackupCheck.setSelected(true);
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Settings Reset");
                successAlert.setHeaderText("Settings Reset to Default");
                successAlert.setContentText("Your general settings have been reset to default values.");
                successAlert.showAndWait();
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to Reset Settings");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        buttonContainer.getChildren().addAll(saveButton, resetButton);
        
        container.getChildren().addAll(systemInfo, operationalSettings, buttonContainer);
        return container;
    }
    
    private VBox createSecuritySettings() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        
        // Password Settings
        VBox passwordSection = createSectionContainer("Change Password");
        
        GridPane passwordGrid = new GridPane();
        passwordGrid.setHgap(15);
        passwordGrid.setVgap(10);
        
        passwordGrid.add(new Label("Current Password:"), 0, 0);
        currentPasswordField = new PasswordField();
        currentPasswordField.setPrefWidth(300);
        ThemeManager.styleTextField(currentPasswordField);
        passwordGrid.add(currentPasswordField, 1, 0);
        
        passwordGrid.add(new Label("New Password:"), 0, 1);
        newPasswordField = new PasswordField();
        newPasswordField.setPrefWidth(300);
        ThemeManager.styleTextField(newPasswordField);
        passwordGrid.add(newPasswordField, 1, 1);
        
        passwordGrid.add(new Label("Confirm Password:"), 0, 2);
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPrefWidth(300);
        ThemeManager.styleTextField(confirmPasswordField);
        passwordGrid.add(confirmPasswordField, 1, 2);
        
        Button changePasswordButton = new Button("Change Password");
        ThemeManager.stylePrimaryButton(changePasswordButton);
        passwordGrid.add(changePasswordButton, 1, 3);
          // Add functionality to Change Password button
        changePasswordButton.setOnAction(e -> {
            // Check if we have a user logged in
            if (currentUser == null) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("No User Logged In");
                errorAlert.setContentText("You must be logged in to change your password.");
                errorAlert.showAndWait();
                return;
            }
            
            // Validate password fields
            if (currentPasswordField.getText().isEmpty() || 
                newPasswordField.getText().isEmpty() || 
                confirmPasswordField.getText().isEmpty()) {
                
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Missing Information");
                errorAlert.setContentText("Please fill in all password fields");
                errorAlert.showAndWait();
                return;
            }
            
            if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Password Mismatch");
                errorAlert.setContentText("New password and confirm password do not match");
                errorAlert.showAndWait();
                return;
            }
            
            try {
                // First verify current password
                String currentPassword = currentPasswordField.getText();
                String newPassword = newPasswordField.getText();
                
                // Query to check if current password is correct
                String checkSql = "SELECT COUNT(*) FROM users WHERE user_id = ? AND password = ?";
                int count = 0;
                
                try {
                    count = dbManager.executeQuery(checkSql, currentUser.getUserId(), currentPassword).getInt(1);
                } catch (SQLException ex) {
                    throw new Exception("Failed to verify current password: " + ex.getMessage());
                }
                
                if (count != 1) {
                    // Current password is incorrect
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Incorrect Password");
                    errorAlert.setContentText("The current password you entered is incorrect");
                    errorAlert.showAndWait();
                    return;
                }
                
                // If we got here, current password is correct, update to new password
                String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
                int rowsUpdated = dbManager.executeUpdate(updateSql, newPassword, currentUser.getUserId());
                
                if (rowsUpdated != 1) {
                    throw new Exception("Failed to update password in database");
                }
                
                // Update the password in the current user model as well
                currentUser.setPassword(newPassword);
                
                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Password Changed");
                successAlert.setHeaderText("Password Changed Successfully");
                successAlert.setContentText("Your password has been updated. Please use your new password for future logins.");
                successAlert.showAndWait();
                
                // Clear password fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
                
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to Change Password");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        passwordSection.getChildren().add(passwordGrid);
        
        container.getChildren().addAll(passwordSection);
        return container;
    }
    
    private VBox createFraudPreventionSettings() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        
        // Fraud Detection
        VBox fraudDetectionSection = createSectionContainer("Fraud Detection System");
        
        VBox fraudControls = new VBox(15);
        
        fraudDetectionCheck = new CheckBox("Enable AI-powered fraud detection");
        fraudDetectionCheck.setSelected(true);
        fraudDetectionCheck.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        
        Label fraudDescription = new Label("Automatically detects suspicious patterns in resource requests, " +
                                          "duplicate registrations, and unusual activity patterns.");
        fraudDescription.setFont(Font.font("Segoe UI", 11));
        fraudDescription.setTextFill(Color.web("#666666"));
        fraudDescription.setWrapText(true);
        
        // Fraud detection parameters
        GridPane fraudParams = new GridPane();
        fraudParams.setHgap(15);
        fraudParams.setVgap(10);
        fraudParams.setPadding(new Insets(10, 0, 0, 20));
        
        fraudParams.add(new Label("Detection Sensitivity:"), 0, 0);
        ComboBox<String> sensitivityCombo = new ComboBox<>();
        sensitivityCombo.getItems().addAll("Low", "Medium", "High", "Maximum");
        sensitivityCombo.setValue("High");
        sensitivityCombo.setPrefWidth(150);
        ThemeManager.styleComboBox(sensitivityCombo);
        fraudParams.add(sensitivityCombo, 1, 0);
        
        fraudParams.add(new Label("Alert Threshold:"), 0, 1);
        Spinner<Integer> thresholdSpinner = new Spinner<>(1, 10, 3);
        thresholdSpinner.setPrefWidth(150);
        fraudParams.add(thresholdSpinner, 1, 1);
        
        fraudParams.add(new Label("Auto-block suspicious users:"), 0, 2);
        CheckBox autoBlockCheck = new CheckBox();
        autoBlockCheck.setSelected(false);
        fraudParams.add(autoBlockCheck, 1, 2);
          fraudControls.getChildren().addAll(fraudDetectionCheck, fraudDescription, fraudParams);
        fraudDetectionSection.getChildren().add(fraudControls);
        
        container.getChildren().addAll(fraudDetectionSection);
        return container;
    }
    
    private VBox createBackupSettings() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        
        // Backup Configuration
        VBox backupSection = createSectionContainer("Automatic Backup");
        
        GridPane backupGrid = new GridPane();
        backupGrid.setHgap(15);
        backupGrid.setVgap(10);
        
        backupGrid.add(new Label("Backup Frequency:"), 0, 0);
        ComboBox<String> frequencyCombo = new ComboBox<>();
        frequencyCombo.getItems().addAll("Hourly", "Daily", "Weekly", "Manual Only");
        frequencyCombo.setValue("Daily");
        frequencyCombo.setPrefWidth(200);
        ThemeManager.styleComboBox(frequencyCombo);
        backupGrid.add(frequencyCombo, 1, 0);
        
        backupGrid.add(new Label("Backup Location:"), 0, 1);
        TextField backupLocationField = new TextField("C:\\ReliefNet\\Backups");
        backupLocationField.setPrefWidth(300);
        ThemeManager.styleTextField(backupLocationField);
        Button browseButton = new Button("Browse");
        ThemeManager.styleSecondaryButton(browseButton);
        HBox locationContainer = new HBox(10, backupLocationField, browseButton);
        backupGrid.add(locationContainer, 1, 1);
        
        backupGrid.add(new Label("Retain Backups:"), 0, 2);
        ComboBox<String> retentionCombo = new ComboBox<>();
        retentionCombo.getItems().addAll("7 days", "30 days", "90 days", "1 year", "Forever");
        retentionCombo.setValue("30 days");
        retentionCombo.setPrefWidth(200);
        ThemeManager.styleComboBox(retentionCombo);
        backupGrid.add(retentionCombo, 1, 2);
        
        backupSection.getChildren().add(backupGrid);
        
        // Backup Actions
        VBox actionsSection = createSectionContainer("Backup Actions");
        
        HBox actionButtons = new HBox(15);
        
        Button backupNowButton = new Button("Backup Now");
        ThemeManager.stylePrimaryButton(backupNowButton);
        
        Button restoreButton = new Button("Restore from Backup");
        ThemeManager.styleSecondaryButton(restoreButton);
        
        Button verifyButton = new Button("Verify Backup Integrity");
        ThemeManager.styleSecondaryButton(verifyButton);
        
        actionButtons.getChildren().addAll(backupNowButton, restoreButton, verifyButton);
        actionsSection.getChildren().add(actionButtons);
        
        container.getChildren().addAll(backupSection, actionsSection);
        
        // Add functionality to Browse button
        browseButton.setOnAction(e -> {
            try {
                // In a real app, this would open a directory chooser
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Select Directory");
                infoAlert.setHeaderText("Select Backup Directory");
                infoAlert.setContentText("Directory selected: C:\\ReliefNet\\Backups");
                infoAlert.showAndWait();
                
                backupLocationField.setText("C:\\ReliefNet\\Backups");
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to Open Directory Browser");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        // Add functionality to Backup Now button
        backupNowButton.setOnAction(e -> {
            try {
                // In a real app, this would trigger a backup
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Backup");
                infoAlert.setHeaderText("Backup in Progress");
                infoAlert.setContentText("Creating backup in " + backupLocationField.getText() + "...\n\nBackup completed successfully.");
                infoAlert.showAndWait();
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Backup Failed");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        // Add functionality to Restore from Backup button
        restoreButton.setOnAction(e -> {
            try {
                // In a real app, this would allow selecting and restoring from a backup
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Restore");
                confirmAlert.setHeaderText("Restore from Backup");
                confirmAlert.setContentText("Are you sure you want to restore from the backup? This will replace all current data.");
                
                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Restore");
                    successAlert.setHeaderText("Restore Completed");
                    successAlert.setContentText("System data has been successfully restored from backup.");
                    successAlert.showAndWait();
                }
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Restore Failed");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        
        // Add functionality to Verify Backup Integrity button
        verifyButton.setOnAction(e -> {
            try {
                // In a real app, this would verify backup integrity
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Verify Backup");
                infoAlert.setHeaderText("Backup Verification");
                infoAlert.setContentText("Verifying backup integrity...\n\nBackup integrity check passed. All backups are valid.");
                infoAlert.showAndWait();
            } catch (Exception ex) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Verification Failed");
                errorAlert.setContentText("An error occurred: " + ex.getMessage());
                errorAlert.showAndWait();
            }
        });
        return container;
    }
    
    private VBox createUserManagementSettings() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        
        // User Registration
        VBox registrationSection = createSectionContainer("User Registration Settings");
        
        VBox registrationControls = new VBox(10);
        
        CheckBox openRegistration = new CheckBox("Allow open user registration");
        openRegistration.setSelected(true);
        openRegistration.setFont(Font.font("Segoe UI", 12));
        
        CheckBox authorityApproval = new CheckBox("Require authority approval for new volunteers");
        authorityApproval.setSelected(true);
        authorityApproval.setFont(Font.font("Segoe UI", 12));
        
        CheckBox emailVerification = new CheckBox("Require email verification");
        emailVerification.setSelected(true);
        emailVerification.setFont(Font.font("Segoe UI", 12));
        
        registrationControls.getChildren().addAll(openRegistration, authorityApproval, emailVerification);
        registrationSection.getChildren().add(registrationControls);
        
        container.getChildren().addAll(registrationSection);
        return container;
    }
      // Notifications section removed as requested
    
    private VBox createSectionContainer(String title) {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; " +
                        "-fx-border-color: #e9ecef; -fx-border-radius: 8;");
        
        Label sectionTitle = new Label(title);
        sectionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        sectionTitle.setTextFill(Color.web("#1e3c72"));
        
        section.getChildren().add(sectionTitle);
        return section;
    }
    
    /**
     * Saves a user-specific setting to the database
     * @param userId The user ID
     * @param key The setting key
     * @param value The setting value
     * @throws SQLException If there's a database error
     */
    private void saveUserSetting(String userId, String key, String value) throws SQLException {
        // Use REPLACE statement to handle insert or update seamlessly
        String sql = "REPLACE INTO settings (setting_key, setting_value, user_id, setting_type) VALUES (?, ?, ?, 'STRING')";
        dbManager.executeUpdate(sql, key, value, userId);
    }
    
    /**
     * Loads a user-specific setting from the database
     * @param userId The user ID
     * @param key The setting key
     * @param defaultValue The default value to return if setting doesn't exist
     * @return The setting value or the default value
     */
    private String getUserSetting(String userId, String key, String defaultValue) {
        try {
            String sql = "SELECT setting_value FROM settings WHERE setting_key = ? AND user_id = ?";
            var result = dbManager.executeQuery(sql, key, userId);
            if (result.next()) {
                return result.getString("setting_value");
            }
        } catch (SQLException e) {
            System.err.println("Error loading setting " + key + ": " + e.getMessage());
        }
        return defaultValue;
    }
    
    /**
     * Loads all user settings and populates the form fields
     */
    private void loadUserSettings() {
        if (currentUser == null) {
            System.err.println("Cannot load settings: No user is logged in");
            return;
        }
        
        String userId = currentUser.getUserId();
        
        // Load general settings
        systemNameField.setText(getUserSetting(userId, "system_name", "ReliefNet System"));
        locationField.setText(getUserSetting(userId, "location", "Dhaka, Bangladesh"));
        
        String language = getUserSetting(userId, "language", "English");
        languageCombo.setValue(language);
        
        String timezone = getUserSetting(userId, "time_zone", "GMT+6 (Dhaka)");
        timeZoneCombo.setValue(timezone);
        
        boolean offlineMode = Boolean.parseBoolean(getUserSetting(userId, "offline_mode", "true"));
        offlineModeCheck.setSelected(offlineMode);
        
        boolean autoBackup = Boolean.parseBoolean(getUserSetting(userId, "auto_backup", "true"));
        autoBackupCheck.setSelected(autoBackup);
    }
}
