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
import javafx.beans.property.SimpleStringProperty;
import com.reliefnet.model.Resource;
import com.reliefnet.model.User;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.util.DataSyncManager;
import com.reliefnet.database.DatabaseManager;

/**
 * ResourceView - Manages resource inventory and distribution
 * Core functionality: Resource tracking, inventory management
 */
public class ResourceView {
      private User currentUser; // Current logged-in user for role-based access control
    private VBox mainContainer;
    private TableView<Resource> resourceTable;
    private TableView<Resource> distributedTable; // Add reference to distributed table
    private ObservableList<Resource> resourceList;
    private ObservableList<Resource> distributedResourcesList; // Add reference to distributed list
    private Label totalResourcesLabel;
    private Label criticalResourcesLabel;
    private Label alertTextLabel;public VBox createResourceView(User user) {
        this.currentUser = user;
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f8f9fa;");
        
        // Initialize empty resource list first
        resourceList = FXCollections.observableArrayList();
        
        // Header section
        VBox header = createHeader();
        
        // Metrics cards
        HBox metricsRow = createMetricsCards();
        
        // Control panel
        HBox controlPanel = createControlPanel();
          // Main content area
        HBox contentArea = createMainContent();
          mainContainer.getChildren().addAll(header, metricsRow, controlPanel, contentArea);          // Load initial data and update metrics
        loadResourceData();
        updateMetrics();
        updateResourceStats();
        
        return mainContainer;
    }      private VBox createHeader() {
        VBox header = new VBox(10);
        
        Label titleLabel = new Label("Resource Management");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1e3c72"));        // Create role-specific subtitle
        String subtitleText;
        if (currentUser != null && currentUser.getUserType() == com.reliefnet.model.User.UserType.VOLUNTEER) {
            subtitleText = "View available resources and distribute supplies to those in need";
        } else {
            subtitleText = "Manage inventory, allocate resources, and monitor critical stock levels";
        }
        
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#666666"));
        
        // Alert banner for critical shortages (threshold: below 100 units)
        HBox alertBanner = createAlertBanner();
        
        header.getChildren().addAll(titleLabel, subtitleLabel, alertBanner);
        return header;
    }
        private HBox createMetricsCards() {
        HBox metricsRow = new HBox(20);
        metricsRow.setAlignment(Pos.CENTER);
        
        // Total Resources Card
        VBox totalCard = createMetricCard("Total Resources", "1,247", "items", "#3498db");
        totalResourcesLabel = (Label) ((VBox) totalCard.getChildren().get(1)).getChildren().get(0);
          // Critical Resources Card (threshold: quantity < 100)
        VBox criticalCard = createMetricCard("Critical Level", "3", "below 100 units", "#e74c3c");
        criticalResourcesLabel = (Label) ((VBox) criticalCard.getChildren().get(1)).getChildren().get(0);
        
        // Remove Distribution Centers Card
        
        metricsRow.getChildren().addAll(totalCard, criticalCard);
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
    }    private HBox createControlPanel() {
        HBox controlPanel = new HBox(15);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setAlignment(Pos.CENTER);
        controlPanel.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        // Add Resource button - only for Authority users
        if (currentUser != null && currentUser.getUserType() == User.UserType.AUTHORITY) {
            Button addButton = new Button("Add Resource");
            ThemeManager.stylePrimaryButton(addButton);
            addButton.setOnAction(e -> showAddResourceDialog());
            controlPanel.getChildren().add(addButton);
        }
          // Refresh button - available for all users
        Button refreshButton = new Button("🔄 Refresh Tables");
        ThemeManager.stylePrimaryButton(refreshButton); // Make it look like Add Resource button
        refreshButton.setOnAction(e -> {
            System.out.println("Refresh button clicked!");
            refreshAllTables();
        });
        
        controlPanel.getChildren().add(refreshButton);
        
        return controlPanel;
    }      private HBox createMainContent() {
        HBox contentArea = new HBox(20);
        contentArea.setPrefHeight(500);
        
        // Resource tables container
        VBox tablesContainer = createResourceTablesContainer();
        
        contentArea.getChildren().add(tablesContainer);
        HBox.setHgrow(tablesContainer, Priority.ALWAYS);
        
        return contentArea;
    }
    
