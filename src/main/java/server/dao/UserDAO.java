package server.dao;

import server.DBConnector;

import java.sql.*;

/**
 * Data Access Object for user authentication and registration.
 * Uses plain text passwords for simplicity (university project).
 */
public class UserDAO {

    /**
     * User info holder (returned from findByUsername).
     */
    public static class UserInfo {
        public final int id;
        public final String username;
        public final String email;
        public final String role;
        public final boolean isActive;

        public UserInfo(int id, String username, String email, String role, boolean isActive) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
            this.isActive = isActive;
        }
    }

    /**
     * Find user by username.
     * 
     * @param username Username to search
     * @return UserInfo or null if not found
     */
    public static UserInfo findByUsername(String username) {
        String sql = "SELECT id, username, email, role, is_active FROM users WHERE username = ?";

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UserInfo(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"));
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        return null;
    }

    /**
     * Find user by email.
     * 
     * @param email Email to search
     * @return UserInfo or null if not found
     */
    public static UserInfo findByEmail(String email) {
        String sql = "SELECT id, username, email, role, is_active FROM users WHERE email = ?";

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UserInfo(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"));
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Authenticate user with username and password.
     * 
     * @param username Username
     * @param password Password (plain text)
     * @return UserInfo if authentication successful, null otherwise
     */
    public static UserInfo authenticate(String username, String password) {
        String sql = "SELECT id, username, email, role, is_active FROM users " +
                "WHERE username = ? AND password_hash = ?";

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Plain text comparison
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                boolean isActive = rs.getBoolean("is_active");
                if (!isActive) {
                    System.out.println("User " + username + " is deactivated");
                    return null;
                }
                return new UserInfo(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        isActive);
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Create a new customer user.
     * 
     * @param username     Username (must be unique)
     * @param email        Email (must be unique)
     * @param password     Password (stored as plain text)
     * @param phone        Phone number (optional)
     * @param paymentToken Mock payment token
     * @param cardLast4    Last 4 digits of card
     * @return Created user ID, or -1 if failed
     */
    public static int createCustomer(String username, String email, String password,
            String phone, String paymentToken, String cardLast4) {
        Connection conn = null;
        try {
            conn = DBConnector.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Create user
            String userSql = "INSERT INTO users (username, email, password_hash, role, phone, is_active) " +
                    "VALUES (?, ?, ?, 'CUSTOMER', ?, TRUE)";
            PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userStmt.setString(1, username);
            userStmt.setString(2, email);
            userStmt.setString(3, password); // Plain text
            userStmt.setString(4, phone);
            userStmt.executeUpdate();

            ResultSet keys = userStmt.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                return -1;
            }
            int userId = keys.getInt(1);

            // 2. Create customer record
            String customerSql = "INSERT INTO customers (user_id, payment_token, card_last4) VALUES (?, ?, ?)";
            PreparedStatement custStmt = conn.prepareStatement(customerSql);
            custStmt.setInt(1, userId);
            custStmt.setString(2, paymentToken);
            custStmt.setString(3, cardLast4);
            custStmt.executeUpdate();

            conn.commit();
            System.out.println("✓ Created new customer: " + username + " (ID: " + userId + ")");
            return userId;

        } catch (SQLException e) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
            }

            // Check for duplicate key errors
            if (e.getMessage().contains("Duplicate")) {
                System.out.println("Registration failed - duplicate username or email");
            } else {
                System.err.println("Error creating customer: " + e.getMessage());
            }
            return -1;
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Check if username already exists.
     */
    public static boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }

    /**
     * Check if email already exists.
     */
    public static boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    /**
     * Update last login timestamp for user.
     */
    public static void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login_at = NOW() WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    // ==================== Phase 6: Customer Profile Methods ====================

    /**
     * Get customer profile by user ID.
     *
     * @param userId User ID
     * @return CustomerProfileDTO or null if not found
     */
    public static common.dto.CustomerProfileDTO getProfile(int userId) {
        String sql = """
                SELECT u.id, u.username, u.email, u.phone, u.created_at, u.last_login_at,
                       c.card_last4,
                       (SELECT COUNT(*) FROM purchases WHERE user_id = u.id) as purchase_count,
                       (SELECT COUNT(*) FROM subscriptions WHERE user_id = u.id) as sub_count,
                       (SELECT COALESCE(SUM(price_paid), 0) FROM purchases WHERE user_id = u.id) +
                       (SELECT COALESCE(SUM(price_paid), 0) FROM subscriptions WHERE user_id = u.id) as total_spent
                FROM users u
                LEFT JOIN customers c ON u.id = c.user_id
                WHERE u.id = ?
                """;

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                common.dto.CustomerProfileDTO profile = new common.dto.CustomerProfileDTO();
                profile.setUserId(rs.getInt("id"));
                profile.setUsername(rs.getString("username"));
                profile.setEmail(rs.getString("email"));
                profile.setPhone(rs.getString("phone"));
                profile.setCardLast4(rs.getString("card_last4"));
                profile.setCreatedAt(rs.getTimestamp("created_at"));
                profile.setLastLoginAt(rs.getTimestamp("last_login_at"));
                profile.setTotalPurchases(rs.getInt("purchase_count") + rs.getInt("sub_count"));
                profile.setTotalSpent(rs.getDouble("total_spent"));
                return profile;
            }
        } catch (SQLException e) {
            System.err.println("Error getting profile: " + e.getMessage());
        }
        return null;
    }

    /**
     * Update customer profile.
     *
     * @param userId User ID
     * @param email  New email (null to keep current)
     * @param phone  New phone (null to keep current)
     * @return true if updated
     */
    public static boolean updateProfile(int userId, String email, String phone) {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (email != null) {
            sql.append("email = ?, ");
            params.add(email);
        }
        if (phone != null) {
            sql.append("phone = ?, ");
            params.add(phone);
        }

        if (params.isEmpty()) {
            return true; // Nothing to update
        }

        // Remove trailing comma
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(userId);

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * List all customers (for admin).
     *
     * @return List of CustomerListItemDTO
     */
    public static java.util.List<common.dto.CustomerListItemDTO> listAllCustomers() {
        java.util.List<common.dto.CustomerListItemDTO> customers = new java.util.ArrayList<>();

        String sql = """
                SELECT u.id, u.username, u.email, u.phone, u.created_at, u.is_active,
                       (SELECT COUNT(*) FROM purchases WHERE user_id = u.id) as purchase_count,
                       (SELECT COUNT(*) FROM subscriptions WHERE user_id = u.id) as sub_count,
                       (SELECT COALESCE(SUM(price_paid), 0) FROM purchases WHERE user_id = u.id) +
                       (SELECT COALESCE(SUM(price_paid), 0) FROM subscriptions WHERE user_id = u.id) as total_spent,
                       (SELECT MAX(purchased_at) FROM purchases WHERE user_id = u.id) as last_purchase
                FROM users u
                WHERE u.role = 'CUSTOMER'
                ORDER BY u.created_at DESC
                """;

        try (Connection conn = DBConnector.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                common.dto.CustomerListItemDTO item = new common.dto.CustomerListItemDTO();
                item.setUserId(rs.getInt("id"));
                item.setUsername(rs.getString("username"));
                item.setEmail(rs.getString("email"));
                item.setPhone(rs.getString("phone"));
                item.setPurchaseCount(rs.getInt("purchase_count"));
                item.setSubscriptionCount(rs.getInt("sub_count"));
                item.setTotalSpent(rs.getDouble("total_spent"));
                item.setLastPurchaseAt(rs.getTimestamp("last_purchase"));
                item.setRegisteredAt(rs.getTimestamp("created_at"));
                item.setActive(rs.getBoolean("is_active"));
                customers.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error listing customers: " + e.getMessage());
        }

        return customers;
    }
}
