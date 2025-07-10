package com.reliefnet.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.model.User;
import com.reliefnet.network.NetworkManager;
import com.reliefnet.util.DataSyncManager;

/**
 * CommunicationView - Manages real-time communication between survivors, volunteers, and authorities
 * Core functionality: Real-time messaging, emergency broadcasts, communication channels
 */
public class CommunicationView implements DataSyncManager.DataChangeListener {
      private VBox mainContainer;
    private HBox sosAlertBanner; // Banner for SOS alerts
    private ListView<String> messagesList;
    private ObservableList<String> messages;
    private TextField messageInput;
    private ComboBox<String> channelSelector;
    private ListView<String> contactsList;    private ObservableList<String> contacts;
    private TextArea broadcastArea;
    private Label chatHeaderLabel;
    private Label participantCountLabel;
    private String currentChannel = "__general_chat"; // Maps to "👥 General Chat"
    private VBox currentBroadcastList; // Reference to current broadcast list for refreshing
    private User currentUser; // Current logged-in user
    private NetworkManager networkManager; // Network manager for real-time sync

    public VBox createCommunicationView(User user) {
        try {
            this.currentUser = user;
            this.networkManager = NetworkManager.getInstance();
            System.out.println("Creating communication view for user: " + (user != null ? user.getFullName() : "null"));
            
            // Register this view as a listener for data changes
            DataSyncManager.getInstance().addListener(this);
            
            mainContainer = new VBox(20);
            mainContainer.setPadding(new Insets(20));
            mainContainer.setStyle("-fx-background-color: #f8f9fa;");
              // Header section - simplified without banner and cards
            VBox header = createSimpleHeader();
            
            // Main communication area
            HBox communicationArea = createCommunicationArea();
            
            // Only add broadcast panel for authority users, not for volunteers
            if (user != null && user.getUserType() == User.UserType.AUTHORITY) {
                // Emergency broadcast panel (authorities only)
                VBox broadcastPanel = createBroadcastPanel();
                mainContainer.getChildren().addAll(header, communicationArea, broadcastPanel);
            } else {
                // For volunteers and other user types, don't include broadcast panel
                mainContainer.getChildren().addAll(header, communicationArea);
            }
            
            // Set the current channel based on the default selector value FIRST
            handleChannelChange(); // This will properly set currentChannel based on selector
            
            // Initialize data AFTER UI components are created
            // initializeCommunicationData(); // MOVED TO AFTER UI CREATION
            
            // Test database operations (for debugging)
            testDatabaseOperations();
            
            // Update participant count for initial channel
            updateParticipantCount();
            
            // NOW initialize communication data after all UI components exist
            initializeCommunicationData();
            
            System.out.println("Communication view created successfully");
            return mainContainer;
            
        } catch (Exception e) {
            System.err.println("Error creating communication view: " + e.getMessage());
            e.printStackTrace();
            
            // Return fallback error view
            VBox errorView = new VBox(20);
            errorView.setPadding(new Insets(20));
            errorView.setStyle("-fx-background-color: #f8f9fa;");
            
            Label errorTitle = new Label("Communication View Error");
            errorTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
            errorTitle.setTextFill(Color.web("#dc3545"));
            
            Label errorMessage = new Label("Failed to load communication view: " + e.getMessage());
            errorMessage.setFont(Font.font("Segoe UI", 14));
            errorMessage.setTextFill(Color.web("#666666"));
            errorMessage.setWrapText(true);
            
            Button retryButton = new Button("Retry");
            retryButton.setOnAction(ev -> createCommunicationView(user));
            
            errorView.getChildren().addAll(errorTitle, errorMessage, retryButton);
            return errorView;
        }
    }      private VBox createSimpleHeader() {
        VBox header = new VBox(10);
        
        Label titleLabel = new Label("Communication Center");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1e3c72"));
        
        Label subtitleLabel = new Label("Real-time communication for emergency coordination");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#666666"));
        
        // SOS Alert Banner (initially hidden) - positioned below title and subtitle
        sosAlertBanner = new HBox(15);
        sosAlertBanner.setPadding(new Insets(10));
        sosAlertBanner.setAlignment(Pos.CENTER_LEFT);
        sosAlertBanner.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 6;");
        sosAlertBanner.setVisible(false);
        sosAlertBanner.setManaged(false); // Don't take space when hidden
        
        Label alertIcon = new Label("🚨");
        alertIcon.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        Label alertText = new Label("SOS Alert Active - Emergency assistance requested");
        alertText.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        Button dismissBtn = new Button("✕");
        dismissBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold;");
        dismissBtn.setOnAction(e -> {
            sosAlertBanner.setVisible(false);
            sosAlertBanner.setManaged(false);
        });
        
        sosAlertBanner.getChildren().addAll(alertIcon, alertText, new Region(), dismissBtn);
        HBox.setHgrow(sosAlertBanner.getChildren().get(2), Priority.ALWAYS);
        
        header.getChildren().addAll(titleLabel, subtitleLabel, sosAlertBanner);
        return header;
    }
      private HBox createCommunicationArea() {
        HBox communicationArea = new HBox(20);
        communicationArea.setPrefHeight(500);
        
        // Contacts panel (left)
        VBox contactsPanel = createContactsPanel();
        
        // Chat area (center)
        VBox chatArea = createChatArea();
        
        // Channel info panel (right)
        VBox channelPanel = createChannelPanel();
        
        communicationArea.getChildren().addAll(contactsPanel, chatArea, channelPanel);
        HBox.setHgrow(chatArea, Priority.ALWAYS);
        
        return communicationArea;
    }    private VBox createContactsPanel() {
        VBox panel = new VBox(15);
        panel.setPrefWidth(250);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label panelTitle = new Label("Contacts & Channels");
        panelTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        panelTitle.setTextFill(Color.web("#1e3c72"));
          // Channel selector with event handler
        channelSelector = new ComboBox<>();
        channelSelector.getItems().addAll(
            "🚨 Emergency Command",
            "👥 General Chat",
            "🏥 Medical Team",
            "🚁 Rescue Operations",
            "📦 Supply Coordination", 
            "📍 Location Updates"
        );
        channelSelector.setValue("👥 General Chat");
        channelSelector.setPrefWidth(220);
        channelSelector.setOnAction(e -> handleChannelChange());
        ThemeManager.styleComboBox(channelSelector);
        
        // Active contacts (only authorities and approved volunteers) - removed search
        Label contactsTitle = new Label("Active Contacts (Authority & Approved Volunteers)");
        contactsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        contactsTitle.setTextFill(Color.web("#666666"));
        
        contactsList = new ListView<>();
        contactsList.setPrefHeight(320);
        contactsList.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 6;");
        
        // SOS button only (removed share location)
        HBox quickActions = new HBox(10);
        Button emergencyBtn = new Button("🚨 SOS");
        emergencyBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-font-weight: bold;");
        emergencyBtn.setOnAction(e -> handleSOSAlert());
        
        quickActions.getChildren().add(emergencyBtn);
        quickActions.setAlignment(Pos.CENTER);
        
        panel.getChildren().addAll(panelTitle, channelSelector, contactsTitle, contactsList, quickActions);
        return panel;
    }
    