    private VBox createResourceTablesContainer() {
        VBox container = new VBox(20);
        
        // Resource Available table
        VBox availableTableContainer = createResourceAvailableTable();
        
        // Resource Distributed table
        VBox distributedTableContainer = createResourceDistributedTable();
        
        container.getChildren().addAll(availableTableContainer, distributedTableContainer);
        return container;
    }
    
    private VBox createResourceAvailableTable() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label tableTitle = new Label("Resource Available");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tableTitle.setTextFill(Color.web("#1e3c72"));        resourceTable = new TableView<>();
        resourceTable.setPrefHeight(200);
        
        // Create columns
        TableColumn<Resource, String> nameCol = new TableColumn<>("Resource Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(200);
        
        TableColumn<Resource, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        categoryCol.setPrefWidth(150);
        
        TableColumn<Resource, Number> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());
        quantityCol.setPrefWidth(100);
        
        TableColumn<Resource, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(cellData -> cellData.getValue().unitProperty());
        unitCol.setPrefWidth(80);
        
        TableColumn<Resource, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        TableColumn<Resource, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        locationCol.setPrefWidth(120);
          // Actions column - role-based button visibility
        TableColumn<Resource, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(param -> new TableCell<Resource, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button distributeBtn = new Button("Distribute");
            private final HBox actionBox = new HBox(5);
            
            {
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
                distributeBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4;");
                actionBox.setAlignment(Pos.CENTER);
                
                // Role-based button visibility
                if (currentUser != null) {
                    switch (currentUser.getUserType()) {
                        case AUTHORITY:
                            // Authority can only edit resources
                            actionBox.getChildren().add(editBtn);
                            break;
                        case VOLUNTEER:
                            // Volunteer can only distribute resources
                            actionBox.getChildren().add(distributeBtn);
                            break;
                        case SURVIVOR:
                            // Survivor cannot edit or distribute (read-only)
                            // No buttons for survivors
                            break;
                    }
                } else {
                    // Default fallback - show both buttons if user is null
                    actionBox.getChildren().addAll(editBtn, distributeBtn);
                }
                
                editBtn.setOnAction(e -> {
                    Resource resource = getTableView().getItems().get(getIndex());
                    ResourceView.this.showEditResourceDialog(resource);
                });
                
                distributeBtn.setOnAction(e -> {
                    Resource resource = getTableView().getItems().get(getIndex());
                    ResourceView.this.showDistributeResourceDialog(resource);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });
        
        @SuppressWarnings("unchecked")
        TableColumn<Resource, ?>[] columns = new TableColumn[] {
            nameCol, categoryCol, quantityCol, unitCol, statusCol, locationCol, actionsCol
        };
        resourceTable.getColumns().addAll(columns);
        
        container.getChildren().addAll(tableTitle, resourceTable);
        return container;
    }
    
    private VBox createResourceDistributedTable() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label tableTitle = new Label("Resource Distributed");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        tableTitle.setTextFill(Color.web("#1e3c72"));        distributedTable = new TableView<>(); // Use instance variable
        distributedTable.setPrefHeight(200);
        
        // Create columns - same structure as available table
        TableColumn<Resource, String> nameCol = new TableColumn<>("Resource Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameCol.setPrefWidth(200);
        
        TableColumn<Resource, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        categoryCol.setPrefWidth(150);
        
        TableColumn<Resource, Number> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());
        quantityCol.setPrefWidth(100);
        
        TableColumn<Resource, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(cellData -> cellData.getValue().unitProperty());
        unitCol.setPrefWidth(80);
        
        TableColumn<Resource, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        TableColumn<Resource, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        locationCol.setPrefWidth(120);
        
        TableColumn<Resource, String> allocatedCol = new TableColumn<>("Allocated To");
        allocatedCol.setCellValueFactory(cellData -> {
            String allocatedTo = cellData.getValue().getAllocatedTo();
            return new SimpleStringProperty(allocatedTo != null ? allocatedTo : "");
        });
        allocatedCol.setPrefWidth(150);
        
        @SuppressWarnings("unchecked")
        TableColumn<Resource, ?>[] columns = new TableColumn[] {
            nameCol, categoryCol, quantityCol, unitCol, statusCol, locationCol, allocatedCol
        };
        distributedTable.getColumns().addAll(columns);
          // Filter and show only distributed resources
        distributedResourcesList = FXCollections.observableArrayList(); // Use instance variable
        loadDistributedResources(distributedResourcesList);
        distributedTable.setItems(distributedResourcesList);
        
        container.getChildren().addAll(tableTitle, distributedTable);
        return container;    }
      private void loadResourceData() {
        resourceList = FXCollections.observableArrayList();
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            // Only load AVAILABLE resources for the available table
            String sql = "SELECT * FROM resources WHERE status = 'AVAILABLE' ORDER BY created_at DESC";
            
            // Execute query and populate resource list
            try (java.sql.ResultSet rs = dbManager.executeQuery(sql)) {
                while (rs.next()) {
                    Resource resource = new Resource();
                    resource.setResourceId(rs.getString("resource_id"));
                    resource.setName(rs.getString("name"));
                    
                    // Parse category
                    String categoryStr = rs.getString("category");
                    if (categoryStr != null) {
                        try {
                            Resource.ResourceCategory categoryEnum = Resource.ResourceCategory.valueOf(categoryStr);
                            resource.setCategoryEnum(categoryEnum);
                        } catch (IllegalArgumentException e) {
                            resource.setCategory(categoryStr); // Use as string if enum doesn't match
                        }
                    }
                    
                    resource.setQuantity(rs.getInt("quantity"));
                    resource.setUnit(rs.getString("unit"));
                    resource.setLocationLat(rs.getDouble("location_lat"));
                    resource.setLocationLng(rs.getDouble("location_lng"));
                    resource.setLocation(rs.getString("location_name"));
                    
                    // Parse status
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        try {
                            Resource.ResourceStatus statusEnum = Resource.ResourceStatus.valueOf(statusStr.toUpperCase());
                            resource.setStatusEnum(statusEnum);
                        } catch (IllegalArgumentException e) {
                            resource.setStatus(statusStr); // Use as string if enum doesn't match
                        }
                    }
                    
                    // Parse expiry date
                    java.sql.Date expiryDate = rs.getDate("expiry_date");
                    if (expiryDate != null) {
                        resource.setExpiryDate(expiryDate.toLocalDate());
                    }
                    
                    resource.setAllocatedTo(rs.getString("allocated_to"));
                    resource.setNotes(rs.getString("notes"));
                    
                    // Parse timestamps
                    java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
                    if (createdTs != null) {
                        resource.setCreatedAt(createdTs.toLocalDateTime());
                    }
                    
                    java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
                    if (updatedTs != null) {
                        resource.setUpdatedAt(updatedTs.toLocalDateTime());
                    }
                    
                    resourceList.add(resource);
                }
            }
            
