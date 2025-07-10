package com.reliefnet.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import com.reliefnet.model.User;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.controller.MainController;
import com.reliefnet.network.CloudSyncManager;
import com.reliefnet.util.EmailService;
import java.util.concurrent.CompletableFuture;

/**
 * AuthenticationView - Login and Registration interface with role selection
 */
public class AuthenticationView {    
    private Stage primaryStage;
    private VBox mainContainer;
    
    // Login fields
    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    
    // Registration fields
    private TextField regUsernameField;
    private TextField regEmailField;
    private TextField regFullNameField;
    private TextField regPhoneField;
    private PasswordField regPasswordField;
    private PasswordField regConfirmPasswordField;
    private ComboBox<String> roleSelector;
    private TextField authCodeField;
    
    public AuthenticationView(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
      public Scene createAuthenticationScene() {
        mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setStyle("-fx-background-color: transparent;");
        
        createAuthenticationForm();
          // Create ScrollPane to handle content that might be too tall
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: linear-gradient(to bottom, #E8F4FD 0%, #B8D4F1 50%, #1e3c72 100%);");
        
        Scene scene = new Scene(scrollPane, 900, 650);
        return scene;
    }
    
    private void createAuthenticationForm() {
        // Main card container with reduced size
        VBox cardContainer = new VBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(25));
        cardContainer.setMaxWidth(420);
        cardContainer.setStyle("-fx-background-color: white; " +
                              "-fx-background-radius: 20; " +
                              "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 10);");
        
        // Close button container
        HBox closeButtonContainer = new HBox();
        closeButtonContainer.setAlignment(Pos.TOP_RIGHT);
        closeButtonContainer.setPadding(new Insets(0, 0, 10, 0));
        
        Button closeButton = new Button("✕");
        closeButton.setStyle("-fx-background-color: transparent; " +
                            "-fx-text-fill: #666666; " +
                            "-fx-font-size: 16; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 5 8; " +
                            "-fx-cursor: hand;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
            "-fx-background-color: #ff4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5 8; " +
            "-fx-background-radius: 12; " +
            "-fx-cursor: hand;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #666666; " +
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5 8; " +
            "-fx-cursor: hand;"));
        closeButton.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });
        
        closeButtonContainer.getChildren().add(closeButton);
        
        // Header with logo and title
        VBox header = createHeader();        
        // Form container (will switch between login and registration)
        ScrollPane formScrollPane = new ScrollPane();
        formScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        formScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScrollPane.setFitToWidth(true);
        formScrollPane.setPrefHeight(350);
        formScrollPane.setMaxHeight(350);
        
        VBox formContainer = new VBox(15);
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setPadding(new Insets(10));
        
        formScrollPane.setContent(formContainer);
        
        // Initially show login form
        showLoginForm(formContainer);
        
        cardContainer.getChildren().addAll(closeButtonContainer, header, formScrollPane);
        
        // Center the card in the main container
        mainContainer.getChildren().add(cardContainer);
    }    
    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
          // Logo
        ImageView logoView = createLogoView();
        if (logoView != null) {
            header.getChildren().add(logoView);
        } else {
            // Fallback logo - simple circle with ReliefNet text
            StackPane logoStack = new StackPane();
            logoStack.setPrefSize(60, 60);
            
            javafx.scene.shape.Circle bgCircle = new javafx.scene.shape.Circle(30);
            bgCircle.setFill(javafx.scene.paint.LinearGradient.valueOf("linear-gradient(to bottom right, #1e3c72, #3498db)"));
            
            Text logoText = new Text("RN");
            logoText.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            logoText.setFill(Color.WHITE);
            
            logoStack.getChildren().addAll(bgCircle, logoText);
            header.getChildren().add(logoStack);
        }
        
        // Title
        Text titleText = new Text("ReliefNet");
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        titleText.setFill(Color.web("#1e3c72"));
        
        Text subtitleText = new Text("Disaster Relief System for Bangladesh");
        subtitleText.setFont(Font.font("Segoe UI", 12));
        subtitleText.setFill(Color.web("#666666"));
        
        header.getChildren().addAll(titleText, subtitleText);
        return header;
    }
    
    private ImageView createLogoView() {
        try {
            // Use logo.png specifically for authentication screen
            Image logoImage = new Image(getClass().getResourceAsStream("/images/logo.png"));
            if (logoImage != null && !logoImage.isError()) {
                ImageView imageView = new ImageView(logoImage);
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                return imageView;
            }
        } catch (Exception e) {
            System.out.println("Could not load logo.png: " + e.getMessage());
        }
        
        return null;
    }    
    private void showLoginForm(VBox container) {
        container.getChildren().clear();
        
        // Form title
        Text formTitle = new Text("Sign In");
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        formTitle.setFill(Color.web("#1e3c72"));
        
        // Login fields
        VBox loginFields = new VBox(12);
        loginFields.setAlignment(Pos.CENTER);
        
        loginUsernameField = createStyledTextField("Username or Email");
        loginPasswordField = createStyledPasswordField("Password");
        
        loginFields.getChildren().addAll(loginUsernameField, loginPasswordField);
        
    // Forgot password link
    Hyperlink forgotPasswordLink = new Hyperlink("Forgot Password?");
    forgotPasswordLink.setTextFill(Color.web("#3498db"));
    forgotPasswordLink.setOnAction(e -> showPasswordResetDialog());
        
        // Login button
        Button loginButton = new Button("Sign In");
        styleMainButton(loginButton, "#27ae60");
        loginButton.setPrefWidth(200);
        loginButton.setOnAction(e -> handleLogin());
        
        // Switch to registration
        HBox switchBox = new HBox(5);
        switchBox.setAlignment(Pos.CENTER);
        
        Text switchText = new Text("Don't have an account? ");
        switchText.setFont(Font.font("Segoe UI", 12));
        switchText.setFill(Color.web("#666666"));
        
        Hyperlink registerLink = new Hyperlink("Register here");
        registerLink.setTextFill(Color.web("#3498db"));
        registerLink.setOnAction(e -> showRegistrationForm(container));
        
        switchBox.getChildren().addAll(switchText, registerLink);
        
        container.getChildren().addAll(formTitle, loginFields, forgotPasswordLink, loginButton, switchBox);
    }    
    private void showRegistrationForm(VBox container) {
        container.getChildren().clear();
        
        // Form title
        Text formTitle = new Text("Create Account");
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        formTitle.setFill(Color.web("#1e3c72"));
        
        // Registration fields
        VBox regFields = new VBox(12);
        regFields.setAlignment(Pos.CENTER);
        
        regFullNameField = createStyledTextField("Full Name");
        regUsernameField = createStyledTextField("Username");
        regEmailField = createStyledTextField("Email Address");
        regPhoneField = createStyledTextField("Phone Number");
        regPasswordField = createStyledPasswordField("Password");
        regConfirmPasswordField = createStyledPasswordField("Confirm Password");
        
        // Role selection
        Label roleLabel = new Label("Register as:");
        roleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        roleLabel.setTextFill(Color.web("#1e3c72"));
        
        roleSelector = new ComboBox<>();
        roleSelector.getItems().addAll(
            "Survivor - Need help or resources",
            "Volunteer - Want to help and provide assistance", 
            "Authority - Government official or organization"
        );
        roleSelector.setValue("Survivor - Need help or resources");
        roleSelector.setPrefWidth(300);
        styleComboBox(roleSelector);
        
        // Auth code field - initially not visible
        Label authCodeLabel = new Label("Authority Verification Code:");
        authCodeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        authCodeLabel.setTextFill(Color.web("#1e3c72"));
        authCodeLabel.setVisible(false);
        
        authCodeField = createStyledTextField("Enter Authority Code");
        authCodeField.setVisible(false);
        
        // Show auth code field only when Authority role is selected
        roleSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isAuthority = newVal != null && newVal.startsWith("Authority");
            authCodeLabel.setVisible(isAuthority);
            authCodeField.setVisible(isAuthority);
        });
        
        regFields.getChildren().addAll(
            regFullNameField, regUsernameField, regEmailField, regPhoneField,
            regPasswordField, regConfirmPasswordField,
            roleLabel, roleSelector,
            authCodeLabel, authCodeField
        );
        
        // Terms and conditions
        CheckBox termsCheckBox = new CheckBox("I agree to the Terms of Service and Privacy Policy");
        termsCheckBox.setTextFill(Color.web("#666666"));
        
        // Register button
        Button registerButton = new Button("Create Account");
        styleMainButton(registerButton, "#e74c3c");
        registerButton.setPrefWidth(200);
        registerButton.setOnAction(e -> {
            if (termsCheckBox.isSelected()) {
                String selectedRole = roleSelector.getValue();
                if (selectedRole.startsWith("Authority")) {
                    // Check authority code
                    String authCode = authCodeField.getText().trim();
                    if (!authCode.equals("auth")) {
                        showAlert("Verification Error", "Invalid authority code. You must provide a valid code to register as an Authority.", Alert.AlertType.ERROR);
                        return;
                    }
                }
                handleRegistration();
            } else {
                showAlert("Agreement Required", "Please accept the Terms of Service to continue.", Alert.AlertType.WARNING);
            }
        });
        
        // Switch to login
        HBox switchBox = new HBox(5);
        switchBox.setAlignment(Pos.CENTER);
        
        Text switchText = new Text("Already have an account? ");
        switchText.setFont(Font.font("Segoe UI", 12));
        switchText.setFill(Color.web("#666666"));
        
        Hyperlink loginLink = new Hyperlink("Sign in here");
        loginLink.setTextFill(Color.web("#3498db"));
        loginLink.setOnAction(e -> showLoginForm(container));
        
        switchBox.getChildren().addAll(switchText, loginLink);
        
        container.getChildren().addAll(formTitle, regFields, termsCheckBox, registerButton, switchBox);
    }    
    private TextField createStyledTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setPrefWidth(300);
        field.setPrefHeight(35);
        field.setStyle("-fx-background-color: #f8f9fa; " +
                      "-fx-border-color: #dee2e6; " +
                      "-fx-border-radius: 8; " +
                      "-fx-background-radius: 8; " +
                      "-fx-padding: 8; " +
                      "-fx-font-size: 14;");
        
        // Focus styling
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle("-fx-background-color: white; " +
                              "-fx-border-color: #3498db; " +
                              "-fx-border-width: 2; " +
                              "-fx-border-radius: 8; " +
                              "-fx-background-radius: 8; " +
                              "-fx-padding: 8; " +
                              "-fx-font-size: 14;");
            } else {
                field.setStyle("-fx-background-color: #f8f9fa; " +
                              "-fx-border-color: #dee2e6; " +
                              "-fx-border-radius: 8; " +
                              "-fx-background-radius: 8; " +
                              "-fx-padding: 8; " +
                              "-fx-font-size: 14;");
            }
        });
        
        return field;
    }
    
    private PasswordField createStyledPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.setPrefWidth(300);
        field.setPrefHeight(35);
        field.setStyle("-fx-background-color: #f8f9fa; " +
                      "-fx-border-color: #dee2e6; " +
                      "-fx-border-radius: 8; " +
                      "-fx-background-radius: 8; " +
                      "-fx-padding: 8; " +
                      "-fx-font-size: 14;");
        
        // Focus styling
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle("-fx-background-color: white; " +
                              "-fx-border-color: #3498db; " +
                              "-fx-border-width: 2; " +
                              "-fx-border-radius: 8; " +
                              "-fx-background-radius: 8; " +
                              "-fx-padding: 8; " +
                              "-fx-font-size: 14;");
            } else {
                field.setStyle("-fx-background-color: #f8f9fa; " +
                              "-fx-border-color: #dee2e6; " +
                              "-fx-border-radius: 8; " +
                              "-fx-background-radius: 8; " +
                              "-fx-padding: 8; " +
                              "-fx-font-size: 14;");
            }
        });
        
        return field;
    }
    
    private void styleMainButton(Button button, String color) {
        button.setStyle("-fx-background-color: " + color + "; " +
                       "-fx-text-fill: white; " +
                       "-fx-font-weight: bold; " +
                       "-fx-font-size: 16; " +
                       "-fx-background-radius: 8; " +
                       "-fx-padding: 12 24 12 24; " +
                       "-fx-cursor: hand;");
        
        button.setOnMouseEntered(e -> {
            button.setStyle("-fx-background-color: derive(" + color + ", -10%); " +
                           "-fx-text-fill: white; " +
                           "-fx-font-weight: bold; " +
                           "-fx-font-size: 16; " +
                           "-fx-background-radius: 8; " +
                           "-fx-padding: 12 24 12 24; " +
                           "-fx-cursor: hand;");
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle("-fx-background-color: " + color + "; " +
                           "-fx-text-fill: white; " +
                           "-fx-font-weight: bold; " +
                           "-fx-font-size: 16; " +
                           "-fx-background-radius: 8; " +
                           "-fx-padding: 12 24 12 24; " +
                           "-fx-cursor: hand;");
        });    }
    
    private void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle("-fx-background-color: #f8f9fa; " +
                         "-fx-border-color: #dee2e6; " +
                         "-fx-border-radius: 8; " +
                         "-fx-background-radius: 8; " +
                         "-fx-padding: 10; " +
                         "-fx-font-size: 14;");
    }
    
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Login Error", "Please enter both username and password.", Alert.AlertType.ERROR);
            return;
        }
        
        // First, try to sync users from other devices
        try {
            CloudSyncManager cloudSync = CloudSyncManager.getInstance();
            cloudSync.connect();
            cloudSync.downloadAllUsers();
            System.out.println("Downloaded users from other devices for cross-device login");
        } catch (Exception e) {
            System.err.println("Warning: Could not sync users from cloud: " + e.getMessage());
        }
        
        // Authenticate user
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            User user = dbManager.authenticateUser(username, password);
            
            if (user != null) {
                // Login successful - proceed to main application
                showMainApplication(user);
            } else {
                showAlert("Login Failed", "Invalid username or password. Please try again.", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Login Error", "An error occurred during login: " + e.getMessage(), Alert.AlertType.ERROR);
        }    }
      private void handleRegistration() {
        String fullName = regFullNameField.getText().trim();
        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String phone = regPhoneField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();
        String roleSelection = roleSelector.getValue();
        
        // Validation
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || 
            phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Registration Error", "Please fill in all required fields.", Alert.AlertType.ERROR);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showAlert("Registration Error", "Passwords do not match.", Alert.AlertType.ERROR);
            return;
        }
        
        if (password.length() < 6) {
            showAlert("Registration Error", "Password must be at least 6 characters long.", Alert.AlertType.ERROR);
            return;
        }
        
        // Extract user type from role selection
        User.UserType userType;
        if (roleSelection.startsWith("Survivor")) {
            userType = User.UserType.SURVIVOR;
        } else if (roleSelection.startsWith("Volunteer")) {
            userType = User.UserType.VOLUNTEER;
        } else {
            userType = User.UserType.AUTHORITY;
        }
        // Basic validation passed, now check username uniqueness
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Check if username already exists (usernames must be unique)
            if (dbManager.userExists(username, email)) {
                showAlert("Registration Error", "Username already exists. Please choose a different username.", Alert.AlertType.ERROR);
                return;
            }
            
            // Check if this email + role combination already exists
            User existingUser = dbManager.getUserByEmailAndType(email, userType);
            if (existingUser != null) {
                showAlert("Registration Error", 
                    "An account with this email already exists for the " + userType.getDisplayName() + " role. " +
                    "You can have multiple roles with the same email, but not duplicate roles.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            // Send verification email
            CompletableFuture.supplyAsync(() -> EmailService.sendVerificationCode(email, "registration"))
                .thenAccept(code -> {
                    Platform.runLater(() -> {
                        if (code != null) {
                            dbManager.storeVerificationCode(email, code, "REGISTRATION");
                            showEmailVerificationDialog(email, fullName, username, phone, password, userType);
                        } else {
                            showAlert("Error", "Failed to send verification email. Please check your email address and try again.", Alert.AlertType.ERROR);
                        }
                    });
                });
                
        } catch (Exception e) {
            showAlert("Registration Error", "An error occurred during registration: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void showMainApplication(User authenticatedUser) {
        try {
            MainController mainController = new MainController(authenticatedUser);
            Scene mainScene = mainController.createMainScene();
            
            primaryStage.setScene(mainScene);
            primaryStage.setTitle("ReliefNet - " + authenticatedUser.getFullName() + " (" + authenticatedUser.getUserType().getDisplayName() + ")");
            primaryStage.setMaximized(true);
            
        } catch (Exception e) {
            showAlert("Application Error", "Failed to load main application: " + e.getMessage(), Alert.AlertType.ERROR);
        }    }
    
    /**
     * Show email verification dialog for registration
     */
    private void showEmailVerificationDialog(String email, String fullName, String username, String phone, String password, User.UserType userType) {
        Stage verificationStage = new Stage();
        verificationStage.setTitle("Email Verification - ReliefNet");
        
        VBox container = new VBox(30);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));
        container.setMaxWidth(450);
        container.setStyle("-fx-background-color: white; " +
                          "-fx-background-radius: 20; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 10);");
        
        // Header
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);
        
        Text titleText = new Text("Email Verification");
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#1e3c72"));
        
        Text subtitleText = new Text("We've sent a verification code to:\n" + email);
        subtitleText.setFont(Font.font("Segoe UI", 12));
        subtitleText.setFill(Color.web("#666666"));
        subtitleText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        header.getChildren().addAll(titleText, subtitleText);
        
        // Form section
        VBox formSection = new VBox(20);
        formSection.setAlignment(Pos.CENTER);
        
        TextField codeField = createStyledTextField("Enter verification code");
        codeField.setPromptText("6-digit code");
        
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(120);
        cancelButton.setStyle("-fx-background-color: #95a5a6; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-weight: bold; " +
                             "-fx-font-size: 14; " +
                             "-fx-background-radius: 8; " +
                             "-fx-padding: 10 20;");
        cancelButton.setOnAction(e -> verificationStage.close());
        
        Button verifyButton = new Button("Verify");
        verifyButton.setPrefWidth(120);
        styleMainButton(verifyButton, "#27ae60");
        verifyButton.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (!code.isEmpty()) {
                handleEmailVerification(email, code, fullName, username, phone, password, userType, verificationStage);
            } else {
                showAlert("Code Required", "Please enter the verification code.", Alert.AlertType.WARNING);
            }
        });
        
        Button resendButton = new Button("Resend Code");
        resendButton.setPrefWidth(120);
        resendButton.setStyle("-fx-background-color: #3498db; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-weight: bold; " +
                             "-fx-font-size: 14; " +
                             "-fx-background-radius: 8; " +
                             "-fx-padding: 10 20;");
        resendButton.setOnAction(e -> {
            // Resend verification code
            CompletableFuture.supplyAsync(() -> EmailService.sendVerificationCode(email, "registration"))
                .thenAccept(newCode -> {
                    Platform.runLater(() -> {
                        if (newCode != null) {
                            DatabaseManager dbManager = DatabaseManager.getInstance();
                            dbManager.storeVerificationCode(email, newCode, "REGISTRATION");
                            showAlert("Code Resent", "A new verification code has been sent to your email.", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to resend verification code. Please try again.", Alert.AlertType.ERROR);
                        }
                    });
                });
        });
        
        buttonSection.getChildren().addAll(cancelButton, verifyButton, resendButton);
        formSection.getChildren().addAll(codeField, buttonSection);
        
        container.getChildren().addAll(header, formSection);
        
        // Create scene with gradient background
        StackPane root = new StackPane();
        root.setStyle("-fx-background: linear-gradient(to bottom, #E8F4FD 0%, #B8D4F1 50%, #1e3c72 100%);");
        root.getChildren().add(container);
        
        Scene scene = new Scene(root, 500, 400);
        verificationStage.setScene(scene);
        verificationStage.centerOnScreen();
        verificationStage.setResizable(false);
        verificationStage.show();
    }
    
    /**
     * Handle email verification during registration
     */
    private void handleEmailVerification(String email, String code, String fullName, String username, String phone, String password, User.UserType userType, Stage verificationStage) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        if (dbManager.verifyEmailCode(email, code, "REGISTRATION")) {
            // Email verified, complete registration
            try {
                User newUser = new User(username, fullName, email, userType);
                newUser.setPhoneNumber(phone);
                newUser.setStatus("ACTIVE");
                
                boolean success = dbManager.registerUser(newUser, password);
                if (success) {
                    // Mark email as verified
                    dbManager.markEmailAsVerified(email);
                    
                    verificationStage.close();
                    showAlert("Registration Successful", "Your account has been created successfully! You can now log in.", Alert.AlertType.INFORMATION);
                    
                    // Switch back to login form
                    VBox cardContainer = (VBox) mainContainer.getChildren().get(0);
                    ScrollPane formScrollPane = (ScrollPane) cardContainer.getChildren().get(2);
                    VBox formContainer = (VBox) formScrollPane.getContent();
                    showLoginForm(formContainer);
                } else {
                    showAlert("Registration Error", "Failed to create account. Please try again.", Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                showAlert("Registration Error", "An error occurred during registration: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Verification Failed", "Invalid or expired verification code. Please try again.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Show password reset dialog with email verification
     */
    private void showPasswordResetDialog() {
        Stage resetStage = new Stage();
        resetStage.setTitle("Password Reset - ReliefNet");
        
        VBox container = new VBox(30);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));
        container.setMaxWidth(450);
        container.setStyle("-fx-background-color: white; " +
                          "-fx-background-radius: 20; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 10);");
        
        // Header
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);
        
        Text titleText = new Text("Reset Password");
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#1e3c72"));
        
        Text subtitleText = new Text("Enter your email address to receive a verification code");
        subtitleText.setFont(Font.font("Segoe UI", 12));
        subtitleText.setFill(Color.web("#666666"));
        
        header.getChildren().addAll(titleText, subtitleText);
        
        // Form section
        VBox formSection = new VBox(20);
        formSection.setAlignment(Pos.CENTER);
        
        TextField emailField = createStyledTextField("Email address");
        
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(120);
        cancelButton.setStyle("-fx-background-color: #95a5a6; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-weight: bold; " +
                             "-fx-font-size: 14; " +
                             "-fx-background-radius: 8; " +
                             "-fx-padding: 10 20;");
        cancelButton.setOnAction(e -> resetStage.close());
        
        Button sendCodeButton = new Button("Send Code");
        sendCodeButton.setPrefWidth(120);
        styleMainButton(sendCodeButton, "#3498db");
        sendCodeButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (!email.isEmpty()) {
                handlePasswordResetRequest(email, resetStage);
            } else {
                showAlert("Email Required", "Please enter your email address.", Alert.AlertType.WARNING);
            }
        });
        
        buttonSection.getChildren().addAll(cancelButton, sendCodeButton);
        formSection.getChildren().addAll(emailField, buttonSection);
        
        container.getChildren().addAll(header, formSection);
        
        // Create scene with gradient background
        StackPane root = new StackPane();
        root.setStyle("-fx-background: linear-gradient(to bottom, #E8F4FD 0%, #B8D4F1 50%, #1e3c72 100%);");
        root.getChildren().add(container);
        
        Scene scene = new Scene(root, 500, 400);
        resetStage.setScene(scene);
        resetStage.centerOnScreen();
        resetStage.setResizable(false);
        resetStage.show();
    }
    
    /**
     * Handle password reset request
     */
    private void handlePasswordResetRequest(String email, Stage resetStage) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        User user = dbManager.getUserByEmail(email);
        
        if (user != null) {
            // User exists, send verification code
            CompletableFuture.supplyAsync(() -> EmailService.sendVerificationCode(email, "password_reset"))
                .thenAccept(code -> {
                    Platform.runLater(() -> {
                        if (code != null) {
                            dbManager.storeVerificationCode(email, code, "PASSWORD_RESET");
                            resetStage.close();
                            showPasswordResetVerificationDialog(email);
                        } else {
                            showAlert("Error", "Failed to send verification code. Please try again.", Alert.AlertType.ERROR);
                        }
                    });
                });
        } else {
            // Don't reveal if email exists or not for security
            showAlert("Code Sent", "If an account with that email exists, you will receive a verification code.", Alert.AlertType.INFORMATION);
        }
    }
    
    /**
     * Show password reset verification dialog
     */
    private void showPasswordResetVerificationDialog(String email) {
        Stage verificationStage = new Stage();
        verificationStage.setTitle("Password Reset Verification - ReliefNet");
        
        VBox container = new VBox(30);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));
        container.setMaxWidth(450);
        container.setStyle("-fx-background-color: white; " +
                          "-fx-background-radius: 20; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 10);");
        
        // Header
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);
        
        Text titleText = new Text("Enter Verification Code");
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#1e3c72"));
        
        Text subtitleText = new Text("We've sent a verification code to:\n" + email);
        subtitleText.setFont(Font.font("Segoe UI", 12));
        subtitleText.setFill(Color.web("#666666"));
        subtitleText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        header.getChildren().addAll(titleText, subtitleText);
        
        // Form section
        VBox formSection = new VBox(20);
        formSection.setAlignment(Pos.CENTER);
        
        TextField codeField = createStyledTextField("Enter verification code");
        codeField.setPromptText("6-digit code");
        
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(120);
        cancelButton.setStyle("-fx-background-color: #95a5a6; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-weight: bold; " +
                             "-fx-font-size: 14; " +
                             "-fx-background-radius: 8; " +
                             "-fx-padding: 10 20;");
        cancelButton.setOnAction(e -> verificationStage.close());
        
        Button verifyButton = new Button("Verify");
        verifyButton.setPrefWidth(120);
        styleMainButton(verifyButton, "#27ae60");
        verifyButton.setOnAction(e -> {
            String code = codeField.getText().trim();
            if (!code.isEmpty()) {
                handlePasswordResetVerification(email, code, verificationStage);
            } else {
                showAlert("Code Required", "Please enter the verification code.", Alert.AlertType.WARNING);
            }
        });
        
        Button resendButton = new Button("Resend Code");
        resendButton.setPrefWidth(120);
        resendButton.setStyle("-fx-background-color: #3498db; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-weight: bold; " +
                             "-fx-font-size: 14; " +
                             "-fx-background-radius: 8; " +
                             "-fx-padding: 10 20;");
        resendButton.setOnAction(e -> {
            // Resend verification code
            CompletableFuture.supplyAsync(() -> EmailService.sendVerificationCode(email, "password_reset"))
                .thenAccept(newCode -> {
                    Platform.runLater(() -> {
                        if (newCode != null) {
                            DatabaseManager dbManager = DatabaseManager.getInstance();
                            dbManager.storeVerificationCode(email, newCode, "PASSWORD_RESET");
                            showAlert("Code Resent", "A new verification code has been sent to your email.", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to resend verification code. Please try again.", Alert.AlertType.ERROR);
                        }
                    });
                });
        });
        
        buttonSection.getChildren().addAll(cancelButton, verifyButton, resendButton);
        formSection.getChildren().addAll(codeField, buttonSection);
        
        container.getChildren().addAll(header, formSection);
        
        // Create scene with gradient background
        StackPane root = new StackPane();
        root.setStyle("-fx-background: linear-gradient(to bottom, #E8F4FD 0%, #B8D4F1 50%, #1e3c72 100%);");
        root.getChildren().add(container);
        
        Scene scene = new Scene(root, 500, 400);
        verificationStage.setScene(scene);
        verificationStage.centerOnScreen();
        verificationStage.setResizable(false);
        verificationStage.show();
    }
    
    /**
     * Handle password reset verification
     */
    private void handlePasswordResetVerification(String email, String code, Stage verificationStage) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        if (dbManager.verifyEmailCode(email, code, "PASSWORD_RESET")) {
            verificationStage.close();
            showNewPasswordDialog(email);
        } else {
            showAlert("Verification Failed", "Invalid or expired verification code. Please try again.", Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Show new password dialog after successful verification
     */
    private void showNewPasswordDialog(String email) {
        Stage newPasswordStage = new Stage();
        newPasswordStage.setTitle("Set New Password - ReliefNet");
        
        VBox container = new VBox(30);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));
        container.setMaxWidth(450);
        container.setStyle("-fx-background-color: white; " +
                          "-fx-background-radius: 20; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 10);");
        
        // Header
        VBox header = new VBox(15);
        header.setAlignment(Pos.CENTER);
        
        Text titleText = new Text("Set New Password");
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#1e3c72"));
        
        Text subtitleText = new Text("Enter your new password");
        subtitleText.setFont(Font.font("Segoe UI", 12));
        subtitleText.setFill(Color.web("#666666"));
        
        header.getChildren().addAll(titleText, subtitleText);
        
        // Form section
        VBox formSection = new VBox(20);
        formSection.setAlignment(Pos.CENTER);
        
        PasswordField newPasswordField = createStyledPasswordField("New password");
        PasswordField confirmPasswordField = createStyledPasswordField("Confirm new password");
        
        HBox buttonSection = new HBox(15);
        buttonSection.setAlignment(Pos.CENTER);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(120);
        cancelButton.setStyle("-fx-background-color: #95a5a6; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-weight: bold; " +
                             "-fx-font-size: 14; " +
                             "-fx-background-radius: 8; " +
                             "-fx-padding: 10 20;");
        cancelButton.setOnAction(e -> newPasswordStage.close());
        
        Button resetButton = new Button("Reset Password");
        resetButton.setPrefWidth(150);
        styleMainButton(resetButton, "#e74c3c");
        resetButton.setOnAction(e -> {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showAlert("Error", "Please fill in both password fields.", Alert.AlertType.WARNING);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                showAlert("Error", "Passwords do not match.", Alert.AlertType.ERROR);
                return;
            }
            
            if (newPassword.length() < 6) {
                showAlert("Error", "Password must be at least 6 characters long.", Alert.AlertType.ERROR);
                return;
            }
            
            // Reset password
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (dbManager.resetPassword(email, newPassword)) {
                newPasswordStage.close();
                showAlert("Success", "Your password has been reset successfully. You can now log in with your new password.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to reset password. Please try again.", Alert.AlertType.ERROR);
            }
        });
        
        buttonSection.getChildren().addAll(cancelButton, resetButton);
        formSection.getChildren().addAll(newPasswordField, confirmPasswordField, buttonSection);
        
        container.getChildren().addAll(header, formSection);
        
        // Create scene with gradient background
        StackPane root = new StackPane();
        root.setStyle("-fx-background: linear-gradient(to bottom, #E8F4FD 0%, #B8D4F1 50%, #1e3c72 100%);");
        root.getChildren().add(container);
        
        Scene scene = new Scene(root, 500, 400);
        newPasswordStage.setScene(scene);
        newPasswordStage.centerOnScreen();
        newPasswordStage.setResizable(false);
        newPasswordStage.show();
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
