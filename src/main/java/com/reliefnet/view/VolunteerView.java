package com.reliefnet.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.reliefnet.model.User;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.util.DataSyncManager;
import com.reliefnet.util.LocationHierarchy;
import com.reliefnet.database.DatabaseManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * VolunteerView - Manages volunteer registration, tracking, and assignments
 */
public class VolunteerView implements DataSyncManager.DataChangeListener {
      private VBox mainContainer;
    private TableView<User> approvedVolunteersTable;
    private TableView<User> pendingVolunteersTable;
    private ObservableList<User> approvedVolunteersList;
    private ObservableList<User> pendingVolunteersList;    private Label totalVolunteersLabel;
    private Label activeVolunteersLabel;
    // Removed assignmentsLabel as Active Assignments card was removed
    private ComboBox<String> emergencyDropdown;
    private TextField locationField;
    private ComboBox<String> requiredSkillDropdown;
    
    // Track Assignment fields
    private TableView<VolunteerAssignment> assignmentTable;
    private Label assignmentSummaryLabel;
      // New fields for charts    private PieChart skillChart;
    
    // New fields for control panel
    private TextField searchField;
    private ComboBox<String> skillFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<String> locationFilter;
      // Flag to track if location map has been printed (for debugging)
    private boolean mapPrintedFlag = false;
      
    public VBox createView() {
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f8f9fa;");
        
        // Header section
        VBox header = createHeader();
        
        // Metrics cards (only 3 cards now)
        HBox metricsRow = createMetricsCards();
          // Add Volunteer button
        HBox addButtonRow = createAddVolunteerSection();
        
        // Approved volunteers table (from authority)
        VBox approvedTableSection = createApprovedVolunteersTable();
        
        // Pending volunteers table (needs approval)
        VBox pendingTableSection = createPendingVolunteersTable();
        
        // Quick assignment center
        VBox assignmentPanel = createAssignmentPanel();
        
        mainContainer.getChildren().addAll(header, metricsRow, addButtonRow, approvedTableSection, pendingTableSection, assignmentPanel);
        
        // Load initial data
        loadVolunteerData();
        updateMetrics();        loadEmergencyDropdown();
        // Location field is auto-filled based on emergency selection
        
        // Register with DataSyncManager for real-time updates
        DataSyncManager.getInstance().addListener(this);
        
        return mainContainer;
    }
      private VBox createHeader() {
        VBox header = new VBox(10);
        
        Label titleLabel = new Label("Volunteer Management");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1e3c72"));
        
        Label subtitleLabel = new Label("Manage volunteers and emergency assignments");
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#666666"));
        
        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }
      private HBox createMetricsCards() {
        HBox metricsRow = new HBox(20);
        metricsRow.setAlignment(Pos.CENTER);
        
        // Total Volunteers Card
        VBox totalCard = createMetricCard("Total Volunteers", "0", "registered", "#3498db");
        totalVolunteersLabel = (Label) ((VBox) totalCard.getChildren().get(1)).getChildren().get(0);
        
        // Active Volunteers Card  
        VBox activeCard = createMetricCard("Active Volunteers", "0", "available", "#27ae60");
        activeVolunteersLabel = (Label) ((VBox) activeCard.getChildren().get(1)).getChildren().get(0);
        
        // Removed Active Assignments card as requested
        
        metricsRow.getChildren().addAll(totalCard, activeCard);
        return metricsRow;
    }
    
    private VBox createMetricCard(String title, String value, String subtitle, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setPrefHeight(120);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", 12));
        titleLabel.setTextFill(Color.web("#666666"));
        
        VBox valueContainer = new VBox(2);
        valueContainer.setAlignment(Pos.CENTER);
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        valueLabel.setTextFill(Color.web(color));
        
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Segoe UI", 10));
        subtitleLabel.setTextFill(Color.web("#999999"));
        
        valueContainer.getChildren().addAll(valueLabel, subtitleLabel);
        card.getChildren().addAll(titleLabel, valueContainer);
        
        return card;
    }
    
    private HBox createControlPanel() {
        HBox controlPanel = new HBox(15);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        controlPanel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Search volunteers...");
        searchField.setPrefWidth(250);
        ThemeManager.styleTextField(searchField);
        
        // Skill filter
        skillFilter = new ComboBox<>();
        skillFilter.getItems().addAll("All Skills", "Medical", "Rescue", "Logistics", "Communication", "IT Support");
        skillFilter.setValue("All Skills");
        skillFilter.setPrefWidth(150);
        ThemeManager.styleComboBox(skillFilter);
        
        // Status filter
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Status", "Available", "Assigned", "On Mission", "Offline");
        statusFilter.setValue("All Status");
        statusFilter.setPrefWidth(120);
        ThemeManager.styleComboBox(statusFilter);
        
        // Location filter
        locationFilter = new ComboBox<>();
        locationFilter.getItems().addAll("All Locations", "Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal");
        locationFilter.setValue("All Locations");
        locationFilter.setPrefWidth(150);
        ThemeManager.styleComboBox(locationFilter);
        
        // Action buttons
        Button addButton = new Button("Add Volunteer");
        ThemeManager.stylePrimaryButton(addButton);
        
        Button assignButton = new Button("Bulk Assign");
        ThemeManager.styleSecondaryButton(assignButton);
        
        Button notifyButton = new Button("Send Alert");
        notifyButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        controlPanel.getChildren().addAll(
            new Label("Search:"), searchField,
            new Label("Skill:"), skillFilter,
            new Label("Status:"), statusFilter,
            new Label("Location:"), locationFilter,
            new Region(), addButton, assignButton, notifyButton
        );
        
        HBox.setHgrow(controlPanel.getChildren().get(controlPanel.getChildren().size() - 4), Priority.ALWAYS);
        
        return controlPanel;
    }
      private HBox createAddVolunteerSection() {
        HBox section = new HBox(15);
        section.setPadding(new Insets(15));
        section.setAlignment(Pos.CENTER_LEFT);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Button addButton = new Button("Add Volunteer");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 6; " +
                          "-fx-padding: 10 20; -fx-font-size: 14; -fx-font-weight: bold;");
        addButton.setOnAction(e -> showAddVolunteerDialog());
        
        Label infoLabel = new Label("Add new volunteers to the system");
        infoLabel.setFont(Font.font("Segoe UI", 12));
        infoLabel.setTextFill(Color.web("#666666"));
        
