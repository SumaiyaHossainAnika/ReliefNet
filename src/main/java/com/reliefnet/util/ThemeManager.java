package com.reliefnet.util;

import javafx.scene.Scene;

/**
 * ThemeManager - Manages consistent styling across the ReliefNet application
 * Uses the blue color scheme from the splash screen
 */
public class ThemeManager {
    
    // Primary Colors (from splash screen)
    public static final String PRIMARY_DARK = "#1e3c72";
    public static final String PRIMARY_MEDIUM = "#2a5298";
    public static final String PRIMARY_LIGHT = "#3498db";
    public static final String PRIMARY_LIGHTEST = "#E8F4FD";
    public static final String PRIMARY_LIGHT_BLUE = "#B8D4F1";
    
    // Semantic Colors
    public static final String SUCCESS_COLOR = "#28a745";
    public static final String WARNING_COLOR = "#ffc107";
    public static final String DANGER_COLOR = "#dc3545";
    public static final String INFO_COLOR = "#17a2b8";
    
    // Text Colors
    public static final String TEXT_PRIMARY = "#1e3c72";
    public static final String TEXT_SECONDARY = "#5A5A5A";
    public static final String TEXT_MUTED = "#6c757d";
    public static final String TEXT_LIGHT = "#ffffff";
    
    // Background Colors
    public static final String BG_PRIMARY = "#ffffff";
    public static final String BG_SECONDARY = "#f8f9fa";
    public static final String BG_LIGHT = "#E8F4FD";
    
    // CSS Style Constants
    public static final String MAIN_CSS = 
        "/* Root Variables */\n" +
        ".root {\n" +
        "    -fx-primary-dark: #1e3c72;\n" +
        "    -fx-primary-medium: #2a5298;\n" +
        "    -fx-primary-light: #3498db;\n" +
        "    -fx-primary-lightest: #E8F4FD;\n" +
        "    -fx-success: #28a745;\n" +
        "    -fx-warning: #ffc107;\n" +
        "    -fx-danger: #dc3545;\n" +
        "    -fx-info: #17a2b8;\n" +
        "}\n" +
        "\n" +
        "/* Card Style */\n" +
        ".card {\n" +
        "    -fx-background-color: white;\n" +
        "    -fx-background-radius: 12;\n" +
        "    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);\n" +
        "    -fx-padding: 20;\n" +
        "}\n" +
        "\n" +
        "/* Primary Button */\n" +
        ".btn-primary {\n" +
        "    -fx-background-color: linear-gradient(to bottom, #2a5298, #1e3c72);\n" +
        "    -fx-text-fill: white;\n" +
        "    -fx-background-radius: 8;\n" +
        "    -fx-padding: 12 24;\n" +
        "    -fx-font-size: 14px;\n" +
        "    -fx-font-weight: bold;\n" +
        "    -fx-cursor: hand;\n" +
        "}\n";
    
    public static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        scene.getRoot().setStyle(MAIN_CSS);
    }
    
    public static String getPrimaryGradient() {
        return "linear-gradient(to bottom, " + PRIMARY_MEDIUM + ", " + PRIMARY_DARK + ")";
    }
    
    public static String getCardStyle() {
        return "-fx-background-color: white; " +
               "-fx-background-radius: 12; " +
               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2); " +
               "-fx-padding: 20;";
    }
    
    public static String getButtonPrimaryStyle() {
        return "-fx-background-color: linear-gradient(to bottom, " + PRIMARY_MEDIUM + ", " + PRIMARY_DARK + "); " +
               "-fx-text-fill: white; " +
               "-fx-background-radius: 8; " +
               "-fx-padding: 12 24; " +
               "-fx-font-size: 14px; " +
               "-fx-font-weight: bold; " +
               "-fx-cursor: hand; " +
               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);";
    }
    
    public static void styleTextField(javafx.scene.control.TextField textField) {
        textField.setStyle("-fx-background-color: white; " +
                          "-fx-border-color: #ddd; " +
                          "-fx-border-radius: 6; " +
                          "-fx-background-radius: 6; " +
                          "-fx-padding: 8 12; " +
                          "-fx-font-size: 14px;");
    }
    
    public static void styleTextArea(javafx.scene.control.TextArea textArea) {
        textArea.setStyle("-fx-background-color: white; " +
                         "-fx-border-color: #ddd; " +
                         "-fx-border-radius: 6; " +
                         "-fx-background-radius: 6; " +
                         "-fx-padding: 8 12; " +
                         "-fx-font-size: 14px; " +
                         "-fx-font-family: 'Segoe UI';");
    }
    
    public static void styleComboBox(javafx.scene.control.ComboBox<?> comboBox) {
        comboBox.setStyle("-fx-background-color: white; " +
                         "-fx-border-color: #ddd; " +
                         "-fx-border-radius: 6; " +
                         "-fx-background-radius: 6; " +
                         "-fx-padding: 8 12; " +
                         "-fx-font-size: 14px;");
    }
    
    public static void stylePrimaryButton(javafx.scene.control.Button button) {
        button.setStyle("-fx-background-color: linear-gradient(to bottom, " + PRIMARY_MEDIUM + ", " + PRIMARY_DARK + "); " +
                       "-fx-text-fill: white; " +
                       "-fx-background-radius: 6; " +
                       "-fx-padding: 8 16; " +
                       "-fx-font-size: 14px; " +
                       "-fx-font-weight: bold; " +
                       "-fx-cursor: hand;");
    }
    
    public static void styleSecondaryButton(javafx.scene.control.Button button) {
        button.setStyle("-fx-background-color: transparent; " +
                       "-fx-border-color: " + PRIMARY_MEDIUM + "; " +
                       "-fx-border-width: 2; " +
                       "-fx-text-fill: " + PRIMARY_MEDIUM + "; " +
                       "-fx-background-radius: 6; " +
                       "-fx-border-radius: 6; " +
                       "-fx-padding: 8 16; " +
                       "-fx-font-size: 14px; " +
                       "-fx-font-weight: bold; " +
                       "-fx-cursor: hand;");
    }
}