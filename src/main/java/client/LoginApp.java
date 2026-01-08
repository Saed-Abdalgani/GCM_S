package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application class to launch the Login page.
 * Run this class to start the login window.
 */
public class LoginApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the login FXML file from resources/client/
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/login.fxml"));
        Parent root = loader.load();

        // Create the scene with the login form
        Scene scene = new Scene(root, 500, 600);

        // Configure the stage (window)
        primaryStage.setTitle("GCM - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);

        // Show the window
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
