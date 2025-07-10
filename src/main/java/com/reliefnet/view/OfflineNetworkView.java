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

import com.reliefnet.network.NetworkManager;
import com.reliefnet.network.NetworkConfig;
import com.reliefnet.network.PeerDiscoveryManager;
import com.reliefnet.model.User;
import com.reliefnet.util.ThemeManager;

import java.util.Set;

/**
 * OfflineNetworkView - Specialized interface for offline networking functionality
 * Shows different features based on user type (Authority/Volunteer/Survivor)
 */
public class OfflineNetworkView {
    
    private final User currentUser;
    private final NetworkManager networkManager;
    
    private VBox mainContainer;
    private Label networkStatusLabel;
    private ListView<String> nearbyDevicesList;
    private ObservableList<String> nearbyDevices;
    private Label deviceSectionTitle;
    private Label deviceSectionInfo;
    private VBox connectedDevicesSection;
    
    public OfflineNetworkView(User user) {
        this.currentUser = user;
        this.networkManager = NetworkManager.getInstance();
        this.nearbyDevices = FXCollections.observableArrayList();
        
        // Initialize network manager with current user
        networkManager.initialize(user);
        
        // Add network status listener
        networkManager.addNetworkStatusListener(this::onNetworkStatusChanged);
    }
    
    public VBox createOfflineNetworkView() {
        try {
            mainContainer = new VBox(20);
            mainContainer.setPadding(new Insets(20));
            mainContainer.setStyle("-fx-background-color: #f8f9fa;");
            
            // Header
            VBox header = createHeader();
            
            // Network status section
            VBox networkStatus = createNetworkStatusSection();
            
            // User-specific controls (removed for volunteers and survivors - they auto-switch)
            VBox controls = null;
            if (currentUser.getUserType() == User.UserType.AUTHORITY) {
                controls = createNetworkControls();
            }
            
            // Device sections (volunteers and survivors get both connected and nearby)
            if (currentUser.getUserType() == User.UserType.VOLUNTEER || currentUser.getUserType() == User.UserType.SURVIVOR) {
                // Connected devices section (when volunteer acts as server, survivors don't act as server but still show for consistency)
                connectedDevicesSection = createConnectedDevicesSection();
                // Nearby devices section (when in mesh mode)
                VBox nearbyDevices = createNearbyDevicesSection();
                
                // Add sections vertically (no controls section for volunteers and survivors)
                mainContainer.getChildren().addAll(
                    header, 
                    networkStatus,
                    connectedDevicesSection,
                    nearbyDevices
                );
            } else {
                // Authority gets single device section + controls
                VBox nearbyDevices = createNearbyDevicesSection();
                
                // Add sections vertically with controls
                if (controls != null) {
                    mainContainer.getChildren().addAll(
                        header, 
                        networkStatus, 
                        controls, 
                        nearbyDevices
                    );
                } else {
                    mainContainer.getChildren().addAll(
                        header, 
                        networkStatus, 
                        nearbyDevices
                    );
                }
            }
            
            // Initial refresh
            refreshNetworkInfo();
            
            return mainContainer;
            
        } catch (Exception e) {
            System.err.println("Error creating offline network view: " + e.getMessage());
            e.printStackTrace();
            return createErrorView();
        }
    }
    
    private VBox createHeader() {
        VBox header = new VBox(10);
        
        Label titleLabel = new Label("Network Connection");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#1e3c72"));
        
        String subtitle = getSubtitleBasedOnUserType();
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(Font.font("Segoe UI", 14));
        subtitleLabel.setTextFill(Color.web("#666666"));
        subtitleLabel.setWrapText(true);
        
        header.getChildren().addAll(titleLabel, subtitleLabel);
        return header;
    }
    
    private String getSubtitleBasedOnUserType() {
        switch (currentUser.getUserType()) {
            case AUTHORITY:
                return "As an Authority, when you have internet connection, you are always ready to serve " +
                       "as a local server for volunteers and survivors who need access.";
            case VOLUNTEER:
                return "As a Volunteer, your device automatically switches between serving as a local server " +
                       "(when online) and joining mesh networks (when offline) to coordinate relief efforts.";
            case SURVIVOR:
                return "As a Survivor, your device automatically connects to available networks " +
                       "(cloud, local servers, or mesh networks) to request help and coordinate relief efforts.";
            default:
                return "Network connection management for disaster response coordination.";
        }
    }
    
