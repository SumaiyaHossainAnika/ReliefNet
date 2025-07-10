package com.reliefnet.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.beans.property.SimpleStringProperty;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.reliefnet.util.ThemeManager;
import com.reliefnet.util.DataSyncManager;
import com.reliefnet.database.DatabaseManager;
import com.reliefnet.model.User;
import com.reliefnet.model.EmergencyRequest;

/**
 * VolunteerEmergencyView - Emergency response interface for volunteers
 * Allows volunteers to view and respond to emergencies in their area
 */
public class VolunteerEmergencyView {
    
    private TableView<EmergencyRequest> emergencyTable;
    private TableView<Object[]> sosTable;
    private TableView<Object[]> myResponsesTable;
    private ComboBox<String> locationFilterCombo;
    
    // Current volunteer info
    private User currentVolunteer;
    private String volunteerLocation;
    private String volunteerDivision;
    private String volunteerDistrict;
    
    // Store counts for display
    private int availableEmergencies = 0;
    private int myActiveResponses = 0;
    private int totalSOSAlerts = 0;
    
    // Status indicator labels
    private Text availableCountText;
    private Text myResponsesCountText;
    private Text sosCountText;
    
    public VolunteerEmergencyView(User volunteer) {
        this.currentVolunteer = volunteer;
        loadVolunteerLocation();
    }
    
    public VBox createView() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        
        // Header section
        VBox header = createHeader();
        
        // Location filter section
        HBox locationFilter = createLocationFilter();
        
        // Emergency response tabs
        VBox emergencyTabs = createEmergencyTabs();
        
        // My responses section
        VBox myResponsesSection = createMyResponsesSection();
        
        mainContainer.getChildren().addAll(header, locationFilter, emergencyTabs, myResponsesSection);
        
