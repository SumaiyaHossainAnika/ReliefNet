package com.reliefnet.view;

import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.util.DataSyncManager;
import com.reliefnet.model.EmergencyRequest;
import com.reliefnet.database.DatabaseManager;

/**
 * EmergencyView - Emergency response management interface
 */
public class EmergencyView {
      private TableView<EmergencyRequest> emergencyTable;
    private TableView<Object[]> sosTable; // SOS alerts table
    
    // Store counts as class variables for reuse
    private int criticalCount = 0;
    private int highCount = 0;
    private int mediumCount = 0;
    
    // Status indicator labels for real-time updates
    private Text criticalCountText;
    private Text highCountText;
    private Text mediumCountText;    public VBox createView() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
          // Sync table assignments to track assignments on load
        syncTableAssignmentsToTrackAssignments();
        
        // Sync track assignments back to tables on load
        syncTrackAssignmentsToTables();        // Header section
        VBox header = createHeader();
        
        // Alert banner for critical emergencies
        HBox alertBanner = createAlertBanner();
        
        // Controls section
        HBox controls = createControls();
          // Emergency management tabs
        VBox tableSection = createEmergencyTabs();
        
        // Action buttons
        HBox actionButtons = createActionButtons();
          mainContainer.getChildren().addAll(header, alertBanner, controls, tableSection, actionButtons);
        
        // Setup automatic refresh every 10 seconds to keep authority view updated
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            System.out.println("Auto-refreshing emergency data...");
            loadEmergencyData();
            updateEmergencyCounts();
            if (sosTable != null) {
                loadSOSData(sosTable);
            }
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
        