    private VBox createNetworkStatusSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label sectionTitle = new Label("Network Status");
        sectionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web("#1e3c72"));
        
        networkStatusLabel = new Label("Checking network status...");
        networkStatusLabel.setFont(Font.font("Segoe UI", 14));
        networkStatusLabel.setWrapText(true);
        
        // Network mode indicators - different for Authority vs Volunteers vs Survivors
        HBox indicators = new HBox(15);
        indicators.setAlignment(Pos.CENTER_LEFT);
        
        if (currentUser.getUserType() == User.UserType.AUTHORITY) {
            // Authority: Only 2 states - Online (ready to serve) or Completely Disconnected
            VBox onlineIndicator = createStatusIndicator("🌐", "Online", "Connected - Ready to serve others");
            VBox disconnectedIndicator = createStatusIndicator("📴", "Disconnected", "No internet - Not working");
            indicators.getChildren().addAll(onlineIndicator, disconnectedIndicator);
        } else if (currentUser.getUserType() == User.UserType.VOLUNTEER) {
            // Volunteers: 3 states - Online (server), Mesh (peer-to-peer), Disconnected
            VBox onlineIndicator = createStatusIndicator("🌐", "Online", "Internet + Can serve others");
            VBox meshIndicator = createStatusIndicator("📱", "Mesh", "Peer-to-peer network");
            VBox disconnectedIndicator = createStatusIndicator("📴", "Disconnected", "No network available");
            indicators.getChildren().addAll(onlineIndicator, meshIndicator, disconnectedIndicator);
        } else {
            // Survivors: 3 states - Online (internet), Mesh (peer-to-peer), Disconnected (cannot act as server)
            VBox onlineIndicator = createStatusIndicator("�", "Online", "Internet connection available");
            VBox meshIndicator = createStatusIndicator("📱", "Mesh", "Peer-to-peer network");
            VBox disconnectedIndicator = createStatusIndicator("📴", "Disconnected", "No network available");
            indicators.getChildren().addAll(onlineIndicator, meshIndicator, disconnectedIndicator);
        }
        
        section.getChildren().addAll(sectionTitle, networkStatusLabel, indicators);
        return section;
    }
    

    private VBox createStatusIndicator(String icon, String title, String description) {
        VBox indicator = new VBox(5);
        indicator.setAlignment(Pos.CENTER);
        indicator.setPadding(new Insets(10));
        indicator.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6; -fx-border-color: #dee2e6; -fx-border-radius: 6;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(20));
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        
        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Segoe UI", 10));
        descLabel.setTextFill(Color.web("#666666"));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(80);
        
        indicator.getChildren().addAll(iconLabel, titleLabel, descLabel);
        return indicator;
    }
    
    private VBox createNetworkControls() {
        VBox controls = new VBox(15);
        controls.setPadding(new Insets(20));
        controls.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                         "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        Label controlsTitle = new Label("Network Controls");
        controlsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        controlsTitle.setTextFill(Color.web("#1e3c72"));
        
        VBox userSpecificControls = createUserSpecificControls();
        
        controls.getChildren().addAll(controlsTitle, userSpecificControls);
        return controls;
    }
    
    private VBox createUserSpecificControls() {
        VBox controls = new VBox(15);
        
        switch (currentUser.getUserType()) {
            case AUTHORITY:
                controls.getChildren().addAll(createAuthorityControls());
                break;
            case VOLUNTEER:
                controls.getChildren().addAll(createVolunteerControls());
                break;
            case SURVIVOR:
                controls.getChildren().addAll(createSurvivorControls());
                break;
        }
        
        return controls;
    }
    
    private VBox createAuthorityControls() {
        VBox controls = new VBox(12);
        
        Label info = new Label("Authority Network Settings:");
        info.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        // Server mode toggle
        CheckBox serverModeCheckbox = new CheckBox("Always act as server when online");
        serverModeCheckbox.setFont(Font.font("Segoe UI", 13));
        serverModeCheckbox.setSelected(true); // Authority should always be ready to serve
        serverModeCheckbox.setOnAction(e -> toggleServerMode(serverModeCheckbox.isSelected()));
        
        Label explanation = new Label("When enabled and you have internet connection, your device will always " +
                                    "be available as a server for volunteers and survivors to connect to.");
        explanation.setFont(Font.font("Segoe UI", 11));
        explanation.setTextFill(Color.web("#666666"));
        explanation.setWrapText(true);
        
        controls.getChildren().addAll(info, serverModeCheckbox, explanation);
        return controls;
    }
    
    private VBox createVolunteerControls() {
        VBox controls = new VBox(12);
        
        Label info = new Label("Network Operations (Automatic):");
        info.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        Label autoSwitchInfo = new Label("Your device automatically switches between serving as a local server " +
                                       "(when online) and joining mesh networks (when offline). No manual intervention required.");
        autoSwitchInfo.setFont(Font.font("Segoe UI", 12));
        autoSwitchInfo.setTextFill(Color.web("#666666"));
        autoSwitchInfo.setWrapText(true);
        
        controls.getChildren().addAll(info, autoSwitchInfo);
        return controls;
    }
    
    private VBox createSurvivorControls() {
        VBox controls = new VBox(12);
        
        Label info = new Label("Network Operations (Automatic):");
        info.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        Label autoSwitchInfo = new Label("Your device automatically connects to available networks " +
                                       "(cloud, local servers, or mesh networks) to request help and coordinate relief efforts. " +
                                       "No manual intervention required.");
        autoSwitchInfo.setFont(Font.font("Segoe UI", 12));
        autoSwitchInfo.setTextFill(Color.web("#666666"));
        autoSwitchInfo.setWrapText(true);
        
        controls.getChildren().addAll(info, autoSwitchInfo);
        return controls;
    }
    
    private VBox createConnectedDevicesSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label sectionTitle = new Label("Connected Devices");
        sectionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        sectionTitle.setTextFill(Color.web("#1e3c72"));
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");
        refreshBtn.setOnAction(e -> refreshConnectedDevices());
        
        headerBox.getChildren().addAll(sectionTitle, refreshBtn);
        
        // Create separate list for connected devices
        ObservableList<String> connectedDevices = FXCollections.observableArrayList();
        ListView<String> connectedDevicesList = new ListView<>();
        connectedDevicesList.setItems(connectedDevices);
        connectedDevicesList.setPrefHeight(120); // Smaller height since there are two sections
        connectedDevicesList.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 6;");
        
        Label devicesInfo;
        if (currentUser.getUserType() == User.UserType.SURVIVOR) {
            devicesInfo = new Label("Survivors cannot act as servers - this section remains empty");
        } else {
            devicesInfo = new Label("Devices currently connected to your server (when acting as local server)");
        }
        devicesInfo.setFont(Font.font("Segoe UI", 12));
        devicesInfo.setTextFill(Color.web("#666666"));
        
        // Initial population
        refreshConnectedDevicesList(connectedDevices);
        
        section.getChildren().addAll(headerBox, connectedDevicesList, devicesInfo);
        
        // Show/hide based on network status and user type
        boolean hasInternet = networkManager.hasInternetAccess();
        boolean canActAsServer = currentUser.getUserType() != User.UserType.SURVIVOR;
        section.setVisible(hasInternet && canActAsServer);
        section.setManaged(hasInternet && canActAsServer);
        
        return section;
    }
    
    private VBox createNearbyDevicesSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 4, 0, 0, 2);");
        
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        // For volunteers and survivors, this section is ALWAYS "Nearby Devices"
        // For authorities, use dynamic title
        String deviceSectionTitleText;
        String deviceSectionInfoText;
        
        if (currentUser.getUserType() == User.UserType.VOLUNTEER || currentUser.getUserType() == User.UserType.SURVIVOR) {
            deviceSectionTitleText = "Nearby Devices";
            deviceSectionInfoText = "Devices discovered within 300m range (mesh network)";
        } else {
            deviceSectionTitleText = getDeviceSectionTitle();
            deviceSectionInfoText = getDeviceSectionInfo();
        }
        
        deviceSectionTitle = new Label(deviceSectionTitleText);
        deviceSectionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        deviceSectionTitle.setTextFill(Color.web("#1e3c72"));
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16;");
        refreshBtn.setOnAction(e -> refreshNearbyDevices());
        
        headerBox.getChildren().addAll(deviceSectionTitle, refreshBtn);
        
        nearbyDevicesList = new ListView<>();
        nearbyDevicesList.setItems(nearbyDevices);
        nearbyDevicesList.setPrefHeight(150); // Reasonable height for scrolling
        nearbyDevicesList.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 6;");
        
        deviceSectionInfo = new Label(deviceSectionInfoText);
        deviceSectionInfo.setFont(Font.font("Segoe UI", 12));
        deviceSectionInfo.setTextFill(Color.web("#666666"));
        
        section.getChildren().addAll(headerBox, nearbyDevicesList, deviceSectionInfo);
        return section;
    }
    
    private String getDeviceSectionTitle() {
        if (currentUser.getUserType() == User.UserType.AUTHORITY) {
            // For authorities: show "Connected Devices" only when they have internet access
            boolean hasInternet = networkManager.hasInternetAccess();
            
            if (hasInternet) {
                return "Connected Devices";
            } else {
                return "Nearby Devices";
            }
        } else if (currentUser.getUserType() == User.UserType.VOLUNTEER) {
            // For volunteers: show "Connected Devices" when online (acting as server), "Nearby Devices" when in mesh
            boolean hasInternet = networkManager.hasInternetAccess();
            
            if (hasInternet) {
                return "Connected Devices"; // When volunteer acts as server
            } else {
                return "Nearby Devices"; // When volunteer is in mesh mode or disconnected
            }
        } else {
            // For survivors: always show "Nearby Devices"
            return "Nearby Devices";
        }
    }
    
    private String getDeviceSectionInfo() {
        if (currentUser.getUserType() == User.UserType.AUTHORITY) {
            // For authorities: show "Connected Devices" info only when they have internet access
            boolean hasInternet = networkManager.hasInternetAccess();
            
            if (hasInternet) {
                return "Devices currently connected to your server";
            } else {
                return "Devices discovered within 300m range";
            }
        } else if (currentUser.getUserType() == User.UserType.VOLUNTEER) {
            // For volunteers: different info based on their current network state
            boolean hasInternet = networkManager.hasInternetAccess();
            
            if (hasInternet) {
                return "Devices currently connected to your server";
            } else {
                return "Devices discovered within 300m range";
            }
        } else {
            // For survivors: always show "Nearby Devices"
            return "Devices discovered within 300m range";
        }
    }
    
    private VBox createErrorView() {
        VBox errorView = new VBox(20);
        errorView.setPadding(new Insets(20));
        errorView.setAlignment(Pos.CENTER);
        errorView.setStyle("-fx-background-color: #f8f9fa;");
        
        Label errorTitle = new Label("Offline Network Error");
        errorTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        errorTitle.setTextFill(Color.web("#dc3545"));
        
        Label errorMessage = new Label("Failed to initialize offline network view");
        errorMessage.setFont(Font.font("Segoe UI", 14));
        errorMessage.setTextFill(Color.web("#666666"));
        
        Button retryButton = new Button("Retry");
        ThemeManager.stylePrimaryButton(retryButton);
        retryButton.setOnAction(e -> {
            VBox newView = createOfflineNetworkView();
            if (mainContainer != null && mainContainer.getParent() instanceof Pane) {
                ((Pane) mainContainer.getParent()).getChildren().setAll(newView);
            }
        });
        
        errorView.getChildren().addAll(errorTitle, errorMessage, retryButton);
        return errorView;
    }
    
    // Event handlers
    
    private void onNetworkStatusChanged(NetworkConfig.NetworkMode mode, boolean isOnline, boolean hasInternet) {
        javafx.application.Platform.runLater(() -> {
            // Only update if the UI components have been created
            if (networkStatusLabel != null) {
                String statusText = networkManager.getNetworkStatusSummary();
                networkStatusLabel.setText(statusText);
            }
            
            // Update device section labels if they exist (but NOT for volunteers' or survivors' nearby section)
            if (deviceSectionTitle != null && deviceSectionInfo != null) {
                // For volunteers and survivors, the nearby devices section should NEVER change its title
                if (currentUser.getUserType() == User.UserType.AUTHORITY) {
                    deviceSectionTitle.setText(getDeviceSectionTitle());
                    deviceSectionInfo.setText(getDeviceSectionInfo());
                }
                // For volunteers and survivors, the nearby section always stays "Nearby Devices"
                // Only the connected devices section (separate) shows/hides
            }
            
            // For volunteers and survivors, show/hide connected devices section based on internet status and user type
            if ((currentUser.getUserType() == User.UserType.VOLUNTEER || currentUser.getUserType() == User.UserType.SURVIVOR) 
                && connectedDevicesSection != null) {
                boolean showConnectedSection = hasInternet && currentUser.getUserType() != User.UserType.SURVIVOR;
                connectedDevicesSection.setVisible(showConnectedSection);
                connectedDevicesSection.setManaged(showConnectedSection);
            }
            
            // Update status indicator colors
            updateStatusIndicators(mode);
            
            // Refresh the device list to update empty state message (only if UI is created)
            if (nearbyDevices != null) {
                refreshNearbyDevices();
            }
        });
    }
    
    private void updateStatusIndicators(NetworkConfig.NetworkMode mode) {
        // This would update the visual indicators based on current mode
        // Implementation would involve styling the indicator boxes
    }
    
    private void refreshNetworkInfo() {
        networkStatusLabel.setText(networkManager.getNetworkStatusSummary());
        refreshNearbyDevices();
    }
    
    private void refreshNearbyDevices() {
        nearbyDevices.clear();
        
        // Get nearby devices from peer discovery
        PeerDiscoveryManager peerDiscovery = new PeerDiscoveryManager();
        Set<PeerDiscoveryManager.PeerInfo> peers = peerDiscovery.getDiscoveredPeers();
        
        for (PeerDiscoveryManager.PeerInfo peer : peers) {
            String deviceInfo = String.format("%s - %s:%d %s", 
                peer.getName(), peer.getAddress(), peer.getPort(),
                peer.isServer() ? "(Server)" : "(Client)");
            nearbyDevices.add(deviceInfo);
        }
        
        if (nearbyDevices.isEmpty()) {
            // Different empty state message based on user type and internet connectivity
            if (currentUser.getUserType() == User.UserType.AUTHORITY) {
                boolean hasInternet = networkManager.hasInternetAccess();
                
                if (hasInternet) {
                    nearbyDevices.add("No connected devices found");
                } else {
                    nearbyDevices.add("No nearby devices found");
                }
            } else if (currentUser.getUserType() == User.UserType.VOLUNTEER || currentUser.getUserType() == User.UserType.SURVIVOR) {
                // Volunteers and Survivors always show nearby devices for this section
                nearbyDevices.add("No nearby devices found");
            }
        }
    }
    
    private void toggleServerMode(boolean enable) {
        if (enable) {
            // Start server mode
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Server Mode");
            alert.setHeaderText("Starting Local Server");
            alert.setContentText("Your device is now acting as a local communication server for nearby ReliefNet devices.");
            alert.show();
        } else {
            // Stop server mode
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Server Mode");
            alert.setHeaderText("Stopping Local Server");
            alert.setContentText("Local server mode disabled.");
            alert.show();
        }
    }
    
    private void refreshConnectedDevices() {
        // This would refresh both connected and nearby devices for volunteers
        refreshNetworkInfo();
    }
    
    private void refreshConnectedDevicesList(ObservableList<String> connectedDevices) {
        connectedDevices.clear();
        
        // For volunteers acting as servers, get connected clients
        boolean hasInternet = networkManager.hasInternetAccess();
        if (hasInternet) {
            // TODO: Get actual connected devices from server
            // For now, simulate some connected devices when online
            // In a real implementation, this would query the WebSocket server for connected clients
            
            if (connectedDevices.isEmpty()) {
                connectedDevices.add("No connected devices found");
            }
        }
    }
    
    /**
     * Cleanup method to remove network listeners and prevent memory leaks
     */
    public void cleanup() {
        try {
            // Remove this view's network status listener
            if (networkManager != null) {
                networkManager.removeNetworkStatusListener(this::onNetworkStatusChanged);
            }
            System.out.println("OfflineNetworkView cleanup completed");
        } catch (Exception e) {
            System.err.println("Error during OfflineNetworkView cleanup: " + e.getMessage());
        }
    }
}