        return mainContainer;
    }
    
    private VBox createHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(20));
        header.setStyle(ThemeManager.getCardStyle());
        
        // Title
        Text title = new Text("Emergency Response");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        // Subtitle with volunteer location
        Text subtitle = new Text("Respond to emergencies in your area • Location: " + 
                                (volunteerLocation != null ? volunteerLocation : "Not Set"));
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        // Status indicators
        HBox statusIndicators = new HBox(30);
        statusIndicators.setAlignment(Pos.CENTER_LEFT);
        statusIndicators.setPadding(new Insets(15, 0, 0, 0));
        
        VBox availableIndicator = createStatusIndicator("Available Emergencies", "0", ThemeManager.DANGER_COLOR);
        availableCountText = (Text) ((VBox) availableIndicator.getChildren().get(0)).getChildren().get(0);
        
        VBox responseIndicator = createStatusIndicator("My Active Responses", "0", ThemeManager.WARNING_COLOR);
        myResponsesCountText = (Text) ((VBox) responseIndicator.getChildren().get(0)).getChildren().get(0);
        
        VBox sosIndicator = createStatusIndicator("SOS Alerts", "0", ThemeManager.INFO_COLOR);
        sosCountText = (Text) ((VBox) sosIndicator.getChildren().get(0)).getChildren().get(0);
        
        statusIndicators.getChildren().addAll(availableIndicator, responseIndicator, sosIndicator);
        
        header.getChildren().addAll(title, subtitle, statusIndicators);
        return header;
    }
    
    private VBox createStatusIndicator(String label, String count, String color) {
        VBox indicator = new VBox(5);
        indicator.setAlignment(Pos.CENTER);
        indicator.setPadding(new Insets(15));
        indicator.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; " +
                          "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        VBox countBox = new VBox(2);
        countBox.setAlignment(Pos.CENTER);
        
        Text countText = new Text(count);
        countText.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        countText.setFill(Color.web(color));
        
        Text labelText = new Text(label);
        labelText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        labelText.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        countBox.getChildren().addAll(countText, labelText);
        indicator.getChildren().add(countBox);
        
        return indicator;
    }
    
    private HBox createLocationFilter() {
        HBox filterSection = new HBox(15);
        filterSection.setAlignment(Pos.CENTER_LEFT);
        filterSection.setPadding(new Insets(15, 20, 15, 20));
        filterSection.setStyle(ThemeManager.getCardStyle());
        
        Label filterLabel = new Label("Show Emergencies:");
        filterLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        filterLabel.setStyle("-fx-text-fill: " + ThemeManager.PRIMARY_DARK + ";");
          locationFilterCombo = new ComboBox<>();
        locationFilterCombo.getItems().addAll("My Area", "All Areas");
        locationFilterCombo.setValue("All Areas"); // Changed from "My Area" for testing
        locationFilterCombo.setPrefWidth(150);
        locationFilterCombo.setStyle("-fx-font-size: 12px;");
        
        // Add change listener to filter data
        locationFilterCombo.setOnAction(e -> {
            loadEmergencyData();
            loadSOSData();
            updateCounts();
        });
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle(ThemeManager.getButtonPrimaryStyle());
        refreshBtn.setOnAction(e -> {
            loadEmergencyData();
            loadSOSData();
            loadMyResponsesData();
            updateCounts();
        });
        
        filterSection.getChildren().addAll(filterLabel, locationFilterCombo, refreshBtn);
        return filterSection;
    }
    
    private VBox createEmergencyTabs() {
        VBox tabSection = new VBox(15);
        tabSection.setPadding(new Insets(20));
        tabSection.setStyle(ThemeManager.getCardStyle());
        
        // Create TabPane
        TabPane tabPane = new TabPane();
        tabPane.setPrefHeight(400);
        
        // Tab 1: Emergency Requests
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
        tableSection.setPadding(new Insets(15));
        
        // Table header
        Text tableTitle = new Text("Available Emergency Requests");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        tableTitle.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        // Create table
        emergencyTable = new TableView<>();
        emergencyTable.setPrefHeight(320);
        
        // Define columns - same as authority but different actions
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
        locationCol.setPrefWidth(150);
        
        TableColumn<EmergencyRequest, EmergencyRequest.RequestStatus> statusCol = new TableColumn<>("Status");
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
            }
        });
        
        TableColumn<EmergencyRequest, String> volunteerCol = new TableColumn<>("Assigned Volunteers");
        volunteerCol.setCellValueFactory(data -> {
            String volunteerName = data.getValue().getAssignedVolunteer();
            return new SimpleStringProperty(volunteerName != null && !volunteerName.trim().isEmpty() ? volunteerName : "None");
        });
        volunteerCol.setPrefWidth(150);
        
        // Action column with volunteer-specific buttons
        TableColumn<EmergencyRequest, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(column -> {
            return new TableCell<EmergencyRequest, Void>() {
                private final Button viewBtn = new Button("View");
                private final Button respondBtn = new Button("Respond");
                private final Button updateBtn = new Button("Update");
                
                {
                    viewBtn.setStyle("-fx-background-color: " + ThemeManager.INFO_COLOR + "; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 4; " +
                                    "-fx-padding: 5 10; " +
                                    "-fx-font-size: 11px; " +
                                    "-fx-cursor: hand;");
                    
                    respondBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
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
                    
                    // Add event handlers
                    viewBtn.setOnAction(e -> {
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        handleViewEmergency(request);
                    });
                    
                    respondBtn.setOnAction(e -> {
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        handleRespondToEmergency(request);
                    });
                    
                    updateBtn.setOnAction(e -> {
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        handleUpdateEmergencyStatus(request);
                    });
                }
                  @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        // Check if volunteer already responded to this emergency
                        EmergencyRequest request = getTableView().getItems().get(getIndex());
                        boolean alreadyResponded = hasVolunteerRespondedToEmergency(request.getRequestId());
                        boolean isCompleted = request.getStatus() == EmergencyRequest.RequestStatus.COMPLETED;
                        boolean isCancelled = request.getStatus() == EmergencyRequest.RequestStatus.CANCELLED;
                        
                        if (alreadyResponded) {
                            respondBtn.setText("✓ Responded");
                            respondBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                               "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                            respondBtn.setDisable(true);
                        } else {
                            respondBtn.setText("Respond");
                            respondBtn.setDisable(isCompleted || isCancelled);
                            if (isCompleted || isCancelled) {
                                // Grey style for disabled button
                                respondBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                                   "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                            } else {
                                // Green style for enabled "Respond" button
                                respondBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                                                   "-fx-text-fill: white; " +
                                                   "-fx-background-radius: 4; " +
                                                   "-fx-padding: 5 10; " +
                                                   "-fx-font-size: 11px; " +
                                                   "-fx-cursor: hand;");
                            }
                        }
                        
                        // Only allow updates if volunteer has responded
                        updateBtn.setDisable(!alreadyResponded || isCompleted || isCancelled);
                        
                        HBox buttonBox = new HBox(5);
                        buttonBox.setAlignment(Pos.CENTER);
                        buttonBox.getChildren().addAll(viewBtn, respondBtn, updateBtn);
                        setGraphic(buttonBox);
                    }
                }
            };
        });
        
        // Add columns to table
        emergencyTable.getColumns().addAll(idCol, typeCol, priorityCol, locationCol, statusCol, volunteerCol, actionCol);
        
        // Load emergency data
        loadEmergencyData();
        
        tableSection.getChildren().addAll(tableTitle, emergencyTable);
        return tableSection;
    }
    
    private VBox createSOSAlertsContent() {
        VBox tableSection = new VBox(15);
        tableSection.setPadding(new Insets(15));
        
        // Table header
        Text tableTitle = new Text("SOS Alerts");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        tableTitle.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        // Create SOS table
        sosTable = new TableView<>();
        sosTable.setPrefHeight(320);
        
        // Define columns for SOS alerts
        TableColumn<Object[], String> sosIdCol = new TableColumn<>("SOS ID");
        sosIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0].toString()));
        sosIdCol.setPrefWidth(100);
        
        TableColumn<Object[], String> senderCol = new TableColumn<>("Sender");
        senderCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1].toString()));
        senderCol.setPrefWidth(120);
        
        TableColumn<Object[], String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2].toString()));
        typeCol.setPrefWidth(100);
        
        TableColumn<Object[], String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3].toString()));
        locationCol.setPrefWidth(150);
        
        TableColumn<Object[], String> urgencyCol = new TableColumn<>("Urgency");
        urgencyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[4].toString()));
        urgencyCol.setPrefWidth(80);
        
        TableColumn<Object[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[5].toString()));
        statusCol.setPrefWidth(130);
        
        TableColumn<Object[], String> volunteerCol = new TableColumn<>("Assigned Volunteers");
        volunteerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[6] != null ? data.getValue()[6].toString() : "None"));
        volunteerCol.setPrefWidth(150);
        
        // Action column for SOS alerts
        TableColumn<Object[], Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(column -> {
            return new TableCell<Object[], Void>() {
                private final Button viewBtn = new Button("View");
                private final Button respondBtn = new Button("Respond");
                private final Button updateBtn = new Button("Update");
                
                {
                    viewBtn.setStyle("-fx-background-color: " + ThemeManager.INFO_COLOR + "; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 4; " +
                                    "-fx-padding: 5 10; " +
                                    "-fx-font-size: 11px; " +
                                    "-fx-cursor: hand;");
                    
                    respondBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
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
                    
                    // Add event handlers
                    viewBtn.setOnAction(e -> {
                        Object[] alert = getTableRow().getItem();
                        if (alert != null) {
                            handleViewSOSAlert(alert);
                        }
                    });
                    
                    respondBtn.setOnAction(e -> {
                        Object[] alert = getTableRow().getItem();
                        if (alert != null) {
                            handleRespondToSOS(alert);
                        }
                    });
                    
                    updateBtn.setOnAction(e -> {
                        Object[] alert = getTableRow().getItem();
                        if (alert != null) {
                            handleUpdateSOSStatus(alert);
                        }
                    });
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
                            boolean alreadyResponded = hasVolunteerRespondedToSOS(alert[0].toString());
                            boolean isResolved = "RESOLVED".equals(status);
                            
                            if (alreadyResponded) {
                                respondBtn.setText("✓ Responded");
                                respondBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                                   "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                                respondBtn.setDisable(true);
                            } else {
                                respondBtn.setText("Respond");
                                respondBtn.setDisable(isResolved);
                                if (isResolved) {
                                    // Grey style for disabled button
                                    respondBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; " +
                                                       "-fx-background-radius: 4; -fx-padding: 5 10; -fx-font-size: 11px;");
                                } else {
                                    // Green style for enabled "Respond" button
                                    respondBtn.setStyle("-fx-background-color: " + ThemeManager.SUCCESS_COLOR + "; " +
                                                       "-fx-text-fill: white; " +
                                                       "-fx-background-radius: 4; " +
                                                       "-fx-padding: 5 10; " +
                                                       "-fx-font-size: 11px; " +
                                                       "-fx-cursor: hand;");
                                }
                            }
                            
                            updateBtn.setDisable(!alreadyResponded || isResolved);
                            
                            HBox buttonBox = new HBox(5);
                            buttonBox.setAlignment(Pos.CENTER);
                            buttonBox.getChildren().addAll(viewBtn, respondBtn, updateBtn);
                            setGraphic(buttonBox);
                        }
                    }
                }
            };
        });
        
        // Add columns to table
        sosTable.getColumns().addAll(sosIdCol, senderCol, typeCol, locationCol, urgencyCol, statusCol, volunteerCol, actionCol);
        
        // Load SOS data
        loadSOSData();
        
        tableSection.getChildren().addAll(tableTitle, sosTable);
        return tableSection;
    }
    
    private VBox createMyResponsesSection() {
        VBox responseSection = new VBox(15);
        responseSection.setPadding(new Insets(20));
        responseSection.setStyle(ThemeManager.getCardStyle());
        
        // Section header
        Text sectionTitle = new Text("My Responses");
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        sectionTitle.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        Text sectionSubtitle = new Text("Emergencies and SOS alerts I am assigned to");
        sectionSubtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        sectionSubtitle.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        // Create responses table
        myResponsesTable = new TableView<>();
        myResponsesTable.setPrefHeight(250);
        
        // Define columns for my responses
        TableColumn<Object[], String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0].toString()));
        typeCol.setPrefWidth(100);
        
        TableColumn<Object[], String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1].toString()));
        idCol.setPrefWidth(120);
        
        TableColumn<Object[], String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2].toString()));
        descriptionCol.setPrefWidth(200);
        
        TableColumn<Object[], String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3].toString()));
        locationCol.setPrefWidth(150);
        
        TableColumn<Object[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[4].toString()));
        statusCol.setPrefWidth(120);
          TableColumn<Object[], String> assignedAtCol = new TableColumn<>("Assigned Date");
        assignedAtCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[5].toString()));
        assignedAtCol.setPrefWidth(150);
        
        // Action column for my responses
        TableColumn<Object[], Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(column -> {
            return new TableCell<Object[], Void>() {
                private final Button viewBtn = new Button("View");
                private final Button updateBtn = new Button("Update");
                
                {
                    viewBtn.setStyle("-fx-background-color: " + ThemeManager.INFO_COLOR + "; " +
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
                        Object[] response = getTableRow().getItem();
                        if (response != null) {
                            handleViewMyResponse(response);
                        }
                    });
                    
                    updateBtn.setOnAction(e -> {
                        Object[] response = getTableRow().getItem();
                        if (response != null) {
                            handleUpdateMyResponse(response);
                        }
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Object[] response = getTableRow().getItem();
                        if (response != null) {
                            String status = response[4].toString();
                            boolean isCompleted = "COMPLETED".equals(status) || "RESOLVED".equals(status);
                            
                            updateBtn.setDisable(isCompleted);
                            
                            HBox buttonBox = new HBox(5);
                            buttonBox.setAlignment(Pos.CENTER);
                            buttonBox.getChildren().addAll(viewBtn, updateBtn);
                            setGraphic(buttonBox);
                        }
                    }
                }
            };
        });
        
        // Add columns to table
        myResponsesTable.getColumns().addAll(typeCol, idCol, descriptionCol, locationCol, statusCol, assignedAtCol, actionCol);
        
        // Load my responses data
        loadMyResponsesData();
        
        responseSection.getChildren().addAll(sectionTitle, sectionSubtitle, myResponsesTable);
        return responseSection;
    }
      // Data loading methods
    private void loadVolunteerLocation() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT location_name, location FROM users WHERE user_id = ?";
            
            try (ResultSet rs = dbManager.executeQuery(sql, currentVolunteer.getUserId())) {
                if (rs.next()) {
                    String locationName = rs.getString("location_name");
                    String location = rs.getString("location");
                    
                    volunteerLocation = locationName != null ? locationName : location;
                    
                    // Enhanced location parsing for division and district
                    if (volunteerLocation != null) {
                        parseVolunteerLocation(volunteerLocation);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading volunteer location: " + e.getMessage());
        }
    }
      /**
     * Enhanced location parsing that handles various location formats and maps districts to divisions
     */
    private void parseVolunteerLocation(String location) {
        // First try to extract from comma-separated format (District, Division)
        String[] parts = location.split(",");
        if (parts.length >= 2) {
            volunteerDistrict = parts[0].trim();
            volunteerDivision = parts[1].trim();
        } else if (parts.length == 1) {
            String singleLocation = parts[0].trim();
            // Try to determine if it's a district or division
            String[] locationInfo = getLocationInfo(singleLocation);
            volunteerDistrict = locationInfo[0];
            volunteerDivision = locationInfo[1];
        }
        
        // If still not found, try direct mapping
        if (volunteerDivision == null || volunteerDivision.isEmpty()) {
            String[] locationInfo = getLocationInfo(volunteerDistrict);
            if (locationInfo[1] != null) {
                volunteerDivision = locationInfo[1];
            }
        }
        
        System.out.println("Volunteer location parsed - District: " + volunteerDistrict + ", Division: " + volunteerDivision);
    }
    
    /**
     * Maps district names to their divisions and handles various location formats
     * Returns [district, division]
     */
    private String[] getLocationInfo(String locationInput) {
        if (locationInput == null) return new String[]{null, null};
        
        String normalized = locationInput.toLowerCase().trim();
        
        // Dhaka Division districts
        if (normalized.contains("dhaka") || normalized.contains("gazipur") || normalized.contains("narayanganj") ||
            normalized.contains("tangail") || normalized.contains("manikganj") || normalized.contains("munshiganj") ||
            normalized.contains("faridpur") || normalized.contains("gopalganj") || normalized.contains("madaripur") ||
            normalized.contains("rajbari") || normalized.contains("shariatpur") || normalized.contains("kishoreganj") ||
            normalized.contains("netrokona")) {
            return new String[]{locationInput, "Dhaka"};
        }
        
        // Chittagong Division districts
        if (normalized.contains("chittagong") || normalized.contains("chattogram") || normalized.contains("cox") ||
            normalized.contains("feni") || normalized.contains("lakshmipur") || normalized.contains("comilla") ||
            normalized.contains("noakhali") || normalized.contains("brahmanbaria") || normalized.contains("chandpur") ||
            normalized.contains("rangamati") || normalized.contains("bandarban") || normalized.contains("khagrachhari")) {
            return new String[]{locationInput, "Chittagong"};
        }
        
        // Rajshahi Division districts
        if (normalized.contains("rajshahi") || normalized.contains("bogura") || normalized.contains("pabna") ||
            normalized.contains("sirajganj") || normalized.contains("natore") || normalized.contains("joypurhat") ||
            normalized.contains("chapainawabganj") || normalized.contains("naogaon")) {
            return new String[]{locationInput, "Rajshahi"};
        }
        
        // Khulna Division districts
        if (normalized.contains("khulna") || normalized.contains("jessore") || normalized.contains("jashore") ||
            normalized.contains("narail") || normalized.contains("magura") || normalized.contains("jhenaidah") ||
            normalized.contains("bagerhat") || normalized.contains("satkhira") || normalized.contains("kushtia") ||
            normalized.contains("chuadanga") || normalized.contains("meherpur")) {
            return new String[]{locationInput, "Khulna"};
        }
        
        // Sylhet Division districts
        if (normalized.contains("sylhet") || normalized.contains("moulvibazar") || normalized.contains("habiganj") ||
            normalized.contains("sunamganj")) {
            return new String[]{locationInput, "Sylhet"};
        }
        
        // Barisal Division districts
        if (normalized.contains("barisal") || normalized.contains("patuakhali") || normalized.contains("bhola") ||
            normalized.contains("pirojpur") || normalized.contains("jhalokati") || normalized.contains("barguna")) {
            return new String[]{locationInput, "Barisal"};
        }
        
        // Rangpur Division districts
        if (normalized.contains("rangpur") || normalized.contains("dinajpur") || normalized.contains("thakurgaon") ||
            normalized.contains("panchagarh") || normalized.contains("nilphamari") || normalized.contains("lalmonirhat") ||
            normalized.contains("kurigram") || normalized.contains("gaibandha")) {
            return new String[]{locationInput, "Rangpur"};
        }
        
        // Mymensingh Division districts
        if (normalized.contains("mymensingh") || normalized.contains("jamalpur") || normalized.contains("sherpur") ||
            normalized.contains("netrokona")) {
            return new String[]{locationInput, "Mymensingh"};
        }
        
        // If no match found, return as is
        return new String[]{locationInput, null};
    }    private void loadEmergencyData() {
        try {
            if (emergencyTable == null) return;
            
            emergencyTable.getItems().clear();
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Build query based on location filter - exclude tasks completed by current volunteer
            String sql = "SELECT er.*, u.name as volunteer_name " +
                        "FROM emergency_requests er " +
                        "LEFT JOIN users u ON er.assigned_volunteer = u.user_id " +
                        "LEFT JOIN volunteer_assignments va ON er.request_id = va.request_id " +
                        "   AND va.volunteer_id = ? AND va.assignment_type = 'EMERGENCY' AND va.status = 'COMPLETED' " +
                        "WHERE er.status IN ('PENDING', 'ASSIGNED', 'IN_PROGRESS') " +
                        "AND va.assignment_id IS NULL "; // Exclude tasks completed by current volunteer
            
            // Add location filter placeholder (will be replaced in executeLocationFilteredQueryWithVolunteer)
            if ("My Area".equals(locationFilterCombo.getValue()) && volunteerDivision != null) {
                sql += "AND (er.location_name LIKE ? OR er.location_name LIKE ?) ";
            }
            
            sql += "ORDER BY er.priority DESC, er.created_at DESC";
            
            try (ResultSet rs = executeLocationFilteredQueryWithVolunteer(dbManager, sql)) {
                loadEmergencyRequestsFromResultSet(rs);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading emergency data: " + e.getMessage());
            e.printStackTrace();
        }
    }      private void loadSOSData() {
        try {
            if (sosTable == null) return;
            
            System.out.println("=== Loading SOS data for volunteer ===");
            sosTable.getItems().clear();
            DatabaseManager dbManager = DatabaseManager.getInstance();
              String sql = "SELECT sa.sos_id, sa.sender_name, sa.sender_type, sa.location_name, sa.urgency_level, sa.status, sa.assigned_volunteer " +
                        "FROM sos_alerts sa " +
                        "LEFT JOIN volunteer_assignments va ON sa.sos_id = va.request_id " +
                        "   AND va.volunteer_id = ? AND va.assignment_type = 'SOS' AND va.status = 'COMPLETED' " +
                        "WHERE sa.status IN ('PENDING', 'ACTIVE', 'ASSIGNED', 'RESPONDED') " +
                        "AND va.assignment_id IS NULL "; // Exclude SOS completed by current volunteer
            
            // Add location filter placeholder (will be replaced in executeLocationFilteredQueryWithVolunteer)
            if ("My Area".equals(locationFilterCombo.getValue()) && volunteerDivision != null) {
                sql += "AND (sa.location_name LIKE ? OR sa.location_name LIKE ?) ";
                System.out.println("Applying enhanced location filter for division: " + volunteerDivision);
                System.out.println("Districts in division: " + String.join(", ", getDistrictsInDivision(volunteerDivision)));
            } else {
                System.out.println("Loading ALL areas (no location filter)");
            }
            
            sql += "ORDER BY sa.urgency_level DESC, sa.created_at DESC";
            System.out.println("SQL Query: " + sql);
            
            try (ResultSet rs = executeLocationFilteredQueryWithVolunteer(dbManager, sql)) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    Object[] alertData = new Object[7];
                    alertData[0] = rs.getString("sos_id");
                    alertData[1] = rs.getString("sender_name");
                    alertData[2] = rs.getString("sender_type");
                    alertData[3] = rs.getString("location_name");
                    alertData[4] = rs.getString("urgency_level");
                    alertData[5] = rs.getString("status");
                    alertData[6] = rs.getString("assigned_volunteer");
                    
                    System.out.println("Found SOS: " + alertData[0] + " | Status: " + alertData[5] + " | Location: " + alertData[3]);
                    
                    sosTable.getItems().add(alertData);
                }
                System.out.println("Total SOS alerts loaded: " + count);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading SOS data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadMyResponsesData() {
        try {
            if (myResponsesTable == null) return;
            
            myResponsesTable.getItems().clear();
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Load volunteer assignments for current volunteer
            String sql = "SELECT va.assignment_type, va.request_id, " +
                        "CASE " +
                        "  WHEN va.assignment_type = 'EMERGENCY' THEN er.description " +
                        "  WHEN va.assignment_type = 'SOS' THEN sa.description " +
                        "END as description, " +
                        "CASE " +
                        "  WHEN va.assignment_type = 'EMERGENCY' THEN er.location_name " +
                        "  WHEN va.assignment_type = 'SOS' THEN sa.location_name " +
                        "END as location_name, " +
                        "va.status, va.assigned_at " +
                        "FROM volunteer_assignments va " +
                        "LEFT JOIN emergency_requests er ON va.request_id = er.request_id AND va.assignment_type = 'EMERGENCY' " +
                        "LEFT JOIN sos_alerts sa ON va.request_id = sa.sos_id AND va.assignment_type = 'SOS' " +
                        "WHERE va.volunteer_id = ? AND va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED') " +
                        "ORDER BY va.assigned_at DESC";
              try (ResultSet rs = dbManager.executeQuery(sql, currentVolunteer.getUserId())) {
                while (rs.next()) {
                    Object[] responseData = new Object[6];
                    responseData[0] = rs.getString("assignment_type");
                    responseData[1] = rs.getString("request_id");
                    responseData[2] = rs.getString("description");
                    responseData[3] = rs.getString("location_name");
                    responseData[4] = rs.getString("status");
                    
                    // Format assigned_at to show only date (without time)
                    String assignedAtStr = rs.getString("assigned_at");
                    if (assignedAtStr != null && !assignedAtStr.isEmpty()) {
                        try {
                            // Parse the datetime and format to show only date
                            LocalDateTime dateTime = LocalDateTime.parse(assignedAtStr.replace(" ", "T"));
                            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            responseData[5] = dateTime.format(dateFormatter);
                        } catch (Exception e) {
                            // If parsing fails, use original string
                            responseData[5] = assignedAtStr;
                        }
                    } else {
                        responseData[5] = assignedAtStr;
                    }
                    
                    myResponsesTable.getItems().add(responseData);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading my responses data: " + e.getMessage());
        }
    }    private ResultSet executeLocationFilteredQueryWithVolunteer(DatabaseManager dbManager, String sql) throws SQLException {
        if ("My Area".equals(locationFilterCombo.getValue()) && volunteerDivision != null) {
            // Get all districts in the same division for comprehensive matching
            String[] sameAreaDistricts = getDistrictsInDivision(volunteerDivision);
            
            // Build a more comprehensive SQL query that matches any district in the same division
            String enhancedSql = sql.replace(
                "AND (sa.location_name LIKE ? OR sa.location_name LIKE ?)", 
                buildLocationFilterClause("sa.location_name", sameAreaDistricts.length)
            ).replace(
                "AND (er.location_name LIKE ? OR er.location_name LIKE ?)", 
                buildLocationFilterClause("er.location_name", sameAreaDistricts.length)
            );
            
            // Prepare parameters: volunteer_id + all district patterns
            Object[] params = new Object[1 + sameAreaDistricts.length];
            params[0] = currentVolunteer.getUserId();
            for (int i = 0; i < sameAreaDistricts.length; i++) {
                params[i + 1] = "%" + sameAreaDistricts[i] + "%";
            }
            
            return dbManager.executeQuery(enhancedSql, params);
        } else {
            return dbManager.executeQuery(sql, currentVolunteer.getUserId());
        }
    }
    
    /**
     * Builds a SQL WHERE clause for location filtering with multiple district patterns
     */
    private String buildLocationFilterClause(String columnName, int districtCount) {
        StringBuilder clause = new StringBuilder("AND (");
        for (int i = 0; i < districtCount; i++) {
            if (i > 0) clause.append(" OR ");
            clause.append(columnName).append(" LIKE ?");
        }
        clause.append(")");
        return clause.toString();
    }
    
    /**
     * Returns all districts within the same division
     */
    private String[] getDistrictsInDivision(String division) {
        if (division == null) return new String[]{volunteerDistrict != null ? volunteerDistrict : ""};
        
        String divisionLower = division.toLowerCase().trim();
        
        switch (divisionLower) {
            case "dhaka":
                return new String[]{"dhaka", "gazipur", "narayanganj", "tangail", "manikganj", 
                                  "munshiganj", "faridpur", "gopalganj", "madaripur", "rajbari", 
                                  "shariatpur", "kishoreganj", "netrokona"};
                
            case "chittagong":
            case "chattogram":
                return new String[]{"chittagong", "chattogram", "cox", "feni", "lakshmipur", 
                                  "comilla", "noakhali", "brahmanbaria", "chandpur", "rangamati", 
                                  "bandarban", "khagrachhari"};
                
            case "rajshahi":
                return new String[]{"rajshahi", "bogura", "pabna", "sirajganj", "natore", 
                                  "joypurhat", "chapainawabganj", "naogaon"};
                
            case "khulna":
                return new String[]{"khulna", "jessore", "jashore", "narail", "magura", 
                                  "jhenaidah", "bagerhat", "satkhira", "kushtia", "chuadanga", "meherpur"};
                
            case "sylhet":
                return new String[]{"sylhet", "moulvibazar", "habiganj", "sunamganj"};
                
            case "barisal":
                return new String[]{"barisal", "patuakhali", "bhola", "pirojpur", "jhalokati", "barguna"};
                
            case "rangpur":
                return new String[]{"rangpur", "dinajpur", "thakurgaon", "panchagarh", 
                                  "nilphamari", "lalmonirhat", "kurigram", "gaibandha"};
                
            case "mymensingh":
                return new String[]{"mymensingh", "jamalpur", "sherpur", "netrokona"};
                
            default:
                return new String[]{volunteerDistrict != null ? volunteerDistrict : ""};
        }
    }
    
    private void loadEmergencyRequestsFromResultSet(ResultSet rs) throws SQLException {
        while (rs.next()) {
            EmergencyRequest request = new EmergencyRequest();
            request.setRequestId(rs.getString("request_id"));
            request.setRequesterId(rs.getString("requester_id"));
            request.setEmergencyType(EmergencyRequest.EmergencyType.valueOf(rs.getString("emergency_type")));
            request.setPriority(EmergencyRequest.Priority.valueOf(rs.getString("priority")));
            request.setLocationLat(rs.getDouble("location_lat"));
            request.setLocationLng(rs.getDouble("location_lng"));
            request.setLocationName(rs.getString("location_name"));
            request.setDescription(rs.getString("description"));
            request.setPeopleCount(rs.getInt("people_count"));
            request.setStatus(EmergencyRequest.RequestStatus.valueOf(rs.getString("status")));
            request.setAssignedVolunteer(rs.getString("assigned_volunteer"));
            
            // Set created time
            String createdAt = rs.getString("created_at");
            if (createdAt != null) {
                request.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            }
            
            emergencyTable.getItems().add(request);
        }
    }
    
    // Helper methods
    private boolean hasVolunteerRespondedToEmergency(String requestId) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT COUNT(*) as count FROM volunteer_assignments " +
                        "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'EMERGENCY' " +
                        "AND status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED')";
            
            try (ResultSet rs = dbManager.executeQuery(sql, currentVolunteer.getUserId(), requestId)) {
                return rs.next() && rs.getInt("count") > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean hasVolunteerRespondedToSOS(String sosId) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT COUNT(*) as count FROM volunteer_assignments " +
                        "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'SOS' " +
                        "AND status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED')";
            
            try (ResultSet rs = dbManager.executeQuery(sql, currentVolunteer.getUserId(), sosId)) {
                return rs.next() && rs.getInt("count") > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private void updateCounts() {
        availableEmergencies = emergencyTable != null ? emergencyTable.getItems().size() : 0;
        totalSOSAlerts = sosTable != null ? sosTable.getItems().size() : 0;
        myActiveResponses = myResponsesTable != null ? (int) myResponsesTable.getItems().stream()
            .filter(response -> {
                String status = response[4].toString();
                return !"COMPLETED".equals(status) && !"RESOLVED".equals(status);
            }).count() : 0;
        
        if (availableCountText != null) availableCountText.setText(String.valueOf(availableEmergencies));
        if (sosCountText != null) sosCountText.setText(String.valueOf(totalSOSAlerts));
        if (myResponsesCountText != null) myResponsesCountText.setText(String.valueOf(myActiveResponses));
    }
    
    // Event handlers - implement volunteer-specific actions
    private void handleViewEmergency(EmergencyRequest request) {
        // Same view dialog as authority but read-only for volunteers
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
        
        int row = 0;
        addDetailRow(detailsGrid, row++, "Request ID:", request.getRequestId());
        addDetailRow(detailsGrid, row++, "Emergency Type:", request.getEmergencyType().getDisplayName());
        addDetailRow(detailsGrid, row++, "Priority:", request.getPriority().getDisplayName());
        addDetailRow(detailsGrid, row++, "Location:", request.getLocationName());
        
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            addDetailRow(detailsGrid, row++, "Description:", request.getDescription());
        }
        
        addDetailRow(detailsGrid, row++, "People Affected:", String.valueOf(request.getPeopleCount()));
        addDetailRow(detailsGrid, row++, "Status:", request.getStatus().getDisplayName());
        addDetailRow(detailsGrid, row++, "Reported:", request.getTimeElapsed());
        
        if (request.getAssignedVolunteer() != null && !request.getAssignedVolunteer().trim().isEmpty()) {
            addDetailRow(detailsGrid, row++, "Assigned Volunteers:", request.getAssignedVolunteer());
        }
        
        content.getChildren().add(detailsGrid);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    private void handleRespondToEmergency(EmergencyRequest request) {
        // Create volunteer assignment and update emergency status
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Check if already responded
            if (hasVolunteerRespondedToEmergency(request.getRequestId())) {
                showAlert(Alert.AlertType.WARNING, "Already Responded", 
                         "You have already responded to this emergency.");
                return;
            }
            
            // Create assignment record
            String assignmentId = "VOLUNTEER_RESPONSE_" + System.currentTimeMillis();
            String insertQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, " +
                               "assignment_type, status, assigned_at) VALUES (?, ?, ?, 'EMERGENCY', 'ASSIGNED', datetime('now'))";
            
            dbManager.executeUpdate(insertQuery, assignmentId, currentVolunteer.getUserId(), request.getRequestId());
            
            // Update emergency assigned volunteer field
            String getExistingQuery = "SELECT assigned_volunteer FROM emergency_requests WHERE request_id = ?";
            String currentAssigned = null;
            
            try (ResultSet rs = dbManager.executeQuery(getExistingQuery, request.getRequestId())) {
                if (rs.next()) {
                    currentAssigned = rs.getString("assigned_volunteer");
                }
            }
            
            String newAssignedValue;
            if (currentAssigned == null || currentAssigned.trim().isEmpty() || "None".equals(currentAssigned)) {
                newAssignedValue = currentVolunteer.getName();
            } else if (!currentAssigned.contains(currentVolunteer.getName())) {
                newAssignedValue = currentAssigned + ", " + currentVolunteer.getName();
            } else {
                newAssignedValue = currentAssigned; // Already in the list
            }
            
            String updateEmergencyQuery = "UPDATE emergency_requests SET status = 'ASSIGNED', assigned_volunteer = ?, " +
                                        "updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
            dbManager.executeUpdate(updateEmergencyQuery, newAssignedValue, request.getRequestId());
            
            showAlert(Alert.AlertType.INFORMATION, "Response Successful", 
                     "You have successfully responded to this emergency. It has been added to your assignments.");
            
            // Refresh data
            loadEmergencyData();
            loadMyResponsesData();
            updateCounts();
            
        } catch (Exception e) {
            System.err.println("Error responding to emergency: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Response Failed", 
                     "Failed to respond to emergency: " + e.getMessage());
        }
    }
    
    private void handleUpdateEmergencyStatus(EmergencyRequest request) {
        // Show dialog to update status (limited options for volunteers)
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Emergency Status");
        dialog.setHeaderText("Update status for: " + request.getRequestId());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label statusLabel = new Label("New Status:");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ComboBox<EmergencyRequest.RequestStatus> statusCombo = new ComboBox<>();
        // Volunteers can only mark as IN_PROGRESS or COMPLETED
        statusCombo.getItems().addAll(
            EmergencyRequest.RequestStatus.IN_PROGRESS,
            EmergencyRequest.RequestStatus.COMPLETED
        );
        statusCombo.setValue(request.getStatus());
        
        Label notesLabel = new Label("Notes (Optional):");
        notesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Add any notes about your response...");
        
        content.getChildren().addAll(statusLabel, statusCombo, notesLabel, notesArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return statusCombo.getValue().name();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            updateEmergencyStatusInDatabase(request.getRequestId(), 
                                          EmergencyRequest.RequestStatus.valueOf(newStatus), 
                                          notesArea.getText());
        });
    }
    
    // Similar methods for SOS alerts
    private void handleViewSOSAlert(Object[] alert) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("SOS Alert Details");
        dialog.setHeaderText("SOS Alert: " + alert[0].toString());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        content.getChildren().add(new Label("SOS ID: " + alert[0].toString()));
        content.getChildren().add(new Label("Sender: " + alert[1].toString() + " (" + alert[2].toString() + ")"));
        content.getChildren().add(new Label("Location: " + alert[3].toString()));
        content.getChildren().add(new Label("Urgency Level: " + alert[4].toString()));
        content.getChildren().add(new Label("Status: " + alert[5].toString()));
        content.getChildren().add(new Label("Assigned Volunteers: " + (alert[6] != null ? alert[6].toString() : "None")));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    private void handleRespondToSOS(Object[] alert) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sosId = alert[0].toString();
            
            // Check if already responded
            if (hasVolunteerRespondedToSOS(sosId)) {
                showAlert(Alert.AlertType.WARNING, "Already Responded", 
                         "You have already responded to this SOS alert.");
                return;
            }
            
            // Create assignment record
            String assignmentId = "SOS_VOLUNTEER_RESPONSE_" + System.currentTimeMillis();
            String insertQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, " +
                               "assignment_type, status, assigned_at) VALUES (?, ?, ?, 'SOS', 'ASSIGNED', datetime('now'))";
            
            dbManager.executeUpdate(insertQuery, assignmentId, currentVolunteer.getUserId(), sosId);
            
            // Update SOS assigned volunteer field
            String getExistingQuery = "SELECT assigned_volunteer FROM sos_alerts WHERE sos_id = ?";
            String currentAssigned = null;
            
            try (ResultSet rs = dbManager.executeQuery(getExistingQuery, sosId)) {
                if (rs.next()) {
                    currentAssigned = rs.getString("assigned_volunteer");
                }
            }
            
            String newAssignedValue;
            if (currentAssigned == null || currentAssigned.trim().isEmpty() || "None".equals(currentAssigned)) {
                newAssignedValue = currentVolunteer.getName();
            } else if (!currentAssigned.contains(currentVolunteer.getName())) {
                newAssignedValue = currentAssigned + ", " + currentVolunteer.getName();
            } else {
                newAssignedValue = currentAssigned;
            }
            
            String updateSOSQuery = "UPDATE sos_alerts SET status = 'ASSIGNED', assigned_volunteer = ?, " +
                                  "updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
            dbManager.executeUpdate(updateSOSQuery, newAssignedValue, sosId);
            
            showAlert(Alert.AlertType.INFORMATION, "Response Successful", 
                     "You have successfully responded to this SOS alert.");
            
            // Refresh data
            loadSOSData();
            loadMyResponsesData();
            updateCounts();
            
        } catch (Exception e) {
            System.err.println("Error responding to SOS: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Response Failed", 
                     "Failed to respond to SOS alert: " + e.getMessage());
        }
    }
    
    private void handleUpdateSOSStatus(Object[] alert) {
        // Similar to emergency status update but for SOS
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update SOS Status");
        dialog.setHeaderText("Update status for SOS: " + alert[0].toString());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label statusLabel = new Label("New Status:");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("RESPONDED", "RESOLVED");
        statusCombo.setValue(alert[5].toString());
        
        content.getChildren().addAll(statusLabel, statusCombo);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return statusCombo.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            updateSOSStatusInDatabase(alert[0].toString(), newStatus);
        });
    }
    
    private void handleViewMyResponse(Object[] response) {
        // View details of volunteer's own response
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("My Response Details");
        dialog.setHeaderText(response[0].toString() + " Response: " + response[1].toString());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        content.getChildren().add(new Label("Type: " + response[0].toString()));
        content.getChildren().add(new Label("ID: " + response[1].toString()));
        content.getChildren().add(new Label("Description: " + response[2].toString()));
        content.getChildren().add(new Label("Location: " + response[3].toString()));
        content.getChildren().add(new Label("Status: " + response[4].toString()));
        content.getChildren().add(new Label("Assigned At: " + response[5].toString()));
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    private void handleUpdateMyResponse(Object[] response) {
        // Update status of volunteer's own response
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update My Response");
        dialog.setHeaderText("Update your response status");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label statusLabel = new Label("New Status:");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("ACCEPTED", "IN_PROGRESS", "COMPLETED");
        statusCombo.setValue(response[4].toString());
        
        content.getChildren().addAll(statusLabel, statusCombo);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return statusCombo.getValue();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(newStatus -> {
            updateMyResponseStatus(response[0].toString(), response[1].toString(), newStatus);
        });    }
      // Database update methods
    private void updateEmergencyStatusInDatabase(String requestId, EmergencyRequest.RequestStatus newStatus, String notes) {
        try {
            System.out.println("=== DEBUG: updateEmergencyStatusInDatabase called ===");
            System.out.println("Request ID: " + requestId);
            System.out.println("New Status: " + newStatus);
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Update volunteer assignment status
            String updateAssignmentSql = "UPDATE volunteer_assignments SET status = ? " +
                                        "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'EMERGENCY'";
            String assignmentStatus = mapEmergencyStatusToAssignmentStatus(newStatus);
            System.out.println("Updating volunteer_assignments with status: " + assignmentStatus);
            int vaRows = dbManager.executeUpdate(updateAssignmentSql, assignmentStatus, currentVolunteer.getUserId(), requestId);
            System.out.println("Volunteer assignment rows updated: " + vaRows);
            
            // If completed, remove this volunteer from assigned_volunteer field AND update emergency status
            if (newStatus == EmergencyRequest.RequestStatus.COMPLETED) {
                System.out.println("Status is COMPLETED - updating emergency_requests");
                // First update the emergency request status to COMPLETED
                String updateSql = "UPDATE emergency_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
                int erRows = dbManager.executeUpdate(updateSql, newStatus.name(), requestId);
                System.out.println("Emergency request rows updated: " + erRows);
                // Then remove volunteer from assigned field
                System.out.println("Removing volunteer from assigned field");
                removeVolunteerFromAssignedField(requestId, "EMERGENCY");
            } else {
                System.out.println("Status is " + newStatus + " - updating emergency_requests normally");
                // For other statuses, update emergency request status normally
                String updateSql = "UPDATE emergency_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
                int erRows = dbManager.executeUpdate(updateSql, newStatus.name(), requestId);
                System.out.println("Emergency request rows updated: " + erRows);
            }
            
            // Add notes if provided
            if (notes != null && !notes.trim().isEmpty()) {
                String notesSql = "INSERT INTO emergency_notes (request_id, note, created_by, created_at) VALUES (?, ?, ?, datetime('now'))";
                dbManager.executeUpdate(notesSql, requestId, notes.trim(), currentVolunteer.getUserId());
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Status Updated", 
                     "Emergency status updated successfully.");
            
            // Refresh data
            loadEmergencyData();
            loadMyResponsesData();
            updateCounts();
            
        } catch (Exception e) {
            System.err.println("Error updating emergency status: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Update Failed", 
                     "Failed to update status: " + e.getMessage());
        }
    }
      private void updateSOSStatusInDatabase(String sosId, String newStatus) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Update volunteer assignment status
            String updateAssignmentSql = "UPDATE volunteer_assignments SET status = ? " +
                                        "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'SOS'";
            String assignmentStatus = mapSOSStatusToAssignmentStatus(newStatus);
            dbManager.executeUpdate(updateAssignmentSql, assignmentStatus, currentVolunteer.getUserId(), sosId);
              // If resolved (completed), remove this volunteer from assigned_volunteer field AND update SOS status
            if ("RESOLVED".equals(newStatus)) {
                removeVolunteerFromAssignedField(sosId, "SOS");
                // Also update the SOS alert status to RESOLVED
                String updateSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                dbManager.executeUpdate(updateSql, newStatus, sosId);
            } else {
                // For other statuses, update SOS status normally
                String updateSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                dbManager.executeUpdate(updateSql, newStatus, sosId);
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Status Updated", 
                     "SOS alert status updated successfully.");
            
            // Refresh data
            loadSOSData();
            loadMyResponsesData();
            updateCounts();
            
        } catch (Exception e) {
            System.err.println("Error updating SOS status: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Update Failed", 
                     "Failed to update status: " + e.getMessage());        }
    }
    
    /**
     * Removes the current volunteer from the assigned_volunteer field when they complete a task
     */
    private void removeVolunteerFromAssignedField(String requestId, String assignmentType) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String tableName = "EMERGENCY".equals(assignmentType) ? "emergency_requests" : "sos_alerts";
            String idColumn = "EMERGENCY".equals(assignmentType) ? "request_id" : "sos_id";
            
            // Get current assigned volunteers
            String selectSql = "SELECT assigned_volunteer FROM " + tableName + " WHERE " + idColumn + " = ?";
            String currentAssigned = null;
            
            try (ResultSet rs = dbManager.executeQuery(selectSql, requestId)) {
                if (rs.next()) {
                    currentAssigned = rs.getString("assigned_volunteer");
                }
            }
            
            if (currentAssigned != null && !currentAssigned.trim().isEmpty()) {
                // Remove current volunteer from the list
                String[] volunteers = currentAssigned.split(",");
                StringBuilder newAssigned = new StringBuilder();
                
                for (String volunteer : volunteers) {
                    String volunteerName = volunteer.trim();
                    if (!volunteerName.equals(currentVolunteer.getName())) {
                        if (newAssigned.length() > 0) {
                            newAssigned.append(", ");
                        }
                        newAssigned.append(volunteerName);
                    }
                }
                
                // Update the assigned_volunteer field
                String newAssignedValue = newAssigned.toString();
                String updateSql;
                  if (newAssignedValue.isEmpty()) {
                    // If no volunteers left, clear assigned_volunteer but don't change status
                    // (Status should remain as set by the completion process)
                    updateSql = "UPDATE " + tableName + " SET assigned_volunteer = NULL, updated_at = CURRENT_TIMESTAMP WHERE " + idColumn + " = ?";
                    dbManager.executeUpdate(updateSql, requestId);
                } else {
                    // Keep other volunteers assigned
                    updateSql = "UPDATE " + tableName + " SET assigned_volunteer = ?, updated_at = CURRENT_TIMESTAMP WHERE " + idColumn + " = ?";
                    dbManager.executeUpdate(updateSql, newAssignedValue, requestId);
                }
                
                System.out.println("Removed volunteer " + currentVolunteer.getName() + " from " + assignmentType + " " + requestId);
            }
            
        } catch (Exception e) {
            System.err.println("Error removing volunteer from assigned field: " + e.getMessage());
        }
    }    private void updateMyResponseStatus(String assignmentType, String requestId, String newStatus) {
        try {
            System.out.println("=== DEBUG: updateMyResponseStatus called ===");
            System.out.println("Assignment Type: " + assignmentType);
            System.out.println("Request ID: " + requestId);
            System.out.println("New Status: " + newStatus);
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Update volunteer assignment status
            String updateSql = "UPDATE volunteer_assignments SET status = ? " +
                             "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = ?";
            
            // If marking as completed, also update completed_at timestamp
            if ("COMPLETED".equals(newStatus)) {
                updateSql = "UPDATE volunteer_assignments SET status = ?, completed_at = CURRENT_TIMESTAMP " +
                           "WHERE volunteer_id = ? AND request_id = ? AND assignment_type = ?";
            }
            
            int vaRows = dbManager.executeUpdate(updateSql, newStatus, currentVolunteer.getUserId(), requestId, assignmentType);
            System.out.println("Volunteer assignment rows updated: " + vaRows);
            
            // ALSO UPDATE THE MAIN TABLE (emergency_requests or sos_alerts)
            if ("EMERGENCY".equals(assignmentType)) {
                if ("COMPLETED".equals(newStatus)) {
                    System.out.println("Updating emergency_requests to COMPLETED");
                    String updateErSql = "UPDATE emergency_requests SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
                    int erRows = dbManager.executeUpdate(updateErSql, requestId);
                    System.out.println("Emergency request rows updated: " + erRows);
                    
                    // Clear assigned volunteer field
                    System.out.println("Clearing assigned volunteer field");
                    removeVolunteerFromAssignedField(requestId, "EMERGENCY");                } else {
                    // For other statuses, map appropriately
                    String mappedStatus = "IN_PROGRESS".equals(newStatus) ? "IN_PROGRESS" : "ASSIGNED";
                    System.out.println("Updating emergency_requests to: " + mappedStatus);
                    String updateErSql = "UPDATE emergency_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE request_id = ?";
                    int erRows = dbManager.executeUpdate(updateErSql, mappedStatus, requestId);
                    System.out.println("Emergency request rows updated: " + erRows);
                }
            } else if ("SOS".equals(assignmentType)) {
                if ("COMPLETED".equals(newStatus)) {
                    System.out.println("Updating sos_alerts to RESOLVED");
                    String updateSosSql = "UPDATE sos_alerts SET status = 'RESOLVED', updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                    int sosRows = dbManager.executeUpdate(updateSosSql, requestId);
                    System.out.println("SOS alert rows updated: " + sosRows);
                    
                    // Clear assigned volunteer field
                    System.out.println("Clearing assigned volunteer field for SOS");
                    removeVolunteerFromAssignedField(requestId, "SOS");                } else {
                    // For other statuses, map appropriately  
                    String mappedStatus = "IN_PROGRESS".equals(newStatus) ? "RESPONDED" : "ACTIVE";
                    System.out.println("Updating sos_alerts to: " + mappedStatus);
                    String updateSosSql = "UPDATE sos_alerts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE sos_id = ?";
                    int sosRows = dbManager.executeUpdate(updateSosSql, mappedStatus, requestId);
                    System.out.println("SOS alert rows updated: " + sosRows);
                }
            }
            
            showAlert(Alert.AlertType.INFORMATION, "Response Updated", 
                     "Your response status has been updated successfully.");
            
            // Refresh data
            loadMyResponsesData();
            updateCounts();
            
            System.out.println("=== DEBUG: updateMyResponseStatus completed ===");
            
        } catch (Exception e) {
            System.err.println("=== ERROR in updateMyResponseStatus ===");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Update Failed", 
                     "Failed to update response status: " + e.getMessage());
        }
    }
    
    // Helper methods
    private String mapEmergencyStatusToAssignmentStatus(EmergencyRequest.RequestStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "IN_PROGRESS";
            case COMPLETED -> "COMPLETED";
            default -> "ASSIGNED";
        };
    }
    
    private String mapSOSStatusToAssignmentStatus(String status) {
        return switch (status) {
            case "RESPONDED" -> "IN_PROGRESS";
            case "RESOLVED" -> "COMPLETED";
            default -> "ASSIGNED";
        };
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label labelControl = new Label(label);
        labelControl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        labelControl.setStyle("-fx-text-fill: #495057;");
        
        Label valueControl = new Label(value);
        valueControl.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        valueControl.setStyle("-fx-text-fill: #6c757d;");
        valueControl.setWrapText(true);
        
        grid.add(labelControl, 0, row);
        grid.add(valueControl, 1, row);
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Public method to refresh all data (called from MainController)
    public void refreshData() {
        loadEmergencyData();
        loadSOSData();
        loadMyResponsesData();
        updateCounts();
    }
}