    private VBox createChatArea() {
        VBox chatArea = new VBox(15);
        chatArea.setPadding(new Insets(15));
        chatArea.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                         "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
          // Chat header
        HBox chatHeader = new HBox(15);
        chatHeader.setPadding(new Insets(10));
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");        chatHeaderLabel = new Label("👥 General Chat");
        chatHeaderLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        chatHeaderLabel.setTextFill(Color.web("#1e3c72"));
        
        participantCountLabel = new Label("");
        participantCountLabel.setFont(Font.font("Segoe UI", 12));
        participantCountLabel.setTextFill(Color.web("#666666"));
        
        // Remove mute and info buttons from the right side
        chatHeader.getChildren().addAll(chatHeaderLabel, participantCountLabel);
        
        // Messages area
        messagesList = new ListView<>();
        messagesList.setPrefHeight(350);
        messagesList.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e9ecef; -fx-border-radius: 6;");
        
        // Message input area
        HBox messageInputArea = createMessageInputArea();
        
        chatArea.getChildren().addAll(chatHeader, messagesList, messageInputArea);
        VBox.setVgrow(messagesList, Priority.ALWAYS);
        
        return chatArea;
    }
      private HBox createMessageInputArea() {
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(10));
        inputArea.setAlignment(Pos.CENTER_LEFT);
        inputArea.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");
        
        // Message input field
        messageInput = new TextField();
        messageInput.setPromptText("Type your message...");
        messageInput.setPrefHeight(35);
        ThemeManager.styleTextField(messageInput);
        
        // Send button only
        Button sendBtn = new Button("Send");
        ThemeManager.stylePrimaryButton(sendBtn);
        sendBtn.setOnAction(e -> handleSendMessage());
        
        // Handle Enter key press to send message
        messageInput.setOnAction(e -> handleSendMessage());
        
