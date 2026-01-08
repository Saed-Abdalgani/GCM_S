package server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user sessions to prevent concurrent logins.
 * Stores session tokens and tracks which users are currently logged in.
 */
public class SessionManager {

    // Singleton instance
    private static SessionManager instance;

    // Maps session token → SessionInfo
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // Maps userId → session token (to check if user already logged in)
    private final Map<Integer, String> userSessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Session information holder.
     */
    public static class SessionInfo {
        public final int userId;
        public final String username;
        public final String role;
        public final long createdAt;

        public SessionInfo(int userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * Check if a user is already logged in (has active session).
     * 
     * @param userId User ID to check
     * @return true if user has an active session
     */
    public boolean isUserLoggedIn(int userId) {
        return userSessions.containsKey(userId);
    }

    /**
     * Create a new session for a user.
     * 
     * @param userId   User ID
     * @param username Username
     * @param role     User role
     * @return Session token, or null if user already logged in
     */
    public String createSession(int userId, String username, String role) {
        // Check if already logged in
        if (isUserLoggedIn(userId)) {
            System.out.println("⚠ User " + username + " already has active session - concurrent login denied");
            return null;
        }

        // Generate unique session token
        String token = UUID.randomUUID().toString();

        // Store session
        sessions.put(token, new SessionInfo(userId, username, role));
        userSessions.put(userId, token);

        System.out.println("✓ Session created for user: " + username + " (token: " + token.substring(0, 8) + "...)");
        return token;
    }

    /**
     * Validate a session token.
     * 
     * @param token Session token to validate
     * @return SessionInfo if valid, null otherwise
     */
    public SessionInfo validateSession(String token) {
        if (token == null)
            return null;
        return sessions.get(token);
    }

    /**
     * Invalidate a session (logout).
     * 
     * @param token Session token to invalidate
     * @return true if session was found and invalidated
     */
    public boolean invalidateSession(String token) {
        SessionInfo info = sessions.remove(token);
        if (info != null) {
            userSessions.remove(info.userId);
            System.out.println("✓ Session invalidated for user: " + info.username);
            return true;
        }
        return false;
    }

    /**
     * Get session by user ID.
     * 
     * @param userId User ID
     * @return Session token or null
     */
    public String getSessionToken(int userId) {
        return userSessions.get(userId);
    }

    /**
     * Invalidate session by user ID (force logout).
     * 
     * @param userId User ID
     * @return true if session was found and invalidated
     */
    public boolean invalidateUserSession(int userId) {
        String token = userSessions.get(userId);
        if (token != null) {
            return invalidateSession(token);
        }
        return false;
    }

    /**
     * Get count of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
