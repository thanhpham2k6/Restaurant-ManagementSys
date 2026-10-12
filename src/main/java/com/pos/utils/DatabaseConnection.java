package com.pos.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final Logger log = LogManager.getLogger(DatabaseConnection.class);
    private static volatile DatabaseConnection instance;
    private HikariDataSource ds;

    private static String dbHost = "localhost";
    private static String dbPort = "1433";
    private static String dbName = "RestaurantManagementSys";
    private static String dbUser = "sa";
    private static String dbPass = "12345";

    static {
        loadConfig();
    }

    private DatabaseConnection() {
        initDataSource();
    }

    public static synchronized void loadConfig() {
        Properties props = new Properties();
        File externalFile = new File("db.properties");
        if (externalFile.exists()) {
            try (InputStream in = new FileInputStream(externalFile)) {
                props.load(in);
                log.info("Loaded config from external db.properties");
            } catch (IOException e) {
                log.warn("Cannot read external db.properties", e);
            }
        } else {
            try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    log.info("Loaded config from classpath db.properties");
                }
            } catch (IOException e) {
                log.warn("Cannot read classpath db.properties", e);
            }
        }

        if (props.containsKey("db.host")) dbHost = props.getProperty("db.host").trim();
        if (props.containsKey("db.port")) dbPort = props.getProperty("db.port").trim();
        if (props.containsKey("db.name")) dbName = props.getProperty("db.name").trim();
        if (props.containsKey("db.user")) dbUser = props.getProperty("db.user").trim();
        if (props.containsKey("db.password")) dbPass = props.getProperty("db.password").trim();
    }

    public static void saveConfig(String host, String port, String name, String user, String pass) throws IOException {
        dbHost = host;
        dbPort = port;
        dbName = name;
        dbUser = user;
        dbPass = pass;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.name", name);
        props.setProperty("db.user", user);
        props.setProperty("db.password", pass);

        try (OutputStream out = new FileOutputStream("db.properties")) {
            props.store(out, "Database Configuration for Restaurant POS");
        }
    }

    private synchronized void initDataSource() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }

        String jdbcUrl = buildJdbcUrl(dbHost, dbPort, dbName);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(dbUser);
        cfg.setPassword(dbPass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(6_000); // 6s timeout để không bị treo giao diện
        cfg.setIdleTimeout(600_000);
        cfg.setMaxLifetime(1_800_000);
        cfg.setPoolName("RestaurantPool");

        try {
            ds = new HikariDataSource(cfg);
            log.info("HikariCP pool initialized successfully to {}", jdbcUrl);
        } catch (Exception e) {
            log.error("Failed to initialize HikariCP pool to {}: {}", jdbcUrl, e.getMessage());
            ds = null;
        }
    }

    public static String buildJdbcUrl(String host, String port, String name) {
        if (host.contains("\\")) {
            // Named instance như localhost\SQLEXPRESS
            return "jdbc:sqlserver://" + host + ";databaseName=" + name + ";encrypt=true;trustServerCertificate=true;";
        }
        return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name + ";encrypt=true;trustServerCertificate=true;";
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (ds == null || ds.isClosed()) {
            initDataSource();
        }
        if (ds == null) {
            throw new SQLException("Không thể kết nối CSDL tới máy chủ " + dbHost + ":" + dbPort + " (Tên CSDL: " + dbName + ")");
        }
        return ds.getConnection();
    }

    public synchronized void reload() {
        initDataSource();
    }

    /** Kiểm tra kết nối thử và trả về chuỗi thông báo lỗi (hoặc null nếu kết nối thành công) */
    public static String testConnection(String host, String port, String name, String user, String pass) {
        String url = buildJdbcUrl(host, port, name);
        try {
            DriverManager.setLoginTimeout(5);
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                if (conn != null && !conn.isClosed()) {
                    return null; // Thành công
                }
            }
        } catch (SQLException e) {
            return e.getMessage();
        }
        return "Kết nối thất bại không rõ nguyên nhân.";
    }

    public static String getDbHost() { return dbHost; }
    public static String getDbPort() { return dbPort; }
    public static String getDbName() { return dbName; }
    public static String getDbUser() { return dbUser; }
    public static String getDbPass() { return dbPass; }

    public void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}