        inputArea.getChildren().addAll(messageInput, sendBtn);
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        
        return inputArea;
    }
      private VBox createChannelPanel() {
        VBox panel = new VBox(15);
        panel.setPrefWidth(280);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label panelTitle = new Label("Channel Information");
        panelTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        panelTitle.setTextFill(Color.web("#1e3c72"));
        
        // Channel stats with real data
        VBox statsContainer = new VBox(10);
        statsContainer.setPadding(new Insets(10));
        statsContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");
        
        HBox onlineStatus = new HBox(10);
        onlineStatus.setAlignment(Pos.CENTER_LEFT);
        Label onlineIcon = new Label("🟢");
        Label onlineText = new Label("Loading online members...");
        onlineText.setFont(Font.font("Segoe UI", 12));        onlineStatus.getChildren().addAll(onlineIcon, onlineText);
          statsContainer.getChildren().addAll(onlineStatus);
        
        // Load real online members data for stats only (no UI display)
        loadOnlineMembers(null, onlineText, null);
        
        // Only add broadcast viewing area for non-survivor users
        if (currentUser != null && currentUser.getUserType() != User.UserType.SURVIVOR) {
            // Add broadcast viewing area (expanded to take the space of online members)
            VBox broadcastViewingArea = createBroadcastViewingArea();
            panel.getChildren().addAll(panelTitle, statsContainer, broadcastViewingArea);
        } else {
            // For survivors, just show the basic panel without broadcasts
            panel.getChildren().addAll(panelTitle, statsContainer);
        }
        
        return panel;
    }
    
    // Add a broadcast viewing area in the channel panel
    private VBox createBroadcastViewingArea() {
        VBox broadcastArea = new VBox(10);
        broadcastArea.setPadding(new Insets(10));
        broadcastArea.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 6; -fx-border-color: #ffeaa7; -fx-border-radius: 6;");
        
        Label broadcastTitle = new Label("📢 Recent Authority Broadcasts");
        broadcastTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        broadcastTitle.setTextFill(Color.web("#856404"));          VBox broadcastList = new VBox(5);
        currentBroadcastList = broadcastList; // Store reference for refreshing
        ScrollPane broadcastScroll = new ScrollPane(broadcastList);
        broadcastScroll.setPrefHeight(400); // Expanded height to take online members space
        broadcastScroll.setStyle("-fx-background-color: transparent;");
        
        // Load recent broadcasts
        loadRecentBroadcasts(broadcastList);
        
        broadcastArea.getChildren().addAll(broadcastTitle, broadcastScroll);
        return broadcastArea;
    }
      private void loadRecentBroadcasts(VBox broadcastList) {
        try {
            // Clear existing broadcasts first
            broadcastList.getChildren().clear();
            
            DatabaseManager dbManager = DatabaseManager.getInstance();// Load recent broadcasts from all authorities (latest 5)
            String broadcastsSql = "SELECT sender_id, content, broadcast_type, priority, sent_at FROM messages " +
                                 "WHERE message_type = 'BROADCAST' AND sent_at >= datetime('now', '-24 hours') " +
                                 "ORDER BY sent_at DESC LIMIT 5";
              try (java.sql.ResultSet rs = dbManager.executeQuery(broadcastsSql)) {
                while (rs.next()) {
                    // String sender = rs.getString("sender_id"); // Not needed for display
                    String content = rs.getString("content");
                    String broadcastType = rs.getString("broadcast_type");
                    String priority = rs.getString("priority");
                    java.sql.Timestamp sentAt = rs.getTimestamp("sent_at");
                    
                    if (sentAt != null) {
                        String timeStr = sentAt.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                        String priorityIcon = priority != null && priority.contains("Critical") ? "🔴" : 
                                            priority != null && priority.contains("High") ? "🟡" : "🟢";
                        
                        VBox broadcastItem = new VBox(2);
                        broadcastItem.setPadding(new Insets(5));
                        broadcastItem.setStyle("-fx-background-color: white; -fx-background-radius: 4;");
                        
                        Label broadcastHeader = new Label(priorityIcon + " " + (broadcastType != null ? broadcastType : "Broadcast") + " - " + timeStr);
                        broadcastHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                        
                        Label broadcastContent = new Label(content);
                        broadcastContent.setFont(Font.font("Segoe UI", 9));
                        broadcastContent.setWrapText(true);
                        broadcastContent.setTextFill(Color.web("#666666"));
                        
                        broadcastItem.getChildren().addAll(broadcastHeader, broadcastContent);
                        broadcastList.getChildren().add(broadcastItem);
                    }
                }
            }
            
            if (broadcastList.getChildren().isEmpty()) {
                Label noBroadcasts = new Label("No recent broadcasts");
                noBroadcasts.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                broadcastList.getChildren().add(noBroadcasts);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading broadcasts: " + e.getMessage());
            Label errorLabel = new Label("Error loading broadcasts");
            errorLabel.setStyle("-fx-text-fill: red;");
            broadcastList.getChildren().add(errorLabel);
        }
    }
      private HBox createUserItem(String name, String role, boolean isOnline) {
        HBox item = new HBox(8);
        item.setPadding(new Insets(4));
        item.setAlignment(Pos.CENTER_LEFT);
        
        Label statusIcon = new Label(isOnline ? "🟢" : "⚫");
        statusIcon.setFont(Font.font(10));
        
        VBox textContainer = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        
        Label roleLabel = new Label(role);
        roleLabel.setFont(Font.font("Segoe UI", 9));
        roleLabel.setTextFill(Color.web("#666666"));
        
        textContainer.getChildren().addAll(nameLabel, roleLabel);
        item.getChildren().addAll(statusIcon, textContainer);
        
        return item;
    }
      private VBox createBroadcastPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label panelTitle = new Label("Emergency Broadcast System");
        panelTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        panelTitle.setTextFill(Color.web("#1e3c72"));
        
        Label subtitle = new Label("Send alerts and important messages to all users simultaneously");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#666666"));
        
        // Broadcast controls with proper spacing
        HBox broadcastControls = new HBox(10);
        broadcastControls.setAlignment(Pos.CENTER_LEFT);
        
        Label typeLabel = new Label("Type:");
        typeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        ComboBox<String> alertType = new ComboBox<>();
        alertType.getItems().addAll("Emergency Alert", "Weather Warning", "Resource Update", "General Notice");
        alertType.setValue("Emergency Alert");
        alertType.setPrefWidth(140);
        ThemeManager.styleComboBox(alertType);        Label priorityLabel = new Label("Priority:");
        priorityLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        ComboBox<String> priority = new ComboBox<>();
        priority.getItems().addAll("🔴 Critical", "🟡 High", "🟢 Normal");
        priority.setValue("🟡 High");
        priority.setPrefWidth(120);
        ThemeManager.styleComboBox(priority);
          broadcastControls.getChildren().addAll(
            typeLabel, alertType,
            priorityLabel, priority
        );
        
        // Broadcast message area
        broadcastArea = new TextArea();
        broadcastArea.setPromptText("Type your emergency broadcast message here...");
        broadcastArea.setPrefRowCount(4);
        broadcastArea.setPrefColumnCount(50);
        ThemeManager.styleTextArea(broadcastArea);
        
        // Broadcast button only (removed preview and schedule)
        HBox broadcastButtons = new HBox(15);
        broadcastButtons.setAlignment(Pos.CENTER_RIGHT);
          Button broadcastBtn = new Button("🚨 BROADCAST NOW");
        broadcastBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; " +
                             "-fx-padding: 12 24; -fx-font-weight: bold; -fx-font-size: 14px;");
        broadcastBtn.setOnAction(e -> handleBroadcastNow(alertType.getValue(), priority.getValue()));
        
        broadcastButtons.getChildren().add(broadcastBtn);
        
        panel.getChildren().addAll(panelTitle, subtitle, broadcastControls, broadcastArea, broadcastButtons);
        return panel;
    }    private void initializeCommunicationData() {
        System.out.println("=== INITIALIZING COMMUNICATION DATA ===");
        System.out.println("Current channel: " + currentChannel);
        System.out.println("MessagesList is null: " + (messagesList == null));
        System.out.println("ContactsList is null: " + (contactsList == null));
        
        // Initialize messages
        messages = FXCollections.observableArrayList();
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();            // Load recent messages from current channel (exclude BROADCAST and EMERGENCY types)
            // Join with users table to get the username instead of user_id
            String messagesSql = "SELECT m.*, u.name as sender_name FROM messages m " +
                               "LEFT JOIN users u ON m.sender_id = CAST(u.id AS TEXT) " +
                               "WHERE m.channel_id = ? AND m.message_type = 'CHAT' " +
                               "ORDER BY m.sent_at ASC";
            System.out.println("Loading messages for channel: " + currentChannel + " using SQL: " + messagesSql);try (java.sql.ResultSet rs = dbManager.executeQuery(messagesSql, currentChannel)) {
                int messageCount = 0;
                while (rs.next()) {
                    String senderId = rs.getString("sender_id");
                    String senderName = rs.getString("sender_name");
                    String content = rs.getString("content");
                    java.sql.Timestamp sentAt = rs.getTimestamp("sent_at");
                    
                    // Use username (from users table) if available, otherwise fall back to user_id
                    String displayName = (senderName != null && !senderName.trim().isEmpty()) ? senderName : senderId;
                    
                    if (sentAt != null) {
                        String timeStr = sentAt.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                        String messageText = "[" + timeStr + "] " + displayName + ": " + content;
                        messages.add(messageText);
                        messageCount++;
                        System.out.println("Loaded message " + messageCount + ": " + messageText);
                    }
                }
                System.out.println("Total messages loaded from database: " + messageCount);
            }
            
            // If no messages found, add system welcome message
            if (messages.isEmpty()) {
                messages.add("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "] System: Welcome to ReliefNet Emergency Communication");
            }
            
            System.out.println("Messages loaded successfully: " + messages.size() + " messages");
            
        } catch (Exception e) {
            System.err.println("Error loading messages: " + e.getMessage());
            // Add system welcome message on error
            messages.add("[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + "] System: Welcome to ReliefNet Emergency Communication");
        }
        
        // Initialize contacts - only authorities and approved volunteers
        contacts = FXCollections.observableArrayList();
          try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Load authorities and ONLY approved volunteers (must have location and be ACTIVE)
            String contactsSql = "SELECT user_id, name, user_type, status FROM users WHERE " +
                               "(user_type = 'AUTHORITY' OR " +
                               "(user_type = 'VOLUNTEER' AND status = 'ACTIVE' AND " +
                               "((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL AND location_name != '')))) " +
                               "AND status IN ('ACTIVE', 'AVAILABLE') " +
                               "ORDER BY user_type DESC, last_seen DESC LIMIT 50";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(contactsSql)) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String userType = rs.getString("user_type");
                    String status = rs.getString("status");
                    
                    String icon = status.equals("ACTIVE") || status.equals("AVAILABLE") ? "🟢" : "⚫";
                    String roleIcon = userType.equals("AUTHORITY") ? "👨‍💼" : "👨‍🚒";
                    String contactText = icon + " " + roleIcon + " " + name + " (" + userType + ")";
                    contacts.add(contactText);
                }
            }
            
            // Always add emergency control center
            if (!contacts.stream().anyMatch(c -> c.contains("Emergency Control Center"))) {
                contacts.add(0, "� �🟢 Emergency Control Center (SYSTEM)");
            }
            
            System.out.println("Contacts loaded successfully: " + contacts.size() + " contacts");
            
        } catch (Exception e) {
            System.err.println("Error loading contacts: " + e.getMessage());
            // Add system contact on error
            contacts.add("� �🟢 Emergency Control Center (SYSTEM)");
        }
          // Set data to UI components
        System.out.println("Setting data to UI components...");
        System.out.println("Messages to set: " + messages.size());
        System.out.println("MessagesList is null: " + (messagesList == null));
        
        if (messagesList != null) {
            messagesList.setItems(messages);
            System.out.println("Messages set to ListView successfully");
            // Auto-scroll to bottom to show the most recent messages
            if (!messages.isEmpty()) {
                // Use Platform.runLater to ensure UI is updated before scrolling
                javafx.application.Platform.runLater(() -> {
                    messagesList.scrollTo(messages.size() - 1);
                    System.out.println("Auto-scrolled to bottom message: " + (messages.size() - 1));
                });
            }
            System.out.println("Scrolled to bottom of messages");
        } else {
            System.err.println("ERROR: messagesList is null, cannot set messages!");
        }
        if (contactsList != null) {
            contactsList.setItems(contacts);
        }
    }
      // Handler methods for UI interactions
    private void handleSOSAlert() {
        // Show SOS information collection dialog
        Dialog<ButtonType> sosDialog = new Dialog<>();
        sosDialog.setTitle("Emergency SOS Alert");
        sosDialog.setHeaderText("Send Emergency SOS Alert");
        
        // Create dialog content
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));
        
        Label instructionLabel = new Label("Please provide the following information for emergency responders:");
        instructionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // User information (auto-filled)
        String senderName = currentUser != null ? currentUser.getFullName() : "Unknown User";
        String senderType = currentUser != null ? currentUser.getUserType().toString() : "SURVIVOR";
        
        Label userInfoLabel = new Label("Sender: " + senderName + " (" + senderType + ")");
        userInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Contact information
        Label contactLabel = new Label("Contact Information (Phone/Email):");
        TextField contactField = new TextField();
        contactField.setPromptText("Enter your phone number or email");
        
        // Location information
        Label locationLabel = new Label("Current Location:");
        TextField locationField = new TextField();
        locationField.setPromptText("Describe your location (e.g., Building A, Room 201)");
        
        // Description
        Label descLabel = new Label("Emergency Description:");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Briefly describe the emergency situation...");
        descArea.setPrefRowCount(3);
        
        // Urgency level
        Label urgencyLabel = new Label("Urgency Level:");
        ComboBox<String> urgencyBox = new ComboBox<>();
        urgencyBox.getItems().addAll("CRITICAL - Life threatening", "HIGH - Immediate assistance needed", "MEDIUM - Urgent but not life threatening");
        urgencyBox.setValue("CRITICAL - Life threatening");
        
        dialogContent.getChildren().addAll(
            instructionLabel, userInfoLabel,
            contactLabel, contactField,
            locationLabel, locationField,
            descLabel, descArea,
            urgencyLabel, urgencyBox
        );
        
        sosDialog.getDialogPane().setContent(dialogContent);
        sosDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style the dialog
        sosDialog.getDialogPane().setPrefWidth(450);
        
        sosDialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                sendSOSAlert(senderName, senderType, contactField.getText().trim(), 
                           locationField.getText().trim(), descArea.getText().trim(), 
                           urgencyBox.getValue());
            }
        });
    }
    
    private void sendSOSAlert(String senderName, String senderType, String contact, 
                             String location, String description, String urgency) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Create SOS alert in the dedicated table
            String sosId = "SOS_" + System.currentTimeMillis();
            String urgencyLevel = urgency.split(" - ")[0]; // Extract just "CRITICAL", "HIGH", "MEDIUM"
            String finalDescription = description.isEmpty() ? "Emergency assistance needed immediately" : description;
            String finalLocation = location.isEmpty() ? "Location not specified" : location;
            
            String insertSql = "INSERT INTO sos_alerts (sos_id, sender_name, sender_type, sender_contact, " +
                             "location_name, description, urgency_level, status, created_at) VALUES " +
                             "(?, ?, ?, ?, ?, ?, ?, 'ACTIVE', datetime('now'))";
            
            int rowsAffected = dbManager.executeUpdate(insertSql, sosId, senderName, senderType, 
                                                     contact, finalLocation, finalDescription, urgencyLevel);
            
            if (rowsAffected > 0) {
                // Show SOS alert in banner instead of chat
                sosAlertBanner.setVisible(true);
                sosAlertBanner.setManaged(true);
                
                // Update banner text with more specific information
                HBox bannerContent = (HBox) sosAlertBanner;
                if (bannerContent.getChildren().size() > 1) {
                    Label alertText = (Label) bannerContent.getChildren().get(1);
                    alertText.setText("🚨 SOS Alert from " + senderName + " - " + urgencyLevel + " Emergency");
                }
                
                showAlert("SOS Alert Sent", 
                         "Your SOS alert has been sent to emergency responders.\nSOS ID: " + sosId, 
                         Alert.AlertType.INFORMATION);
                
                System.out.println("SOS Alert sent successfully: " + sosId + " from " + senderName);
            } else {
                showAlert("SOS Error", "Failed to send SOS alert - no rows affected.", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            System.err.println("Error sending SOS alert: " + e.getMessage());
            e.printStackTrace();
            showAlert("SOS Error", "Failed to send SOS alert: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void handleSendMessage() {
        String messageText = messageInput.getText().trim();
        if (messageText.isEmpty()) {
            return;
        }
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Insert message into database
            String messageId = "MSG_" + System.currentTimeMillis();            
            // Use the same channel mapping as handleChannelChange()
            String channel = currentChannel; // Use the current channel which is already properly mapped
              System.out.println("Sending message to channel: " + channel + " (from selector: " + channelSelector.getValue() + ")");
              
            // Use actual user info instead of hardcoded values - with better debugging
            String senderId;
            String senderName;
            
            if (currentUser != null) {
                senderId = currentUser.getUserId();
                senderName = currentUser.getFullName(); // We'll use username from database for display
                
                // Debug current user info
                System.out.println("Current user: " + currentUser.getFullName() + " (ID: " + senderId + ")");
                
                // Fallback if userId is null/empty
                if (senderId == null || senderId.trim().isEmpty()) {
                    senderId = "USER_" + System.currentTimeMillis();
                    System.out.println("Warning: User ID was null/empty, using fallback: " + senderId);
                }
                
                // Fallback if name is null/empty
                if (senderName == null || senderName.trim().isEmpty()) {
                    senderName = "Unknown User";
                    System.out.println("Warning: User name was null/empty, using fallback: " + senderName);
                }
            } else {
                senderId = "UNKNOWN_USER";
                senderName = "Unknown User";
                System.out.println("Warning: currentUser is null, using fallback values");
            }
            
            String insertSql = "INSERT INTO messages (message_id, sender_id, content, message_type, " +
                             "sent_at, channel_id) VALUES (?, ?, ?, 'CHAT', datetime('now'), ?)";
            
            int rowsAffected = dbManager.executeUpdate(insertSql, messageId, senderId, messageText, channel);
            System.out.println("Message saved - Rows affected: " + rowsAffected + " | Message ID: " + messageId);
            
            // **NEW: Send message through network for real-time sync**
            if (networkManager != null) {
                try {
                    // Send through network manager for real-time sync to other devices
                    boolean syncSuccess = networkManager.sendMessage(messageId, senderId, messageText, channel);
                    if (syncSuccess) {
                        System.out.println("Message sent through network for real-time sync");
                    } else {
                        System.out.println("Network sync failed, but message saved locally");
                    }
                } catch (Exception networkError) {
                    System.err.println("Network sync error (message still saved locally): " + networkError.getMessage());
                }
            }
            
            // Verify the message was saved by querying it back
            String verifySql = "SELECT * FROM messages WHERE message_id = ?";
            try (java.sql.ResultSet rs = dbManager.executeQuery(verifySql, messageId)) {
                if (rs.next()) {
                    System.out.println("Message verified in database: " + rs.getString("content"));
                } else {
                    System.err.println("Message NOT found in database after insert!");
                }
            }
              // Add to UI immediately (at the bottom) - get the username from database
            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            
            // Query the database to get the actual username for display
            String displayName = senderId; // fallback
            try {
                String userQuery = "SELECT name FROM users WHERE id = ?";
                try (java.sql.ResultSet userRs = dbManager.executeQuery(userQuery, senderId)) {
                    if (userRs.next()) {
                        String userName = userRs.getString("name");
                        if (userName != null && !userName.trim().isEmpty()) {
                            displayName = userName;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting username for display: " + e.getMessage());
            }
            
            messages.add("[" + timeStr + "] " + displayName + ": " + messageText);
            
            // Auto-scroll to bottom to show the new message
            if (messagesList != null) {
                messagesList.scrollTo(messages.size() - 1);
            }
            
            // Clear input
            messageInput.clear();
            
            // Notify other views that communication data has changed
            DataSyncManager.getInstance().notifyCommunicationDataChanged();
            
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            showAlert("Send Error", "Failed to send message. Please try again.", Alert.AlertType.ERROR);
        }
    }    private void handleBroadcastNow(String alertType, String priority) {
        String broadcastText = broadcastArea.getText().trim();
        if (broadcastText.isEmpty()) {
            showAlert("Broadcast Error", "Please enter a broadcast message.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Insert broadcast message
            String messageId = "BROADCAST_" + System.currentTimeMillis();
            String insertSql = "INSERT INTO messages (message_id, sender_id, content, message_type, " +
                             "sent_at, channel_id, broadcast_type, priority) VALUES " +
                             "(?, 'AUTHORITY', ?, 'BROADCAST', datetime('now'), 'emergency_broadcast', ?, ?)";
            
            int rowsAffected = dbManager.executeUpdate(insertSql, messageId, broadcastText, alertType, priority);
              if (rowsAffected > 0) {
                // Clear broadcast area
                broadcastArea.clear();
                
                // Refresh broadcast viewing area in channel panel
                refreshBroadcastViews();
                
                showAlert("Broadcast Sent", "Emergency broadcast has been sent to all users", Alert.AlertType.INFORMATION);
                
                System.out.println("Broadcast sent successfully: " + messageId);
            } else {
                showAlert("Broadcast Error", "Failed to send broadcast - no rows affected.", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            System.err.println("Error sending broadcast: " + e.getMessage());
            e.printStackTrace();
            showAlert("Broadcast Error", "Failed to send broadcast: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void loadOnlineMembers(VBox usersList, Label onlineText, Label messageText) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Count online authorities and ONLY approved volunteers with location
            String onlineCountSql = "SELECT COUNT(*) as count FROM users WHERE " +
                                  "(user_type = 'AUTHORITY' OR " +
                                  "(user_type = 'VOLUNTEER' AND status = 'ACTIVE' AND " +
                                  "((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL AND location_name != '')))) " +
                                  "AND status IN ('ACTIVE', 'AVAILABLE')";
            
            int onlineCount = 0;
            try (java.sql.ResultSet rs = dbManager.executeQuery(onlineCountSql)) {
                if (rs.next()) {
                    onlineCount = rs.getInt("count");
                }            }
            onlineText.setText(onlineCount + " members online");
            
            // Count messages for current channel only if messageText is provided
            if (messageText != null) {
                String messagesCountSql = "SELECT COUNT(*) as count FROM messages WHERE " +
                                         "channel_id = ? AND date(sent_at) = date('now')";
                int messageCount = 0;
                try (java.sql.ResultSet rs = dbManager.executeQuery(messagesCountSql, currentChannel)) {
                    if (rs.next()) {
                        messageCount = rs.getInt("count");
                    }
                }
                messageText.setText(messageCount + " messages today");
            }
              // Load actual online members (authorities and approved volunteers only) - only if usersList is provided
            if (usersList != null) {
                String membersSql = "SELECT name, user_type FROM users WHERE " +
                                  "(user_type = 'AUTHORITY' OR " +
                                  "(user_type = 'VOLUNTEER' AND status = 'ACTIVE' AND " +
                                  "((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL AND location_name != '')))) " +
                                  "AND status IN ('ACTIVE', 'AVAILABLE') " +
                                  "ORDER BY user_type DESC, name ASC LIMIT 10";
                
                usersList.getChildren().clear();
                try (java.sql.ResultSet rs = dbManager.executeQuery(membersSql)) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        String userType = rs.getString("user_type");
                        HBox userItem = createUserItem(name, userType, true);
                        usersList.getChildren().add(userItem);
                    }
                }
                
                // Add system user if no members found
                if (usersList.getChildren().isEmpty()) {
                    HBox systemUser = createUserItem("Emergency Control Center", "SYSTEM", true);
                    usersList.getChildren().add(systemUser);
                }
            }        } catch (Exception e) {
            System.err.println("Error loading online members: " + e.getMessage());
            onlineText.setText("Error loading data");
            if (messageText != null) {
                messageText.setText("Error loading data");
            }
            
            // Add fallback system user only if usersList is provided
            if (usersList != null) {
                usersList.getChildren().clear();
                HBox systemUser = createUserItem("Emergency Control Center", "SYSTEM", true);
                usersList.getChildren().add(systemUser);
            }
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Handler method for channel switching
    private void handleChannelChange() {
        String selectedChannel = channelSelector.getValue();
        if (selectedChannel != null) {            // Map display name to channel ID (consistent with message sending)
            // Fix channel mapping to be consistent
            switch (selectedChannel) {
                case "👥 General Chat":
                    currentChannel = "__general_chat";
                    break;
                case "🚨 Emergency Command":
                    currentChannel = "__emergency_command";
                    break;
                case "🏥 Medical Team":
                    currentChannel = "__medical_team";
                    break;
                case "🚁 Rescue Operations":
                    currentChannel = "__rescue_operations";
                    break;
                case "📦 Supply Coordination":
                    currentChannel = "__supply_coordination";
                    break;
                case "📍 Location Updates":
                    currentChannel = "__location_updates";
                    break;
                default:
                    currentChannel = "__general_chat";
                    break;
            }
            
            System.out.println("Channel changed to: " + currentChannel + " (from selector: " + selectedChannel + ")");
            
            // Update chat header
            chatHeaderLabel.setText(selectedChannel);
            
            // Update participant count
            updateParticipantCount();
            
            // Reload messages for new channel
            loadChannelMessages();
        }
    }
    
    private void updateParticipantCount() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Count participants for current channel (authorities + approved volunteers only)
            String countSql = "SELECT COUNT(*) as count FROM users WHERE " +
                            "(user_type = 'AUTHORITY' OR " +
                            "(user_type = 'VOLUNTEER' AND status = 'ACTIVE' AND " +
                            "((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL && location_name != '')))) " +
                            "AND status IN ('ACTIVE', 'AVAILABLE')";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(countSql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    participantCountLabel.setText(count + " participants");
                }
            }        } catch (Exception e) {
            System.err.println("Error updating participant count: " + e.getMessage());
            participantCountLabel.setText(""); // Remove Loading... text
        }
    }      private void loadChannelMessages() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Initialize messages if null
            if (messages == null) {
                messages = FXCollections.observableArrayList();
            }
            
            // Clear current messages
            messages.clear();
            
            System.out.println("Loading messages for channel: " + currentChannel);
              // Load messages for current channel (exclude broadcasts and emergency messages)
            // Join with users table to get the username instead of user_id
            String messagesSql = "SELECT m.*, u.name as sender_name FROM messages m " +
                               "LEFT JOIN users u ON m.sender_id = CAST(u.id AS TEXT) " +
                               "WHERE m.channel_id = ? AND m.message_type = 'CHAT' " +
                               "ORDER BY m.sent_at ASC";
            try (java.sql.ResultSet rs = dbManager.executeQuery(messagesSql, currentChannel)) {
                int messageCount = 0;
                while (rs.next()) {
                    String senderId = rs.getString("sender_id");
                    String senderName = rs.getString("sender_name");
                    String content = rs.getString("content");
                    java.sql.Timestamp sentAt = rs.getTimestamp("sent_at");
                    messageCount++;
                    
                    // Use username (from users table) if available, otherwise fall back to user_id
                    String displayName = (senderName != null && !senderName.trim().isEmpty()) ? senderName : senderId;
                    
                    if (sentAt != null) {
                        String timeStr = sentAt.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                        String messageText = "[" + timeStr + "] " + displayName + ": " + content;
                        messages.add(messageText);
                    }
                }
                System.out.println("Loaded " + messageCount + " messages from database for channel: " + currentChannel);
            }
            
            // If no messages, add welcome message
            if (messages.isEmpty()) {
                String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                messages.add("[" + timeStr + "] System: Welcome to " + channelSelector.getValue());
            }
            
            // Update the ListView and auto-scroll to bottom on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                if (messagesList != null) {
                    messagesList.setItems(messages);
                    messagesList.refresh(); // Force refresh the ListView
                    if (!messages.isEmpty()) {
                        // Use another Platform.runLater for better auto-scroll timing
                        javafx.application.Platform.runLater(() -> {
                            messagesList.scrollTo(messages.size() - 1);
                            System.out.println("Auto-scrolled to bottom message: " + (messages.size() - 1));
                        });
                    }
                    System.out.println("UI updated with " + messages.size() + " messages for channel: " + currentChannel);
                }
            });
              } catch (Exception e) {
            System.err.println("Error loading channel messages: " + e.getMessage());
            
            // Initialize messages if null
            if (messages == null) {
                messages = FXCollections.observableArrayList();
            }
            
            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            messages.clear();
            messages.add("[" + timeStr + "] System: Error loading messages for this channel");
            
            // Update the ListView and auto-scroll to bottom on JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                if (messagesList != null) {
                    messagesList.setItems(messages);
                    messagesList.refresh(); // Force refresh the ListView
                    if (!messages.isEmpty()) {
                        // Use another Platform.runLater for better auto-scroll timing
                        javafx.application.Platform.runLater(() -> {
                            messagesList.scrollTo(messages.size() - 1);
                            System.out.println("Auto-scrolled to bottom message: " + (messages.size() - 1));
                        });
                    }
                }
            });
        }
    }
      private void refreshBroadcastViews() {
        // Refresh the broadcast list if it exists
        if (currentBroadcastList != null) {
            loadRecentBroadcasts(currentBroadcastList);
            System.out.println("Broadcast views refreshed - reloading recent broadcasts");
        }
    }
    
    // Test method to verify database operations
    private void testDatabaseOperations() {
        System.out.println("=== Testing Database Operations ===");
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Test 1: Check if messages table exists and has correct structure
            String checkTableSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='messages'";
            try (java.sql.ResultSet rs = dbManager.executeQuery(checkTableSql)) {
                if (rs.next()) {
                    System.out.println("✓ Messages table exists");
                } else {
                    System.err.println("✗ Messages table does NOT exist!");
                    return;
                }
            }
            
            // Test 2: Check table structure
            String tableInfoSql = "PRAGMA table_info(messages)";
            System.out.println("Messages table structure:");
            try (java.sql.ResultSet rs = dbManager.executeQuery(tableInfoSql)) {
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    String columnType = rs.getString("type");
                    System.out.println("  - " + columnName + " (" + columnType + ")");
                }
            }
            
            // Test 3: Count existing messages
            String countSql = "SELECT COUNT(*) as count FROM messages";
            try (java.sql.ResultSet rs = dbManager.executeQuery(countSql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✓ Total messages in database: " + count);
                }
            }
            
            // Test 4: Count CHAT messages
            String chatCountSql = "SELECT COUNT(*) as count FROM messages WHERE message_type = 'CHAT'";
            try (java.sql.ResultSet rs = dbManager.executeQuery(chatCountSql)) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✓ Total CHAT messages in database: " + count);
                }
            }
            
            // Test 5: Show recent messages by channel
            String channelMessagesSql = "SELECT channel_id, COUNT(*) as count FROM messages WHERE message_type = 'CHAT' GROUP BY channel_id";
            System.out.println("Messages by channel:");
            try (java.sql.ResultSet rs = dbManager.executeQuery(channelMessagesSql)) {
                while (rs.next()) {
                    String channelId = rs.getString("channel_id");
                    int count = rs.getInt("count");
                    System.out.println("  - " + channelId + ": " + count + " messages");
                }
            }
            
        } catch (Exception e) {
            System.err.println("✗ Database test failed: " + e.getMessage());
            e.printStackTrace();
        }        System.out.println("=== End Database Test ===");
    }
    
    // DataChangeListener interface implementation
    @Override
    public void onResourceDataChanged() {
        // Not used in this view
    }
    
    @Override
    public void onEmergencyDataChanged() {
        // Not used in this view
    }
    
    @Override
    public void onUserDataChanged() {
        // Refresh contacts list when user data changes
        javafx.application.Platform.runLater(() -> {
            System.out.println("CommunicationView: Refreshing contacts due to user data change");
            initializeCommunicationData();
        });
    }
    
    @Override
    public void onDashboardDataChanged() {
        // Not used in this view
    }
    
    @Override
    public void onVolunteerDataChanged() {
        // Refresh contacts list when volunteer data changes
        javafx.application.Platform.runLater(() -> {
            System.out.println("CommunicationView: Refreshing contacts due to volunteer data change");
            initializeCommunicationData();
        });
    }
    
    @Override
    public void onCommunicationDataChanged() {
        // This is the key method - refresh messages when communication data changes
        System.out.println("🔄 CommunicationView: onCommunicationDataChanged() called!");
        System.out.println("   Current channel: " + currentChannel);
        System.out.println("   Messages list null: " + (messages == null));
        System.out.println("   UI messagesList null: " + (messagesList == null));
        
        javafx.application.Platform.runLater(() -> {
            System.out.println("📱 CommunicationView: Running UI refresh on JavaFX thread");
            int beforeCount = messages != null ? messages.size() : 0;
            System.out.println("   Messages before refresh: " + beforeCount);
            
            loadChannelMessages();
            
            int afterCount = messages != null ? messages.size() : 0;
            System.out.println("   Messages after refresh: " + afterCount);
            
            if (afterCount != beforeCount) {
                System.out.println("✅ Messages updated! UI should show new content.");
            } else {
                System.out.println("⚠️ No new messages found during refresh.");
            }
        });
    }
    
    @Override
    public void onSettingsDataChanged() {
        // Not used in this view
    }
}