        section.getChildren().addAll(addButton, infoLabel);
        return section;
    }private VBox createApprovedVolunteersTable() {
        VBox container = new VBox(10);
        
        Label tableTitle = new Label("Volunteers (From Authority)");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        tableTitle.setTextFill(Color.web("#1e3c72"));
        
        // Initialize table and list
        approvedVolunteersList = FXCollections.observableArrayList();
        approvedVolunteersTable = new TableView<>(approvedVolunteersList);
        approvedVolunteersTable.setPrefHeight(250);
        approvedVolunteersTable.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
          // Create columns
        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(140);
        
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        emailCol.setPrefWidth(150);
        
        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        // Status column with colored indicators
        statusCol.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color;
                    switch (item) {
                        case "ACTIVE":
                            color = "#27ae60";
                            break;
                        case "ASSIGNED":
                            color = "#f39c12";
                            break;
                        case "INACTIVE":
                            color = "#95a5a6";
                            break;
                        default:
                            color = "#666666";
                            break;
                    }
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
        
        TableColumn<User, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        locationCol.setPrefWidth(110);
        
        TableColumn<User, String> contactCol = new TableColumn<>("Contact");
        contactCol.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());
        contactCol.setPrefWidth(110);        TableColumn<User, String> expertiseCol = new TableColumn<>("Expertise");
        expertiseCol.setCellValueFactory(cellData -> cellData.getValue().skillsProperty());
        expertiseCol.setPrefWidth(120);        // Actions column with Edit button for authority
        TableColumn<User, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(80);
        actionsCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button editBtn = new Button("Edit");
            
            {
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4; " +
                               "-fx-padding: 4 8; -fx-font-size: 10; -fx-font-weight: bold;");
                editBtn.setOnAction(e -> {
                    User volunteer = getTableView().getItems().get(getIndex());
                    showEditVolunteerDialog(volunteer, true); // true = from authority table
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                }
            }
        });
        
        approvedVolunteersTable.getColumns().add(nameCol);
        approvedVolunteersTable.getColumns().add(emailCol);
        approvedVolunteersTable.getColumns().add(statusCol);
        approvedVolunteersTable.getColumns().add(locationCol);
        approvedVolunteersTable.getColumns().add(contactCol);
        approvedVolunteersTable.getColumns().add(expertiseCol);
        approvedVolunteersTable.getColumns().add(actionsCol);
        
        container.getChildren().addAll(tableTitle, approvedVolunteersTable);
        return container;
    }

    private VBox createPendingVolunteersTable() {
        VBox container = new VBox(10);
        
        Label tableTitle = new Label("Volunteers (Needs Approval)");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        tableTitle.setTextFill(Color.web("#e74c3c"));
        
        // Initialize table and list
        pendingVolunteersList = FXCollections.observableArrayList();
        pendingVolunteersTable = new TableView<>(pendingVolunteersList);
        pendingVolunteersTable.setPrefHeight(250);
        pendingVolunteersTable.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        
        // Create columns
        TableColumn<User, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(150);
        
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        emailCol.setPrefWidth(150);
        
        TableColumn<User, String> contactCol = new TableColumn<>("Contact");
        contactCol.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());
        contactCol.setPrefWidth(120);
        
        TableColumn<User, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        locationCol.setPrefWidth(120);
        
        TableColumn<User, String> expertiseCol = new TableColumn<>("Expertise");
        expertiseCol.setCellValueFactory(cellData -> cellData.getValue().skillsProperty());
        expertiseCol.setPrefWidth(150);        // Actions column - Authority only sees Approve/Reject buttons
        TableColumn<User, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final HBox actionBox = new HBox(5);
            
            {
                approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 10;");
                rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 10;");
                actionBox.setAlignment(Pos.CENTER);
                
                approveBtn.setOnAction(e -> {
                    User volunteer = getTableView().getItems().get(getIndex());
                    approveVolunteer(volunteer);
                });
                
                rejectBtn.setOnAction(e -> {
                    User volunteer = getTableView().getItems().get(getIndex());
                    rejectVolunteer(volunteer);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User volunteer = getTableView().getItems().get(getIndex());
                    actionBox.getChildren().clear();
                    
                    boolean hasLocation = volunteer.getLocation() != null && !volunteer.getLocation().trim().isEmpty();
                    
                    // Approve button - enabled only if location is set
                    if (hasLocation) {
                        approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 10;");
                        approveBtn.setDisable(false);
                    } else {
                        approveBtn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 10;");
                        approveBtn.setDisable(true);
                    }
                    
                    actionBox.getChildren().addAll(approveBtn, rejectBtn);
                    setGraphic(actionBox);
                }
            }
        });
        
        pendingVolunteersTable.getColumns().add(nameCol);
        pendingVolunteersTable.getColumns().add(emailCol);
        pendingVolunteersTable.getColumns().add(contactCol);
        pendingVolunteersTable.getColumns().add(locationCol);
        pendingVolunteersTable.getColumns().add(expertiseCol);
        pendingVolunteersTable.getColumns().add(actionsCol);
          // Add row factory to grey out volunteers without location
        pendingVolunteersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<User>() {
                @Override
                protected void updateItem(User item, boolean empty) {
                    super.updateItem(item, empty);
                    updateRowStyle();
                }
                
                private void updateRowStyle() {
                    User volunteer = getItem();
                    if (volunteer != null && !isEmpty()) {
                        boolean hasLocation = volunteer.getLocation() != null && !volunteer.getLocation().trim().isEmpty();
                        // Only apply no-location styling if row is not selected
                        if (!hasLocation && !isSelected()) {
                            setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #999999; -fx-opacity: 0.7;");
                        } else {
                            // Clear custom styling to allow default selection styling
                            setStyle("");
                        }
                    } else {
                        setStyle("");
                    }
                }
                
                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    // Update styling when selection changes
                    updateRowStyle();
                }
            };
            
            // Also add listener for item changes
            row.itemProperty().addListener((obs, oldVolunteer, newVolunteer) -> {
                if (row.getItem() != null) {
                    boolean hasLocation = row.getItem().getLocation() != null && !row.getItem().getLocation().trim().isEmpty();
                    // Only apply no-location styling if row is not selected
                    if (!hasLocation && !row.isSelected()) {
                        row.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #999999; -fx-opacity: 0.7;");
                    } else {
                        // Clear custom styling to allow default selection styling
                        row.setStyle("");
                    }
                }
            });
            
            return row;
        });

        container.getChildren().addAll(tableTitle, pendingVolunteersTable);
        return container;
    }
    
    private VBox createChartsContainer() {
        VBox container = new VBox(15);
        container.setPrefWidth(400);
        
        // Skill distribution chart
        VBox skillChartContainer = new VBox(10);
        skillChartContainer.setPadding(new Insets(15));
        skillChartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                                   "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label skillChartTitle = new Label("Volunteer Skills Distribution");
        skillChartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        skillChartTitle.setTextFill(Color.web("#1e3c72"));
        
        skillChartContainer.getChildren().addAll(skillChartTitle);
        
        // Performance chart
        VBox performanceChartContainer = new VBox(10);
        performanceChartContainer.setPadding(new Insets(15));
        performanceChartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                                         "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label performanceChartTitle = new Label("Monthly Performance");
        performanceChartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        performanceChartTitle.setTextFill(Color.web("#1e3c72"));
        
        performanceChartContainer.getChildren().addAll(performanceChartTitle);
        
        container.getChildren().addAll(skillChartContainer, performanceChartContainer);
        return container;
    }
    
    private VBox createAssignmentPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label panelTitle = new Label("Quick Assignment Center");
        panelTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        panelTitle.setTextFill(Color.web("#1e3c72"));
        
        Label subtitle = new Label("Assign volunteers to emergency situations");
        subtitle.setFont(Font.font("Segoe UI", 12));
        subtitle.setTextFill(Color.web("#666666"));
        
        // Assignment controls - split into two lines for better readability
        VBox assignmentControls = new VBox(10);
        
        // First line: Emergency and Required Skill
        HBox firstLine = new HBox(15);
        firstLine.setAlignment(Pos.CENTER_LEFT);
          Label emergencyLabel = new Label("Select Emergency:");
        emergencyLabel.setPrefWidth(120);
        
        emergencyDropdown = new ComboBox<>();
        emergencyDropdown.setPromptText("Select Emergency");
        emergencyDropdown.setPrefWidth(250);
          // Add listener to auto-fill location when emergency is selected
        emergencyDropdown.setOnAction(e -> {
            String selectedEmergency = emergencyDropdown.getValue();            if (selectedEmergency != null && !selectedEmergency.equals("No active emergencies")) {
                autoFillLocationFromEmergency(selectedEmergency);
            } else {
                // Reset location field when no emergency selected
                if (locationField != null) {
                    locationField.setText("");
                    locationField.setStyle("-fx-background-color: #f8f9fa; -fx-opacity: 0.8;");
                }
            }
        });
        
        Label skillLabel = new Label("Required Skill:");
        skillLabel.setPrefWidth(120);
        requiredSkillDropdown = new ComboBox<>();
        requiredSkillDropdown.getItems().addAll("Medical", "Rescue", "Logistics", "Communication", "Technical", "IT", "Any");
        requiredSkillDropdown.setPromptText("Required Skill");
        requiredSkillDropdown.setPrefWidth(150);
        
        firstLine.getChildren().addAll(emergencyLabel, emergencyDropdown, skillLabel, requiredSkillDropdown);          // Second line: Location (auto-filled) and note about multiple assignments
        HBox secondLine = new HBox(15);
        secondLine.setAlignment(Pos.CENTER_LEFT);
        
        Label locationLabel = new Label("Location:");
        locationLabel.setPrefWidth(120);
          TextField locationField = new TextField();
        locationField.setPromptText("Location will be auto-filled from selected emergency");
        locationField.setPrefWidth(250);
        locationField.setEditable(false);
        locationField.setStyle("-fx-background-color: #f8f9fa; -fx-opacity: 0.8;");
        
        // Store reference for auto-fill
        this.locationField = locationField;
        
        Label noteLabel = new Label("Multiple volunteers can be assigned to one emergency");
        noteLabel.setPrefWidth(350);
        noteLabel.setFont(Font.font("Segoe UI", 10));
        noteLabel.setTextFill(Color.web("#666666"));
        
        secondLine.getChildren().addAll(locationLabel, locationField, noteLabel);
          // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button findVolunteers = new Button("Find Volunteers");
        findVolunteers.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");
        findVolunteers.setOnAction(e -> findAvailableVolunteers());
          Button trackButton = new Button("Track Assignments");
        trackButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");
        trackButton.setOnAction(e -> trackAssignments());
        
        buttonBox.getChildren().addAll(findVolunteers, trackButton);
        
        assignmentControls.getChildren().addAll(firstLine, secondLine, buttonBox);
        
        panel.getChildren().addAll(panelTitle, subtitle, assignmentControls);
        return panel;
    }
    
    private HBox createAssignmentItem(String volunteer, String task, String location, String time) {
        HBox item = new HBox(10);
        item.setPadding(new Insets(8));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");
        
        Label volunteerLabel = new Label(volunteer);
        volunteerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        volunteerLabel.setPrefWidth(120);
        
        Label taskLabel = new Label(task);
        taskLabel.setFont(Font.font("Segoe UI", 12));
        taskLabel.setPrefWidth(150);
        
        Label locationLabel = new Label(location);
        locationLabel.setFont(Font.font("Segoe UI", 12));
        locationLabel.setPrefWidth(100);
        
        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("Segoe UI", 10));
        timeLabel.setTextFill(Color.web("#666666"));
        
        Button statusButton = new Button("Track");
        statusButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 2 8;");
        
        item.getChildren().addAll(volunteerLabel, taskLabel, locationLabel, timeLabel, new Region(), statusButton);
        HBox.setHgrow(item.getChildren().get(4), Priority.ALWAYS);
        
        return item;
    }    private void loadVolunteerData() {
        // Ensure location column exists
        ensureLocationColumn();
        
        approvedVolunteersList.clear();
        pendingVolunteersList.clear();
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();            // Load approved volunteers (from authority or approved self-registered) - must have location (either field)
            String approvedSql = "SELECT * FROM users WHERE user_type = 'VOLUNTEER' AND (status = 'ACTIVE' OR status = 'INACTIVE' OR status = 'ASSIGNED') AND ((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL AND location_name != '')) ORDER BY created_at DESC";
            
            try (ResultSet rs = dbManager.executeQuery(approvedSql)) {
                while (rs.next()) {
                    User volunteer = new User();
                    volunteer.setUserId(rs.getString("user_id"));
                    volunteer.setName(rs.getString("name"));
                    volunteer.setPhone(rs.getString("phone"));
                    volunteer.setEmail(rs.getString("email"));
                    // Try both location and location_name columns
                    String location = rs.getString("location");
                    if (location == null || location.trim().isEmpty()) {
                        location = rs.getString("location_name");
                    }
                    volunteer.setLocation(location);
                    volunteer.setStatus(rs.getString("status"));
                    volunteer.setSkills(rs.getString("skills"));
                    volunteer.setUserType(User.UserType.VOLUNTEER);
                    
                    approvedVolunteersList.add(volunteer);
                }
            }            // Load pending volunteers (self-registered, waiting for approval)
            String pendingSql = "SELECT * FROM users WHERE user_type = 'VOLUNTEER' AND status = 'PENDING' ORDER BY created_at DESC";
            
            System.out.println("DEBUG: Loading pending volunteers with SQL: " + pendingSql);
            
            try (ResultSet rs = dbManager.executeQuery(pendingSql)) {
                int pendingCount = 0;
                while (rs.next()) {
                    pendingCount++;
                    User volunteer = new User();
                    volunteer.setUserId(rs.getString("user_id"));
                    volunteer.setName(rs.getString("name"));
                    volunteer.setPhone(rs.getString("phone"));
                    volunteer.setEmail(rs.getString("email"));
                    // Try both location and location_name columns
                    String location = rs.getString("location");
                    if (location == null || location.trim().isEmpty()) {
                        location = rs.getString("location_name");
                    }
                    volunteer.setLocation(location);
                    volunteer.setStatus(rs.getString("status"));
                    volunteer.setSkills(rs.getString("skills"));
                    volunteer.setUserType(User.UserType.VOLUNTEER);
                    
                    System.out.println("DEBUG: Found pending volunteer - ID: " + volunteer.getUserId() + 
                                     ", Name: " + volunteer.getName() + 
                                     ", Status: " + volunteer.getStatus());
                    
                    pendingVolunteersList.add(volunteer);
                }
                System.out.println("DEBUG: Total pending volunteers loaded: " + pendingCount);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading volunteer data: " + e.getMessage());
            e.printStackTrace();
        }
    }    private void updateMetrics() {
        try {
            // Count total approved volunteers only (exclude pending)
            int totalVolunteers = approvedVolunteersList.size();
            
            // Count active volunteers from approved list only
            long activeVolunteers = approvedVolunteersList.stream()
                .filter(v -> "ACTIVE".equals(v.getStatus()))
                .count();
            
            // Count assignments from approved volunteers only
            long assignedVolunteers = approvedVolunteersList.stream()
                .filter(v -> "ASSIGNED".equals(v.getStatus()))
                .count();
              // Update labels
            if (totalVolunteersLabel != null) {
                totalVolunteersLabel.setText(String.valueOf(totalVolunteers));
            }
            if (activeVolunteersLabel != null) {
                activeVolunteersLabel.setText(String.valueOf(activeVolunteers));
            }
            // Removed assignmentsLabel update as Active Assignments card was removed
            
            System.out.println("DEBUG: Metrics updated - Total: " + totalVolunteers + 
                             ", Active: " + activeVolunteers + ", Assigned: " + assignedVolunteers);
            
        } catch (Exception e) {
            System.err.println("Error updating metrics: " + e.getMessage());
        }
    }      private void loadEmergencyDropdown() {
        if (emergencyDropdown == null) return;
        
        emergencyDropdown.getItems().clear();        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Load regular emergency requests - using correct column names
            String emergencySql = "SELECT request_id, emergency_type, description, priority, location_name, status FROM emergency_requests WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY created_at DESC";
            
            try (ResultSet rs = dbManager.executeQuery(emergencySql)) {
                while (rs.next()) {
                    String requestId = rs.getString("request_id");
                    String emergencyType = rs.getString("emergency_type");
                    String description = rs.getString("description");
                    String priority = rs.getString("priority");
                    String locationName = rs.getString("location_name");
                    
                    if (description != null && !description.trim().isEmpty()) {
                        String displayText = String.format("REQUEST|%s|%s|%s (%s Priority) - %s", 
                            requestId, locationName != null ? locationName : "Unknown",
                            emergencyType, priority, 
                            description.length() > 45 ? description.substring(0, 45) + "..." : description);
                        emergencyDropdown.getItems().add(displayText);
                    } else {
                        String displayText = String.format("REQUEST|%s|%s|%s (%s Priority) - %s", 
                            requestId, locationName != null ? locationName : "Unknown",
                            emergencyType, priority, locationName != null ? locationName : "Unknown Location");
                        emergencyDropdown.getItems().add(displayText);
                    }
                }
            }
            
            // Load SOS alerts - using correct column names  
            String sosSql = "SELECT sos_id, description, location_name, urgency_level, status FROM sos_alerts WHERE status IN ('ACTIVE', 'PENDING', 'ASSIGNED') ORDER BY created_at DESC";
            
            try (ResultSet rs = dbManager.executeQuery(sosSql)) {
                while (rs.next()) {
                    String sosId = rs.getString("sos_id");
                    String description = rs.getString("description");
                    String location = rs.getString("location_name");
                    String priority = rs.getString("urgency_level");
                    
                    if (description != null && !description.trim().isEmpty()) {
                        String displayText = String.format("SOS|%s|%s|SOS Alert (%s Priority) - %s", 
                            sosId, location != null ? location : "Unknown",
                            priority != null ? priority : "HIGH",
                            description.length() > 45 ? description.substring(0, 45) + "..." : description);
                        emergencyDropdown.getItems().add(displayText);
                    } else {
                        String displayText = String.format("SOS|%s|%s|SOS Alert (%s Priority) - %s", 
                            sosId, location != null ? location : "Unknown",
                            priority != null ? priority : "HIGH", 
                            location != null ? location : "Unknown Location");
                        emergencyDropdown.getItems().add(displayText);
                    }
                }}
            
            if (emergencyDropdown.getItems().isEmpty()) {
                emergencyDropdown.getItems().add("No active emergencies or SOS alerts");
            }
            
        } catch (Exception e) {
            System.err.println("Error loading emergency dropdown: " + e.getMessage());
        }
    }    private void autoFillLocationFromEmergency(String selectedEmergency) {
        try {
            if (locationField == null || selectedEmergency == null || selectedEmergency.trim().isEmpty()) {
                return;
            }
            
            // Parse the new format: TYPE|ID|LOCATION|DESCRIPTION
            String[] parts = selectedEmergency.split("\\|");
            if (parts.length >= 3) {
                String location = parts[2];
                if (location != null && !location.trim().isEmpty() && !"Unknown".equals(location)) {
                    locationField.setText(location);
                    locationField.setStyle("-fx-background-color: #e8f5e8; -fx-opacity: 1.0;");
                } else {
                    locationField.setText("Location not specified");
                    locationField.setStyle("-fx-background-color: #fff3cd; -fx-opacity: 1.0;");
                }
            } else {
                // Fallback for old format - try to extract from database
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String locationName = null;
                
                if (selectedEmergency.startsWith("REQUEST")) {
                    // Handle emergency requests
                    String emergencyDesc = selectedEmergency.substring(selectedEmergency.lastIndexOf(" - ") + 3);
                    String getLocationSql = "SELECT location_name FROM emergency_requests WHERE description = ? AND request_status IN ('PENDING', 'IN_PROGRESS') LIMIT 1";
                    
                    try (ResultSet rs = dbManager.executeQuery(getLocationSql, emergencyDesc)) {
                        if (rs.next()) {
                            locationName = rs.getString("location_name");
                        }
                    }
                } else if (selectedEmergency.startsWith("SOS")) {
                    // Handle SOS alerts
                    String sosId = selectedEmergency.split(" - ")[0].replace("SOS Alert: ", "");
                    String getLocationSql = "SELECT location FROM sos_alerts WHERE sos_id = ? AND status IN ('ACTIVE', 'PENDING', 'ASSIGNED') LIMIT 1";
                    
                    try (ResultSet rs = dbManager.executeQuery(getLocationSql, sosId)) {
                        if (rs.next()) {
                            locationName = rs.getString("location");
                        }
                    }
                }
                
                if (locationName != null && !locationName.trim().isEmpty()) {
                    locationField.setText(locationName);
                    locationField.setStyle("-fx-background-color: #e8f5e8; -fx-opacity: 1.0;");
                } else {
                    locationField.setText("Location not specified");
                    locationField.setStyle("-fx-background-color: #fff3cd; -fx-opacity: 1.0;");
                }
            }
        } catch (Exception e) {
            System.err.println("Error auto-filling location: " + e.getMessage());
            locationField.setText("Error loading location");
            locationField.setStyle("-fx-background-color: #f8d7da; -fx-opacity: 1.0;");
        }
    }    
    // Location dropdown is no longer needed - location is auto-filled from emergency selection
    
    private void showAddVolunteerDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add New Volunteer");
        dialog.setHeaderText("Register a new volunteer");
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");
        
        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        
        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh");
        locationBox.setPromptText("Select location");
        
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("ACTIVE", "INACTIVE");
        statusBox.setValue("ACTIVE");
        
        TextField expertiseField = new TextField();
        expertiseField.setPromptText("Expertise (optional - e.g., Medical, Rescue, IT)");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locationBox, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusBox, 1, 4);
        grid.add(new Label("Expertise:"), 0, 5);
        grid.add(expertiseField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Handle OK button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    // Validate input
                    String nameText = nameField.getText();
                    if (nameText == null || nameText.trim().isEmpty()) {
                        showAlert("Validation Error", "Name is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    String phoneText = phoneField.getText();
                    if (phoneText == null || phoneText.trim().isEmpty()) {
                        showAlert("Validation Error", "Phone number is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    String emailText = emailField.getText();
                    if (emailText == null || emailText.trim().isEmpty()) {
                        showAlert("Validation Error", "Email is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    if (locationBox.getValue() == null) {
                        showAlert("Validation Error", "Please select a location.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    // Create new volunteer
                    String userId = java.util.UUID.randomUUID().toString();
                    String skills = expertiseField.getText() != null ? expertiseField.getText().trim() : "";
                    
                    // Save to database
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String sql = "INSERT INTO users (user_id, name, phone, email, location, user_type, status, skills, created_at) " +
                               "VALUES (?, ?, ?, ?, ?, 'VOLUNTEER', ?, ?, ?)";
                    
                    int rowsAffected = dbManager.executeUpdate(sql,
                        userId,
                        nameText.trim(),
                        phoneText.trim(),
                        emailText.trim(),
                        locationBox.getValue(),
                        statusBox.getValue(),
                        skills,
                        java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())
                    );
                    
                    if (rowsAffected > 0) {
                        showAlert("Success", "Volunteer added successfully!", Alert.AlertType.INFORMATION);
                        loadVolunteerData(); // Refresh the table
                        updateMetrics(); // Update metrics
                        
                        // Notify data sync manager
                        DataSyncManager.getInstance().notifyVolunteerDataChanged();
                    } else {
                        showAlert("Error", "Failed to add volunteer.", Alert.AlertType.ERROR);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error adding volunteer: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Error", "Failed to add volunteer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
      private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }      
    
    // Chart methods commented out to fix compilation errors
    /*
    private void createSkillChart() {
        Map<String, Integer> skillCounts = new java.util.HashMap<>();
        
        // Combine both approved and pending volunteers for skill analysis
        List<User> allVolunteers = new ArrayList<>(approvedVolunteersList);
        allVolunteers.addAll(pendingVolunteersList);
        
        for (User volunteer : allVolunteers) {
            String skill = volunteer.getSkills() != null ? volunteer.getSkills() : "General";
            skillCounts.put(skill, skillCounts.getOrDefault(skill, 0) + 1);
        }
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        if (skillCounts.isEmpty()) {
            // If no data, show placeholder
            pieChartData.addAll(
                new PieChart.Data("No Data", 1)
            );
        } else {
            for (Map.Entry<String, Integer> entry : skillCounts.entrySet()) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }
        
        skillChart = new PieChart(pieChartData);
        skillChart.setTitle("Volunteer Skills");
        skillChart.setPrefSize(300, 200);
    }
    
    private void createPerformanceChart() {
        // Implementation for performance bar chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
          performanceChart = new BarChart<>(xAxis, yAxis);
        performanceChart.setTitle("Monthly Assignments Completed");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Assignments");
        
        // Calculate performance based on volunteer data
        // For now, use basic simulation based on number of volunteers
        int totalVolunteers = approvedVolunteersList.size() + pendingVolunteersList.size();
        if (totalVolunteers == 0) {
            // Show placeholder data if no volunteers
            series.getData().add(new XYChart.Data<>("Jan", 0));
            series.getData().add(new XYChart.Data<>("Feb", 0));
            series.getData().add(new XYChart.Data<>("Mar", 0));
            series.getData().add(new XYChart.Data<>("Apr", 0));
            series.getData().add(new XYChart.Data<>("May", 0));
            series.getData().add(new XYChart.Data<>("Jun", 0));
        } else {
            // Simulate monthly data based on volunteer count
            series.getData().add(new XYChart.Data<>("Jan", totalVolunteers * 2));
            series.getData().add(new XYChart.Data<>("Feb", totalVolunteers * 3));
            series.getData().add(new XYChart.Data<>("Mar", totalVolunteers * 2));
            series.getData().add(new XYChart.Data<>("Apr", totalVolunteers * 4));
            series.getData().add(new XYChart.Data<>("May", totalVolunteers * 3));
            series.getData().add(new XYChart.Data<>("Jun", totalVolunteers * 5));
        }        
        performanceChart.getData().add(series);
        performanceChart.setPrefSize(300, 200);
        
        // Calculate performance based on volunteer data
        // For now, use basic simulation based on number of volunteers
        int totalVolunteers = approvedVolunteersList.size() + pendingVolunteersList.size();
        if (totalVolunteers == 0) {
            // Show placeholder data if no volunteers
            series.getData().add(new XYChart.Data<>("Jan", 0));
            series.getData().add(new XYChart.Data<>("Feb", 0));
            series.getData().add(new XYChart.Data<>("Mar", 0));
            series.getData().add(new XYChart.Data<>("Apr", 0));
            series.getData().add(new XYChart.Data<>("May", 0));
            series.getData().add(new XYChart.Data<>("Jun", 0));
        } else {
            // Simulate monthly data based on volunteer count
            series.getData().add(new XYChart.Data<>("Jan", totalVolunteers * 2));
            series.getData().add(new XYChart.Data<>("Feb", totalVolunteers * 3));
            series.getData().add(new XYChart.Data<>("Mar", totalVolunteers * 2));
            series.getData().add(new XYChart.Data<>("Apr", totalVolunteers * 4));
            series.getData().add(new XYChart.Data<>("May", totalVolunteers * 3));
            series.getData().add(new XYChart.Data<>("Jun", totalVolunteers * 5));
        }
        
        performanceChart.getData().add(series);
        performanceChart.setPrefSize(300, 200);
    }
    */
    /**
     * Update volunteer statistics display
     */
    private void updateVolunteerStats() {
        // Update labels based on combined volunteer data
        if (totalVolunteersLabel != null) {
            totalVolunteersLabel.setText(String.valueOf(approvedVolunteersList.size() + pendingVolunteersList.size()));
        }
          if (activeVolunteersLabel != null) {
            long activeCount = approvedVolunteersList.stream()
                    .filter(v -> "ACTIVE".equals(v.getStatus()))
                    .count();
            activeVolunteersLabel.setText(String.valueOf(activeCount));
        }
        
        // Removed assignmentsLabel update as Active Assignments card was removed
    }      private void findAvailableVolunteers() {
        try {
            String selectedLocation = locationField != null ? locationField.getText() : "";
            String requiredSkill = requiredSkillDropdown.getValue();
            String selectedEmergency = emergencyDropdown.getValue();
            
            if (selectedLocation == null || selectedLocation.isEmpty()) {
                showAlert("Selection Required", "Please select an emergency first to auto-fill location.", Alert.AlertType.WARNING);
                return;
            }
            
            if (selectedEmergency == null || selectedEmergency.isEmpty() || "No active emergencies".equals(selectedEmergency)) {
                showAlert("Selection Required", "Please select an emergency.", Alert.AlertType.WARNING);
                return;
            }
            
            showVolunteerAssignmentDialog(selectedLocation, requiredSkill, selectedEmergency);
            
        } catch (Exception e) {
            System.err.println("Error finding volunteers: " + e.getMessage());
            showAlert("Error", "Failed to find volunteers: " + e.getMessage(), Alert.AlertType.ERROR);
        }    }
    
    private void showVolunteerAssignmentDialog(String location, String requiredSkill, String emergencyDescription) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assign Volunteers");
        dialog.setHeaderText("Available Volunteers for " + location);
        
        // Create the content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(400);
        
        // Info label
        Label infoLabel = new Label("Emergency: " + emergencyDescription);
        infoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        infoLabel.setTextFill(Color.web("#1e3c72"));
        
        // Scrollable list of volunteers
        VBox volunteerList = new VBox(5);
        ScrollPane scrollPane = new ScrollPane(volunteerList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background-color: white;");
        
        // Load volunteers and their assignment status
        loadVolunteersForAssignment(volunteerList, location, requiredSkill, emergencyDescription);
        
        // Note label
        Label noteLabel = new Label("You can assign multiple volunteers as needed.");
        noteLabel.setFont(Font.font("Segoe UI", 10));
        noteLabel.setTextFill(Color.web("#666666"));
        
        content.getChildren().addAll(infoLabel, scrollPane, noteLabel);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }    private void loadVolunteersForAssignment(VBox volunteerList, String location, String requiredSkill, String emergencyDescription) {
        try {
            volunteerList.getChildren().clear();
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Parse the emergency dropdown format: "TYPE|ID|LOCATION|DESCRIPTION"
            String recordId = null;
            String recordType = null;
            
            if (emergencyDescription != null && emergencyDescription.contains("|")) {
                String[] parts = emergencyDescription.split("\\|", 4);
                if (parts.length >= 2) {
                    String type = parts[0];
                    recordId = parts[1];
                    
                    if ("REQUEST".equals(type)) {
                        recordType = "EMERGENCY";
                    } else if ("SOS".equals(type)) {
                        recordType = "SOS";
                    }
                }
            }
            
            if (recordId == null) {
                Label noRecordLabel = new Label("Selected emergency/SOS not found. Please select a valid emergency from the dropdown.");
                noRecordLabel.setTextFill(Color.web("#e74c3c"));
                volunteerList.getChildren().add(noRecordLabel);
                return;
            }// Get volunteers query with location hierarchy support - only approved volunteers
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM users WHERE user_type = 'VOLUNTEER' AND status IN ('ACTIVE', 'ASSIGNED') AND ((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL AND location_name != ''))");
            List<String> params = new ArrayList<>();            if (location != null && !location.isEmpty()) {
                // Add support for location hierarchy - include nearby locations
                Set<String> locationVariants = getLocationVariants(location);
                
                // Debug - print locations we're searching for
                System.out.println("DEBUG: Searching for volunteers in these locations: " + locationVariants);
                
                if (locationVariants.size() > 1) {
                    queryBuilder.append(" AND (");
                    int variantIndex = 0;
                    for (String variant : locationVariants) {
                        if (variantIndex > 0) queryBuilder.append(" OR ");
                        // Use LIKE queries to match any location containing the division/district name
                        queryBuilder.append("(location_name LIKE ? OR location LIKE ? OR location_name LIKE ? OR location LIKE ?)");
                        // Add both exact and wildcard patterns
                        params.add(variant); // Exact match
                        params.add(variant); // Exact match
                        params.add("%" + variant + "%"); // Contains match
                        params.add("%" + variant + "%"); // Contains match
                        variantIndex++;
                    }
                    queryBuilder.append(")");
                } else {
                    // Use LIKE queries for single location as well
                    queryBuilder.append(" AND (location_name LIKE ? OR location LIKE ? OR location_name LIKE ? OR location LIKE ?)");
                    params.add(location); // Exact match
                    params.add(location); // Exact match  
                    params.add("%" + location + "%"); // Contains match
                    params.add("%" + location + "%"); // Contains match
                }
            }
            
            if (requiredSkill != null && !requiredSkill.isEmpty() && !"Any".equals(requiredSkill)) {
                queryBuilder.append(" AND (skills LIKE ? OR skills IS NULL)");
                params.add("%" + requiredSkill + "%");
            }
            
            queryBuilder.append(" ORDER BY name ASC");            // Get already assigned volunteers for this record (emergency or SOS)
            String assignedVolunteersSql;
            if ("EMERGENCY".equals(recordType)) {
                assignedVolunteersSql = "SELECT volunteer_id FROM volunteer_assignments WHERE request_id = ? AND status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS')";
            } else { // SOS
                assignedVolunteersSql = "SELECT volunteer_id FROM volunteer_assignments WHERE request_id = ? AND assignment_type = 'SOS' AND status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS')";
            }
            Set<String> assignedVolunteerIds = new HashSet<>();
            
            System.out.println("Checking for existing assignments for " + recordType + ": " + recordId);
            
            try (ResultSet rs = dbManager.executeQuery(assignedVolunteersSql, recordId)) {
                while (rs.next()) {
                    String assignedVolunteerId = rs.getString("volunteer_id");
                    assignedVolunteerIds.add(assignedVolunteerId);
                    System.out.println("Found existing assignment for volunteer: " + assignedVolunteerId);
                }
            }
            
            System.out.println("Total volunteers already assigned: " + assignedVolunteerIds.size());
              // Load volunteers
            try (ResultSet rs = dbManager.executeQuery(queryBuilder.toString(), params.toArray())) {
                int count = 0;
                while (rs.next()) {
                    String volunteerId = rs.getString("user_id");
                    String volunteerName = rs.getString("name");
                    String volunteerSkills = rs.getString("skills");
                    String volunteerEmail = rs.getString("email");
                    // Get location from either field
                    String volunteerLocation = rs.getString("location");
                    if (volunteerLocation == null || volunteerLocation.trim().isEmpty()) {
                        volunteerLocation = rs.getString("location_name");
                    }
                      HBox volunteerRow = createVolunteerRow(volunteerId, volunteerName, volunteerSkills, volunteerEmail, volunteerLocation,
                                                         assignedVolunteerIds.contains(volunteerId), recordId, recordType);
                    volunteerList.getChildren().add(volunteerRow);
                    count++;
                }
                
                if (count == 0) {
                    Label noVolunteersLabel = new Label("No volunteers found matching the criteria.");
                    noVolunteersLabel.setTextFill(Color.web("#666666"));
                    volunteerList.getChildren().add(noVolunteersLabel);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading volunteers for assignment: " + e.getMessage());
            Label errorLabel = new Label("Error loading volunteers: " + e.getMessage());
            errorLabel.setTextFill(Color.web("#e74c3c"));
            volunteerList.getChildren().add(errorLabel);
        }
    }    private HBox createVolunteerRow(String volunteerId, String name, String skills, String email, String location, boolean isAssigned, String recordId, String recordType) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6;");        VBox volunteerInfo = new VBox(2);
        
        // Get assignment count
        int assignmentCount = 0;
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String query = "SELECT COUNT(va.volunteer_id) as assignment_count " +
                          "FROM users u " +
                          "LEFT JOIN volunteer_assignments va ON u.user_id = va.volunteer_id " +
                          "AND va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS') " +
                          "WHERE u.user_id = ? " +
                          "GROUP BY u.user_id";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(query, volunteerId)) {
                if (rs.next()) {
                    assignmentCount = rs.getInt("assignment_count");
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting assignment count: " + e.getMessage());
        }
        
        // Volunteer name with assignment count
        String displayName = name;
        if (assignmentCount > 0) {
            displayName += " [" + assignmentCount + " assignment" + (assignmentCount > 1 ? "s" : "") + "]";
        }
        
        Label nameLabel = new Label(displayName);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        
        // Location label below name
        Label locationLabel = new Label("Location: " + (location != null && !location.isEmpty() ? location : "No location"));
        locationLabel.setFont(Font.font("Segoe UI", 10));
        locationLabel.setTextFill(Color.web("#666666"));
        
        // Skills and email label
        Label detailsLabel = new Label("Skills: " + (skills != null ? skills : "General") + " | " + email);
        detailsLabel.setFont(Font.font("Segoe UI", 10));
        detailsLabel.setTextFill(Color.web("#666666"));
        
        volunteerInfo.getChildren().addAll(nameLabel, locationLabel, detailsLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button actionButton;
        if (isAssigned) {
            actionButton = new Button("Assigned");
            actionButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 12;");
            actionButton.setDisable(true);
        } else {
            actionButton = new Button("Assign");
            actionButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 12;");
            actionButton.setOnAction(e -> assignVolunteerToRecord(volunteerId, name, recordId, recordType, actionButton));
        }
        
        row.getChildren().addAll(volunteerInfo, spacer, actionButton);
        return row;
    }    private void assignVolunteerToRecord(String volunteerId, String volunteerName, String recordId, String recordType, Button button) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Generate unique assignment ID
            String assignmentId = "ASSIGN_" + System.currentTimeMillis() + "_" + volunteerId.substring(0, Math.min(6, volunteerId.length()));
            
            // Debug logging
            System.out.println("Assigning volunteer: " + volunteerName + " (ID: " + volunteerId + ") to " + recordType + ": " + recordId);
            System.out.println("Assignment ID: " + assignmentId);
            
            // Insert assignment
            String insertSql = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, assignment_type, status, assigned_at) VALUES (?, ?, ?, ?, 'ASSIGNED', datetime('now'))";
            
            int rowsAffected = dbManager.executeUpdate(insertSql, assignmentId, volunteerId, recordId, recordType);
            
            System.out.println("Rows affected by assignment insert: " + rowsAffected);
            
            if (rowsAffected > 0) {                // Update the appropriate table with assigned volunteer (handle multiple volunteers)
                if ("EMERGENCY".equals(recordType)) {
                    // Get existing assigned volunteers
                    String getExistingSql = "SELECT assigned_volunteer FROM emergency_requests WHERE request_id = ?";
                    String currentAssigned = null;
                    try (ResultSet rs = dbManager.executeQuery(getExistingSql, recordId)) {
                        if (rs.next()) {
                            currentAssigned = rs.getString("assigned_volunteer");
                        }
                    }
                    
                    // Update with new volunteer (append if others exist)
                    String newAssignedValue;
                    if (currentAssigned == null || currentAssigned.trim().isEmpty() || "None".equals(currentAssigned)) {
                        newAssignedValue = volunteerName;
                    } else if (!currentAssigned.contains(volunteerName)) {
                        newAssignedValue = currentAssigned + ", " + volunteerName;
                    } else {
                        newAssignedValue = currentAssigned; // Already assigned
                    }
                    
                    String updateEmergencySql = "UPDATE emergency_requests SET assigned_volunteer = ?, status = 'ASSIGNED' WHERE request_id = ?";
                    int emergencyUpdateRows = dbManager.executeUpdate(updateEmergencySql, newAssignedValue, recordId);
                    System.out.println("Emergency table update rows affected: " + emergencyUpdateRows);
                    
                } else if ("SOS".equals(recordType)) {
                    // Get existing assigned volunteers
                    String getExistingSql = "SELECT assigned_volunteer FROM sos_alerts WHERE sos_id = ?";
                    String currentAssigned = null;
                    try (ResultSet rs = dbManager.executeQuery(getExistingSql, recordId)) {
                        if (rs.next()) {
                            currentAssigned = rs.getString("assigned_volunteer");
                        }
                    }
                    
                    // Update with new volunteer (append if others exist)
                    String newAssignedValue;
                    if (currentAssigned == null || currentAssigned.trim().isEmpty() || "None".equals(currentAssigned)) {
                        newAssignedValue = volunteerName;
                    } else if (!currentAssigned.contains(volunteerName)) {
                        newAssignedValue = currentAssigned + ", " + volunteerName;
                    } else {
                        newAssignedValue = currentAssigned; // Already assigned
                    }
                    
                    String updateSosSql = "UPDATE sos_alerts SET assigned_volunteer = ?, status = 'ASSIGNED' WHERE sos_id = ?";
                    int sosUpdateRows = dbManager.executeUpdate(updateSosSql, newAssignedValue, recordId);
                    System.out.println("SOS table update rows affected: " + sosUpdateRows);
                }
                
                // Update button to show assigned
                button.setText("Assigned");
                button.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 4 12;");
                button.setDisable(true);
                  showAlert("Success", volunteerName + " has been assigned to this " + recordType.toLowerCase() + ".", Alert.AlertType.INFORMATION);                // Notify data sync manager for both volunteer and emergency data
                System.out.println("VolunteerView: Sending data change notifications for " + recordType + " assignment");
                DataSyncManager.getInstance().notifyVolunteerDataChanged();
                DataSyncManager.getInstance().notifyEmergencyDataChanged();
                
                System.out.println("Assignment completed successfully");
            } else {
                System.out.println("Assignment failed - no rows affected");
                showAlert("Error", "Failed to assign volunteer.", Alert.AlertType.ERROR);
            }
            
        } catch (Exception e) {
            System.err.println("Error assigning volunteer: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to assign volunteer: " + e.getMessage(), Alert.AlertType.ERROR);
        }    }private void trackAssignments() {
        // First sync table assignments to track assignments
        // This is a simple approach - in real app, would get EmergencyView reference
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Sync Emergency Requests to volunteer_assignments
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
                                    
                                    // Check if assignment already exists
                                    String checkQuery = "SELECT COUNT(*) as count FROM volunteer_assignments WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'EMERGENCY'";
                                    try (java.sql.ResultSet checkRs = dbManager.executeQuery(checkQuery, volunteerId, requestId)) {
                                        if (checkRs.next() && checkRs.getInt("count") == 0) {
                                            // Create assignment record
                                            String assignmentId = "SYNC_" + System.currentTimeMillis() + "_" + volunteerId.substring(0, Math.min(4, volunteerId.length()));
                                            String insertQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, assignment_type, status, assigned_at) VALUES (?, ?, ?, 'EMERGENCY', 'ASSIGNED', datetime('now'))";
                                            dbManager.executeUpdate(insertQuery, assignmentId, volunteerId, requestId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Sync SOS Alerts to volunteer_assignments  
            String sosQuery = "SELECT sos_id, assigned_volunteer FROM sos_alerts WHERE assigned_volunteer IS NOT NULL AND assigned_volunteer != '' AND assigned_volunteer != 'None'";
            
            try (java.sql.ResultSet rs = dbManager.executeQuery(sosQuery)) {
                while (rs.next()) {
                    String sosId = rs.getString("sos_id");
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
                                    
                                    // Check if assignment already exists
                                    String checkQuery = "SELECT COUNT(*) as count FROM volunteer_assignments WHERE volunteer_id = ? AND request_id = ? AND assignment_type = 'SOS'";
                                    try (java.sql.ResultSet checkRs = dbManager.executeQuery(checkQuery, volunteerId, sosId)) {
                                        if (checkRs.next() && checkRs.getInt("count") == 0) {
                                            // Create assignment record
                                            String assignmentId = "SOS_SYNC_" + System.currentTimeMillis() + "_" + volunteerId.substring(0, Math.min(4, volunteerId.length()));
                                            String insertQuery = "INSERT INTO volunteer_assignments (assignment_id, volunteer_id, request_id, assignment_type, status, assigned_at) VALUES (?, ?, ?, 'SOS', 'ASSIGNED', datetime('now'))";
                                            dbManager.executeUpdate(insertQuery, assignmentId, volunteerId, sosId);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
              } catch (Exception e) {
            System.err.println("Error syncing assignments: " + e.getMessage());
        }
        
        // Now sync track assignments back to tables
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Get all EMERGENCY assignments from volunteer_assignments and sync to emergency_requests
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
                        String updateQuery = "UPDATE emergency_requests SET assigned_volunteer = ?, status = 'ASSIGNED' WHERE request_id = ?";
                        dbManager.executeUpdate(updateQuery, volunteerNames, requestId);
                    }
                }
            }
            
            // Get all SOS assignments from volunteer_assignments and sync to sos_alerts
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
                        String updateQuery = "UPDATE sos_alerts SET assigned_volunteer = ?, status = 'ASSIGNED' WHERE sos_id = ?";
                        dbManager.executeUpdate(updateQuery, volunteerNames, sosId);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error syncing track assignments to tables: " + e.getMessage());
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assignment Tracking");
        dialog.setHeaderText("Volunteer Assignment Summary");
        
        // Create the content
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(800);
        content.setPrefHeight(500);
          // Create table for assignments
        assignmentTable = new TableView<>();
        assignmentTable.setPrefHeight(400);
        
        // Volunteer Name column
        TableColumn<VolunteerAssignment, String> volunteerCol = new TableColumn<>("Volunteer");
        volunteerCol.setCellValueFactory(new PropertyValueFactory<>("volunteerName"));
        volunteerCol.setPrefWidth(150);
        
        // Status column
        TableColumn<VolunteerAssignment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        
        // Emergency column
        TableColumn<VolunteerAssignment, String> emergencyCol = new TableColumn<>("Emergency");
        emergencyCol.setCellValueFactory(new PropertyValueFactory<>("emergencyDescription"));
        emergencyCol.setPrefWidth(200);
        
        // Location column
        TableColumn<VolunteerAssignment, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));        locationCol.setPrefWidth(120);
          // Assigned At column - show only date
        TableColumn<VolunteerAssignment, String> assignedAtCol = new TableColumn<>("Assigned Date");
        assignedAtCol.setCellValueFactory(new PropertyValueFactory<>("assignedAt"));
        assignedAtCol.setPrefWidth(130);
        
        assignmentTable.getColumns().add(volunteerCol);
        assignmentTable.getColumns().add(statusCol);
        assignmentTable.getColumns().add(emergencyCol);
        assignmentTable.getColumns().add(locationCol);
        assignmentTable.getColumns().add(assignedAtCol);
        
        // Load assignment data
        loadAssignmentData(assignmentTable);
          // Summary statistics
        assignmentSummaryLabel = new Label();
        assignmentSummaryLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        assignmentSummaryLabel.setTextFill(Color.web("#1e3c72"));
        updateAssignmentSummary(assignmentSummaryLabel, assignmentTable);
        
        content.getChildren().addAll(assignmentSummaryLabel, assignmentTable);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }
    
    private void loadAssignmentData(TableView<VolunteerAssignment> table) {
        try {
            ObservableList<VolunteerAssignment> assignments = FXCollections.observableArrayList();
            
            DatabaseManager dbManager = DatabaseManager.getInstance();            String sql = """
                SELECT va.assignment_id, va.volunteer_id, va.request_id, va.assignment_type, va.status, 
                       date(va.assigned_at) as assigned_date,
                       u.name as volunteer_name, u.location as volunteer_location,
                       CASE 
                         WHEN va.assignment_type = 'EMERGENCY' THEN er.description
                         WHEN va.assignment_type = 'SOS' THEN 'SOS Alert: ' || sa.sos_id
                         ELSE 'Unknown Assignment'
                       END as emergency_description,                       CASE 
                         WHEN va.assignment_type = 'EMERGENCY' THEN er.location_name
                         WHEN va.assignment_type = 'SOS' THEN sa.location_name
                         ELSE 'Unknown Location'
                       END as emergency_location
                FROM volunteer_assignments va
                JOIN users u ON va.volunteer_id = u.user_id
                LEFT JOIN emergency_requests er ON va.request_id = er.request_id AND va.assignment_type = 'EMERGENCY'
                LEFT JOIN sos_alerts sa ON va.request_id = sa.sos_id AND va.assignment_type = 'SOS'
                WHERE va.status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED')
                ORDER BY va.assigned_at DESC            """;
            
            System.out.println("Loading assignment data with query: " + sql);
            
            try (ResultSet rs = dbManager.executeQuery(sql)) {
                int count = 0;
                while (rs.next()) {
                    count++;                    VolunteerAssignment assignment = new VolunteerAssignment(
                        rs.getString("assignment_id"),
                        rs.getString("volunteer_id"),
                        rs.getString("volunteer_name"),
                        rs.getString("status"),
                        rs.getString("emergency_description"),
                        rs.getString("emergency_location"),
                        rs.getString("assigned_date"),
                        rs.getString("assignment_type"),
                        rs.getString("request_id")
                    );
                    assignments.add(assignment);
                    System.out.println("Loaded assignment: " + rs.getString("volunteer_name") + " -> " + rs.getString("emergency_description"));
                }
                System.out.println("Total assignments loaded: " + count);
            }
            
            table.setItems(assignments);
            
        } catch (Exception e) {
            System.err.println("Error loading assignment data: " + e.getMessage());
            showAlert("Error", "Failed to load assignment data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void updateAssignmentSummary(Label summaryLabel, TableView<VolunteerAssignment> table) {
        int totalAssignments = table.getItems().size();
        long activeAssignments = table.getItems().stream()
            .filter(a -> "ASSIGNED".equals(a.getStatus()) || "ACCEPTED".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
            .count();
        long completedAssignments = table.getItems().stream()
            .filter(a -> "COMPLETED".equals(a.getStatus()))
            .count();
            
        summaryLabel.setText(String.format("Total Assignments: %d | Active: %d | Completed: %d", 
                                          totalAssignments, activeAssignments, completedAssignments));
    }
    
    private void removeAssignment(VolunteerAssignment assignment, TableView<VolunteerAssignment> table) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Remove Assignment");
        confirmAlert.setHeaderText("Remove volunteer assignment?");
        confirmAlert.setContentText("Are you sure you want to remove " + assignment.getVolunteerName() + 
                                   " from this emergency assignment?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                String sql = "UPDATE volunteer_assignments SET status = 'CANCELLED' WHERE assignment_id = ?";
                
                int rowsAffected = dbManager.executeUpdate(sql, assignment.getAssignmentId());                if (rowsAffected > 0) {
                    // Sync the cancellation back to the source emergency/SOS table
                    syncAssignmentStatusToSource(assignment.getRequestId(), assignment.getAssignmentType(), "CANCELLED");
                    
                    // Free volunteers from this cancelled assignment
                    freeVolunteersFromCompletedAssignment(assignment.getRequestId(), assignment.getAssignmentType());
                    
                    table.getItems().remove(assignment);
                    updateAssignmentSummary((Label) ((VBox) ((DialogPane) table.getParent().getParent()).getContent()).getChildren().get(0), table);
                    showAlert("Success", "Assignment removed successfully.", Alert.AlertType.INFORMATION);
                    DataSyncManager.getInstance().notifyVolunteerDataChanged();
                    DataSyncManager.getInstance().notifyEmergencyDataChanged();
                } else {
                    showAlert("Error", "Failed to remove assignment.", Alert.AlertType.ERROR);
                }
                
            } catch (Exception e) {
                System.err.println("Error removing assignment: " + e.getMessage());
                showAlert("Error", "Failed to remove assignment: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    // Helper class for assignment tracking
    public static class VolunteerAssignment {
        private final String assignmentId;
        private final String volunteerId;
        private final String volunteerName;
        private final String status;
        private final String emergencyDescription;
        private final String location;
        private final String assignedAt;
        private final String assignmentType;
        private final String requestId;
        
        public VolunteerAssignment(String assignmentId, String volunteerId, String volunteerName, 
                                 String status, String emergencyDescription, String location, String assignedAt,
                                 String assignmentType, String requestId) {            this.assignmentId = assignmentId;
            this.volunteerId = volunteerId;
            this.volunteerName = volunteerName;
            this.status = status;
            this.emergencyDescription = emergencyDescription;
            this.location = location;
            this.assignedAt = assignedAt;
            this.assignmentType = assignmentType;
            this.requestId = requestId;
        }
        
        public String getAssignmentId() { return assignmentId; }
        public String getVolunteerId() { return volunteerId; }
        public String getVolunteerName() { return volunteerName; }
        public String getStatus() { return status; }
        public String getEmergencyDescription() { return emergencyDescription; }
        public String getLocation() { return location; }
        public String getAssignedAt() { return assignedAt; }
        public String getAssignmentType() { return assignmentType; }
        public String getRequestId() { return requestId; }
    }
    
    private void approveVolunteer(User volunteer) {
        if (volunteer.getLocation() == null || volunteer.getLocation().trim().isEmpty()) {
            showAlert("Cannot Approve", "Volunteer must have a location before approval.", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "UPDATE users SET status = 'ACTIVE' WHERE user_id = ?";
            
            int rowsAffected = dbManager.executeUpdate(sql, volunteer.getUserId());
            
            if (rowsAffected > 0) {
                volunteer.setStatus("ACTIVE");
                pendingVolunteersList.remove(volunteer);
                approvedVolunteersList.add(volunteer);
                updateMetrics();
                showAlert("Success", "Volunteer approved successfully!", Alert.AlertType.INFORMATION);
                DataSyncManager.getInstance().notifyVolunteerDataChanged();
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to approve volunteer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void rejectVolunteer(User volunteer) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "DELETE FROM users WHERE user_id = ?";
            dbManager.executeUpdate(sql, volunteer.getUserId());
            pendingVolunteersList.remove(volunteer);
            updateMetrics();
            showAlert("Success", "Volunteer application rejected.", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to reject volunteer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void showAddLocationDialog(User volunteer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Location");
        dialog.setHeaderText("Add location for: " + volunteer.getName());
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh");
        
        grid.add(new Label("Location:"), 0, 0);
        grid.add(locationBox, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK && locationBox.getValue() != null) {
                try {
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String sql = "UPDATE users SET location = ? WHERE user_id = ?";
                    dbManager.executeUpdate(sql, locationBox.getValue(), volunteer.getUserId());
                    volunteer.setLocation(locationBox.getValue());
                    pendingVolunteersTable.refresh();
                    showAlert("Success", "Location added successfully!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Error", "Failed to add location: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
      private void showEditVolunteerDialog(User volunteer, boolean isFromAuthorityTable) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Volunteer");
        dialog.setHeaderText("Edit volunteer details: " + volunteer.getName());
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(volunteer.getName());
        TextField phoneField = new TextField(volunteer.getPhone());
        TextField emailField = new TextField(volunteer.getEmail());
        
        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh");
        locationBox.setValue(volunteer.getLocation());
        
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("ACTIVE", "INACTIVE");
        statusBox.setValue(volunteer.getStatus());
        
        TextField expertiseField = new TextField(volunteer.getSkills() != null ? volunteer.getSkills() : "");
        expertiseField.setPromptText("Optional - e.g., Medical, Rescue, IT");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Location:"), 0, 3);
        grid.add(locationBox, 1, 3);
        
        // Only show status field for authority table
        if (isFromAuthorityTable) {
            grid.add(new Label("Status:"), 0, 4);
            grid.add(statusBox, 1, 4);
            grid.add(new Label("Expertise (Optional):"), 0, 5);
            grid.add(expertiseField, 1, 5);
        } else {
            grid.add(new Label("Expertise (Optional):"), 0, 4);
            grid.add(expertiseField, 1, 4);
        }
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Handle OK button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    // Validate and update volunteer
                    String nameText = nameField.getText().trim();
                    String phoneText = phoneField.getText().trim();
                    String emailText = emailField.getText().trim();
                    
                    if (nameText.isEmpty() || phoneText.isEmpty() || emailText.isEmpty()) {
                        showAlert("Validation Error", "Name, phone, and email are required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    if (locationBox.getValue() == null) {
                        showAlert("Validation Error", "Please select a location.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    // Update in database
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String skills = expertiseField.getText().trim(); // Optional field
                      String sql;
                    int rowsAffected = 0;
                    if (isFromAuthorityTable) {
                        // Authority can update status
                        sql = "UPDATE users SET name = ?, phone = ?, email = ?, location = ?, status = ?, skills = ? WHERE user_id = ?";
                        rowsAffected = dbManager.executeUpdate(sql, nameText, phoneText, emailText, 
                            locationBox.getValue(), statusBox.getValue(), skills, volunteer.getUserId());
                        
                        if (rowsAffected > 0) {
                            volunteer.setStatus(statusBox.getValue());
                        }
                    } else {
                        // Pending volunteers can't change status
                        sql = "UPDATE users SET name = ?, phone = ?, email = ?, location = ?, skills = ? WHERE user_id = ?";
                        rowsAffected = dbManager.executeUpdate(sql, nameText, phoneText, emailText, 
                            locationBox.getValue(), skills, volunteer.getUserId());
                    }
                    
                    if (rowsAffected > 0) {
                        // Common updates
                        volunteer.setName(nameText);
                        volunteer.setPhone(phoneText);
                        volunteer.setEmail(emailText);
                        volunteer.setLocation(locationBox.getValue());
                        volunteer.setSkills(skills);
                        
                        // Refresh appropriate table
                        if (isFromAuthorityTable) {
                            approvedVolunteersTable.refresh();
                        } else {
                            pendingVolunteersTable.refresh();
                        }
                        updateMetrics();
                        
                        showAlert("Success", "Volunteer updated successfully!", Alert.AlertType.INFORMATION);
                        DataSyncManager.getInstance().notifyVolunteerDataChanged();
                    } else {
                        showAlert("Error", "Failed to update volunteer.", Alert.AlertType.ERROR);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error updating volunteer: " + e.getMessage());
                    showAlert("Error", "Failed to update volunteer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Handle self-registration of a volunteer
     * Check if they match an authority-added volunteer (by name/email) for auto-approval
     */
    public boolean handleSelfRegistration(String name, String email, String phone, String skills) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // First, check if this volunteer already exists in authority-added volunteers
            String checkSql = "SELECT * FROM users WHERE user_type = 'VOLUNTEER' AND " +
                            "(LOWER(name) = LOWER(?) OR LOWER(email) = LOWER(?)) AND " +
                            "(status = 'ACTIVE' OR status = 'INACTIVE')";
            
            try (ResultSet rs = dbManager.executeQuery(checkSql, name, email)) {
                if (rs.next()) {
                    // Match found - update the existing record with login details
                    String updateSql = "UPDATE users SET phone = ?, email = ?, skills = ?, " +
                                     "status = 'ACTIVE', last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";
                    
                    int rowsAffected = dbManager.executeUpdate(updateSql, 
                        phone, email, skills, rs.getString("user_id"));
                    
                    if (rowsAffected > 0) {
                        // Refresh data to show the updated volunteer
                        loadVolunteerData();
                        updateMetrics();
                        return true; // Auto-approved
                    }
                }
            }
            
            // No match found - add to pending volunteers
            String userId = java.util.UUID.randomUUID().toString();
            String insertSql = "INSERT INTO users (user_id, name, phone, email, user_type, status, skills, created_at) " +
                             "VALUES (?, ?, ?, ?, 'VOLUNTEER', 'PENDING', ?, ?)";
            
            int rowsAffected = dbManager.executeUpdate(insertSql,
                userId, name, phone, email, skills,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            
            if (rowsAffected > 0) {
                // Refresh data to show the new pending volunteer
                loadVolunteerData();
                updateMetrics();
                return false; // Needs approval
            }
            
        } catch (Exception e) {
            System.err.println("Error handling self-registration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a volunteer (by user ID) has access to volunteer features
     * Only approved volunteers in the authority table can access volunteer features
     */
    public boolean hasVolunteerAccess(String userId) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT COUNT(*) as count FROM users WHERE user_id = ? AND " +
                        "user_type = 'VOLUNTEER' AND (status = 'ACTIVE' OR status = 'INACTIVE')";
            
            try (ResultSet rs = dbManager.executeQuery(sql, userId)) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking volunteer access: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get volunteer status for UI display
     */
    public String getVolunteerStatus(String userId) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT status FROM users WHERE user_id = ? AND user_type = 'VOLUNTEER'";
            
            try (ResultSet rs = dbManager.executeQuery(sql, userId)) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting volunteer status: " + e.getMessage());
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Update the database location column name to match our code
     */
    private void ensureLocationColumn() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            // Add location column if it doesn't exist
            String alterSql = "ALTER TABLE users ADD COLUMN location TEXT";
            try {
                dbManager.executeUpdate(alterSql);
                System.out.println("Added location column to users table");
            } catch (Exception e) {
                // Column might already exist, ignore error
                if (!e.getMessage().contains("duplicate column")) {
                    System.err.println("Error adding location column: " + e.getMessage());
                }
            }        } catch (Exception e) {
            System.err.println("Error ensuring location column: " + e.getMessage());
        }
    }    private Set<String> getLocationVariants(String location) {
        Set<String> variants = new HashSet<>();
        if (location == null || location.trim().isEmpty()) {
            return variants;
        }
          // Print location hierarchy map for debugging (only once)
        if (!mapPrintedFlag) {
            LocationHierarchy.printLocationMap();
            mapPrintedFlag = true;
        }
        
        // Normalize the location name
        String normalizedLocation = location.trim();
        variants.add(normalizedLocation); // Always include the original location
        
        System.out.println("DEBUG: Finding variants for location: '" + normalizedLocation + "'");
        
        // Handle compound locations like "Old Dhaka", "Puran Dhaka", "Mirpur, Dhaka", etc.
        String[] locationParts = normalizedLocation.split("[,;-]");
        for (String part : locationParts) {
            String trimmedPart = part.trim();
            if (!trimmedPart.isEmpty()) {
                variants.add(trimmedPart);
                
                // Check if this part contains any known division/district names
                checkAndAddDivisionVariants(trimmedPart, variants);
            }
        }
        
        // Check the full location for division/district matches
        checkAndAddDivisionVariants(normalizedLocation, variants);
        
        // Special case handling for common area names
        addSpecialLocationVariants(normalizedLocation, variants);
          System.out.println("DEBUG: Final location variants for '" + normalizedLocation + "': " + variants);
        return variants;    
    }
    
    private void checkAndAddDivisionVariants(String locationPart, Set<String> variants) {
        // Check if this part is a district
        String division = LocationHierarchy.getDivisionForDistrict(locationPart);
        System.out.println("DEBUG: Division for " + locationPart + " is: " + division);
        
        if (!division.isEmpty() && !division.equals(locationPart)) {
            // This is a district, add all districts from same division
            List<String> districtsInSameDivision = LocationHierarchy.getDistrictsInDivision(division);
            System.out.println("DEBUG: Districts in " + division + " division: " + districtsInSameDivision);
            variants.addAll(districtsInSameDivision);
            variants.add(division); // Also add the division itself
        } else {
            // This might be a division, add all districts in this division
            List<String> districts = LocationHierarchy.getDistrictsInDivision(locationPart);
            System.out.println("DEBUG: Districts in " + locationPart + " (as division): " + districts);
            if (!districts.isEmpty()) {
                variants.addAll(districts);
            }
        }
        
        // Also check if the location contains any division/district name
        for (String div : Arrays.asList("Dhaka", "Chittagong", "Sylhet", "Khulna", "Rajshahi", "Barisal", "Rangpur", "Mymensingh")) {
            if (locationPart.toLowerCase().contains(div.toLowerCase())) {
                variants.add(div);
                List<String> districts = LocationHierarchy.getDistrictsInDivision(div);
                variants.addAll(districts);
                System.out.println("DEBUG: Found division '" + div + "' in location, added districts: " + districts);
            }
        }
    }
    
    private void addSpecialLocationVariants(String location, Set<String> variants) {
        String lowerLocation = location.toLowerCase();
        
        // Special mappings for common areas
        if (lowerLocation.contains("old dhaka") || lowerLocation.contains("puran dhaka") || 
            lowerLocation.contains("old town") || lowerLocation.contains("gulshan") || 
            lowerLocation.contains("dhanmondi") || lowerLocation.contains("mirpur") ||
            lowerLocation.contains("uttara") || lowerLocation.contains("banani")) {
            variants.add("Dhaka");
            List<String> dhakaDistricts = LocationHierarchy.getDistrictsInDivision("Dhaka");
            variants.addAll(dhakaDistricts);
            System.out.println("DEBUG: Detected Dhaka area, added Dhaka division variants");
        }
        
        if (lowerLocation.contains("feni")) {
            variants.add("Chittagong");
            List<String> chittagongDistricts = LocationHierarchy.getDistrictsInDivision("Chittagong");
            variants.addAll(chittagongDistricts);
            System.out.println("DEBUG: Feni detected, added Chittagong division variants");
        }
          if (lowerLocation.contains("tangail")) {
            variants.add("Dhaka");
            List<String> dhakaDistricts = LocationHierarchy.getDistrictsInDivision("Dhaka");
            variants.addAll(dhakaDistricts);
            System.out.println("DEBUG: Tangail detected, added Dhaka division variants");
        }
    }

    // DataChangeListener implementation
    @Override
    public void onResourceDataChanged() {
        // Not used in this view
    }
      @Override
    public void onEmergencyDataChanged() {
        // Refresh emergency dropdown when new emergencies are added
        javafx.application.Platform.runLater(() -> {
            loadEmergencyDropdown();
            // Also refresh assignment table if it exists
            if (assignmentTable != null) {
                loadAssignmentData(assignmentTable);
                updateAssignmentSummary(assignmentSummaryLabel, assignmentTable);
            }
        });
    }
      @Override
    public void onUserDataChanged() {
        // Refresh volunteer data when user data changes
        System.out.println("DEBUG: VolunteerView received user data change notification");
        javafx.application.Platform.runLater(() -> {
            System.out.println("DEBUG: Refreshing volunteer data due to user data change");
            loadVolunteerData();
            updateMetrics();
        });
    }
    
    @Override
    public void onDashboardDataChanged() {
        // Not used in this view
    }
      @Override
    public void onVolunteerDataChanged() {
        // Refresh volunteer data
        javafx.application.Platform.runLater(() -> {
            loadVolunteerData();
            updateMetrics();
            // Also refresh assignment table if it exists
            if (assignmentTable != null) {
                loadAssignmentData(assignmentTable);
                updateAssignmentSummary(assignmentSummaryLabel, assignmentTable);
            }
        });
    }
    
    @Override
    public void onCommunicationDataChanged() {
        // Not used in this view
    }    @Override
    public void onSettingsDataChanged() {
        // Not used in this view
    }
    
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
}