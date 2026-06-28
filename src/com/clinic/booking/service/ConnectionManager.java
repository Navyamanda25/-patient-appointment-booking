package com.clinic.booking.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ConnectionManager handles everything related to connecting Java to MySQL.
 *
 * ── SINGLETON PATTERN ────────────────────────────────────────────────────────
 * We use the Singleton pattern here.
 *
 * What is Singleton?
 * It is a design pattern that ensures only ONE instance of a class
 * is ever created during the entire program run.
 *
 * Why do we need it here?
 * 1. We only want to read db.properties ONCE (not every time a DAO runs a query)
 * 2. We only want to load the MySQL driver ONCE
 * 3. All DAOs share the same ConnectionManager object
 *
 * How Singleton works:
 * - The constructor is PRIVATE → nobody outside can do "new ConnectionManager()"
 * - We keep one static instance variable inside the class
 * - getInstance() returns that one instance (creates it first time, reuses after)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ConnectionManager {

    // ── Singleton instance ────────────────────────────────────────────────
    // This is the ONE instance that the whole program shares.
    // It starts as null and gets created the first time getInstance() is called.
    private static ConnectionManager instance;

    // ── Database credentials ──────────────────────────────────────────────
    // These are loaded from config/db.properties
    // We store them here so we only read the file once.
    private String url;
    private String user;
    private String password;

    // ── PRIVATE Constructor ───────────────────────────────────────────────
    /**
     * Private constructor — this is what makes it a Singleton.
     * Nobody outside this class can call "new ConnectionManager()".
     * The only way to get the instance is through getInstance().
     *
     * When the constructor runs:
     *   1. It reads config/db.properties to get url, user, password
     *   2. It loads the MySQL JDBC driver into memory
     */
    private ConnectionManager() {
        loadPropertiesFromFile();
        registerJdbcDriver();
    }

    // ── getInstance ───────────────────────────────────────────────────────
    /**
     * Returns the single shared instance of ConnectionManager.
     *
     * First call  → creates the instance, stores it, returns it
     * Every call after → just returns the already-created instance
     *
     * "synchronized" means: if two threads call this at the same time,
     * only one runs at a time. Prevents creating two instances accidentally.
     * (For this console app it does not matter much, but it is good practice.)
     */
    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    // ── loadPropertiesFromFile ────────────────────────────────────────────
    /**
     * Reads config/db.properties and extracts the 3 values:
     *   db.url      → the JDBC connection string
     *   db.user     → the database username
     *   db.password → the database password
     *
     * The path "config/db.properties" is RELATIVE.
     * This means Java looks for it starting from wherever you RUN the program.
     * Since we always run from the project root, it finds config/db.properties.
     *
     * We use try-with-resources to open the file.
     * This guarantees the file stream is closed after reading,
     * even if an exception occurs.
     */
    private void loadPropertiesFromFile() {

        // Build the path to config/db.properties
        Path configFilePath = Paths.get("config", "db.properties");

        // Check if the file actually exists before trying to open it
        if (!Files.exists(configFilePath)) {
            throw new RuntimeException(
                "\n[ERROR] config/db.properties file not found!" +
                "\nMake sure you:" +
                "\n  1. Created the file config/db.properties" +
                "\n  2. Are running the program from the project root folder" +
                "\n  3. Copied db.properties.example and filled in your credentials"
            );
        }

        // Read the properties file
        Properties props = new Properties();

        try (InputStream inputStream = Files.newInputStream(configFilePath)) {
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(
                "[ERROR] Could not read config/db.properties: " + e.getMessage(), e
            );
        }

        // Extract the 3 values
        url      = props.getProperty("db.url");
        user     = props.getProperty("db.user");
        password = props.getProperty("db.password");

        // Validate — make sure none of them are missing
        if (url == null || url.isBlank()) {
            throw new RuntimeException(
                "[ERROR] db.url is missing or empty in config/db.properties"
            );
        }
        if (user == null || user.isBlank()) {
            throw new RuntimeException(
                "[ERROR] db.user is missing or empty in config/db.properties"
            );
        }
        if (password == null) {
            throw new RuntimeException(
                "[ERROR] db.password is missing in config/db.properties"
            );
        }

        // Tell the user it worked (helpful during development)
        System.out.println("[DB] Properties loaded successfully.");
        System.out.println("[DB] Connecting to: " + url);
    }

    // ── registerJdbcDriver ────────────────────────────────────────────────
    /**
     * Loads the MySQL JDBC driver class into memory.
     *
     * What is a JDBC driver?
     * JDBC (Java Database Connectivity) is Java's standard API for databases.
     * But Java does not know HOW to talk to MySQL specifically.
     * The MySQL JDBC driver (the JAR file in lib/) teaches Java how.
     *
     * Class.forName("com.mysql.cj.jdbc.Driver") tells Java:
     * "Find this class in the classpath and load it."
     * When the Driver class loads, it registers itself with DriverManager.
     * After that, DriverManager.getConnection() knows how to create MySQL connections.
     *
     * If this fails, it means the JAR file is not in the classpath.
     * Solution: make sure you compiled and ran with -cp including lib/*.jar
     */
    private void registerJdbcDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DB] MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "\n[ERROR] MySQL JDBC Driver not found!" +
                "\nMake sure:" +
                "\n  1. mysql-connector-j-9.7.0.jar is inside the lib/ folder" +
                "\n  2. You compiled with: javac -cp \"lib\\mysql-connector-j-9.7.0.jar;.\"" +
                "\n  3. You ran with: java -cp \"bin;lib\\mysql-connector-j-9.7.0.jar\"", e
            );
        }
    }

    // ── getConnection ─────────────────────────────────────────────────────
    /**
     * Opens and returns a fresh connection to the MySQL database.
     *
     * IMPORTANT: The CALLER is responsible for closing this connection.
     * All DAOs use try-with-resources which closes it automatically.
     *
     * Example of how DAOs use this:
     *
     *   try (Connection conn = ConnectionManager.getInstance().getConnection()) {
     *       // use conn here
     *   }
     *   // conn is automatically closed here, even if exception occurred
     *
     * Why a new connection each time (not one shared connection)?
     * For simplicity. One shared connection causes problems if two
     * operations run at the same time. In production you would use
     * a connection pool (like HikariCP), but that is beyond this project.
     *
     * @return a new open Connection to clinic_db
     * @throws SQLException if the database is down, credentials are wrong,
     *                      or the network has an issue
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}