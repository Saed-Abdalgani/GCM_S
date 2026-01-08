package client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;

/**
 * Controller for the Login page.
 * Handles username and password validation with min/max length restrictions.
 * Supports navigation to main page after successful login.
 */
public class LoginController implements GCMClient.MessageHandler {

    // Validation constants - easily adjustable
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final int PASSWORD_MIN_LENGTH = 4;
    private static final int PASSWORD_MAX_LENGTH = 30;

    // User roles
    public enum UserRole {
        ANONYMOUS, CUSTOMER, EMPLOYEE, MANAGER, SUPPORT_AGENT
    }

    // Current logged in user info (static to share across controllers)
    public static String currentUsername;
    public static UserRole currentUserRole;
    public static boolean isSubscribed;
    public static String currentSessionToken; // Session token for authenticated requests
    public static int currentUserId; // User ID for database operations

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label usernameErrorLabel;
    @FXML
    private Label passwordErrorLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button guestButton;

    // Client instance
    private GCMClient client;

    @FXML
    public void initialize() {
        // Add listeners for real-time validation feedback
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateUsername(newValue);
        });

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validatePassword(newValue);
        });

        // Initialize client connection
        try {
            client = GCMClient.getInstance();
            client.setMessageHandler(this);
            statusLabel.setText("Connected to server");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to server");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            loginButton.setDisable(true);
            guestButton.setDisable(true);
        }
    }

    // ... Validation Methods (unchanged) ...
    private boolean validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            usernameErrorLabel.setText("Username is required");
            usernameErrorLabel.setStyle("-fx-text-fill: #e74c3c;");
            return false;
        }
        if (username.length() < USERNAME_MIN_LENGTH) {
            usernameErrorLabel.setText("Min " + USERNAME_MIN_LENGTH + " chars");
            usernameErrorLabel.setStyle("-fx-text-fill: #e74c3c;");
            return false;
        }
        usernameErrorLabel.setText("✓");
        usernameErrorLabel.setStyle("-fx-text-fill: #27ae60;");
        return true;
    }

    private boolean validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            passwordErrorLabel.setText("Password is required");
            passwordErrorLabel.setStyle("-fx-text-fill: #e74c3c;");
            return false;
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            passwordErrorLabel.setText("Min " + PASSWORD_MIN_LENGTH + " chars");
            passwordErrorLabel.setStyle("-fx-text-fill: #e74c3c;");
            return false;
        }
        passwordErrorLabel.setText("✓");
        passwordErrorLabel.setStyle("-fx-text-fill: #27ae60;");
        return true;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (!validateUsername(username) || !validatePassword(password)) {
            statusLabel.setText("Fix errors above");
            return;
        }

        statusLabel.setText("Logging in...");
        statusLabel.setStyle("-fx-text-fill: #3498db;");

        // Send login request via singleton client
        try {
            if (client == null)
                client = GCMClient.getInstance();
            client.setMessageHandler(this);
            client.sendToServer("login " + username + " " + password);
        } catch (IOException e) {
            statusLabel.setText("Connection lost");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGuestLogin() {
        statusLabel.setText("Entering as guest...");

        // Ensure client is connected (anonymous)
        try {
            if (client == null)
                client = GCMClient.getInstance();
            client.setMessageHandler(this);
            // No login command needed, just set local state
            currentUsername = "Guest";
            currentUserRole = UserRole.ANONYMOUS;
            isSubscribed = false;

            navigateToMainPage();
        } catch (IOException e) {
            statusLabel.setText("Connection failed");
        }
    }

    @Override
    public void displayMessage(Object msg) {
        javafx.application.Platform.runLater(() -> {
            // Handle legacy string responses
            if (msg instanceof String) {
                String responseStr = (String) msg;
                if (responseStr.startsWith("login_success")) {
                    handleLoginSuccessLegacy(responseStr);
                } else if (responseStr.equals("login_failed")) {
                    statusLabel.setText("Invalid credentials");
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            }
            // Handle new Response protocol
            else if (msg instanceof common.Response) {
                common.Response response = (common.Response) msg;
                if (response.getRequestType() == common.MessageType.LOGIN) {
                    if (response.isOk() && response.getPayload() instanceof common.dto.LoginResponse) {
                        handleLoginSuccessNew((common.dto.LoginResponse) response.getPayload());
                    } else {
                        statusLabel.setText(response.getErrorMessage());
                        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    }
                }
            }
        });
    }

    /**
     * Handle new protocol login success.
     */
    private void handleLoginSuccessNew(common.dto.LoginResponse loginResp) {
        currentUsername = loginResp.getUsername();
        currentSessionToken = loginResp.getSessionToken();
        currentUserId = loginResp.getUserId();
        isSubscribed = loginResp.isSubscribed();

        String role = loginResp.getRole();
        switch (role.toUpperCase()) {
            case "CONTENT_MANAGER":
            case "MANAGER":
                currentUserRole = UserRole.MANAGER;
                break;
            case "CONTENT_EDITOR":
            case "EMPLOYEE":
                currentUserRole = UserRole.EMPLOYEE;
                break;
            case "SUPPORT_AGENT":
                currentUserRole = UserRole.SUPPORT_AGENT;
                break;
            default:
                currentUserRole = UserRole.CUSTOMER;
        }

        System.out.println("Login successful: " + currentUsername + " (token: " +
                currentSessionToken.substring(0, 8) + "...)");
        navigateToMainPage();
    }

    /**
     * Handle legacy string login success.
     */
    private void handleLoginSuccessLegacy(String responseStr) {
        // Parse: "login_success [role] [isSubscribed]"
        String[] parts = responseStr.split(" ");
        String role = parts.length > 1 ? parts[1] : "customer";
        boolean subscribed = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : false;

        currentUsername = usernameField.getText();
        isSubscribed = subscribed;
        currentSessionToken = null; // Legacy doesn't have token
        currentUserId = 0;

        switch (role.toUpperCase()) {
            case "CONTENT_MANAGER":
            case "MANAGER":
                currentUserRole = UserRole.MANAGER;
                break;
            case "CONTENT_EDITOR":
            case "EMPLOYEE":
                currentUserRole = UserRole.EMPLOYEE;
                break;
            case "SUPPORT_AGENT":
                currentUserRole = UserRole.SUPPORT_AGENT;
                break;
            default:
                currentUserRole = UserRole.CUSTOMER;
        }

        System.out.println("Login successful: " + currentUsername + " (" + currentUserRole + ")");
        navigateToMainPage();
    }

    // Reuse existing navigation methods...
    private void navigateToMainPage() {
        if (currentUserRole == UserRole.EMPLOYEE || currentUserRole == UserRole.MANAGER
                || currentUserRole == UserRole.SUPPORT_AGENT) {
            // Privileged User: Offer choice
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Workspace Selection");
            alert.setHeaderText("Select Workspace");
            alert.setContentText("Where would you like to go?");

            ButtonType btnDashboard = new ButtonType("User Dashboard");
            ButtonType btnEditor = new ButtonType("Content Management Portal");

            alert.getButtonTypes().setAll(btnDashboard, btnEditor, ButtonType.CANCEL);

            alert.showAndWait().ifPresent(type -> {
                if (type == btnDashboard) {
                    loadDashboard();
                } else if (type == btnEditor) {
                    loadContentPortal();
                }
            });

        } else {
            // Standard User: Go to Dashboard directly
            loadDashboard();
        }
    }

    // ... loadDashboard(), loadContentPortal(), handleBrowseCatalog(),
    // handleRegisterLink() ...

    @FXML
    private void handleBrowseCatalog() {
        // Guest browsing also needs persistent connection
        try {
            if (client == null)
                client = GCMClient.getInstance();
            // Just navigate, no login
            currentUsername = "Guest"; // Mark as guest so Back button works
            currentUserRole = UserRole.ANONYMOUS;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/catalog_search.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GCM - City Catalog");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GCM - Register");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GCM Dashboard - " + currentUsername);
            stage.setWidth(1000);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadContentPortal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/map_editor.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("GCM Content Management - " + currentUsername);
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static UserRole getCurrentUserRole() {
        return currentUserRole;
    }

    public static boolean isUserSubscribed() {
        return isSubscribed;
    }

    public static boolean isAnonymousUser() {
        return currentUserRole == UserRole.ANONYMOUS;
    }
}
