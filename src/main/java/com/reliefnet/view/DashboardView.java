package com.reliefnet.view;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import com.reliefnet.util.ThemeManager;
import com.reliefnet.database.DatabaseManager;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * DashboardView - Main dashboard showing system overview and key metrics
 */
public class DashboardView {    // Dashboard data
    private int activeEmergencies = 0;
    private int criticalEmergencies = 0;
    private int availableResources = 0;
    private int activeVolunteers = 0;
    private int onDutyVolunteers = 0;
    private int areasCovered = 0; // Will be loaded from database
    private List<ActivityItem> recentActivities = new ArrayList<>();
    
    // References to metric labels for real-time updates
    private Text emergencyValueLabel;
    private Text emergencySubtitleLabel;
    private Text resourceValueLabel;
    private Text resourceSubtitleLabel;
    private Text volunteerValueLabel;
    private Text volunteerSubtitleLabel;
    private Text coverageValueLabel;
    private Text coverageSubtitleLabel;
    
    // Activity item class
    private static class ActivityItem {
        String icon, message, time, color;
        
        ActivityItem(String icon, String message, String time, String color) {
            this.icon = icon;
            this.message = message;
            this.time = time;
            this.color = color;
        }
    }    public VBox createView() {
        // Load real data from database
        loadDashboardData();
        
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        
        // Header
        VBox header = createHeader();
        
        // Key metrics cards
        HBox metricsRow = createMetricsCards();
        
        // Charts and activity sections
        HBox chartsRow = createChartsSection();
        
        // Recent activity
        VBox activitySection = createActivitySection();
        
        mainContainer.getChildren().addAll(header, metricsRow, chartsRow, activitySection);
        
        return mainContainer;
    }    private VBox createHeader() {
        VBox header = new VBox(10);
        
        Text title = new Text("Relief Operations Dashboard");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        Text subtitle = new Text("Real-time overview of disaster relief operations across Bangladesh");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        header.getChildren().addAll(title, subtitle);        return header;
    }
      private void loadDashboardData() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
              // Load active emergencies from both emergency_requests and sos_alerts
            String emergencyQuery = "SELECT SUM(total) as total_emergencies, SUM(critical) as critical_emergencies FROM (" +
                                  "SELECT COUNT(*) as total, " +
                                  "SUM(CASE WHEN priority = 'CRITICAL' THEN 1 ELSE 0 END) as critical " +
                                  "FROM emergency_requests WHERE status = 'PENDING' OR status = 'IN_PROGRESS' " +
                                  "UNION ALL " +
                                  "SELECT COUNT(*) as total, " +
                                  "SUM(CASE WHEN urgency_level = 'CRITICAL' THEN 1 ELSE 0 END) as critical " +
                                  "FROM sos_alerts WHERE status = 'ACTIVE' OR status = 'RESPONDED'" +
                                  ")";
            try (ResultSet rs = dbManager.executeQuery(emergencyQuery)) {
                if (rs.next()) {
                    activeEmergencies = rs.getInt("total_emergencies");
                    criticalEmergencies = rs.getInt("critical_emergencies");
                }
            }            // Load total resources (matching Resource Management window count - only AVAILABLE resources)
            String resourceQuery = "SELECT COUNT(*) as total FROM resources WHERE status = 'AVAILABLE'";
            try (ResultSet rs = dbManager.executeQuery(resourceQuery)) {
                if (rs.next()) {
                    availableResources = rs.getInt("total");
                }
            }// Load active volunteers (only approved volunteers, not pending) - must match VolunteerView logic
            String volunteerQuery = "SELECT COUNT(*) as total, " +
                                  "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as on_duty " +
                                  "FROM users WHERE user_type = 'VOLUNTEER' AND status IN ('ACTIVE', 'INACTIVE', 'ASSIGNED') " +
                                  "AND ((location IS NOT NULL AND location != '') OR (location_name IS NOT NULL AND location_name != ''))";
            try (ResultSet rs = dbManager.executeQuery(volunteerQuery)) {
                if (rs.next()) {
                    activeVolunteers = rs.getInt("total");
                    onDutyVolunteers = rs.getInt("on_duty");
                }
            }            // Load areas covered from districts in population_data table
            String areaQuery = "SELECT COUNT(DISTINCT district) as total_districts FROM population_data";
            try (ResultSet rs = dbManager.executeQuery(areaQuery)) {
                if (rs.next()) {
                    areasCovered = rs.getInt("total_districts");
                    System.out.println("Areas covered from database: " + areasCovered);
                } else {
                    System.out.println("No districts found in population_data table");
                    areasCovered = 0;
                }
            }
            