            System.out.println("Resource data loaded successfully: " + resourceList.size() + " resources");
            
        } catch (Exception e) {
            System.err.println("Error loading resource data: " + e.getMessage());
            e.printStackTrace();
            // Initialize with empty list on error
            resourceList = FXCollections.observableArrayList();
        }          if (resourceTable != null) {
            resourceTable.setItems(resourceList);
        }
    }      private void updateMetrics() {
        if (resourceList == null) {
            if (totalResourcesLabel != null) {
                totalResourcesLabel.setText("0");
            }
            if (criticalResourcesLabel != null) {
                criticalResourcesLabel.setText("0");
            }
            if (alertTextLabel != null) {
                updateAlertBanner(alertTextLabel);
            }
            return;
        }
        
        int totalResources = resourceList.size();
        // Critical threshold: resources with quantity below 100 units
        long criticalResources = resourceList.stream()
            .filter(r -> r.getQuantity() < 100)
            .count();
        
        if (totalResourcesLabel != null) {
            totalResourcesLabel.setText(String.valueOf(totalResources));
        }
        if (criticalResourcesLabel != null) {
            criticalResourcesLabel.setText(String.valueOf(criticalResources));
        }
        if (alertTextLabel != null) {
            updateAlertBanner(alertTextLabel);
        }
    }
      /**
     * Update resource statistics display
     */    
    private void updateResourceStats() {
        if (resourceList == null) {
            if (totalResourcesLabel != null) {
                totalResourcesLabel.setText("0");
            }
            return;
        }
        
        // Update total resources
        if (totalResourcesLabel != null) {
            totalResourcesLabel.setText(String.valueOf(resourceList.size()));
        }
    }
    
    /**
     * Load distributed resources
     */    private void loadDistributedResources(ObservableList<Resource> distributedResources) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "SELECT * FROM resources WHERE status = 'DISTRIBUTED' ORDER BY updated_at DESC";
            
            // Execute query and populate distributed resource list
            try (java.sql.ResultSet rs = dbManager.executeQuery(sql)) {
                while (rs.next()) {
                    Resource resource = new Resource();
                    resource.setResourceId(rs.getString("resource_id"));
                    resource.setName(rs.getString("name"));
                    resource.setCategory(rs.getString("category"));
                    resource.setQuantity(rs.getInt("quantity")); // This now shows the distributed quantity
                    resource.setUnit(rs.getString("unit"));
                    resource.setLocation(rs.getString("location_name"));
                    resource.setStatus(rs.getString("status"));
                    resource.setAllocatedTo(rs.getString("allocated_to"));
                    
                    distributedResources.add(resource);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading distributed resources: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show Add Resource Dialog
     */
    private void showAddResourceDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add New Resource");
        dialog.setHeaderText("Add a new resource to inventory");
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));        TextField nameField = new TextField();
        nameField.setPromptText("Resource name");
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Food and Nutrition", "Water and Sanitation", "Medical Supplies", "Shelter and Housing", "Clothing and Textiles", "Fuel and Energy", "Equipment and Tools", "Transportation", "Communication", "Other");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity");
        TextField unitField = new TextField();
        unitField.setPromptText("Unit (e.g., kg, liters, pieces)");
        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh");
        locationBox.setPromptText("Select division");        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("AVAILABLE", "EXPIRED"); // Only show AVAILABLE and EXPIRED for authorities
        statusBox.setValue("AVAILABLE");
        TextArea notesArea = new TextArea();        notesArea.setPromptText("Additional notes");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryBox, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Unit:"), 0, 3);
        grid.add(unitField, 1, 3);
        grid.add(new Label("Location:"), 0, 4);
        grid.add(locationBox, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusBox, 1, 5);
        grid.add(new Label("Notes:"), 0, 6);
        grid.add(notesArea, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Handle OK button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {                    // Validate input
                    String nameText = nameField.getText();
                    if (nameText == null || nameText.trim().isEmpty()) {
                        showAlert("Validation Error", "Resource name is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    if (categoryBox.getValue() == null) {
                        showAlert("Validation Error", "Please select a category.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    String quantityText = quantityField.getText();
                    if (quantityText == null || quantityText.trim().isEmpty()) {
                        showAlert("Validation Error", "Quantity is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    int quantity;
                    try {
                        quantity = Integer.parseInt(quantityText.trim());
                        if (quantity < 0) {
                            showAlert("Validation Error", "Quantity must be positive.", Alert.AlertType.ERROR);
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Validation Error", "Please enter a valid quantity number.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    // Create new resource with null-safe assignments
                    Resource newResource = new Resource();
                    newResource.setResourceId(java.util.UUID.randomUUID().toString());
                    newResource.setName(nameText.trim());
                    newResource.setCategory(categoryBox.getValue());
                    newResource.setQuantity(quantity);
                    newResource.setUnit(unitField.getText() != null ? unitField.getText().trim() : "");
                    newResource.setLocation(locationBox.getValue() != null ? locationBox.getValue() : "");
                    newResource.setStatus(statusBox.getValue() != null ? statusBox.getValue() : "AVAILABLE");
                    newResource.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : "");
                    newResource.setCreatedAt(java.time.LocalDateTime.now());
                    newResource.setUpdatedAt(java.time.LocalDateTime.now());
                      // Save to database
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String sql = "INSERT INTO resources (resource_id, name, category, quantity, unit, location_name, status, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
                    int rowsAffected = dbManager.executeUpdate(sql,
                        newResource.getResourceId(),
                        newResource.getName(),
                        newResource.getCategory(),
                        newResource.getQuantity(),
                        newResource.getUnit(),
                        newResource.getLocation(),
                        newResource.getStatus(),
                        newResource.getNotes(),
                        java.sql.Timestamp.valueOf(newResource.getCreatedAt()),
                        java.sql.Timestamp.valueOf(newResource.getUpdatedAt())
                    );                    if (rowsAffected > 0) {
                        // Log activity
                        logActivity("RESOURCE_ADDED", "Added resource: " + newResource.getName() + " (Qty: " + quantity + ")");
                        
                        // Refresh tables
                        refreshAllTables();
                        
                        // 🔄 NOTIFY ALL USERS: Resource data changed
                        DataSyncManager.getInstance().notifyResourceDataChanged();
                        
                        showAlert("Success", "Resource added successfully!", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Failed to add resource.", Alert.AlertType.ERROR);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error adding resource: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Error", "An error occurred while adding the resource: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Show dialog to edit an existing resource
     */
    private void showEditResourceDialog(Resource resource) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Resource");
        dialog.setHeaderText("Edit resource: " + resource.getName());
        
        // Create form fields pre-filled with current values
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));        TextField nameField = new TextField(resource.getName() != null ? resource.getName() : "");
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Food and Nutrition", "Water and Sanitation", "Medical Supplies", "Shelter and Housing", "Clothing and Textiles", "Fuel and Energy", "Equipment and Tools", "Transportation", "Communication", "Other");
        categoryBox.setValue(resource.getCategory() != null ? resource.getCategory() : "Other");
        TextField quantityField = new TextField(String.valueOf(resource.getQuantity()));
        TextField unitField = new TextField(resource.getUnit() != null ? resource.getUnit() : "");
        ComboBox<String> locationBox = new ComboBox<>();
        locationBox.getItems().addAll("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Rangpur", "Mymensingh");
        locationBox.setValue(resource.getLocation() != null ? resource.getLocation() : "Dhaka");        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("AVAILABLE", "EXPIRED"); // Only show AVAILABLE and EXPIRED for authorities
        statusBox.setValue(resource.getStatus() != null ? resource.getStatus() : "AVAILABLE");
        TextArea notesArea = new TextArea(resource.getNotes() != null ? resource.getNotes() : "");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryBox, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Unit:"), 0, 3);
        grid.add(unitField, 1, 3);        grid.add(new Label("Location:"), 0, 4);
        grid.add(locationBox, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusBox, 1, 5);
        grid.add(new Label("Notes:"), 0, 6);
        grid.add(notesArea, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Handle OK button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {                    // Validate input
                    String nameText = nameField.getText();
                    if (nameText == null || nameText.trim().isEmpty()) {
                        showAlert("Validation Error", "Resource name is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    String quantityText = quantityField.getText();
                    if (quantityText == null || quantityText.trim().isEmpty()) {
                        showAlert("Validation Error", "Quantity is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    int quantity;
                    try {
                        quantity = Integer.parseInt(quantityText.trim());
                        if (quantity < 0) {
                            showAlert("Validation Error", "Quantity must be positive.", Alert.AlertType.ERROR);
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Validation Error", "Please enter a valid quantity number.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    // Update resource with null-safe assignments
                    String originalName = resource.getName();
                    resource.setName(nameText.trim());
                    resource.setCategory(categoryBox.getValue() != null ? categoryBox.getValue() : "OTHER");
                    resource.setQuantity(quantity);
                    resource.setUnit(unitField.getText() != null ? unitField.getText().trim() : "");
                    resource.setLocation(locationBox.getValue() != null ? locationBox.getValue() : "");
                    resource.setStatus(statusBox.getValue() != null ? statusBox.getValue() : "AVAILABLE");
                    resource.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : "");
                    resource.setUpdatedAt(java.time.LocalDateTime.now());
                      // Update in database
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String sql = "UPDATE resources SET name=?, category=?, quantity=?, unit=?, location_name=?, status=?, notes=?, updated_at=? WHERE resource_id=?";
                    
                    int rowsAffected = dbManager.executeUpdate(sql,
                        resource.getName(),
                        resource.getCategory(),
                        resource.getQuantity(),
                        resource.getUnit(),
                        resource.getLocation(),
                        resource.getStatus(),
                        resource.getNotes(),
                        java.sql.Timestamp.valueOf(resource.getUpdatedAt()),
                        resource.getResourceId()
                    );
                      if (rowsAffected > 0) {
                        // Log activity
                        logActivity("RESOURCE_UPDATED", "Updated resource: " + originalName + " → " + resource.getName());
                        
                        // Refresh tables
                        refreshAllTables();
                        
                        // 🔄 NOTIFY ALL USERS: Resource data changed
                        DataSyncManager.getInstance().notifyResourceDataChanged();
                        
                        showAlert("Success", "Resource updated successfully!", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Failed to update resource.", Alert.AlertType.ERROR);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error updating resource: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Error", "An error occurred while updating the resource: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Show dialog to distribute a resource
     */
    private void showDistributeResourceDialog(Resource resource) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Distribute Resource");
        dialog.setHeaderText("Distribute: " + resource.getName() + " (Available: " + resource.getQuantity() + " " + resource.getUnit() + ")");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Quantity to distribute");
        TextField recipientField = new TextField();
        recipientField.setPromptText("Recipient/Location");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Distribution notes");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Quantity to Distribute:"), 0, 0);
        grid.add(quantityField, 1, 0);
        grid.add(new Label("Allocated To:"), 0, 1);
        grid.add(recipientField, 1, 1);
        grid.add(new Label("Notes:"), 0, 2);
        grid.add(notesArea, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Handle OK button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {                    String quantityText = quantityField.getText();
                    if (quantityText == null || quantityText.trim().isEmpty()) {
                        showAlert("Validation Error", "Quantity is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    int distributeQuantity;
                    try {
                        distributeQuantity = Integer.parseInt(quantityText.trim());
                        if (distributeQuantity <= 0) {
                            showAlert("Validation Error", "Quantity must be positive.", Alert.AlertType.ERROR);
                            return null;
                        }
                        if (distributeQuantity > resource.getQuantity()) {
                            showAlert("Validation Error", "Cannot distribute more than available quantity (" + resource.getQuantity() + ").", Alert.AlertType.ERROR);
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        showAlert("Validation Error", "Please enter a valid quantity number.", Alert.AlertType.ERROR);
                        return null;
                    }
                    
                    String recipientText = recipientField.getText();
                    if (recipientText == null || recipientText.trim().isEmpty()) {
                        showAlert("Validation Error", "Recipient/Location is required.", Alert.AlertType.ERROR);
                        return null;
                    }
                      // Update resource quantity and status with null-safe assignments
                    int newQuantity = resource.getQuantity() - distributeQuantity;
                    resource.setQuantity(newQuantity);
                    resource.setStatus(newQuantity > 0 ? "AVAILABLE" : "DISTRIBUTED");
                    resource.setUpdatedAt(java.time.LocalDateTime.now());
                    
                    // Update original resource in database
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    String updateSql = "UPDATE resources SET quantity=?, status=?, updated_at=? WHERE resource_id=?";
                    
                    int rowsAffected = dbManager.executeUpdate(updateSql,
                        resource.getQuantity(),
                        resource.getStatus(),
                        java.sql.Timestamp.valueOf(resource.getUpdatedAt()),
                        resource.getResourceId()
                    );
                    
                    // Create a separate distribution record for the distributed table
                    if (rowsAffected > 0) {
                        String distributionId = java.util.UUID.randomUUID().toString();
                        String insertSql = "INSERT INTO resources (resource_id, name, category, quantity, unit, location_name, status, allocated_to, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        
                        java.sql.Timestamp now = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                        
                        dbManager.executeUpdate(insertSql,
                            distributionId,
                            resource.getName(),
                            resource.getCategory(),
                            distributeQuantity, // This shows the DISTRIBUTED quantity, not remaining
                            resource.getUnit(),
                            resource.getLocation(),
                            "DISTRIBUTED",
                            recipientText.trim(),
                            notesArea.getText() != null ? notesArea.getText().trim() : "",
                            now,
                            now                        );
                        
                        // Log activity
                        logActivity("RESOURCE_DISTRIBUTED", "Distributed " + distributeQuantity + " " + resource.getUnit() + " of " + resource.getName() + " to " + recipientText);
                        
                        // Refresh tables
                        refreshAllTables();
                        
                        // 🔄 NOTIFY ALL USERS: Resource data changed
                        DataSyncManager.getInstance().notifyResourceDataChanged();
                        
                        showAlert("Success", "Resource distributed successfully!", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Error", "Failed to distribute resource.", Alert.AlertType.ERROR);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error distributing resource: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Error", "An error occurred while distributing the resource: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    /**
     * Log activity to database for dashboard recent activities
     */
    private void logActivity(String activityType, String description) {
        try {            DatabaseManager dbManager = DatabaseManager.getInstance();
            String sql = "CREATE TABLE IF NOT EXISTS activities (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, activity_type TEXT, description TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            dbManager.executeUpdate(sql);
            
            String insertSql = "INSERT INTO activities (user_id, activity_type, description) VALUES (?, ?, ?)";
            int rowsAffected = dbManager.executeUpdate(insertSql,
                currentUser != null ? currentUser.getUserId() : "unknown",
                activityType,
                description
            );
            if (rowsAffected > 0) {
                System.out.println("Activity logged: " + description);
            }
        } catch (Exception e) {
            System.err.println("Error logging activity: " + e.getMessage());
            // Don't show error to user for logging failures
        }
    }
    
    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private HBox createAlertBanner() {
        HBox banner = new HBox(15);
        banner.setPadding(new Insets(12));
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: linear-gradient(to right, #ff6b6b, #ee5a52); " +
                       "-fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label alertIcon = new Label("⚠");
        alertIcon.setFont(Font.font(20));
        alertIcon.setTextFill(Color.WHITE);
        
        // Dynamic alert text based on critical resources (threshold: below 100 units)
        alertTextLabel = new Label();
        alertTextLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        alertTextLabel.setTextFill(Color.WHITE);
        updateAlertBanner(alertTextLabel);
        
        Button viewButton = new Button("View Details");
        viewButton.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; " +
                           "-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: white;");
        viewButton.setOnAction(e -> showCriticalResourcesDialog());
        
        banner.getChildren().addAll(alertIcon, alertTextLabel, new Region(), viewButton);
        HBox.setHgrow(banner.getChildren().get(2), Priority.ALWAYS);
        
        return banner;
    }
      private void updateAlertBanner(Label alertText) {
        if (resourceList == null || resourceList.isEmpty()) {
            alertText.setText("No resources available yet. Add resources to start monitoring.");
            return;
        }
        
        // Critical threshold: quantity below 100 units
        long criticalCount = resourceList.stream()
            .filter(r -> r.getQuantity() < 100)
            .count();
        
        if (criticalCount == 0) {
            alertText.setText("All resources are at healthy levels (100+ units each)");
        } else {
            alertText.setText("Critical: " + criticalCount + " resource" + 
                            (criticalCount == 1 ? "" : "s") + " below 100 units threshold");
        }
    }
      private void showCriticalResourcesDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Critical Resources Details");
        dialog.setHeaderText("Resources Below 100 Units Threshold");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        TableView<Resource> criticalTable = new TableView<>();
        
        // Check if there are any resources at all
        if (resourceList == null || resourceList.isEmpty()) {
            Label noDataLabel = new Label("No resources have been added yet. Add resources to monitor critical levels.");
            noDataLabel.setFont(Font.font("Segoe UI", 14));
            noDataLabel.setStyle("-fx-text-fill: #666666;");
            content.getChildren().add(noDataLabel);
        } else {
            // Filter critical resources (threshold: below 100 units)
            ObservableList<Resource> criticalResources = FXCollections.observableArrayList();
            criticalResources.addAll(resourceList.stream()
                .filter(r -> r.getQuantity() < 100)
                .toList());
            
            if (criticalResources.isEmpty()) {
                Label noDataLabel = new Label("No critical resources found. All resources have 100+ units.");
                noDataLabel.setFont(Font.font("Segoe UI", 14));
                noDataLabel.setStyle("-fx-text-fill: #27ae60;");
                content.getChildren().add(noDataLabel);
            } else {
                // Create table columns
                TableColumn<Resource, String> nameCol = new TableColumn<>("Resource Name");
                nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
                nameCol.setPrefWidth(200);
                
                TableColumn<Resource, String> categoryCol = new TableColumn<>("Category");
                categoryCol.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
                categoryCol.setPrefWidth(150);
                
                TableColumn<Resource, Number> quantityCol = new TableColumn<>("Current Quantity");
                quantityCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());
                quantityCol.setPrefWidth(120);
                
                TableColumn<Resource, String> unitCol = new TableColumn<>("Unit");
                unitCol.setCellValueFactory(cellData -> cellData.getValue().unitProperty());
                unitCol.setPrefWidth(80);
                
                TableColumn<Resource, String> locationCol = new TableColumn<>("Location");
                locationCol.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
                locationCol.setPrefWidth(150);
                
                criticalTable.getColumns().add(nameCol);
                criticalTable.getColumns().add(categoryCol);
                criticalTable.getColumns().add(quantityCol);
                criticalTable.getColumns().add(unitCol);
                criticalTable.getColumns().add(locationCol);
                criticalTable.setItems(criticalResources);
                criticalTable.setPrefHeight(300);
                
                content.getChildren().add(criticalTable);
            }
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
      /**
     * Refresh all tables and metrics - called by the refresh button
     */
    private void refreshAllTables() {
        System.out.println("🔄 Refreshing all resource tables and dashboard...");
        
        try {
            // Reload resource data from database
            loadResourceData();
            
            // Update metrics cards
            updateMetrics();
            updateResourceStats();
            
            // Refresh the distributed resources table
            refreshDistributedTable();
              System.out.println("✅ All resource tables refreshed successfully!");
            
            // Don't show success alert - it's annoying for users
            
        } catch (Exception e) {
            System.err.println("❌ Error refreshing tables: " + e.getMessage());
            e.printStackTrace();
            showAlert("Refresh Error", "An error occurred while refreshing the tables: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
      /**
     * Refresh the distributed resources table
     */
    private void refreshDistributedTable() {
        try {
            // Clear the existing distributed resources list
            if (distributedResourcesList != null) {
                distributedResourcesList.clear();
                // Reload distributed resources from database
                loadDistributedResources(distributedResourcesList);
                System.out.println("Distributed resources table refreshed with " + distributedResourcesList.size() + " items");
            }
        } catch (Exception e) {
            System.err.println("Error refreshing distributed table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Clear dashboard cache to force refresh - this will be called from MainController
     */
    public void clearDashboardCache() {
        // This method can be called by MainController to clear dashboard cache
        System.out.println("Dashboard cache clear requested from ResourceView");
    }
}
