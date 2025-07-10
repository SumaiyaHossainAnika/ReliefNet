package com.reliefnet.database;

import java.sql.*;
import com.reliefnet.model.User;

/**
 * DatabaseManager - Handles SQLite database operations for offline functionality
 * Manages all data persistence for the ReliefNet system
 */
public class DatabaseManager {
    
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_NAME = "reliefnet.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;
      private DatabaseManager() {
        // Private constructor for singleton
        // Add shutdown hook to ensure database is closed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook: Closing database connection...");
            closeConnection();
        }));
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }    public void initializeDatabase() throws SQLException {
        System.out.println("Initializing database...");
        
        // Load SQLite JDBC driver explicitly
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
            throw new SQLException("SQLite JDBC driver not available", e);
        }
        
        System.out.println("Connecting to database: " + DB_URL);
        
        // Close any existing connection first
        closeConnection();
        
        // Add connection properties for better lock handling
        String connectionUrl = DB_URL + "?busy_timeout=30000&journal_mode=WAL";
        
        int retryCount = 0;
        int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try {
                connection = DriverManager.getConnection(connectionUrl);
                System.out.println("Database connection established successfully");
                break;
            } catch (SQLException e) {
                retryCount++;
                if (e.getMessage().contains("locked") && retryCount < maxRetries) {
                    System.err.println("Database is locked, retrying in 2 seconds... (attempt " + retryCount + "/" + maxRetries + ")");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Database connection interrupted", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        
        if (connection == null) {
            throw new SQLException("Failed to establish database connection after " + maxRetries + " attempts");
        }
        
        // Configure SQLite for better performance and reduced locking
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");  // Write-Ahead Logging reduces locks
            stmt.execute("PRAGMA synchronous=NORMAL;");
            stmt.execute("PRAGMA temp_store=MEMORY;");
            stmt.execute("PRAGMA cache_size=10000;");            stmt.execute("PRAGMA busy_timeout=30000;"); // 30 second timeout for locks
            System.out.println("SQLite pragmas configured for optimal performance");
        }
        
        // Set auto-commit to true for immediate writes
        connection.setAutoCommit(true);
        
        createTables();
        System.out.println("Database tables created successfully");
        insertDefaultData();
        System.out.println("Database initialization completed");
        
        // Clean up any existing SOS entries from emergency_requests table and test messages
        try {
            System.out.println("Cleaning up database...");
            
            // Remove SOS entries from emergency_requests table
            String deleteSOSFromEmergency = "DELETE FROM emergency_requests WHERE description LIKE '%SOS%' OR requester_id = 'SOS_USER'";
            int sosDeleted = connection.createStatement().executeUpdate(deleteSOSFromEmergency);
            if (sosDeleted > 0) {
                System.out.println("Removed " + sosDeleted + " SOS entries from emergency_requests");
            }
            
            // Clear test messages from messages table
            String clearMessages = "DELETE FROM messages WHERE sender_id IN ('CURRENT_USER', 'TEST_USER', 'SOS_USER')";
            int messagesDeleted = connection.createStatement().executeUpdate(clearMessages);
            if (messagesDeleted > 0) {
                System.out.println("Removed " + messagesDeleted + " test messages");
            }
            
        } catch (SQLException cleanupError) {
            System.err.println("Warning: Database cleanup failed: " + cleanupError.getMessage());
        }
    }
    
    private void createTables() throws SQLException {        // Users table (authorities, volunteers, survivors)
        String createUsersTable = 
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user_id TEXT UNIQUE NOT NULL," +
            "name TEXT NOT NULL," +
            "email TEXT," +
            "phone TEXT," +
            "password TEXT," +
            "user_type TEXT NOT NULL," +
            "location_lat REAL," +
            "location_lng REAL," +
            "location_name TEXT," +
            "status TEXT DEFAULT 'ACTIVE'," +
            "skills TEXT," +
            "verified BOOLEAN DEFAULT 0," +
            "assignment_count INTEGER DEFAULT 0," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        // Resources table
        String createResourcesTable = """
            CREATE TABLE IF NOT EXISTS resources (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                resource_id TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL, -- 'FOOD', 'WATER', 'MEDICAL', 'SHELTER', 'CLOTHING'
                quantity INTEGER NOT NULL,
                unit TEXT NOT NULL,
                location_lat REAL,
                location_lng REAL,
                location_name TEXT,
                status TEXT DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'ALLOCATED', 'DELIVERED', 'EXPIRED'
                expiry_date DATE,
                allocated_to TEXT, -- user_id if allocated
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Emergency requests table
        String createEmergencyTable = """
            CREATE TABLE IF NOT EXISTS emergency_requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                request_id TEXT UNIQUE NOT NULL,
                requester_id TEXT NOT NULL,
                emergency_type TEXT NOT NULL, -- 'MEDICAL', 'RESCUE', 'FOOD', 'WATER', 'SHELTER'
                priority TEXT NOT NULL, -- 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
                location_lat REAL NOT NULL,
                location_lng REAL NOT NULL,
                location_name TEXT,
                description TEXT,
                people_count INTEGER DEFAULT 1,
                status TEXT DEFAULT 'PENDING', -- 'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
                assigned_volunteer TEXT, -- user_id of assigned volunteer
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (requester_id) REFERENCES users(user_id),
                FOREIGN KEY (assigned_volunteer) REFERENCES users(user_id)
            )
        """;
          // Communications/Messages table with chat support
        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id TEXT UNIQUE NOT NULL,
                sender_id TEXT NOT NULL,
                receiver_id TEXT, -- NULL for broadcast/channel messages
                message_type TEXT NOT NULL, -- 'DIRECT', 'BROADCAST', 'EMERGENCY', 'SYSTEM', 'CHAT'
                channel_id TEXT DEFAULT 'general_chat', -- For channel-based chat
                subject TEXT,
                content TEXT NOT NULL,
                priority TEXT DEFAULT 'NORMAL', -- 'CRITICAL', 'HIGH', 'NORMAL', 'LOW'
                broadcast_type TEXT, -- For broadcasts: 'Emergency Alert', 'Weather Warning', etc.
                audience TEXT, -- For broadcasts: 'All Users', 'Volunteers Only', etc.
                read_status BOOLEAN DEFAULT 0,
                sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                delivered_at TIMESTAMP,
                FOREIGN KEY (sender_id) REFERENCES users(user_id),
                FOREIGN KEY (receiver_id) REFERENCES users(user_id)
            )        """;
          // SOS alerts table - separate from emergency_requests for immediate alerts
        String createSOSAlertsTable = """
            CREATE TABLE IF NOT EXISTS sos_alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sos_id TEXT UNIQUE NOT NULL,
                sender_name TEXT NOT NULL,
                sender_type TEXT NOT NULL, -- 'AUTHORITY', 'VOLUNTEER', 'SURVIVOR'
                sender_contact TEXT, -- Phone number or other contact info
                location_name TEXT,
                location_lat REAL,
                location_lng REAL,
                description TEXT DEFAULT 'Emergency assistance needed immediately',
                urgency_level TEXT DEFAULT 'CRITICAL', -- 'CRITICAL', 'HIGH', 'MEDIUM'
                status TEXT DEFAULT 'ACTIVE', -- 'ACTIVE', 'RESPONDED', 'RESOLVED', 'CANCELLED'
                assigned_volunteer TEXT, -- Name of assigned volunteer
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                responded_at TIMESTAMP,
                resolved_at TIMESTAMP,
                responder_id TEXT,
                responder_notes TEXT,
                FOREIGN KEY (responder_id) REFERENCES users(user_id)
            )
        """;
        
        // Volunteer assignments table
        String createAssignmentsTable = """
            CREATE TABLE IF NOT EXISTS volunteer_assignments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                assignment_id TEXT UNIQUE NOT NULL,
                volunteer_id TEXT NOT NULL,
                request_id TEXT NOT NULL,
                assignment_type TEXT NOT NULL, -- 'EMERGENCY', 'RESOURCE_DELIVERY', 'SURVEY'
                status TEXT DEFAULT 'ASSIGNED', -- 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
                assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                started_at TIMESTAMP,
                completed_at TIMESTAMP,
                notes TEXT,
                FOREIGN KEY (volunteer_id) REFERENCES users(user_id),
                FOREIGN KEY (request_id) REFERENCES emergency_requests(request_id)
            )
        """;
        
        // Population data for resource prediction
        String createPopulationTable = """
            CREATE TABLE IF NOT EXISTS population_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                area_id TEXT UNIQUE NOT NULL,
                area_name TEXT NOT NULL,
                district TEXT NOT NULL,
                division TEXT NOT NULL,
                population INTEGER NOT NULL,
                households INTEGER,
                vulnerable_population INTEGER, -- elderly, disabled, children
                location_lat REAL,
                location_lng REAL,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // System logs for fraud prevention
        String createSystemLogsTable = """
            CREATE TABLE IF NOT EXISTS system_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                log_id TEXT UNIQUE NOT NULL,
                user_id TEXT,
                action TEXT NOT NULL,
                resource_affected TEXT,
                ip_address TEXT,
                user_agent TEXT,
                suspicious_score INTEGER DEFAULT 0, -- 0-100, higher = more suspicious
                fraud_indicators TEXT, -- JSON array of detected indicators
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )
        """;
          // Settings table
        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                setting_key TEXT NOT NULL,
                setting_value TEXT NOT NULL,
                user_id TEXT, -- NULL for global settings, user_id for user-specific settings
                setting_type TEXT DEFAULT 'STRING', -- 'STRING', 'INTEGER', 'BOOLEAN', 'JSON'
                description TEXT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(setting_key, user_id)
            )
        """;
        
        // Verification codes table for two-factor authentication
        String createVerificationCodesTable = """
            CREATE TABLE IF NOT EXISTS verification_codes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL,
                code TEXT NOT NULL,
                purpose TEXT NOT NULL, -- 'REGISTRATION', 'PASSWORD_RESET'
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NOT NULL,
                used BOOLEAN DEFAULT 0,
                attempts INTEGER DEFAULT 0
            )
        """;
        
        // Execute table creation
        Statement stmt = connection.createStatement();        stmt.execute(createUsersTable);
        stmt.execute(createResourcesTable);
        stmt.execute(createEmergencyTable);
        stmt.execute(createMessagesTable);
        stmt.execute(createSOSAlertsTable);
        stmt.execute(createAssignmentsTable);
        stmt.execute(createPopulationTable);
        stmt.execute(createSystemLogsTable);
        stmt.execute(createSettingsTable);
        stmt.execute(createVerificationCodesTable);        // Add password column to existing users table if it doesn't exist
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN password TEXT");
            System.out.println("Added password column to users table");
        } catch (SQLException e) {
            // Column already exists, ignore
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add password column: " + e.getMessage());
            }
        }
        
        // Add assignment_count column to existing users table if it doesn't exist
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN assignment_count INTEGER DEFAULT 0");
            System.out.println("Added assignment_count column to users table");
        } catch (SQLException e) {
            // Column already exists, ignore
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add assignment_count column: " + e.getMessage());
            }
        }
        
        // Add assigned_volunteer column to existing sos_alerts table if it doesn't exist
        try {
            stmt.execute("ALTER TABLE sos_alerts ADD COLUMN assigned_volunteer TEXT");
            System.out.println("Added assigned_volunteer column to sos_alerts table");
        } catch (SQLException e) {
            // Column already exists, ignore
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add assigned_volunteer column: " + e.getMessage());
            }
        }
          // Add updated_at column to existing sos_alerts table if it doesn't exist
        try {
            stmt.execute("ALTER TABLE sos_alerts ADD COLUMN updated_at TIMESTAMP");
            System.out.println("Added updated_at column to sos_alerts table");
            
            // Initialize updated_at for existing records
            stmt.execute("UPDATE sos_alerts SET updated_at = created_at WHERE updated_at IS NULL");
            System.out.println("Initialized updated_at values for existing SOS alerts");
        } catch (SQLException e) {
            // Column already exists, ignore
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add updated_at column: " + e.getMessage());
            }
        }
        
        // Add missing columns to messages table for chat functionality
        try {
            stmt.execute("ALTER TABLE messages ADD COLUMN channel_id TEXT DEFAULT 'general_chat'");
            System.out.println("Added channel_id column to messages table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add channel_id column: " + e.getMessage());
            }
        }
        
        try {
            stmt.execute("ALTER TABLE messages ADD COLUMN broadcast_type TEXT");
            System.out.println("Added broadcast_type column to messages table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add broadcast_type column: " + e.getMessage());
            }
        }
        
        try {
            stmt.execute("ALTER TABLE messages ADD COLUMN sync_status TEXT DEFAULT 'PENDING'");
            System.out.println("Added sync_status column to messages table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add sync_status column: " + e.getMessage());
            }
        }
        
        try {
            stmt.execute("ALTER TABLE emergency_requests ADD COLUMN sync_status TEXT DEFAULT 'PENDING'");
            System.out.println("Added sync_status column to emergency_requests table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add sync_status column to emergency_requests: " + e.getMessage());
            }
        }
        
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN sync_status TEXT DEFAULT 'PENDING'");
            System.out.println("Added sync_status column to users table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add sync_status column to users: " + e.getMessage());
            }
        }
        
        try {
            stmt.execute("ALTER TABLE resources ADD COLUMN sync_status TEXT DEFAULT 'PENDING'");
            System.out.println("Added sync_status column to resources table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add sync_status column to resources: " + e.getMessage());
            }
        }
        
        // Migrate settings table to support user-specific settings
        try {
            stmt.execute("ALTER TABLE settings ADD COLUMN user_id TEXT");
            System.out.println("Added user_id column to settings table");
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column")) {
                System.err.println("Warning: Could not add user_id column to settings: " + e.getMessage());
            }
        }
        
        // Update the unique constraint for settings table to handle user-specific settings
        try {
            // Drop the old unique constraint and recreate the table with the new structure
            stmt.execute("CREATE TABLE IF NOT EXISTS settings_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "setting_key TEXT NOT NULL," +
                        "setting_value TEXT NOT NULL," +
                        "user_id TEXT," +
                        "setting_type TEXT DEFAULT 'STRING'," +
                        "description TEXT," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "UNIQUE(setting_key, user_id)" +
                        ")");
            
            // Copy existing data to new table
            stmt.execute("INSERT INTO settings_new (setting_key, setting_value, setting_type, description, updated_at) " +
                        "SELECT setting_key, setting_value, setting_type, description, updated_at FROM settings");
            
            // Drop old table and rename new one
            stmt.execute("DROP TABLE settings");
            stmt.execute("ALTER TABLE settings_new RENAME TO settings");
            
            System.out.println("Successfully migrated settings table structure");
        } catch (SQLException e) {
            // If migration fails, we'll work with the existing structure
            System.err.println("Warning: Could not migrate settings table structure: " + e.getMessage());
        }
        
        stmt.close();
    }
    
    private void insertDefaultData() throws SQLException {
        // Insert default settings
        insertDefaultSettings();
        
        // Insert sample population data for Bangladesh districts        insertSamplePopulationData();
        
        // No default admin user needed - users can register
    }
    
    private void insertDefaultSettings() throws SQLException {
        String[] defaultSettings = {
            "('offline_mode_enabled', 'true', 'BOOLEAN', 'Enable offline functionality')",
            "('fraud_detection_enabled', 'true', 'BOOLEAN', 'Enable fraud detection system')",
            "('resource_prediction_enabled', 'true', 'BOOLEAN', 'Enable resource shortage prediction')",
            "('emergency_alert_radius', '10', 'INTEGER', 'Emergency alert radius in kilometers')",
            "('volunteer_auto_assign', 'false', 'BOOLEAN', 'Automatically assign volunteers to requests')",
            "('system_name', 'ReliefNet', 'STRING', 'System display name')",
            "('max_resource_per_request', '5', 'INTEGER', 'Maximum resources per request to prevent fraud')",
            "('volunteer_verification_required', 'true', 'BOOLEAN', 'Require volunteer verification')"
        };
        
        String insertSQL = "INSERT OR IGNORE INTO settings (setting_key, setting_value, setting_type, description) VALUES ";
        
        for (String setting : defaultSettings) {
            Statement stmt = connection.createStatement();
            stmt.execute(insertSQL + setting);
            stmt.close();
        }
    }
    
    private void insertSamplePopulationData() throws SQLException {
        // Sample population data for major districts in Bangladesh
        String insertPopulation = """
            INSERT OR IGNORE INTO population_data 
            (area_id, area_name, district, division, population, households, vulnerable_population, location_lat, location_lng) 
            VALUES 
            ('DH001', 'Dhaka Metropolitan', 'Dhaka', 'Dhaka', 9540000, 2000000, 1500000, 23.8103, 90.4125),
            ('CH001', 'Chittagong Metropolitan', 'Chittagong', 'Chittagong', 5200000, 1100000, 800000, 22.3569, 91.7832),
            ('SY001', 'Sylhet Metropolitan', 'Sylhet', 'Sylhet', 680000, 145000, 95000, 24.8949, 91.8687),
            ('RJ001', 'Rajshahi Metropolitan', 'Rajshahi', 'Rajshahi', 840000, 180000, 120000, 24.3636, 88.6241),
            ('KH001', 'Khulna Metropolitan', 'Khulna', 'Khulna', 1800000, 380000, 250000, 22.8456, 89.5403),
            ('BR001', 'Barisal Metropolitan', 'Barisal', 'Barisal', 420000, 90000, 65000, 22.7010, 90.3535),
            ('RG001', 'Rangpur Metropolitan', 'Rangpur', 'Rangpur', 750000, 160000, 110000, 25.7439, 89.2752),
            ('MY001', 'Mymensingh Metropolitan', 'Mymensingh', 'Mymensingh', 580000, 125000, 85000, 24.7471, 90.4203)
        """;        
        Statement stmt = connection.createStatement();
        stmt.execute(insertPopulation);
        stmt.close();
    }
    
    // Utility methods for common operations
    @Deprecated
    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        // WARNING: This method can cause memory leaks. Use executeQueryWithCallback instead.
        // Ensure connection is available
        if (connection == null || connection.isClosed()) {
            System.err.println("Database connection is not available, attempting to reconnect...");
            initializeDatabase();
        }
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
        // Note: ResultSet and PreparedStatement should be closed by the caller
        // using try-with-resources or explicit close()
        return pstmt.executeQuery();
    }
    
    public int executeUpdate(String sql, Object... params) throws SQLException {
        // Ensure connection is available
        if (connection == null || connection.isClosed()) {
            System.err.println("Database connection is not available, attempting to reconnect...");
            initializeDatabase();
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        }
    }
    
    public boolean isOnline() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }      public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                // Ensure all pending transactions are committed
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                
                // Execute final cleanup to release all locks
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA optimize;");  // Optimize database before closing
                } catch (SQLException e) {
                    // Ignore optimization errors during shutdown
                }
                
                connection.close();
                System.out.println("Database connection closed successfully");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connection = null;
        }
    }
    
    // Method to check database health
    public boolean testConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            boolean hasResult = rs.next();
            rs.close();
            stmt.close();
            
            return hasResult;
        } catch (SQLException e) {
            return false;
        }
    }
      /**
     * Authenticate user with username/email and password
     */
    public User authenticateUser(String usernameOrEmail, String password) {
        try {
            // First check against database stored users
            String sql = "SELECT * FROM users WHERE (user_id = ? OR email = ?) AND password = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            stmt.setString(3, password); // In real app, this would be hashed
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Create user from database record
                User user = new User(
                    rs.getString("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    User.UserType.valueOf(rs.getString("user_type"))
                );
                user.setPhoneNumber(rs.getString("phone"));
                user.setLastLoginAt(java.time.LocalDateTime.now());
                
                // Update last_seen in database
                String updateSql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                updateStmt.setString(1, user.getUserId());
                updateStmt.executeUpdate();
                
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Database error during authentication: " + e.getMessage());
        }        
        // No hardcoded users - all must register through proper channels
        return null; // Authentication failed
    }    /**
     * Check if user exists with given username (emails can be reused for multiple roles)
     */
    public boolean userExists(String username, String email) {
        System.out.println("Checking if username exists: " + username);
          if (connection == null) {
            System.err.println("Database connection is null during userExists check!");            // Return false for proper user registration
            return false;
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Database query result: " + count + " users found with username: " + username);
                if (count > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error checking user existence: " + e.getMessage());            e.printStackTrace();
        }
        
        // No hardcoded users - return database check result only
        System.out.println("Username exists check completed via database");
        return false;
    }/**
     * Register a new user
     */
    public boolean registerUser(User user, String password) {
        return registerUser(user, password, null);
    }
    
    /**
     * Register a new user with optional authority code
     */
    public boolean registerUser(User user, String password, String authCode) {
        System.out.println("Attempting to register user: " + user.getFullName() + " with username: " + user.getUserId());
        
        if (connection == null) {
            System.err.println("Database connection is null!");
            return false;
        }
        
        // Verify authority code if user is an Authority
        if (user.getUserType() == User.UserType.AUTHORITY) {
            if (!"auth".equals(authCode)) {
                System.err.println("Invalid authority code provided for: " + user.getFullName());
                return false;
            }
            System.out.println("Authority code verified for: " + user.getFullName());
        }
        
        try {
            // Insert user into database
            String sql = "INSERT INTO users (user_id, name, email, phone, user_type, password, status, created_at, last_seen) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
              PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, user.getUserId());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPhoneNumber());
            stmt.setString(5, user.getUserType().name());            stmt.setString(6, password); // In real app, this would be hashed
            String statusToSet = user.getStatus() != null ? user.getStatus() : "ACTIVE";
            stmt.setString(7, statusToSet);
            
            System.out.println("DEBUG: Setting user status to: " + statusToSet + " for user: " + user.getFullName());
            
            System.out.println("Executing SQL: " + sql);
            System.out.println("Parameters: " + user.getUserId() + ", " + user.getFullName() + ", " + user.getEmail() + ", " + user.getPhoneNumber() + ", " + user.getUserType().name());
            
            int rowsAffected = stmt.executeUpdate();
              if (rowsAffected > 0) {
                System.out.println("Successfully registered user: " + user.getFullName() + " as " + user.getUserType().getDisplayName());
                user.setRegisteredAt(java.time.LocalDateTime.now());
                user.setStatus(user.getStatus() != null ? user.getStatus() : "Active");
                  // Notify data sync manager about the new user
                try {
                    System.out.println("DEBUG: Notifying DataSyncManager about new user registration");
                    com.reliefnet.util.DataSyncManager.getInstance().notifyUserDataChanged();
                } catch (Exception e) {
                    System.err.println("Error notifying data sync manager: " + e.getMessage());
                }
                
                return true;
            } else {
                System.err.println("No rows affected during registration");
            }
            
        } catch (SQLException e) {
            System.err.println("SQL error during registration: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.err.println("User with this username or email already exists");
            }
        } catch (Exception e) {
            System.err.println("General error during registration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Helper method to safely execute queries with proper resource cleanup
     * Use this when you need to process ResultSet and close resources immediately
     */
    public void executeQueryWithCallback(String sql, ResultSetCallback callback, Object... params) throws SQLException {
        // Ensure connection is available
        if (connection == null || connection.isClosed()) {
            System.err.println("Database connection is not available, attempting to reconnect...");
            initializeDatabase();
        }
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                callback.process(rs);
            }
        }
    }
    
    /**
     * Functional interface for ResultSet processing
     */
    @FunctionalInterface
    public interface ResultSetCallback {
        void process(ResultSet rs) throws SQLException;
    }
    
    /**
     * Check if user needs first-time password setup (user synced from another device)
     */
    public boolean userNeedsFirstTimeSetup(String usernameOrEmail) {
        try {
            String sql = "SELECT password FROM users WHERE (user_id = ? OR email = ?) AND password = 'NEEDS_FIRST_TIME_SETUP'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            
            ResultSet rs = stmt.executeQuery();
            boolean needsSetup = rs.next();
            rs.close();
            stmt.close();
            
            return needsSetup;
        } catch (SQLException e) {
            System.err.println("Database error checking first-time setup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Set password for user who needs first-time setup
     */
    public boolean setFirstTimePassword(String usernameOrEmail, String newPassword) {
        try {
            String sql = "UPDATE users SET password = ? WHERE (user_id = ? OR email = ?) AND password = 'NEEDS_FIRST_TIME_SETUP'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, newPassword);
            stmt.setString(2, usernameOrEmail);
            stmt.setString(3, usernameOrEmail);
            
            int updated = stmt.executeUpdate();
            stmt.close();
            
            if (updated > 0) {
                System.out.println("Successfully set password for first-time user: " + usernameOrEmail);
                return true;
            } else {
                System.out.println("No user found needing first-time setup: " + usernameOrEmail);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Database error setting first-time password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user information for first-time setup (without authentication)
     */
    public User getUserForFirstTimeSetup(String usernameOrEmail) {
        try {
            String sql = "SELECT * FROM users WHERE (user_id = ? OR email = ?) AND password = 'NEEDS_FIRST_TIME_SETUP'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                    rs.getString("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    User.UserType.valueOf(rs.getString("user_type"))
                );
                user.setPhoneNumber(rs.getString("phone"));
                
                rs.close();
                stmt.close();
                return user;
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Database error getting user for first-time setup: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Store verification code for email verification
     */
    public boolean storeVerificationCode(String email, String code, String purpose) {
        try {
            // Clean up any existing codes for this email and purpose
            String cleanupSql = "DELETE FROM verification_codes WHERE email = ? AND purpose = ?";
            PreparedStatement cleanupStmt = connection.prepareStatement(cleanupSql);
            cleanupStmt.setString(1, email);
            cleanupStmt.setString(2, purpose);
            cleanupStmt.executeUpdate();
            cleanupStmt.close();
            
            // Insert new code
            String sql = "INSERT INTO verification_codes (email, code, purpose, expires_at) VALUES (?, ?, ?, datetime('now', '+10 minutes'))";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, code);
            stmt.setString(3, purpose);
            
            int result = stmt.executeUpdate();
            stmt.close();
            
            System.out.println("Verification code stored for: " + email + " (" + purpose + ")");
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error storing verification code: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify email verification code
     */
    public boolean verifyEmailCode(String email, String code, String purpose) {
        try {
            String sql = "SELECT * FROM verification_codes WHERE email = ? AND code = ? AND purpose = ? AND used = 0 AND expires_at > datetime('now')";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, code);
            stmt.setString(3, purpose);
            
            ResultSet rs = stmt.executeQuery();
            boolean isValid = rs.next();
            
            if (isValid) {
                // Mark code as used
                String updateSql = "UPDATE verification_codes SET used = 1 WHERE email = ? AND code = ? AND purpose = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                updateStmt.setString(1, email);
                updateStmt.setString(2, code);
                updateStmt.setString(3, purpose);
                updateStmt.executeUpdate();
                updateStmt.close();
                
                System.out.println("Email verification successful for: " + email);
            } else {
                // Increment attempts
                String attemptsql = "UPDATE verification_codes SET attempts = attempts + 1 WHERE email = ? AND purpose = ?";
                PreparedStatement attemptStmt = connection.prepareStatement(attemptsql);
                attemptStmt.setString(1, email);
                attemptStmt.setString(2, purpose);
                attemptStmt.executeUpdate();
                attemptStmt.close();
                
                System.out.println("Email verification failed for: " + email);
            }
            
            rs.close();
            stmt.close();
            return isValid;
        } catch (SQLException e) {
            System.err.println("Error verifying email code: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if email verification is required for user
     */
    public boolean isEmailVerificationRequired(String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND verified = 0";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            
            ResultSet rs = stmt.executeQuery();
            int count = rs.getInt(1);
            
            rs.close();
            stmt.close();
            
            return count > 0;
        } catch (SQLException e) {
            System.err.println("Error checking email verification requirement: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Mark email as verified
     */
    public boolean markEmailAsVerified(String email) {
        try {
            String sql = "UPDATE users SET verified = 1 WHERE email = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            
            int updated = stmt.executeUpdate();
            stmt.close();
            
            if (updated > 0) {
                System.out.println("Email marked as verified: " + email);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error marking email as verified: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user by email for password reset
     */
    public User getUserByEmail(String email) {
        try {
            String sql = "SELECT * FROM users WHERE email = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                    rs.getString("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    User.UserType.valueOf(rs.getString("user_type"))
                );
                user.setPhoneNumber(rs.getString("phone"));
                
                rs.close();
                stmt.close();
                return user;
            }
            
            rs.close();
            stmt.close();
            return null;
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Reset user password using email verification
     */
    public boolean resetPassword(String email, String newPassword) {
        try {
            String sql = "UPDATE users SET password = ? WHERE email = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, newPassword);
            stmt.setString(2, email);
            
            int updated = stmt.executeUpdate();
            stmt.close();
            
            if (updated > 0) {
                System.out.println("Password reset successful for: " + email);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user by email and user type combination
     * This allows checking if a specific email + role combination already exists
     */
    public User getUserByEmailAndType(String email, User.UserType userType) {
        try {
            String sql = "SELECT * FROM users WHERE email = ? AND user_type = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, userType.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                    rs.getString("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    User.UserType.valueOf(rs.getString("user_type"))
                );
                user.setPhoneNumber(rs.getString("phone"));
                
                rs.close();
                stmt.close();
                return user;
            }
            
            rs.close();
            stmt.close();
            return null;
        } catch (SQLException e) {
            System.err.println("Error getting user by email and type: " + e.getMessage());
            return null;
        }
    }
}