            // Verify the database connection and data
            try (ResultSet rs = dbManager.executeQuery("SELECT district FROM population_data")) {
                System.out.println("Districts in database:");
                while (rs.next()) {
                    System.out.println(" - " + rs.getString("district"));
                }
            }
            
            // Load recent activities (emergency requests, resource updates, user registrations)
            loadRecentActivities(dbManager);
            
        } catch (Exception e) {
            System.err.println("Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
        }
    }
      private void loadRecentActivities(DatabaseManager dbManager) {
        recentActivities.clear();
          try {
            // Get recent emergency requests and SOS alerts combined
            String emergencyQuery = "SELECT 'EMERGENCY' as type, description, created_at, priority as urgency " +
                                  "FROM emergency_requests " +
                                  "UNION ALL " +
                                  "SELECT 'SOS' as type, description, created_at, urgency_level as urgency " +
                                  "FROM sos_alerts " +
                                  "ORDER BY created_at DESC LIMIT 5";
            try (ResultSet rs = dbManager.executeQuery(emergencyQuery)) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    String description = rs.getString("description");
                    String time = getTimeAgo(rs.getString("created_at"));
                    String urgency = rs.getString("urgency");
                    String color = "CRITICAL".equals(urgency) ? ThemeManager.DANGER_COLOR : ThemeManager.WARNING_COLOR;
                    
                    String icon = "SOS".equals(type) ? "🆘" : "🚨";
                    String prefix = "SOS".equals(type) ? "SOS Alert: " : "Emergency: ";
                    
                    recentActivities.add(new ActivityItem(icon, 
                        prefix + description, time, color));
                }
            }
            
            // Get recent user registrations
            String userQuery = "SELECT name, user_type, created_at FROM users " +
                             "ORDER BY created_at DESC LIMIT 3";
            try (ResultSet rs = dbManager.executeQuery(userQuery)) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String userType = rs.getString("user_type");
                    String time = getTimeAgo(rs.getString("created_at"));
                    
                    String icon = "VOLUNTEER".equals(userType) ? "👥" : "🆔";
                    recentActivities.add(new ActivityItem(icon, 
                        name + " registered as " + userType.toLowerCase(), time, ThemeManager.SUCCESS_COLOR));
                }
            }
              // Get recent resource updates
            String resourceQuery = "SELECT name, quantity, unit, updated_at FROM resources " +
                                 "ORDER BY updated_at DESC LIMIT 3";
            try (ResultSet rs = dbManager.executeQuery(resourceQuery)) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int quantity = rs.getInt("quantity");
                    String unit = rs.getString("unit");
                    String time = getTimeAgo(rs.getString("updated_at"));
                    
                    recentActivities.add(new ActivityItem("📦", 
                        quantity + " " + unit + " of " + name + " updated", 
                        time, ThemeManager.INFO_COLOR));
                }
            }
            
            // Get activities from the new activities table (logged by ResourceView)
            String activityQuery = "SELECT activity_type, description, timestamp FROM activities " +
                                 "ORDER BY timestamp DESC LIMIT 5";
            try (ResultSet rs = dbManager.executeQuery(activityQuery)) {
                while (rs.next()) {
                    String activityType = rs.getString("activity_type");
                    String description = rs.getString("description");
                    String time = getTimeAgo(rs.getString("timestamp"));
                    
                    String icon = "📦";
                    String color = ThemeManager.SUCCESS_COLOR;
                    
                    // Set icon and color based on activity type
                    switch (activityType) {
                        case "RESOURCE_ADDED":
                            icon = "➕";
                            color = ThemeManager.SUCCESS_COLOR;
                            break;
                        case "RESOURCE_UPDATED":
                            icon = "✏️";
                            color = ThemeManager.INFO_COLOR;
                            break;
                        case "RESOURCE_DISTRIBUTED":
                            icon = "🚚";
                            color = ThemeManager.WARNING_COLOR;
                            break;
                        default:
                            icon = "📦";
                            color = ThemeManager.INFO_COLOR;
                    }
                    
                    recentActivities.add(new ActivityItem(icon, description, time, color));
                }
            } catch (Exception e) {
                // Ignore if activities table doesn't exist yet
                System.out.println("Activities table not found (will be created when first activity is logged)");
            }
            
            // Sort all activities by recency (doesn't matter for now since they're added in order)
            
        } catch (Exception e) {
            System.err.println("Error loading recent activities: " + e.getMessage());
        }
    }
    
    private String getTimeAgo(String timestamp) {
        try {
            // Simple time calculation - in a real app you'd use proper date parsing
            return "Recently";
        } catch (Exception e) {
            return "Unknown time";
        }
    }    private HBox createMetricsCards() {
        HBox metricsRow = new HBox(20);
        metricsRow.setAlignment(Pos.CENTER);
        
        // Emergency Requests Card - use real data
        String emergencyValue = String.valueOf(activeEmergencies);
        String emergencySubtitle = criticalEmergencies + " Critical";
        VBox emergencyCard = createMetricCard("🚨", "Active Emergencies", emergencyValue, emergencySubtitle, ThemeManager.DANGER_COLOR, "emergency");
          // Resources Card - use real data (total resources count)
        String resourceValue = String.valueOf(availableResources);
        String resourceSubtitle = availableResources > 0 ? "Total Resources" : "No Resources";
        VBox resourceCard = createMetricCard("📦", "Total Resources", resourceValue, resourceSubtitle, ThemeManager.SUCCESS_COLOR, "resource");
        
        // Volunteers Card - use real data
        String volunteerValue = String.valueOf(activeVolunteers);
        String volunteerSubtitle = onDutyVolunteers + " On Duty";
        VBox volunteerCard = createMetricCard("👥", "Active Volunteers", volunteerValue, volunteerSubtitle, ThemeManager.INFO_COLOR, "volunteer");
          // Areas Covered Card - now uses data from database
        VBox coverageCard = createMetricCard("🌍", "Areas Covered", String.valueOf(areasCovered), 
            areasCovered + " Districts", ThemeManager.PRIMARY_MEDIUM, "coverage");
        
        metricsRow.getChildren().addAll(emergencyCard, resourceCard, volunteerCard, coverageCard);
        
        return metricsRow;
    }    private VBox createMetricCard(String icon, String title, String value, String subtitle, String accentColor, String type) {
        VBox card = new VBox(15);
        card.setPrefSize(280, 140);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle(ThemeManager.getCardStyle() + 
                     "-fx-border-color: " + accentColor + "; " +
                     "-fx-border-width: 0 0 4 0;");
        
        // Icon and title row
        HBox iconRow = new HBox(15);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        
        Text iconText = new Text(icon);
        iconText.setFont(Font.font("Arial", FontWeight.NORMAL, 32));
        
        VBox titleSection = new VBox(5);
        
        Text titleText = new Text(title);
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        titleText.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
        
        Text valueText = new Text(value);
        valueText.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        valueText.setFill(Color.web(accentColor));
        
        titleSection.getChildren().addAll(titleText, valueText);
        iconRow.getChildren().addAll(iconText, titleSection);
        
        // Subtitle
        Text subtitleText = new Text(subtitle);
        subtitleText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        subtitleText.setFill(Color.web(ThemeManager.TEXT_MUTED));
        
        // Store references to labels for updating
        switch (type) {
            case "emergency":
                emergencyValueLabel = valueText;
                emergencySubtitleLabel = subtitleText;
                break;
            case "resource":
                resourceValueLabel = valueText;
                resourceSubtitleLabel = subtitleText;
                break;
            case "volunteer":
                volunteerValueLabel = valueText;
                volunteerSubtitleLabel = subtitleText;
                break;
            case "coverage":
                coverageValueLabel = valueText;
                coverageSubtitleLabel = subtitleText;
                break;
        }        
        card.getChildren().addAll(iconRow, subtitleText);
        
        return card;
    }

    private HBox createChartsSection() {
        HBox chartsRow = new HBox(20);
        
        // Emergency Response Chart
        VBox emergencyChartCard = createEmergencyChart();
        
        // Resource Distribution Chart
        VBox resourceChartCard = createResourceChart();
        
        chartsRow.getChildren().addAll(emergencyChartCard, resourceChartCard);
        
        return chartsRow;
    }
      private VBox createEmergencyChart() {
        VBox chartCard = new VBox(15);
        chartCard.setPrefWidth(500);
        chartCard.setPadding(new Insets(20));
        chartCard.setStyle(ThemeManager.getCardStyle());
        
        // Card header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Text title = new Text("Emergency Response Timeline");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        header.getChildren().add(title);
        
        // Line chart with real data
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Last 7 Days");
        lineChart.setPrefHeight(250);
        
        // Load real emergency data for the last 7 days
        Map<String, Integer> emergencyData = loadEmergencyTimelineData();
        Map<String, Integer> resolvedData = loadResolvedTimelineData();
        
        XYChart.Series<String, Number> emergencySeries = new XYChart.Series<>();
        emergencySeries.setName("Emergency Requests");
        
        XYChart.Series<String, Number> resolvedSeries = new XYChart.Series<>();
        resolvedSeries.setName("Resolved");
        
        // Get last 7 days
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E"); // Mon, Tue, etc.
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String day = date.format(formatter);
            String dateStr = date.toString();
            
            int emergencyCount = emergencyData.getOrDefault(dateStr, 0);
            int resolvedCount = resolvedData.getOrDefault(dateStr, 0);
            
            emergencySeries.getData().add(new XYChart.Data<>(day, emergencyCount));
            resolvedSeries.getData().add(new XYChart.Data<>(day, resolvedCount));
        }
        
        lineChart.getData().add(emergencySeries);
        lineChart.getData().add(resolvedSeries);
        
        chartCard.getChildren().addAll(header, lineChart);
        
        return chartCard;
    }    private Map<String, Integer> loadEmergencyTimelineData() {
        Map<String, Integer> data = new HashMap<>();
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Count from both emergency_requests and sos_alerts tables
            String query = "SELECT date, SUM(count) as total_count FROM (" +
                          "SELECT DATE(created_at) as date, COUNT(*) as count " +
                          "FROM emergency_requests " +
                          "WHERE created_at >= date('now', '-7 days') " +
                          "GROUP BY DATE(created_at) " +
                          "UNION ALL " +
                          "SELECT DATE(created_at) as date, COUNT(*) as count " +
                          "FROM sos_alerts " +
                          "WHERE created_at >= date('now', '-7 days') " +
                          "GROUP BY DATE(created_at)" +
                          ") GROUP BY date";
            
            try (ResultSet rs = dbManager.executeQuery(query)) {
                while (rs.next()) {
                    data.put(rs.getString("date"), rs.getInt("total_count"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading emergency timeline data: " + e.getMessage());
        }
        return data;
    }    private Map<String, Integer> loadResolvedTimelineData() {
        Map<String, Integer> data = new HashMap<>();
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Count COMPLETED and RESOLVED from both emergency_requests and sos_alerts tables
            String query = "SELECT date, SUM(count) as total_count FROM (" +
                          "SELECT DATE(updated_at) as date, COUNT(*) as count " +
                          "FROM emergency_requests " +
                          "WHERE (status = 'COMPLETED' OR status = 'RESOLVED') AND updated_at >= date('now', '-7 days') " +
                          "GROUP BY DATE(updated_at) " +
                          "UNION ALL " +
                          "SELECT DATE(updated_at) as date, COUNT(*) as count " +
                          "FROM sos_alerts " +
                          "WHERE (status = 'COMPLETED' OR status = 'RESOLVED') AND updated_at >= date('now', '-7 days') " +
                          "GROUP BY DATE(updated_at)" +
                          ") GROUP BY date";
            
            try (ResultSet rs = dbManager.executeQuery(query)) {
                while (rs.next()) {
                    data.put(rs.getString("date"), rs.getInt("total_count"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading resolved timeline data: " + e.getMessage());
        }
        return data;
    }    private VBox createResourceChart() {
        VBox chartCard = new VBox(15);
        chartCard.setPrefWidth(520); // Slightly increased width to fit legend text
        chartCard.setMaxWidth(520);
        chartCard.setPadding(new Insets(20));
        chartCard.setStyle(ThemeManager.getCardStyle());
          // Card header
        Text title = new Text("Resources by Category");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        // Pie chart with real data (keeping original pretty styling)
        PieChart pieChart = new PieChart();
        pieChart.setPrefHeight(250);
          // Load real resource data by category
        Map<String, Integer> resourceData = loadResourceDistributionData();
        
        if (resourceData.isEmpty()) {
            // Show message when no resources are available
            VBox noDataMessage = new VBox(10);
            noDataMessage.setAlignment(Pos.CENTER);
            noDataMessage.setPrefHeight(250);
            
            Text noDataText = new Text("No resource data available");
            noDataText.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
            noDataText.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
            
            Text subText = new Text("Resources will appear here once they are added to the system");
            subText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            subText.setFill(Color.web(ThemeManager.TEXT_MUTED));
            
            noDataMessage.getChildren().addAll(noDataText, subText);
            chartCard.getChildren().addAll(title, noDataMessage);        } else {
            // Populate pie chart with real data (original pretty version)
            for (Map.Entry<String, Integer> entry : resourceData.entrySet()) {
                if (entry.getValue() > 0) {
                    pieChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
            }
              // Position legend at bottom to arrange items side by side
            pieChart.setLegendSide(Side.BOTTOM);
            
            // Apply CSS styling for smaller font and better layout
            pieChart.setStyle("-fx-font-size: 10px; -fx-font-family: Arial;");
            
            chartCard.getChildren().addAll(title, pieChart);
        }
        
        return chartCard;
    }private Map<String, Integer> loadResourceDistributionData() {
        Map<String, Integer> data = new HashMap<>();
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            // Show only AVAILABLE resources by category (matches dashboard total and ResourceView available table)
            String query = "SELECT category, COUNT(*) as count " +
                          "FROM resources WHERE status = 'AVAILABLE' " +
                          "GROUP BY category";
            
            try (ResultSet rs = dbManager.executeQuery(query)) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    int count = rs.getInt("count");
                    data.put(category, count);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading resource distribution data: " + e.getMessage());
        }
        return data;
    }
      private VBox createActivitySection() {
        VBox activitySection = new VBox(15);
        activitySection.setPadding(new Insets(20));
        activitySection.setStyle(ThemeManager.getCardStyle());
        
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Text title = new Text("Recent Activity");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setFill(Color.web(ThemeManager.PRIMARY_DARK));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button viewAllBtn = new Button("View All");
        viewAllBtn.setStyle("-fx-background-color: transparent; " +
                           "-fx-text-fill: " + ThemeManager.PRIMARY_MEDIUM + "; " +
                           "-fx-cursor: hand;");
        
        // Implement View All button functionality
        viewAllBtn.setOnAction(e -> showAllActivities());
        
        header.getChildren().addAll(title, spacer, viewAllBtn);
        
        // Activity list with real data
        VBox activityList = new VBox(10);
        
        if (recentActivities.isEmpty()) {
            // Show message when no activities
            VBox noActivityMessage = new VBox(10);
            noActivityMessage.setAlignment(Pos.CENTER);
            noActivityMessage.setPadding(new Insets(20));
            
            Text noActivityText = new Text("No recent activity");
            noActivityText.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
            noActivityText.setFill(Color.web(ThemeManager.TEXT_SECONDARY));
            
            Text subText = new Text("Activities will appear here as users interact with the system");
            subText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            subText.setFill(Color.web(ThemeManager.TEXT_MUTED));
            
            noActivityMessage.getChildren().addAll(noActivityText, subText);
            activityList.getChildren().add(noActivityMessage);        } else {
            // Show actual activities (limit to 8 for dashboard, increased from 5)
            int limit = Math.min(recentActivities.size(), 8);
            for (int i = 0; i < limit; i++) {
                ActivityItem activity = recentActivities.get(i);
                activityList.getChildren().add(
                    createActivityItem(activity.icon, activity.message, activity.time, activity.color)
                );
            }
            
            // If we have many activities, add a "more" indicator
            if (recentActivities.size() > 8) {
                Text moreActivities = new Text("+ " + (recentActivities.size() - 8) + " more activities...");
                moreActivities.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
                moreActivities.setFill(Color.web(ThemeManager.TEXT_MUTED));
                
                HBox moreContainer = new HBox();
                moreContainer.setAlignment(Pos.CENTER);
                moreContainer.setPadding(new Insets(5, 0, 0, 0));
                moreContainer.getChildren().add(moreActivities);
                
                activityList.getChildren().add(moreContainer);
            }
        }
        
        activitySection.getChildren().addAll(header, new Separator(), activityList);
        
        return activitySection;
    }
    
    private void showAllActivities() {
        // Create a new window or dialog to show all activities
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("All Recent Activities");
        alert.setHeaderText("Complete Activity Log");
        
        StringBuilder content = new StringBuilder();
        if (recentActivities.isEmpty()) {
            content.append("No activities recorded yet.\n\n");
            content.append("Activities will appear here as:\n");
            content.append("• Emergency requests are submitted\n");
            content.append("• Users register in the system\n");
            content.append("• Resources are added or distributed\n");
            content.append("• Volunteers are assigned to tasks");
        } else {
            content.append("Recent Activities (").append(recentActivities.size()).append(" total):\n\n");
            for (int i = 0; i < recentActivities.size(); i++) {
                ActivityItem activity = recentActivities.get(i);
                content.append((i + 1)).append(". ")
                       .append(activity.message)
                       .append(" (").append(activity.time).append(")\n");
            }
        }
        
        alert.setContentText(content.toString());
        
        // Expand dialog size
        alert.getDialogPane().setPrefSize(600, 400);
        alert.setResizable(true);
        
        alert.showAndWait();
    }
    
    private HBox createActivityItem(String icon, String message, String time, String color) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-radius: 8; " +
                     "-fx-background-color: " + ThemeManager.BG_LIGHT + ";");
        
        // Icon
        Text iconText = new Text(icon);
        iconText.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        
        // Message and time
        VBox messageSection = new VBox(2);
        
        Text messageText = new Text(message);
        messageText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        messageText.setFill(Color.web(ThemeManager.TEXT_PRIMARY));
        
        Text timeText = new Text(time);
        timeText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        timeText.setFill(Color.web(ThemeManager.TEXT_MUTED));
        
        messageSection.getChildren().addAll(messageText, timeText);
        
        // Status indicator
        Circle statusDot = new Circle(4);
        statusDot.setFill(Color.web(color));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        item.getChildren().addAll(iconText, messageSection, spacer, statusDot);
        
        return item;
    }
      /**
     * Refresh dashboard data and update UI
     */    public void refreshDashboard() {
        System.out.println("Refreshing dashboard data...");
        loadDashboardData();
        System.out.println("Dashboard data refreshed - Available resources: " + availableResources);
        
        // Update the metric labels if they exist
        if (emergencyValueLabel != null) {
            emergencyValueLabel.setText(String.valueOf(activeEmergencies));
            emergencySubtitleLabel.setText(criticalEmergencies + " Critical");
        }
          if (resourceValueLabel != null) {
            resourceValueLabel.setText(String.valueOf(availableResources));
            resourceSubtitleLabel.setText(availableResources > 0 ? "Total Resources" : "No Resources");
        }
        
        if (volunteerValueLabel != null) {
            volunteerValueLabel.setText(String.valueOf(activeVolunteers));
            volunteerSubtitleLabel.setText(onDutyVolunteers + " On Duty");
        }
        
        if (coverageValueLabel != null) {
            coverageValueLabel.setText(String.valueOf(areasCovered));
            coverageSubtitleLabel.setText(areasCovered + " Districts");
        }
        
        // Refresh recent activities
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            loadRecentActivities(dbManager);
            System.out.println("Recent activities refreshed");
        } catch (Exception e) {
            System.err.println("Error refreshing recent activities: " + e.getMessage());
        }
        
        System.out.println("Dashboard UI labels updated successfully");
    }
}