        return mainContainer;
    }
    
    private VBox createHeader() {
        VBox header = new VBox(10);
        
        HBox titleRow = new HBox(20);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Text title = new Text("🚨 Emergency Response Center");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Update emergency counts from database
        updateEmergencyCounts();
          // Emergency status indicators
        HBox statusIndicators = new HBox(15);
        statusIndicators.setAlignment(Pos.CENTER_RIGHT);
        
        VBox criticalIndicator = createStatusIndicator("Critical", String.valueOf(criticalCount), ThemeManager.DANGER_COLOR);
        criticalCountText = (Text) criticalIndicator.getChildren().get(0); // Get the count text
        
        VBox highIndicator = createStatusIndicator("High", String.valueOf(highCount), ThemeManager.WARNING_COLOR);
        highCountText = (Text) highIndicator.getChildren().get(0); // Get the count text
        
        VBox mediumIndicator = createStatusIndicator("Medium", String.valueOf(mediumCount), ThemeManager.INFO_COLOR);
        mediumCountText = (Text) mediumIndicator.getChildren().get(0); // Get the count text
        
        statusIndicators.getChildren().addAll(criticalIndicator, highIndicator, mediumIndicator);
        
        titleRow.getChildren().addAll(title, spacer, statusIndicators);
        
        Text subtitle = new Text("Coordinate emergency responses and manage relief operations in real-time");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        header.getChildren().addAll(titleRow, subtitle);
        
        return header;
    }
    
    private VBox createStatusIndicator(String label, String count, String color) {
        VBox indicator = new VBox(5);
        indicator.setAlignment(Pos.CENTER);
        indicator.setPadding(new Insets(10));
        indicator.setStyle("-fx-background-color: white; " +
                          "-fx-background-radius: 8; " +
                          "-fx-border-color: " + color + "; " +
                          "-fx-border-width: 2; " +
                          "-fx-border-radius: 8;");
        
        Text countText = new Text(count);
        countText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        countText.setFill(Color.web(color));
        
        Text labelText = new Text(label);
        labelText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        labelText.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        indicator.getChildren().addAll(countText, labelText);
        
        return indicator;
    }      private HBox createAlertBanner() {
        HBox banner = new HBox(15);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(15));
        banner.setStyle("-fx-background-color: " + ThemeManager.DANGER_COLOR + "; " +
                       "-fx-background-radius: 8; " +
                       "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.4), 8, 0, 0, 2);");
        
        // Simple circle with exclamation mark
        StackPane alertIcon = new StackPane();
        alertIcon.setMinSize(30, 30);
        alertIcon.setPrefSize(30, 30);
        
        Circle circle = new Circle(15);
        circle.setFill(Color.WHITE);
        
        Text exclamation = new Text("!");
        exclamation.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        exclamation.setFill(Color.web(ThemeManager.DANGER_COLOR));
        
        alertIcon.getChildren().addAll(circle, exclamation);
        
        VBox alertContent = new VBox(5);
        
        Text alertTitle = new Text("CRITICAL ALERT");
        alertTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        alertTitle.setFill(Color.WHITE);
        
        // Dynamic message based on critical count
        String message = (criticalCount > 0) 
            ? criticalCount + " critical " + (criticalCount == 1 ? "emergency requires" : "emergencies require") + " immediate attention"
            : "No critical emergencies at this time";
        
        Text alertMessage = new Text(message);
        alertMessage.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        alertMessage.setFill(Color.WHITE);
          alertContent.getChildren().addAll(alertTitle, alertMessage);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        banner.getChildren().addAll(alertIcon, alertContent, spacer);
        
        return banner;
    }
      private HBox createControls() {
        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(20));
        controls.setStyle(ThemeManager.getCardStyle());
        
        // Action buttons section
        VBox actionsSection = new VBox(5);
        Label actionsLabel = new Label("Quick Actions");
        actionsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        actionsLabel.setTextFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        HBox actionButtons = new HBox(10);
          Button newEmergencyBtn = new Button("+ New Emergency");
        newEmergencyBtn.setStyle(ThemeManager.getButtonPrimaryStyle());
        newEmergencyBtn.setOnAction(e -> handleNewEmergency());
        
        actionButtons.getChildren().add(newEmergencyBtn);
        actionsSection.getChildren().addAll(actionsLabel, actionButtons);
        controls.getChildren().add(actionsSection);
        
        return controls;
    }
    
    private VBox createEmergencyTabs() {
        VBox tabSection = new VBox(15);
        tabSection.setPadding(new Insets(20));
        tabSection.setStyle(ThemeManager.getCardStyle());
        
        // Create TabPane
        TabPane tabPane = new TabPane();
        tabPane.setPrefHeight(450);
        
        // Tab 1: Emergency Requests (existing)
        Tab emergencyRequestsTab = new Tab("Emergency Requests");
        emergencyRequestsTab.setClosable(false);
        VBox emergencyContent = createEmergencyRequestsContent();
        emergencyRequestsTab.setContent(emergencyContent);
        
        // Tab 2: SOS Alerts
        Tab sosAlertsTab = new Tab("SOS Alerts");
        sosAlertsTab.setClosable(false);
        VBox sosContent = createSOSAlertsContent();
        sosAlertsTab.setContent(sosContent);
        
        tabPane.getTabs().addAll(emergencyRequestsTab, sosAlertsTab);
        tabSection.getChildren().add(tabPane);
        return tabSection;
    }

    private VBox createEmergencyRequestsContent() {
        VBox tableSection = new VBox(15);
        tableSection.setPadding(new Insets(20));
        tableSection.setStyle(ThemeManager.getCardStyle());
        
        // Table header
        Text tableTitle = new Text("Emergency Requests");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        tableTitle.setFill(Color.web(ThemeManager.PRIMARY_DARK));        // Create table
        emergencyTable = new TableView<>();
        emergencyTable.setPrefHeight(400);
        
        // Define columns
        TableColumn<EmergencyRequest, String> idCol = new TableColumn<>("Request ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        idCol.setPrefWidth(120);
          TableColumn<EmergencyRequest, EmergencyRequest.EmergencyType> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("emergencyType"));
        typeCol.setPrefWidth(120);
        typeCol.setCellFactory(column -> new TableCell<EmergencyRequest, EmergencyRequest.EmergencyType>() {
            @Override
            protected void updateItem(EmergencyRequest.EmergencyType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                } else {
                    setText(type.getDisplayName());
                }
            }
        });
          TableColumn<EmergencyRequest, EmergencyRequest.Priority> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityCol.setPrefWidth(100);
        priorityCol.setCellFactory(column -> new TableCell<EmergencyRequest, EmergencyRequest.Priority>() {
            @Override
            protected void updateItem(EmergencyRequest.Priority priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(priority.getDisplayName());
                    String color = priority.getColor();
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<EmergencyRequest, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("locationName"));
        locationCol.setPrefWidth(150);        TableColumn<EmergencyRequest, EmergencyRequest.RequestStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);
        statusCol.setCellFactory(column -> new TableCell<EmergencyRequest, EmergencyRequest.RequestStatus>() {
            @Override
            protected void updateItem(EmergencyRequest.RequestStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status.getDisplayName());
                }
            }        });        TableColumn<EmergencyRequest, String> volunteerCol = new TableColumn<>("Assigned Volunteer");
        volunteerCol.setCellValueFactory(data -> {
            String volunteerName = data.getValue().getAssignedVolunteer();
            return new SimpleStringProperty(volunteerName != null && !volunteerName.trim().isEmpty() ? volunteerName : "None");
        });
        volunteerCol.setPrefWidth(150);

        TableColumn<EmergencyRequest, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);        actionCol.setCellFactory(column -> {
            return new TableCell<EmergencyRequest, Void>() {
                private final Button viewBtn = new Button("View");
                private final Button assignBtn = new Button("Assign");
                private final Button updateBtn = new Button("Update");
                
                {
                    viewBtn.setStyle("-fx-background-color: " + ThemeManager.INFO_COLOR + "; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 4; " +
                                    "-fx-padding: 5 10; " +
                                    "-fx-font-size: 11px; " +
                                    "-fx-cursor: hand;");
                    
                    assignBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                                      "-fx-text-fill: white; " +
                                      "-fx-background-radius: 4; " +
                                      "-fx-padding: 5 10; " +
                                      "-fx-font-size: 11px; " +
                                      "-fx-cursor: hand;");
                    
                    updateBtn.setStyle("-fx-background-color: " + ThemeManager.WARNING_COLOR + "; " +
                                      "-fx-text-fill: #212529; " +
                                      "-fx-background-radius: 4; " +
                                      "-fx-padding: 5 10; " +
                                      "-fx-font-size: 11px; " +
                                      "-fx-cursor: hand;");
                      // Add event handlers for the buttons
                    viewBtn.setOnAction(e -> {
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        EmergencyView.this.handleViewEmergency(request);
                    });
                    
                    assignBtn.setOnAction(e -> {
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        EmergencyView.this.handleAssignEmergency(request);
                    });
                      updateBtn.setOnAction(e -> {
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        EmergencyView.this.handleUpdateEmergencyStatus(request);
                    });
                }
                  @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        // Get the current request to check status
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        
                        // Disable assign button for completed or cancelled emergencies
                        boolean isCompleted = request.getStatus() == EmergencyRequest.RequestStatus.COMPLETED;
                        boolean isCancelled = request.getStatus() == EmergencyRequest.RequestStatus.CANCELLED;
                        
                        assignBtn.setDisable(isCompleted || isCancelled);
                        updateBtn.setDisable(isCompleted || isCancelled);
                        
                        // Style disabled buttons
                        if (isCompleted || isCancelled) {
                            assignBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                             "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                            updateBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                             "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");                        } else {
                            // Reset to original styles
                            assignBtn.setStyle("-fx-background-color: " + ThemeManager.PRIMARY_LIGHT + "; " +
                                             "-fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 5 10; " +
                                             "-fx-font-size: 11px; -fx-cursor: hand;");
                            updateBtn.setStyle("-fx-background-color: " + ThemeManager.WARNING_COLOR + "; " +
                                             "-fx-text-fill: #212529; -fx-background-radius: 4; -fx-padding: 5 10; " +
                                             "-fx-font-size: 11px; -fx-cursor: hand;");
                        }
                        
                        HBox buttonBox = new HBox(5);
                        buttonBox.setAlignment(Pos.CENTER);
                        buttonBox.getChildren().addAll(viewBtn, assignBtn, updateBtn);
                        setGraphic(buttonBox);
                    }
                }
            };
        });
        
        // Add columns to table individually to avoid generic array warning
        emergencyTable.getColumns().add(idCol);
        emergencyTable.getColumns().add(typeCol);
        emergencyTable.getColumns().add(priorityCol);        emergencyTable.getColumns().add(locationCol);
        emergencyTable.getColumns().add(statusCol);
        emergencyTable.getColumns().add(volunteerCol);
        emergencyTable.getColumns().add(actionCol);// Load emergency data from database
        loadEmergencyData();

        tableSection.getChildren().addAll(tableTitle, emergencyTable);

        return tableSection;
    }
    
    private HBox createActionButtons() {
        HBox actionButtons = new HBox(15);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        actionButtons.setPadding(new Insets(20));        actionButtons.setStyle(ThemeManager.getCardStyle());

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle(ThemeManager.getButtonPrimaryStyle());
        // Add refresh functionality
        refreshBtn.setOnAction(e -> handleRefresh());

        actionButtons.getChildren().addAll(refreshBtn);        return actionButtons;
    }
    
    private void loadEmergencyData() {
        // Load emergency requests from database
        try {
            System.out.println("Loading emergency data from database...");
            
            if (emergencyTable == null) {
                System.err.println("Error: emergencyTable is null");
                return;
            }
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (dbManager == null) {
                System.err.println("Error: Unable to get DatabaseManager instance");
                return;
            }
              // Join with users table to get assigned volunteer name
            String sql = "SELECT er.*, u.name as volunteer_name " +
                        "FROM emergency_requests er " +
                        "LEFT JOIN users u ON er.assigned_volunteer = u.user_id " +
                        "ORDER BY er.created_at DESC";
            System.out.println("Executing SQL: " + sql);
            
            // Clear existing data
            emergencyTable.getItems().clear();
                
            // Execute query and populate table
            try (java.sql.ResultSet rs = dbManager.executeQuery(sql)) {
                if (rs == null) {
                    System.err.println("Error: ResultSet is null");
                } else {
                    loadEmergencyRequestsFromResultSet(rs);
                    System.out.println("ResultSet processed successfully");
                }
            }
            
            // Update the emergency counts after loading data
            updateEmergencyCounts();
            
            System.out.println("Emergency data loaded successfully: " + emergencyTable.getItems().size() + " requests");
        } catch (Exception e) {
            System.err.println("Error loading emergency data: " + e.getMessage());
            e.printStackTrace();
            // Clear table on error but don't throw any UI exceptions
            try {
                emergencyTable.getItems().clear();
            } catch (Exception ex) {
                System.err.println("Error clearing emergency table: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Updates the counts of emergencies by priority from the database
     */
    private void updateEmergencyCounts() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Reset counts to 0
            criticalCount = 0;
            highCount = 0;
            mediumCount = 0;
            
            // Query for counts by priority
            String countQuery = "SELECT priority, COUNT(*) as count FROM emergency_requests " +
                              "WHERE status != 'COMPLETED' AND status != 'CANCELLED' " +
                              "GROUP BY priority";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(countQuery)) {
                while (rs.next()) {
                    String priority = rs.getString("priority");
                    int count = rs.getInt("count");
                    
                    switch (priority) {
                        case "CRITICAL":
                            criticalCount = count;
                            break;
                        case "HIGH":
                            highCount = count;
                            break;
                        case "MEDIUM":
                            mediumCount = count;
                            break;
                        default:
                            // Ignore other priorities (LOW)
                            break;
                    }
                }
            }
              System.out.println("Emergency counts updated: " + 
                             criticalCount + " critical, " + 
                             highCount + " high, " + 
                             mediumCount + " medium");
            
            // Update UI indicators if they exist
            if (criticalCountText != null) {
                criticalCountText.setText(String.valueOf(criticalCount));
            }
            if (highCountText != null) {
                highCountText.setText(String.valueOf(highCount));
            }
            if (mediumCountText != null) {
                mediumCountText.setText(String.valueOf(mediumCount));
            }
                             
        } catch (Exception e) {
            System.err.println("Error updating emergency counts: " + e.getMessage());
            e.printStackTrace();
            
            // Reset counts on error
            criticalCount = 0;
            highCount = 0;
            mediumCount = 0;
        }
    }
    
    /**
     * Handles the response to critical emergencies when the "Respond Now" button is clicked
     */
    private void handleRespondToCriticalEmergencies() {
        if (criticalCount == 0) {
            // No critical emergencies to respond to
            showAlert(Alert.AlertType.INFORMATION, 
                    "No Critical Emergencies", 
                    "There are currently no critical emergencies that require attention.");
            return;
        }
          // Find critical emergencies and focus on them
        try {
            // Load critical emergencies
            emergencyTable.getItems().clear();
              DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT er.*, u.name as volunteer_name " +
                       "FROM emergency_requests er " +
                       "LEFT JOIN users u ON er.assigned_volunteer = u.user_id " +
                       "WHERE er.priority = 'CRITICAL' AND (er.status = 'PENDING' OR er.status = 'ASSIGNED') " +
                       "ORDER BY er.created_at DESC";
                
                try (java.sql.ResultSet rs = dbManager.executeQuery(sql)) {
                    loadEmergencyRequestsFromResultSet(rs);
                }
                  // Show response dialog with options for each critical emergency
                if (emergencyTable.getItems().size() > 0) {
                    showCriticalResponseDialog();
                } else {
                    showAlert(Alert.AlertType.INFORMATION, 
                            "No Active Critical Emergencies", 
                            "There are no pending critical emergencies that require attention at this time.");
                }
        } catch (Exception e) {
            System.err.println("Error responding to critical emergencies: " + e.getMessage());
            e.printStackTrace();
            
            showAlert(Alert.AlertType.ERROR, 
                    "Error", 
                    "An error occurred while trying to respond to critical emergencies. Please try again.");
        }
    }
    
    /**
     * Shows a dialog with options for responding to critical emergencies
     */
    private void showCriticalResponseDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Critical Emergency Response");
        dialog.setHeaderText("Critical Emergencies Requiring Immediate Attention");
        
        ButtonType respondAllType = new ButtonType("Respond to All", ButtonBar.ButtonData.OK_DONE);
        ButtonType assignType = new ButtonType("Assign Volunteers", ButtonBar.ButtonData.APPLY);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(respondAllType, assignType, cancelType);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20, 10, 10, 10));
        
        // Show list of critical emergencies
        for (EmergencyRequest request : emergencyTable.getItems()) {
            HBox emergencyItem = new HBox(10);
            emergencyItem.setAlignment(Pos.CENTER_LEFT);
            
            Label idLabel = new Label(request.getRequestId());
            idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            
            Label typeLabel = new Label(request.getEmergencyType().getDisplayName());
            
            Label locationLabel = new Label(request.getLocationName());
            
            emergencyItem.getChildren().addAll(
                idLabel, 
                new Separator(javafx.geometry.Orientation.VERTICAL),
                typeLabel, 
                new Separator(javafx.geometry.Orientation.VERTICAL),
                locationLabel
            );
            
            content.getChildren().add(emergencyItem);
        }
        
        dialog.getDialogPane().setContent(content);
        
        // Handle response action based on button clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == respondAllType) {
                // Mark all as IN_PROGRESS
                for (EmergencyRequest request : emergencyTable.getItems()) {
                    updateEmergencyStatus(request.getRequestId(), EmergencyRequest.RequestStatus.IN_PROGRESS);
                }
                return "respond_all";
            } else if (dialogButton == assignType) {
                showAssignVolunteersDialog();
                return "assign";
            }
            return null;
        });
        
        dialog.showAndWait();
        
        // Refresh the emergency table after response
        loadEmergencyData();
    }
    
    /**
     * Shows a dialog to assign volunteers to critical emergencies
     */
    private void showAssignVolunteersDialog() {
        // In a real implementation, this would show available volunteers
        // and allow assigning them to emergencies
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Assign Volunteers");
        alert.setHeaderText("Volunteer Assignment");
        alert.setContentText("This would show a list of available volunteers to assign to critical emergencies.");
        alert.showAndWait();
    }
    
    /**
     * Updates the status of an emergency request
     * 
     * @param requestId The ID of the request to update
     * @param newStatus The new status to set
     */
    private void updateEmergencyStatus(String requestId, EmergencyRequest.RequestStatus newStatus) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "UPDATE emergency_requests SET status = ?, updated_at = CURRENT_TIMESTAMP " +
                       "WHERE request_id = ?";
              dbManager.executeUpdate(sql, newStatus.toString(), requestId);
            
            // Notify all views that emergency data has changed
            DataSyncManager.getInstance().notifyEmergencyDataChanged();
            
            System.out.println("Updated emergency " + requestId + " status to " + newStatus + " - notified all views");
        } catch (Exception e) {
            System.err.println("Error updating emergency status: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Shows an alert dialog with the specified type, title, and message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Loads emergency requests from a ResultSet into the table
     * 
     * @param rs The ResultSet containing emergency request data
     * @throws java.sql.SQLException if an error occurs while reading the ResultSet
     */    private void loadEmergencyRequestsFromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        int count = 0;
        while (rs.next()) {
            try {
                EmergencyRequest request = new EmergencyRequest();
                request.setRequestId(rs.getString("request_id"));
                request.setRequesterId(rs.getString("requester_id"));
            
                // Parse emergency type
                String typeStr = rs.getString("emergency_type");
                if (typeStr != null) {
                    try {
                        request.setEmergencyType(EmergencyRequest.EmergencyType.valueOf(typeStr));
                    } catch (IllegalArgumentException e) {
                        request.setEmergencyType(EmergencyRequest.EmergencyType.MEDICAL); // Default
                    }
                }
            
                // Parse priority
                String priorityStr = rs.getString("priority");
                if (priorityStr != null) {
                    try {
                        request.setPriority(EmergencyRequest.Priority.valueOf(priorityStr));
                    } catch (IllegalArgumentException e) {
                        request.setPriority(EmergencyRequest.Priority.MEDIUM); // Default
                    }
                }
            
                request.setLocationLat(rs.getDouble("location_lat"));
                request.setLocationLng(rs.getDouble("location_lng"));
                request.setLocationName(rs.getString("location_name"));
                request.setDescription(rs.getString("description"));
                request.setPeopleCount(rs.getInt("people_count"));
            
                // Parse status
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    try {
                        request.setStatus(EmergencyRequest.RequestStatus.valueOf(statusStr));
                    } catch (IllegalArgumentException e) {
                        request.setStatus(EmergencyRequest.RequestStatus.PENDING); // Default
                    }
                }
            
                // Use volunteer name from JOIN query, fallback to assigned_volunteer ID if name is null
                String volunteerName = rs.getString("volunteer_name");
                String assignedVolunteer = rs.getString("assigned_volunteer");
                
                if (volunteerName != null && !volunteerName.trim().isEmpty()) {
                    request.setAssignedVolunteer(volunteerName);
                } else if (assignedVolunteer != null && !assignedVolunteer.trim().isEmpty()) {
                    request.setAssignedVolunteer(assignedVolunteer); // Fallback to ID if name not found
                } else {
                    request.setAssignedVolunteer(null);
                }
            
                // Parse timestamps
                java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
                if (createdTs != null) {
                    request.setCreatedAt(createdTs.toLocalDateTime());
                }
            
                java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
                if (updatedTs != null) {
                    request.setUpdatedAt(updatedTs.toLocalDateTime());
                }
            
                emergencyTable.getItems().add(request);                count++;
            } catch (Exception e) {
                System.err.println("Error processing emergency request row: " + e.getMessage());                e.printStackTrace();
                // Continue to next row
            }
        }
        
        System.out.println("Processed " + count + " emergency request records");
    }
    
    /**
     * Refreshes the emergency data from the database
     */    private void handleRefresh() {
        // Show loading indicator
        System.out.println("Refreshing emergency data...");
          // Load fresh data from database
        loadEmergencyData();
        
        // Also refresh SOS data if SOS table exists
        if (sosTable != null) {
            loadSOSData(sosTable);
            System.out.println("SOS data refreshed");
        }
        
        // Update emergency counts to refresh header indicators
        updateEmergencyCounts();
        
        // Refresh the table view to update time displays
        emergencyTable.refresh();
        
        // Show success message
        showAlert(Alert.AlertType.INFORMATION,
                "Data Refreshed",
                "Emergency and SOS data have been successfully refreshed from the database.");
    }
      /**
     * Shows a dialog to notify volunteers about emergencies
     */
    private void handleNotifyVolunteers() {
        if (criticalCount == 0 && highCount == 0) {
            showAlert(Alert.AlertType.INFORMATION,
                    "No Urgent Emergencies",
                    "There are no critical or high priority emergencies to notify volunteers about.");
            return;
        }
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Notify Volunteers");
        dialog.setHeaderText("Send Emergency Notifications to Volunteers");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 10, 10, 10));
        
        Label messageLabel = new Label("Notification Message:");
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        TextArea messageArea = new TextArea();
        messageArea.setPrefRowCount(5);
        messageArea.setText("EMERGENCY ALERT: " + criticalCount + " critical and " + highCount + 
                          " high priority emergencies require immediate assistance. " +
                          "Please check your assignments and respond ASAP.");
        
        Label selectVolunteersLabel = new Label("Select Volunteers:");
        selectVolunteersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        VBox checkboxGroup = new VBox(5);
        CheckBox allActiveCheck = new CheckBox("All Active Volunteers");
        CheckBox nearbyCheck = new CheckBox("Nearby Volunteers Only");
        CheckBox specializedCheck = new CheckBox("Specialized Skills Only");
        
        allActiveCheck.setSelected(true);
        
        checkboxGroup.getChildren().addAll(allActiveCheck, nearbyCheck, specializedCheck);
        
        Label notificationTypeLabel = new Label("Notification Type:");
        notificationTypeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        HBox notificationOptions = new HBox(15);
        notificationOptions.setAlignment(Pos.CENTER_LEFT);
        
        CheckBox smsCheck = new CheckBox("SMS");
        CheckBox appCheck = new CheckBox("In-App");
        CheckBox emailCheck = new CheckBox("Email");
        
        smsCheck.setSelected(true);
        appCheck.setSelected(true);
        
        notificationOptions.getChildren().addAll(smsCheck, appCheck, emailCheck);
        
        content.getChildren().addAll(
            messageLabel, messageArea, 
            selectVolunteersLabel, checkboxGroup,
            notificationTypeLabel, notificationOptions
        );
        
        dialog.getDialogPane().setContent(content);
        
        ButtonType sendButtonType = new ButtonType("Send Notifications", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, cancelButtonType);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                return messageArea.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(message -> {
            if (message != null && !message.isEmpty()) {
                int volunteerCount = 0;
                
                // In a real app, this would actually send notifications to volunteers
                // For now, we'll simulate it
                try {
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String countQuery = "SELECT COUNT(*) as count FROM users WHERE user_type = 'VOLUNTEER' AND status = 'ACTIVE'";
                    
                    try (java.sql.ResultSet rs = dbManager.executeQuery(countQuery)) {
                        if (rs.next()) {
                            volunteerCount = rs.getInt("count");
                        }
                    }
                    
                    String confirmMessage = "Notifications sent successfully to " + volunteerCount + " volunteers.";
                    showAlert(Alert.AlertType.INFORMATION, "Notifications Sent", confirmMessage);
                    
                } catch (Exception e) {
                    System.err.println("Error sending notifications: " + e.getMessage());
                    e.printStackTrace();
                    
                    showAlert(Alert.AlertType.ERROR, "Error", 
                            "An error occurred while sending notifications. Please try again.");
                }
            }
        });
    }
    
    /**
     * Handles creating a new emergency request
     */
    private void handleNewEmergency() {
        Dialog<EmergencyRequest> dialog = new Dialog<>();
        dialog.setTitle("New Emergency Request");
        dialog.setHeaderText("Create a New Emergency Request");
        
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, cancelButtonType);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        // Type dropdown
        ComboBox<EmergencyRequest.EmergencyType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(EmergencyRequest.EmergencyType.values());
        typeCombo.setValue(EmergencyRequest.EmergencyType.MEDICAL);
        
        // Priority dropdown
        ComboBox<EmergencyRequest.Priority> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(EmergencyRequest.Priority.values());
        priorityCombo.setValue(EmergencyRequest.Priority.MEDIUM);
          // Location field
        TextField locationNameField = new TextField();
        locationNameField.setPromptText("Enter district or specific location");
        
        // Location help text
        Label locationHelpLabel = new Label("Enter a Bangladesh district name (e.g., Dhaka, Chittagong, Cox's Bazar)");
        locationHelpLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        locationHelpLabel.setTextFill(Color.web("#666666"));
        
        // Description
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        
        // People affected
        Spinner<Integer> peopleSpinner = new Spinner<>(1, 1000, 1);
        peopleSpinner.setEditable(true);
        
        // Requester ID (would be auto-filled from current user in a real app)
        TextField requesterIdField = new TextField("ADMIN001");
        requesterIdField.setDisable(true);
        
        // Add fields to the grid
        int row = 0;
        grid.add(new Label("Emergency Type:"), 0, row);
        grid.add(typeCombo, 1, row++);
        
        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityCombo, 1, row++);
        
        grid.add(new Label("Location:"), 0, row);
        VBox locationBox = new VBox(2);
        locationBox.getChildren().addAll(locationNameField, locationHelpLabel);
        grid.add(locationBox, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);
        
        grid.add(new Label("People Affected:"), 0, row);
        grid.add(peopleSpinner, 1, row++);
        
        grid.add(new Label("Requester ID:"), 0, row);
        grid.add(requesterIdField, 1, row++);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert the result when the create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {                try {
                    // Get the location name
                    String locationName = locationNameField.getText().trim();
                    if (locationName.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", 
                                "Please enter a valid location name.");
                        return null;
                    }
                    
                    // Create request
                    EmergencyRequest request = new EmergencyRequest();
                    request.setRequestId("ER" + System.currentTimeMillis()); // Generate unique ID
                    request.setRequesterId(requesterIdField.getText());
                    request.setEmergencyType(typeCombo.getValue());
                    request.setPriority(priorityCombo.getValue());
                    request.setLocationName(locationName);
                      // Set default coordinates (center of Bangladesh: Dhaka)
                    // Bangladesh roughly: 20.5-26.5 lat, 88-92.5 long
                    double lat = 23.8103; // Dhaka latitude as default
                    double lng = 90.4125; // Dhaka longitude as default
                    
                    request.setLocationLat(lat);
                    request.setLocationLng(lng);
                    request.setDescription(descriptionArea.getText());
                    request.setPeopleCount(peopleSpinner.getValue());
                    request.setStatus(EmergencyRequest.RequestStatus.PENDING);
                    request.setCreatedAt(java.time.LocalDateTime.now());
                    request.setUpdatedAt(java.time.LocalDateTime.now());
                    return request;
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error Creating Emergency", 
                            "An error occurred: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(request -> {
            // Save the new emergency to the database
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String sql = "INSERT INTO emergency_requests " +
                           "(request_id, requester_id, emergency_type, priority, location_lat, location_lng, " +
                           "location_name, description, people_count, status, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                
                dbManager.executeUpdate(sql,
                    request.getRequestId(),
                    request.getRequesterId(),
                    request.getEmergencyType().toString(),
                    request.getPriority().toString(),
                    request.getLocationLat(),
                    request.getLocationLng(),
                    request.getLocationName(),
                    request.getDescription(),
                    request.getPeopleCount(),
                    request.getStatus().toString()
                );                System.out.println("Emergency created successfully. Refreshing data...");
                
                // Notify all views that emergency data has changed
                DataSyncManager.getInstance().notifyEmergencyDataChanged();
                
                // Directly add to table for immediate feedback
                emergencyTable.getItems().add(0, request); // Add at top of the list
                
                // Update counts
                if (request.getPriority() == EmergencyRequest.Priority.CRITICAL) {
                    criticalCount++;
                } else if (request.getPriority() == EmergencyRequest.Priority.HIGH) {
                    highCount++;
                } else if (request.getPriority() == EmergencyRequest.Priority.MEDIUM) {
                    mediumCount++;
                }
                
                // Then refresh from database
                try {
                    loadEmergencyData();
                    updateEmergencyCounts();
                } catch (Exception ex) {
                    System.err.println("Error refreshing data after creation: " + ex.getMessage());
                    ex.printStackTrace();
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Emergency Created", 
                        "The emergency request has been created successfully.");
                
                // If it's a critical emergency, update the UI accordingly
                if (request.getPriority() == EmergencyRequest.Priority.CRITICAL) {
                    showAlert(Alert.AlertType.WARNING, "Critical Emergency", 
                            "A new CRITICAL emergency has been added to the system.");
                }
                
            } catch (Exception e) {
                System.err.println("Error creating emergency request: " + e.getMessage());
                e.printStackTrace();
                
                showAlert(Alert.AlertType.ERROR, "Database Error", 
                        "Could not save the emergency request. Please try again.");
            }
        });
    }
    
    /**
     * Handles broadcasting an emergency alert
     */
    private void handleBroadcastAlert() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Broadcast Emergency Alert");
        dialog.setHeaderText("Send an Emergency Alert to All Users");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Alert type
        Label alertTypeLabel = new Label("Alert Type:");
        alertTypeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ComboBox<String> alertTypeCombo = new ComboBox<>();
        alertTypeCombo.getItems().addAll(
            "General Emergency", 
            "Evacuation Order", 
            "Weather Warning", 
            "Medical Emergency", 
            "Security Threat"
        );
        alertTypeCombo.setValue("General Emergency");
        
        // Alert level
        Label alertLevelLabel = new Label("Alert Level:");
        alertLevelLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        HBox alertLevelBox = new HBox(15);
        alertLevelBox.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup alertLevelGroup = new ToggleGroup();
        RadioButton lowRadio = new RadioButton("Low");
        RadioButton mediumRadio = new RadioButton("Medium");
        RadioButton highRadio = new RadioButton("High");
        RadioButton criticalRadio = new RadioButton("Critical");
        
        lowRadio.setToggleGroup(alertLevelGroup);
        mediumRadio.setToggleGroup(alertLevelGroup);
        highRadio.setToggleGroup(alertLevelGroup);
        criticalRadio.setToggleGroup(alertLevelGroup);
        
        mediumRadio.setSelected(true);
        
        alertLevelBox.getChildren().addAll(lowRadio, mediumRadio, highRadio, criticalRadio);
        
        // Alert message
        Label messageLabel = new Label("Alert Message:");
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        TextArea messageArea = new TextArea();
        messageArea.setPrefRowCount(5);
        messageArea.setText("EMERGENCY ALERT: Please remain calm and follow the instructions from authorities.");
        
        // Target recipients
        Label recipientsLabel = new Label("Send To:");
        recipientsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        VBox recipientOptions = new VBox(5);
        CheckBox allUsersCheck = new CheckBox("All Users");
        CheckBox affectedAreasCheck = new CheckBox("Users in Affected Areas");
        CheckBox authoritiesCheck = new CheckBox("Authorities Only");
        CheckBox volunteersCheck = new CheckBox("Volunteers Only");
        
        allUsersCheck.setSelected(true);
        
        recipientOptions.getChildren().addAll(allUsersCheck, affectedAreasCheck, authoritiesCheck, volunteersCheck);
        
        // Broadcast channels
        Label channelsLabel = new Label("Broadcast Channels:");
        channelsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        HBox channelsBox = new HBox(15);
        channelsBox.setAlignment(Pos.CENTER_LEFT);
        
        CheckBox appCheck = new CheckBox("In-App");
        CheckBox smsCheck = new CheckBox("SMS");
        CheckBox emailCheck = new CheckBox("Email");
        CheckBox socialCheck = new CheckBox("Social Media");
        
        appCheck.setSelected(true);
        smsCheck.setSelected(true);
        
        channelsBox.getChildren().addAll(appCheck, smsCheck, emailCheck, socialCheck);
        
        content.getChildren().addAll(
            alertTypeLabel, alertTypeCombo,
            alertLevelLabel, alertLevelBox,
            messageLabel, messageArea,
            recipientsLabel, recipientOptions,
            channelsLabel, channelsBox
        );
        
        dialog.getDialogPane().setContent(content);
        
        ButtonType broadcastButtonType = new ButtonType("Broadcast", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(broadcastButtonType, cancelButtonType);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == broadcastButtonType) {
                return messageArea.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(message -> {
            if (message != null && !message.isEmpty()) {
                // In a real app, this would send the broadcast through various channels
                String alertLevel = "";
                if (lowRadio.isSelected()) alertLevel = "Low";
                else if (mediumRadio.isSelected()) alertLevel = "Medium";
                else if (highRadio.isSelected()) alertLevel = "High";
                else if (criticalRadio.isSelected()) alertLevel = "Critical";
                
                try {
                    // Record the broadcast in the database
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String sql = "INSERT INTO messages " +
                               "(message_id, sender_id, message_type, subject, content, priority, sent_at) " +
                               "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                    
                    String messageId = "MSG" + System.currentTimeMillis();
                    String senderId = "ADMIN001"; // Current user
                    String messageType = "BROADCAST";
                    String subject = alertTypeCombo.getValue() + " - " + alertLevel;
                      dbManager.executeUpdate(sql, messageId, senderId, messageType, subject, message, alertLevel.toUpperCase());
                    
                    // Notify all views that communication data has changed
                    DataSyncManager.getInstance().notifyCommunicationDataChanged();
                    
                    // Show confirmation
                    showAlert(Alert.AlertType.INFORMATION, "Alert Broadcasted", 
                            "The emergency alert has been broadcasted successfully.\n\n" +
                            "Recipients: " + (allUsersCheck.isSelected() ? "All Users" : "Selected Groups") + "\n" +
                            "Channels: " + getSelectedChannels(appCheck, smsCheck, emailCheck, socialCheck));
                    
                } catch (Exception e) {
                    System.err.println("Error broadcasting alert: " + e.getMessage());
                    e.printStackTrace();
                    
                    showAlert(Alert.AlertType.ERROR, "Broadcast Error", 
                            "Could not broadcast the alert. Please try again.");
                }
            }
        });
    }
    
    /**
     * Helper method to get selected broadcast channels
     */
    private String getSelectedChannels(CheckBox... checkboxes) {
        StringBuilder channels = new StringBuilder();
        for (CheckBox checkbox : checkboxes) {
            if (checkbox.isSelected()) {
                if (channels.length() > 0) {
                    channels.append(", ");
                }
                channels.append(checkbox.getText());
            }
        }
        return channels.toString();    }
    
    /**
     * Handle viewing detailed information about an emergency
     */
    private void handleViewEmergency(EmergencyRequest request) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Emergency Details");
        dialog.setHeaderText("Emergency Request: " + request.getRequestId());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        
        // Emergency details
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(10);
        
        // Add details
        int row = 0;
        
        // Request ID
        addDetailRow(detailsGrid, row++, "Request ID:", request.getRequestId());
        
        // Emergency Type
        addDetailRow(detailsGrid, row++, "Emergency Type:", request.getEmergencyType().getDisplayName());
        
        // Priority
        addDetailRow(detailsGrid, row++, "Priority:", request.getPriority().getDisplayName());
        
        // Location
        addDetailRow(detailsGrid, row++, "Location:", request.getLocationName());
        
        // Description
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            addDetailRow(detailsGrid, row++, "Description:", request.getDescription());
        }
        
        // People Count
        addDetailRow(detailsGrid, row++, "People Affected:", String.valueOf(request.getPeopleCount()));
        
        // Status
        addDetailRow(detailsGrid, row++, "Status:", request.getStatus().getDisplayName());
        
        // Time
        addDetailRow(detailsGrid, row++, "Reported:", request.getTimeElapsed());
        
        // Assigned Volunteer
        if (request.getAssignedVolunteer() != null && !request.getAssignedVolunteer().trim().isEmpty()) {
            addDetailRow(detailsGrid, row++, "Assigned Volunteer:", request.getAssignedVolunteer());
        }
        
        // Requester ID
        if (request.getRequesterId() != null && !request.getRequesterId().trim().isEmpty()) {
            addDetailRow(detailsGrid, row++, "Requester ID:", request.getRequesterId());
        }
        
        content.getChildren().add(detailsGrid);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    /**
     * Helper method to add a detail row to the grid
     */
    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label labelControl = new Label(label);
        labelControl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        labelControl.setTextFill(Color.web(ThemeManager.TEXT_PRIMARY));
        
        Label valueControl = new Label(value);
        valueControl.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        valueControl.setTextFill(Color.web(ThemeManager.TEXT_SECONDARY));
        valueControl.setWrapText(true);
        
        grid.add(labelControl, 0, row);
        grid.add(valueControl, 1, row);
    }    /**
     * Handle assigning a volunteer to an emergency
     */
    private void handleAssignEmergency(EmergencyRequest request) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assign Volunteers");
        dialog.setHeaderText("Assign volunteers to: " + request.getDescription());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(650);
        content.setPrefHeight(550);
        
        // Emergency info
        Label emergencyInfo = new Label("Emergency: " + request.getRequestId() + " - " + request.getLocationName());
        emergencyInfo.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        emergencyInfo.setStyle("-fx-text-fill: #1e3c72;");
        
        // Multiple selection instructions
        Label instructionsLabel = new Label("Select multiple volunteers by checking boxes, then click 'Assign Selected':");
        instructionsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        instructionsLabel.setStyle("-fx-text-fill: #666666;");
        
        // Volunteers list
        Label volunteersLabel = new Label("Available Volunteers:");
        volunteersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        ScrollPane scrollPane = new ScrollPane();
        VBox volunteersList = new VBox(8);
        scrollPane.setContent(volunteersList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: white;");        // Load volunteers
        loadVolunteersForEmergencyAssignment(volunteersList, request);
        
        content.getChildren().addAll(emergencyInfo, instructionsLabel, volunteersLabel, scrollPane);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
      /**
     * Load volunteers for emergency assignment with location info and assignment counts
     */
    private void loadVolunteersForEmergencyAssignment(VBox volunteersList, EmergencyRequest request) {
        try {
            volunteersList.getChildren().clear();
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Get all approved volunteers with their assignment counts and check if already assigned to this emergency
            String volunteersQuery = "SELECT u.user_id, u.name, u.email, " +
                                   "COALESCE(u.location_name, u.location) as location, u.skills, " +
                                   "COUNT(va.volunteer_id) as assignment_count, " +
                                   "CASE WHEN va_current.volunteer_id IS NOT NULL THEN 1 ELSE 0 END as already_assigned " +
                                   "FROM users u " +
                                   "LEFT JOIN volunteer_assignments va ON u.user_id = va.volunteer_id " +
                                   "AND va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                                   "LEFT JOIN volunteer_assignments va_current ON u.user_id = va_current.volunteer_id " +
                                   "AND va_current.request_id = ? AND va_current.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                                   "WHERE u.user_type = 'VOLUNTEER' AND u.status IN ('ACTIVE', 'ASSIGNED') " +
                                   "AND ((u.location IS NOT NULL AND u.location != '') OR (u.location_name IS NOT NULL AND u.location_name != '')) " +
                                   "GROUP BY u.user_id, u.name, u.email, u.location_name, u.location, u.skills, va_current.volunteer_id " +
                                   "ORDER BY assignment_count ASC, u.name ASC";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(volunteersQuery, request.getRequestId())) {
                boolean hasVolunteers = false;                while (rs.next()) {
                    hasVolunteers = true;
                    String volunteerId = rs.getString("user_id");
                    String name = rs.getString("name");
                    // Email not needed for display
                    String location = rs.getString("location");  // Now uses COALESCE from query
                    String skills = rs.getString("skills");
                    int assignmentCount = rs.getInt("assignment_count");
                    boolean alreadyAssigned = rs.getInt("already_assigned") == 1;
                    
                    // Create volunteer row - show location below name like skills
                    HBox volunteerRow = createVolunteerAssignmentRow(volunteerId, name, location, skills, 
                                                                   assignmentCount, request, alreadyAssigned);
                    volunteersList.getChildren().add(volunteerRow);
                }
                
                if (!hasVolunteers) {
                    Label noVolunteersLabel = new Label("No volunteers are currently registered in the system.");
                    noVolunteersLabel.setStyle("-fx-text-fill: #666666; -fx-font-style: italic;");
                    volunteersList.getChildren().add(noVolunteersLabel);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading volunteers for assignment: " + e.getMessage());
            Label errorLabel = new Label("Error loading volunteers: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red;");
            volunteersList.getChildren().add(errorLabel);
        }
    }    /**
     * Create a volunteer row for assignment dialog - Enhanced to match SOS assignment style
     */
    private HBox createVolunteerAssignmentRow(String volunteerId, String name, String location, 
                                            String skills, int assignmentCount, EmergencyRequest request, boolean alreadyAssigned) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 5;");
        
        // Volunteer info
        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: #212529;");
          Label locationLabel = new Label("Location: " + (location != null && !location.isEmpty() ? location : "Location not set"));
        locationLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        
        Label skillsLabel = new Label("Skills: " + (skills != null && !skills.trim().isEmpty() ? skills : "No skills listed"));
        skillsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        
        infoBox.getChildren().addAll(nameLabel, locationLabel, skillsLabel);
        
        // Assignment count
        VBox countBox = new VBox(3);
        countBox.setAlignment(Pos.CENTER);
        countBox.setPrefWidth(80);
        
        Label countLabel = new Label(String.valueOf(assignmentCount));
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        countLabel.setStyle("-fx-text-fill: " + (assignmentCount == 0 ? ThemeManager.SUCCESS_COLOR : 
                          assignmentCount <= 2 ? ThemeManager.WARNING_COLOR : ThemeManager.DANGER_COLOR) + ";");
        
        Label assignmentsLabel = new Label("assignments");
        assignmentsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 10px;");
        
        countBox.getChildren().addAll(countLabel, assignmentsLabel);
        
        // Assign button - check if already assigned to this emergency
        Button assignButton;
        if (alreadyAssigned) {
            assignButton = new Button("✓ Assigned");
            assignButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                "-fx-background-radius: 5; -fx-padding: 8 16; " +
                                "-fx-font-weight: bold;");
            assignButton.setDisable(true);
        } else {
            assignButton = new Button("Assign");
            assignButton.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 5; " +
                                "-fx-padding: 8 16; " +
                                "-fx-font-weight: bold; " +
                                "-fx-cursor: hand;");
            
            assignButton.setOnAction(e -> {
                assignVolunteerToEmergency(volunteerId, name, request.getRequestId(), assignButton);
            });
        }
        
        row.getChildren().addAll(infoBox, countBox, assignButton);
        return row;
    }
    
    /**
     * Assign a specific volunteer to an emergency
     */
    private void assignVolunteerToEmergency(String volunteerId, String volunteerName, 
                                          String requestId, Button assignButton) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Check if volunteer is already assigned to this emergency
            String checkQuery = "SELECT COUNT(*) as count FROM volunteer_assignments " +
                              "WHERE volunteer_id = ? AND request_id = ? AND status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS')";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(checkQuery, volunteerId, requestId)) {
                if (rs.next() && rs.getInt("count") > 0) {
                    showAlert(Alert.AlertType.WARNING, "Already Assigned", 
                            volunteerName + " is already assigned to this emergency.");
                    return;
                }
            }
              // Create assignment
            String assignmentId = "ASSIGN_" + System.currentTimeMillis();
            String insertQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, " +
                               "assignment_type, status, assigned_at) VALUES (?, ?, ?, 'EMERGENCY', 'ASSIGNED', datetime('now'))";
              dbManager.executeUpdate(insertQuery, assignmentId, volunteerId, requestId);
            
            // Update emergency status and assigned volunteer field (handle multiple volunteers)
            String getExistingQuery = "SELECT assigned_volunteer FROM emergency_requests WHERE request_id = ?";
            String currentAssigned = null;
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(getExistingQuery, requestId)) {
                if (rs.next()) {
                    currentAssigned = rs.getString("assigned_volunteer");
                }
            }
            
            String newAssignedValue;
            if (currentAssigned == null || currentAssigned.trim().isEmpty() || "None".equals(currentAssigned)) {
                newAssignedValue = volunteerName;
            } else if (!currentAssigned.contains(volunteerName)) {
                newAssignedValue = currentAssigned + ", " + volunteerName;
            } else {
                newAssignedValue = currentAssigned; // Already in the list
            }
              String updateEmergencyQuery = "UPDATE emergency_requests SET status = 'ASSIGNED', assigned_volunteer = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
            dbManager.executeUpdate(updateEmergencyQuery, newAssignedValue, requestId);
              // Show success but don't disable button (allow multiple assignments)
            showAlert(Alert.AlertType.INFORMATION, "Assignment Successful", 
                    volunteerName + " has been assigned to this emergency.");
              assignButton.setText("✓ Added");
            assignButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                                "-fx-background-radius: 5; -fx-padding: 8 16; " +
                                "-fx-font-weight: bold;");
            // Keep button enabled - allow multiple assignments
            
            // Change button text back after a delay to indicate more assignments are possible
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds
                    javafx.application.Platform.runLater(() -> {
                        assignButton.setText("Assign More");
                        assignButton.setStyle("-fx-background-color: " + ThemeManager.PRIMARY_LIGHT + "; " +
                                            "-fx-text-fill: white; " +
                                            "-fx-background-radius: 5; " +
                                            "-fx-padding: 8 16; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-cursor: hand;");
                    });
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();
              // Refresh emergency table to show updated assigned volunteers
            loadEmergencyData();
            
            // Sync to track assignments
            syncTableAssignmentsToTrackAssignments();
            
        } catch (Exception e) {
            System.err.println("Error assigning volunteer: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Assignment Failed", 
                    "Failed to assign volunteer: " + e.getMessage());
        }
    }    /**
     * Assign a specific volunteer to a SOS alert
     */
    private void assignVolunteerToSOS(String volunteerId, String volunteerName, 
                                    String sosId, Button assignButton) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Check if this specific volunteer is already assigned to this SOS
            String checkQuery = "SELECT COUNT(*) as count FROM volunteer_assignments " +
                              "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'SOS' AND status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS')";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(checkQuery, volunteerId, sosId)) {
                if (rs.next() && rs.getInt("count") > 0) {
                    showAlert(Alert.AlertType.WARNING, "Already Assigned", 
                            volunteerName + " is already assigned to this SOS alert.");
                    return;
                }
            }            // Update SOS status and assigned volunteer field (handle multiple volunteers)
            String getExistingQuery = "SELECT assigned_volunteer FROM sos_alerts WHERE sos_id = ?";
            String currentAssigned = null;
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(getExistingQuery, sosId)) {
                if (rs.next()) {
                    currentAssigned = rs.getString("assigned_volunteer");
                }
            }
            
            String newAssignedValue;
            if (currentAssigned == null || currentAssigned.trim().isEmpty() || "None".equals(currentAssigned)) {
                newAssignedValue = volunteerName;
            } else if (!currentAssigned.contains(volunteerName)) {
                newAssignedValue = currentAssigned + ", " + volunteerName;
            } else {
                newAssignedValue = currentAssigned; // Already in the list
            }
              String updateQuery = "UPDATE sos_alerts SET status = 'ASSIGNED', assigned_volunteer = ?, updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
            dbManager.executeUpdate(updateQuery, newAssignedValue, sosId);
              // Create volunteer assignment record for tracking
            String assignmentId = "SOS_ASSIGN_" + System.currentTimeMillis();
            String insertAssignmentQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, " +
                                         "assignment_type, status, assigned_at) VALUES (?, ?, ?, 'SOS', 'ASSIGNED', datetime('now'))";
            dbManager.executeUpdate(insertAssignmentQuery, assignmentId, volunteerId, sosId);
            
            // Update volunteer assignment count
            String updateVolunteerQuery = "UPDATE users SET assignment_count = COALESCE(assignment_count, 0) + 1, " +
                                        "status = 'ASSIGNED' WHERE user_id = ?";            dbManager.executeUpdate(updateVolunteerQuery, volunteerId);
              // Refresh SOS table
            loadSOSData(sosTable);
            
            // Sync to track assignments
            syncTableAssignmentsToTrackAssignments();
              // Update button state but don't disable (allow multiple assignments)
            assignButton.setText("✓ Added");
            assignButton.setStyle("-fx-background-color: #27ae60; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 5; " +
                                "-fx-padding: 8 16; " +
                                "-fx-font-weight: bold;");
            // Keep button enabled - allow multiple assignments
            
            // Change button text back after a delay to indicate more assignments are possible
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds
                    javafx.application.Platform.runLater(() -> {
                        assignButton.setText("Assign More");
                        assignButton.setStyle("-fx-background-color: " + ThemeManager.PRIMARY_LIGHT + "; " +
                                            "-fx-text-fill: white; " +
                                            "-fx-background-radius: 5; " +
                                            "-fx-padding: 8 16; " +
                                            "-fx-font-weight: bold; " +
                                            "-fx-cursor: hand;");
                    });
                } catch (InterruptedException ex) {
                    // Ignore
                }            }).start();
              showAlert(Alert.AlertType.INFORMATION, "Assignment Successful", 
                    volunteerName + " has been assigned to SOS alert " + sosId);
            
            // Don't close the dialog - allow more assignments
            // ((Stage) assignButton.getScene().getWindow()).close();
            
        } catch (Exception e) {
            System.err.println("Error assigning volunteer to SOS: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Assignment Failed", 
                    "Failed to assign volunteer: " + e.getMessage());
        }
    }
      private VBox createSOSAlertsContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // SOS Alerts header
        Text sosTitle = new Text("SOS Emergency Alerts");
        sosTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sosTitle.setFill(Color.web("#e74c3c")); // Red color for urgency// Create SOS table
        sosTable = new TableView<>();
        sosTable.setPrefHeight(350);
        // Apply custom table styling since ThemeManager.styleTableView doesn't exist
        sosTable.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 6;");
          // Create SOS columns        
        TableColumn<Object[], String> sosIdCol = new TableColumn<>("SOS ID");
        sosIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0].toString()));
        sosIdCol.setPrefWidth(120); // Adjusted for better visibility
          
        TableColumn<Object[], String> senderCol = new TableColumn<>("Sender");
        senderCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1].toString()));
        senderCol.setPrefWidth(90); // Reduced from 120
        
        TableColumn<Object[], String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2].toString()));
        typeCol.setPrefWidth(80);
        
        TableColumn<Object[], String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3].toString()));
        locationCol.setPrefWidth(120); // Reduced from 150
        
        TableColumn<Object[], String> urgencyCol = new TableColumn<>("Urgency");
        urgencyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[4].toString()));
        urgencyCol.setPrefWidth(80);        TableColumn<Object[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[5].toString()));        
        statusCol.setPrefWidth(130); // Increased for better visibility
          
        TableColumn<Object[], String> volunteerCol = new TableColumn<>("Assigned Volunteer");
        volunteerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[6] != null ? data.getValue()[6].toString() : "None"));
        volunteerCol.setPrefWidth(130);
        
        // Action column with buttons to match emergency requests table
        TableColumn<Object[], Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(param -> new TableCell<Object[], Void>() {
            private final Button viewBtn = new Button("View");
            private final Button assignBtn = new Button("Assign");
            private final Button updateBtn = new Button("Update");
            private final HBox buttons = new HBox(5);
            
            {
                viewBtn.setStyle("-fx-background-color: " + ThemeManager.INFO_COLOR + "; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 4; " +
                                "-fx-padding: 5 10; " +
                                "-fx-font-size: 11px; " +
                                "-fx-cursor: hand;");
                
                assignBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                                  "-fx-text-fill: white; " +
                                  "-fx-background-radius: 4; " +
                                  "-fx-padding: 5 10; " +
                                  "-fx-font-size: 11px; " +
                                  "-fx-cursor: hand;");
                
                updateBtn.setStyle("-fx-background-color: " + ThemeManager.WARNING_COLOR + "; " +
                                  "-fx-text-fill: #212529; " +
                                  "-fx-background-radius: 4; " +
                                  "-fx-padding: 5 10; " +
                                  "-fx-font-size: 11px; " +
                                  "-fx-cursor: hand;");
                
                viewBtn.setOnAction(e -> {
                    Object[] alert = getTableRow().getItem();
                    if (alert != null) {
                        handleViewSOSAlert(alert);
                    }
                });
                
                assignBtn.setOnAction(e -> {
                    Object[] alert = getTableRow().getItem();
                    if (alert != null) {
                        handleAssignSOSAlert(alert);
                    }
                });
                
                updateBtn.setOnAction(e -> {
                    Object[] alert = getTableRow().getItem();
                    if (alert != null) {
                        handleUpdateSOSAlert(alert);
                    }
                });
                
                buttons.getChildren().addAll(viewBtn, assignBtn, updateBtn);
                buttons.setAlignment(Pos.CENTER);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Object[] alert = getTableRow().getItem();
                    if (alert != null) {
                        String status = alert[5].toString();
                        boolean isResolved = "RESOLVED".equals(status);
                        
                        assignBtn.setDisable(isResolved);
                        updateBtn.setDisable(isResolved);
                        
                        if (isResolved) {
                            assignBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                             "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                            updateBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                             "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                        } else {
                            assignBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                                             "-fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 5 10; " +
                                             "-fx-font-size: 11px; -fx-cursor: hand;");
                            updateBtn.setStyle("-fx-background-color: " + ThemeManager.WARNING_COLOR + "; " +
                                             "-fx-text-fill: #212529; -fx-background-radius: 4; -fx-padding: 5 10; " +
                                             "-fx-font-size: 11px; -fx-cursor: hand;");
                        }
                    }
                    setGraphic(buttons);
                }
            }
        });
          // Add columns one by one to avoid generic array warning
        sosTable.getColumns().add(sosIdCol);
        sosTable.getColumns().add(senderCol);
        sosTable.getColumns().add(typeCol);
        sosTable.getColumns().add(locationCol);
        sosTable.getColumns().add(urgencyCol);
        sosTable.getColumns().add(statusCol);
        sosTable.getColumns().add(volunteerCol);
        sosTable.getColumns().add(actionCol);
        
        // Load SOS data
        loadSOSData(sosTable);
        
        // Remove separate action buttons - actions are now in table
        content.getChildren().addAll(sosTitle, sosTable);
        return content;
    }
      /**
     * Loads SOS alerts data into the table
     */
    private void loadSOSData(TableView<Object[]> sosTable) {
        try {
            System.out.println("Loading SOS data from database...");
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT * FROM sos_alerts ORDER BY created_at DESC";
            
            // Clear existing data
            sosTable.getItems().clear();
                  
            // Execute query and populate table
            try (java.sql.ResultSet rs = dbManager.executeQuery(sql)) {
                while (rs.next()) {
                    Object[] row = new Object[] {
                        rs.getString("sos_id"),
                        rs.getString("sender_name"),
                        rs.getString("sender_type"),
                        rs.getString("location_name"),
                        rs.getString("urgency_level"),
                        rs.getString("status"),
                                               rs.getString("assigned_volunteer") // Changed from created_at to assigned_volunteer
                    };
                    sosTable.getItems().add(row);
                }
            }
            
            System.out.println("SOS data loaded successfully: " + sosTable.getItems().size() + " alerts");
        } catch (Exception e) {
            System.err.println("Error loading SOS data: " + e.getMessage());
            e.printStackTrace();
        }
    }
      /**
     * Handles viewing SOS alert details
     */
    private void handleViewSOSAlert(Object[] alert) {
        if (alert == null) {
            showAlert(Alert.AlertType.WARNING, "No SOS Alert", "No SOS alert data available.");
            return;
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("SOS Alert Details");
        dialog.setHeaderText("SOS Alert: " + alert[0].toString());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
          // SOS details
        content.getChildren().add(new Label("SOS ID: " + alert[0].toString()));
        content.getChildren().add(new Label("Sender: " + alert[1].toString()));
        content.getChildren().add(new Label("Sender Type: " + alert[2].toString()));
        content.getChildren().add(new Label("Location: " + alert[3].toString()));
        content.getChildren().add(new Label("Urgency Level: " + alert[4].toString()));
        content.getChildren().add(new Label("Status: " + alert[5].toString()));
        content.getChildren().add(new Label("Assigned Volunteer: " + (alert[6] != null ? alert[6].toString() : "None")));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
      /**
     * Handles assigning volunteers to SOS alerts
     */
    private void handleAssignSOSAlert(Object[] alert) {
        if (alert == null) {
            showAlert(Alert.AlertType.WARNING, "No SOS Alert", "No SOS alert data available.");
            return;
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assign Volunteers to SOS Alert");
        dialog.setHeaderText("Assign volunteers to SOS: " + alert[0].toString());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(500);
        
        // SOS info
        Label sosInfo = new Label("SOS Alert: " + alert[0].toString() + " - " + alert[3].toString());
        sosInfo.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        sosInfo.setStyle("-fx-text-fill: #e74c3c;");
        
        // Show SOS details
        VBox detailsBox = new VBox(5);
        detailsBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-border-radius: 5;");
        
        Label senderLabel = new Label("Sender: " + alert[1].toString() + " (" + alert[2].toString() + ")");
        Label locationLabel = new Label("Location: " + alert[3].toString());
        Label urgencyLabel = new Label("Urgency: " + alert[4].toString());
        Label statusLabel = new Label("Status: " + alert[5].toString());
        
        detailsBox.getChildren().addAll(senderLabel, locationLabel, urgencyLabel, statusLabel);
        
        // Volunteers list
        Label volunteersLabel = new Label("Available Volunteers:");
        volunteersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        ScrollPane scrollPane = new ScrollPane();
        VBox volunteersList = new VBox(8);
        scrollPane.setContent(volunteersList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: white;");
        
        // Load volunteers for SOS assignment
        loadVolunteersForSOSAssignment(volunteersList, alert);
        
        content.getChildren().addAll(sosInfo, detailsBox, volunteersLabel, scrollPane);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    /**
     * Load volunteers for SOS assignment
     */
    private void loadVolunteersForSOSAssignment(VBox volunteersList, Object[] alert) {
        try {
            volunteersList.getChildren().clear();
            
            DatabaseManager dbManager = DatabaseManager.getInstance();            // Get all approved volunteers with their assignment counts and check if already assigned to this SOS
            String volunteersQuery = "SELECT u.user_id, u.name, u.email, " +
                                   "COALESCE(u.location_name, u.location) as location, u.skills, " +
                                   "COUNT(va.volunteer_id) as assignment_count, " +
                                   "CASE WHEN va_current.volunteer_id IS NOT NULL THEN 1 ELSE 0 END as already_assigned " +
                                   "FROM users u " +
                                   "LEFT JOIN volunteer_assignments va ON u.user_id = va.volunteer_id " +
                                   "AND va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                                   "LEFT JOIN volunteer_assignments va_current ON u.user_id = va_current.volunteer_id " +
                                   "AND va_current.request_id = ? AND va_current.assignment_type = 'SOS' AND va_current.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                                   "WHERE u.user_type = 'VOLUNTEER' AND u.status IN ('ACTIVE', 'ASSIGNED') " +
                                   "AND ((u.location IS NOT NULL AND u.location != '') OR (u.location_name IS NOT NULL AND u.location_name != '')) " +
                                   "GROUP BY u.user_id, u.name, u.email, u.location_name, u.location, u.skills, va_current.volunteer_id " +
                                   "ORDER BY assignment_count ASC, u.name ASC";
              String sosId = alert[0].toString();
            try (java.sql.ResultSet rs = dbManager.executeQuery(volunteersQuery, sosId)) {
                boolean hasVolunteers = false;
                int count = 0;
                while (rs.next()) {
                    hasVolunteers = true;
                    count++;
                    String volunteerId = rs.getString("user_id");
                    String name = rs.getString("name");                    String location = rs.getString("location");
                    String skills = rs.getString("skills");
                    int assignmentCount = rs.getInt("assignment_count");
                    boolean alreadyAssigned = rs.getInt("already_assigned") > 0;
                    
                    System.out.println("Found volunteer for SOS: " + name + " at " + location + " with " + assignmentCount + " assignments, already assigned: " + alreadyAssigned);
                      // Create volunteer row for SOS assignment
                    HBox volunteerRow = createVolunteerSOSAssignmentRow(volunteerId, name, location, 
                                                                      skills, assignmentCount, alert, alreadyAssigned);
                    volunteersList.getChildren().add(volunteerRow);
                }
                
                System.out.println("Total volunteers found for SOS assignment: " + count);                if (!hasVolunteers) {
                    Label noVolunteersLabel = new Label("No approved volunteers available for assignment.\nPlease ensure volunteers have location information.");
                    noVolunteersLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic; -fx-padding: 20;");
                    volunteersList.getChildren().add(noVolunteersLabel);
                    System.out.println("No volunteers found for SOS - adding 'no volunteers' message");
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading volunteers for SOS assignment: " + e.getMessage());
            e.printStackTrace();
            
            Label errorLabel = new Label("Error loading volunteers: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-padding: 20;");
            volunteersList.getChildren().add(errorLabel);
        }
    }
      /**
     * Creates a volunteer row for SOS assignment
     */    private HBox createVolunteerSOSAssignmentRow(String volunteerId, String name, String location, 
                                               String skills, int assignmentCount, Object[] alert, boolean alreadyAssigned) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 5;");
        
        // Volunteer info
        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: #212529;");
          Label locationLabel = new Label("Location: " + (location != null ? location : "Location not set"));
        locationLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        
        Label skillsLabel = new Label("Skills: " + (skills != null && !skills.trim().isEmpty() ? skills : "No skills listed"));
        skillsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        
        infoBox.getChildren().addAll(nameLabel, locationLabel, skillsLabel);
        
        // Assignment count
        VBox countBox = new VBox(3);
        countBox.setAlignment(Pos.CENTER);
        countBox.setPrefWidth(80);
        
        Label countLabel = new Label(String.valueOf(assignmentCount));
        countLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        countLabel.setStyle("-fx-text-fill: " + (assignmentCount == 0 ? ThemeManager.SUCCESS_COLOR : 
                          assignmentCount <= 2 ? ThemeManager.WARNING_COLOR : ThemeManager.DANGER_COLOR) + ";");
        
        Label assignmentsLabel = new Label("assignments");
        assignmentsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 10px;");
          countBox.getChildren().addAll(countLabel, assignmentsLabel);
        
        // Assign button - check if already assigned to this SOS
        Button assignBtn;
        if (alreadyAssigned) {
            assignBtn = new Button("✓ Assigned");
            assignBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                              "-fx-background-radius: 5; -fx-padding: 8 16; " +
                              "-fx-font-weight: bold;");
            assignBtn.setDisable(true);
        } else {
            assignBtn = new Button("Assign");
            assignBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                              "-fx-text-fill: white; " +
                              "-fx-background-radius: 5; " +
                              "-fx-padding: 8 16; " +
                              "-fx-font-weight: bold; " +
                              "-fx-cursor: hand;");
              assignBtn.setOnAction(e -> {
                assignVolunteerToSOS(volunteerId, name, alert[0].toString(), assignBtn);
            });
        }
          
        row.getChildren().addAll(infoBox, countBox, assignBtn);
        return row;
    }
    
    /**
     * Handles updating SOS alert status
     */
    private void handleUpdateSOSAlert(Object[] alert) {
        if (alert == null) {
            showAlert(Alert.AlertType.WARNING, "No SOS Alert", "No SOS alert data available.");
            return;
        }
        
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update SOS Alert");
        dialog.setHeaderText("Update SOS Alert: " + alert[0].toString());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);
        
        Label statusLabel = new Label("Status:");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
          ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("PENDING", "ASSIGNED", "RESPONDED", "RESOLVED", "CANCELLED");
        statusCombo.setValue(alert[5].toString());
        statusCombo.setPrefWidth(200);
        
        content.getChildren().addAll(statusLabel, statusCombo);
        
        dialog.getDialogPane().setContent(content);
        
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                return statusCombo.getValue();
            }
            return null;
        });
          dialog.showAndWait().ifPresent(newStatus -> {
            try {                DatabaseManager dbManager = DatabaseManager.getInstance();
                String updateSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                
                // Also update resolved_at if status is RESOLVED
                if ("RESOLVED".equals(newStatus)) {
                    updateSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP, resolved_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                }
                
                dbManager.executeUpdate(updateSql, newStatus, alert[0].toString());                // Sync status change to volunteer_assignments table if SOS is assigned
                if ("RESOLVED".equals(newStatus)) {
                    String updateAssignmentSql = "UPDATE volunteer_assignments SET status = 'COMPLETED', completed_at = datetime('now') " +
                                                "WHERE request_id = ? AND assignment_type = 'SOS' AND status != 'COMPLETED'";
                    dbManager.executeUpdate(updateAssignmentSql, alert[0].toString());
                    // Free volunteers from this completed assignment
                    freeVolunteersFromCompletedAssignment(alert[0].toString(), "SOS");
                } else if ("RESPONDED".equals(newStatus)) {
                    String updateAssignmentSql = "UPDATE volunteer_assignments SET status = 'IN_PROGRESS', started_at = datetime('now') " +
                                                "WHERE request_id = ? AND assignment_type = 'SOS' AND status IN ('ASSIGNED', 'ACCEPTED')";
                    dbManager.executeUpdate(updateAssignmentSql, alert[0].toString());
                } else if ("CANCELLED".equals(newStatus)) {
                    String updateAssignmentSql = "UPDATE volunteer_assignments SET status = 'CANCELLED' " +
                                                "WHERE request_id = ? AND assignment_type = 'SOS' AND status != 'COMPLETED'";
                    dbManager.executeUpdate(updateAssignmentSql, alert[0].toString());
                    // Free volunteers from this cancelled assignment
                    freeVolunteersFromCompletedAssignment(alert[0].toString(), "SOS");
                }
                
                // Notify other components about the data change
                DataSyncManager.getInstance().notifyEmergencyDataChanged();
                DataSyncManager.getInstance().notifyVolunteerDataChanged();
                
                // Refresh SOS table
                loadSOSData(sosTable);
                
                showAlert(Alert.AlertType.INFORMATION, "Update Successful", 
                        "SOS alert status has been updated to " + newStatus + ".");
                
            } catch (Exception e) {
                System.err.println("Error updating SOS alert: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Update Failed", 
                        "Failed to update SOS alert: " + e.getMessage());
            }
        });
    }
    
    private void handleUpdateEmergencyStatus(EmergencyRequest request) {
        Dialog<EmergencyRequest.RequestStatus> dialog = new Dialog<>();
        dialog.setTitle("Update Emergency Status");
        dialog.setHeaderText("Emergency Request: " + request.getRequestId());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);
        
        // Current status info
        Label currentStatusLabel = new Label("Current Status: " + request.getStatus().getDisplayName());
        currentStatusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Status selection
        Label selectLabel = new Label("Select New Status:");
        selectLabel.setStyle("-fx-font-weight: bold;");
        
        ComboBox<EmergencyRequest.RequestStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(
            EmergencyRequest.RequestStatus.PENDING,
            EmergencyRequest.RequestStatus.ASSIGNED,
            EmergencyRequest.RequestStatus.IN_PROGRESS,
            EmergencyRequest.RequestStatus.COMPLETED,
            EmergencyRequest.RequestStatus.CANCELLED
        );
        statusCombo.setValue(request.getStatus());
        statusCombo.setPrefWidth(300);
        
        // Notes
        Label notesLabel = new Label("Update Notes (Optional):");
        notesLabel.setStyle("-fx-font-weight: bold;");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter any notes about this status update...");
        notesArea.setPrefRowCount(3);
        notesArea.setPrefWidth(300);
        
        content.getChildren().addAll(currentStatusLabel, selectLabel, statusCombo, notesLabel, notesArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Enable OK button only if status is different from current
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(
            statusCombo.valueProperty().isEqualTo(request.getStatus())
        );
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return statusCombo.getValue();
            }
            return null;
        });
        
        Optional<EmergencyRequest.RequestStatus> result = dialog.showAndWait();
        if (result.isPresent()) {
            updateEmergencyStatusInDatabase(request.getRequestId(), result.get(), notesArea.getText());
        }
    }
      private void updateEmergencyStatusInDatabase(String requestId, EmergencyRequest.RequestStatus newStatus, String notes) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Update status
            String updateSql = "UPDATE emergency_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
            int rowsUpdated = dbManager.executeUpdate(updateSql, newStatus.name(), requestId);
            
            if (rowsUpdated > 0) {
                // Add notes to history if provided
                if (notes != null && !notes.trim().isEmpty()) {
                    String notesSql = "INSERT INTO emergency_notes (request_id, note, created_at) VALUES (?, ?, datetime('now'))";
                    dbManager.executeUpdate(notesSql, requestId, notes.trim());
                }                // Sync status change to volunteer_assignments table if emergency is assigned
                if ("COMPLETED".equals(newStatus.name()) || "RESOLVED".equals(newStatus.name())) {
                    String updateAssignmentSql = "UPDATE volunteer_assignments SET status = 'COMPLETED', completed_at = datetime('now') " +
                                                "WHERE request_id = ? AND assignment_type = 'EMERGENCY' AND status != 'COMPLETED'";
                    dbManager.executeUpdate(updateAssignmentSql, requestId);
                    // Free volunteers from this completed assignment
                    freeVolunteersFromCompletedAssignment(requestId, "EMERGENCY");
                } else if ("IN_PROGRESS".equals(newStatus.name())) {
                    String updateAssignmentSql = "UPDATE volunteer_assignments SET status = 'IN_PROGRESS', started_at = datetime('now') " +
                                                "WHERE request_id = ? AND assignment_type = 'EMERGENCY' AND status IN ('ASSIGNED', 'ACCEPTED')";
                    dbManager.executeUpdate(updateAssignmentSql, requestId);
                } else if ("CANCELLED".equals(newStatus.name())) {
                    String updateAssignmentSql = "UPDATE volunteer_assignments SET status = 'CANCELLED' " +
                                                "WHERE request_id = ? AND assignment_type = 'EMERGENCY' AND status != 'COMPLETED'";
                    dbManager.executeUpdate(updateAssignmentSql, requestId);
                    // Free volunteers from this cancelled assignment
                    freeVolunteersFromCompletedAssignment(requestId, "EMERGENCY");
                }
                
                // Notify other components about the data change
                DataSyncManager.getInstance().notifyEmergencyDataChanged();
                DataSyncManager.getInstance().notifyVolunteerDataChanged();
                  // Refresh the table
                loadEmergencyData();
                
                showAlert(Alert.AlertType.INFORMATION, "Status Updated", 
                    "Emergency request status updated successfully to: " + newStatus.getDisplayName());
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", 
                    "Failed to update emergency request status.");            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Update Failed", 
                "Failed to update status: " + e.getMessage());
        }    }
    
    /**
     * Frees volunteers from completed/cancelled assignments by removing their names from assigned_volunteer field
     */
    private void freeVolunteersFromCompletedAssignment(String requestId, String assignmentType) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            if ("EMERGENCY".equals(assignmentType)) {
                // Clear assigned_volunteer field in emergency_requests
                String updateEmergencySql = "UPDATE emergency_requests SET assigned_volunteer = NULL WHERE request_id = ?";
                dbManager.executeUpdate(updateEmergencySql, requestId);
                System.out.println("Freed volunteers from completed/cancelled Emergency Request: " + requestId);
            } else if ("SOS".equals(assignmentType)) {
                // Clear assigned_volunteer field in sos_alerts
                String updateSOSSql = "UPDATE sos_alerts SET assigned_volunteer = NULL WHERE sos_id = ?";
                dbManager.executeUpdate(updateSOSSql, requestId);
                System.out.println("Freed volunteers from completed/cancelled SOS Alert: " + requestId);
            }
            
        } catch (Exception e) {
            System.err.println("Error freeing volunteers from assignment: " + e.getMessage());
        }
    }

    /**
     * Synchronizes volunteer assignment status changes with source emergency/SOS tables
     */
    private void syncAssignmentStatusToSource(String requestId, String assignmentType, String assignmentStatus) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            if ("EMERGENCY".equals(assignmentType)) {
                String emergencyStatus = mapAssignmentStatusToEmergencyStatus(assignmentStatus);
                if (emergencyStatus != null) {
                    String updateSql = "UPDATE emergency_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
                    dbManager.executeUpdate(updateSql, emergencyStatus, requestId);
                    System.out.println("Synced Emergency Request " + requestId + " status to: " + emergencyStatus);
                }
            } else if ("SOS".equals(assignmentType)) {
                String sosStatus = mapAssignmentStatusToSOSStatus(assignmentStatus);
                if (sosStatus != null) {
                    String updateSql;
                    if ("RESOLVED".equals(sosStatus)) {
                        updateSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP, resolved_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                    } else {
                        updateSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                    }
                    dbManager.executeUpdate(updateSql, sosStatus, requestId);
                    System.out.println("Synced SOS Alert " + requestId + " status to: " + sosStatus);
                }
            }
        } catch (Exception e) {
            System.err.println("Error syncing assignment status to source: " + e.getMessage());
        }
    }
    
    /**
     * Maps volunteer assignment status to emergency request status
     */
    private String mapAssignmentStatusToEmergencyStatus(String assignmentStatus) {
        return switch (assignmentStatus) {
            case "ASSIGNED", "ACCEPTED" -> "ASSIGNED";
            case "IN_PROGRESS" -> "IN_PROGRESS"; 
            case "COMPLETED" -> "COMPLETED";
            case "CANCELLED" -> "PENDING"; // Return to pending if cancelled
            default -> null;
        };
    }
      /**
     * Maps volunteer assignment status to SOS alert status
     */
    private String mapAssignmentStatusToSOSStatus(String assignmentStatus) {
        return switch (assignmentStatus) {
            case "ASSIGNED", "ACCEPTED" -> "ASSIGNED";
            case "IN_PROGRESS" -> "RESPONDED";
            case "COMPLETED" -> "RESOLVED";
            case "CANCELLED" -> "CANCELLED";
            default -> null;
        };
    }

    /**
     * Sync assigned_volunteer field changes to volunteer_assignments table
     * This ensures two-way sync between tables and track assignments
     */
    public void syncTableAssignmentsToTrackAssignments() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Sync Emergency Requests
            String emergencyQuery = "SELECT request_id, assigned_volunteer FROM emergency_requests WHERE assigned_volunteer IS NOT NULL AND assigned_volunteer != '' AND assigned_volunteer != 'None'";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(emergencyQuery)) {
                while (rs.next()) {
                    String requestId = rs.getString("request_id");
                    String assignedVolunteers = rs.getString("assigned_volunteer");
                    
                    if (assignedVolunteers != null && !assignedVolunteers.trim().isEmpty()) {
                        String[] volunteerNames = assignedVolunteers.split(",");
                        
                        for (String volunteerName : volunteerNames) {
                            volunteerName = volunteerName.trim();
                            
                            // Get volunteer ID from name
                            String getVolunteerIdQuery = "SELECT user_id FROM users WHERE name = ? AND user_type = 'VOLUNTEER'";
                            try (java.sql.ResultSet volunteerRs = dbManager.executeQuery(getVolunteerIdQuery, volunteerName)) {
                                if (volunteerRs.next()) {
                                    String volunteerId = volunteerRs.getString("user_id");
                                    
                                    // Check if assignment already exists in volunteer_assignments
                                    String checkAssignmentQuery = "SELECT COUNT(*) as count FROM volunteer_assignments WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'EMERGENCY'";
                                    try (java.sql.ResultSet checkRs = dbManager.executeQuery(checkAssignmentQuery, volunteerId, requestId)) {
                                        if (checkRs.next() && checkRs.getInt("count") == 0) {
                                            // Create assignment record
                                            String assignmentId = "SYNC_" + System.currentTimeMillis() + "_" + volunteerId.substring(0, Math.min(4, volunteerId.length()));
                                            String insertQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, assignment_type, status, assigned_at) VALUES (?, ?, ?, 'EMERGENCY', 'ASSIGNED', datetime('now'))";
                                            dbManager.executeUpdate(insertQuery, assignmentId, volunteerId, requestId);
                                            System.out.println("Synced emergency assignment: " + volunteerName + " -> " + requestId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
              } catch (Exception e) {
            System.err.println("Error syncing table assignments to track assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sync volunteer_assignments (Track Assignment) back to assigned_volunteer fields in tables
     * This ensures that assignments made via Quick Assignment are reflected in Emergency/SOS tables
     */
    public void syncTrackAssignmentsToTables() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Get all EMERGENCY assignments from volunteer_assignments
            String emergencyAssignmentsQuery = "SELECT va.request_id, GROUP_CONCAT(u.name, ', ') as volunteer_names " +
                                             "FROM volunteer_assignments va " +
                                             "JOIN users u ON va.volunteer_id = u.user_id " +
                                             "WHERE va.assignment_type = 'EMERGENCY' " +
                                             "AND va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                                             "GROUP BY va.request_id";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(emergencyAssignmentsQuery)) {
                while (rs.next()) {
                    String requestId = rs.getString("request_id");
                    String volunteerNames = rs.getString("volunteer_names");
                    
                    if (volunteerNames != null && !volunteerNames.trim().isEmpty()) {
                        // Update emergency_requests table
                        String updateQuery = "UPDATE emergency_requests SET assigned_volunteer = ?, status = 'ASSIGNED' WHERE request_id = ?";
                        dbManager.executeUpdate(updateQuery, volunteerNames, requestId);
                        System.out.println("Synced track assignment to emergency table: " + requestId + " -> " + volunteerNames);
                    }
                }
            }
            
            // Get all SOS assignments from volunteer_assignments
            String sosAssignmentsQuery = "SELECT va.request_id, GROUP_CONCAT(u.name, ', ') as volunteer_names " +
                                       "FROM volunteer_assignments va " +
                                       "JOIN users u ON va.volunteer_id = u.user_id " +
                                       "WHERE va.assignment_type = 'SOS' " +
                                       "AND va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                                       "GROUP BY va.request_id";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(sosAssignmentsQuery)) {
                while (rs.next()) {
                    String sosId = rs.getString("request_id");
                    String volunteerNames = rs.getString("volunteer_names");
                    
                    if (volunteerNames != null && !volunteerNames.trim().isEmpty()) {
                        // Update sos_alerts table
                        String updateQuery = "UPDATE sos_alerts SET assigned_volunteer = ?, status = 'ASSIGNED' WHERE sos_id = ?";
                        dbManager.executeUpdate(updateQuery, volunteerNames, sosId);
                        System.out.println("Synced track assignment to SOS table: " + sosId + " -> " + volunteerNames);
                    }
                }
            }
            
        } catch (Exception e) {            System.err.println("Error syncing track assignments to tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Public method to refresh emergency data from external calls
     */
    public void refreshData() {
        // Load fresh data from database
        loadEmergencyData();
        
        // Also refresh SOS data if SOS table exists
        if (sosTable != null) {
            loadSOSData(sosTable);
        }
        
        // Update emergency counts to refresh header indicators
        updateEmergencyCounts();
        
        // Refresh the table view to update time displays
        if (emergencyTable != null) {
            emergencyTable.refresh();
        }
        
        System.out.println("Emergency data refreshed from external call");
    }
}