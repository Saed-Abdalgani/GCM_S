package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {

    // ADJUST THESE TO MATCH YOUR MYSQL SETUP
    private static final String URL = "jdbc:mysql://localhost:3306/gcm_db?serverTimezone=Asia/Jerusalem";
    private static final String USER = "root";
    private static final String PASS = ""; // <--- Put your MySQL password here

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            return conn;
        } catch (SQLException e) {
            System.out.println("Database Connection Failed! Check if:");
            System.out.println("  1. MySQL is running");
            System.out.println("  2. Database 'gcm_db' exists (run: mysql -u root -p < dummy_db.sql)");
            System.out.println("  3. Password in DBConnector is correct");
            System.out.println("Error: " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL Driver not found!");
            e.printStackTrace();
            return null;
        }
    }

    // Right-click inside this file and choose "Run 'DBConnector.main()'"
    public static void main(String[] args) {
        if (getConnection() != null) {
            System.out.println("SUCCESS: Connected to Database!");
        }
    }
}