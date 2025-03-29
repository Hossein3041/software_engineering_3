package com.example.app.utils;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DataBaseUtil {

    private static final DataSource dataSource;

    static {
        Properties props = new Properties();
        try (InputStream is = DataBaseUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                throw new RuntimeException("application.properties not found in classpath!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading application.properties", e);
        }

        PoolProperties poolProps = new PoolProperties();
        poolProps.setUrl(props.getProperty("jdbc.url"));
        poolProps.setDriverClassName(props.getProperty("jdbc.driverClassName"));
        poolProps.setUsername(props.getProperty("jdbc.username"));
        poolProps.setPassword(props.getProperty("jdbc.password"));
        poolProps.setMaxActive(Integer.parseInt(props.getProperty("pool.maxActive", "10")));
        poolProps.setInitialSize(Integer.parseInt(props.getProperty("pool.initialSize", "5")));
        poolProps.setMaxIdle(10);
        poolProps.setMinIdle(5);
        poolProps.setValidationInterval(30000);
        poolProps.setFairQueue(true);
        poolProps.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("pool.testOnBorrow", "true")));
        poolProps.setValidationQuery(props.getProperty("pool.validationQuery", "SELECT 1"));
        poolProps.setMaxWait(30000);
        poolProps.setRemoveAbandoned(true);
        poolProps.setRemoveAbandonedTimeout(60);
        poolProps.setLogAbandoned(true);
        poolProps.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;" +
                "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");

        dataSource = new DataSource();
        dataSource.setPoolProperties(poolProps);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void initializeDatabaseFromSQLFile() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                InputStream is = DataBaseUtil.class.getClassLoader().getResourceAsStream("init.sql")) {

            if (is == null) {
                throw new RuntimeException("init.sql not found in classpath!");
            }

            String sql = new String(is.readAllBytes());
            String[] sqlStatements = sql.split(";\\s*[\r\n]+");

            for (String statement : sqlStatements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException e) {
                        System.err.println("Error executing SQL: " + trimmed);
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Database initialized successfully from SQL file");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database from SQL file", e);
        }
    }

    public static void initializeDatabase() {
        Properties props = new Properties();
        try (InputStream is = DataBaseUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                throw new RuntimeException("application.properties not found in classpath!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading application.properties", e);
        }

        ensureRolesExist();

        initializeCentersFromArray(props);

        initializeAdminUsersFromArray(props);
    }

    private static void ensureRolesExist() {
        try (Connection conn = getConnection()) {
            String checkAdminSql = "SELECT COUNT(*) FROM rolle WHERE name = 'ADMIN'";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(checkAdminSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO rolle (name) VALUES ('ADMIN')";
                    try (Statement insertStmt = conn.createStatement()) {
                        insertStmt.execute(insertSql);
                        System.out.println("Created ADMIN role");
                    }
                }
            }

            String checkUserSql = "SELECT COUNT(*) FROM rolle WHERE name = 'USER'";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(checkUserSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO rolle (name) VALUES ('USER')";
                    try (Statement insertStmt = conn.createStatement()) {
                        insertStmt.execute(insertSql);
                        System.out.println("Created USER role");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void initializeCentersFromArray(Properties props) {
        int index = 0;
        String nameKey = "centers[" + index + "].name";

        while (props.containsKey(nameKey)) {
            String name = props.getProperty(nameKey);
            String ort = props.getProperty("centers[" + index + "].ort");
            String strasse = props.getProperty("centers[" + index + "].strasse");
            String hausnummer = props.getProperty("centers[" + index + "].hausnummer");
            String plz = props.getProperty("centers[" + index + "].plz");
            String cabinsStr = props.getProperty("centers[" + index + "].cabins");

            if (name == null || ort == null || strasse == null || hausnummer == null || plz == null
                    || cabinsStr == null) {
                System.out.println("Center data not fully defined for index: " + index);
                index++;
                nameKey = "centers[" + index + "].name";
                continue;
            }

            int cabins;
            try {
                cabins = Integer.parseInt(cabinsStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid cabins count for center: " + cabinsStr);
                index++;
                nameKey = "centers[" + index + "].name";
                continue;
            }

            boolean centerExists = false;
            String checkSql = "SELECT COUNT(*) FROM impfzentrum WHERE name = ?";
            try (Connection conn = getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setString(1, name);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        centerExists = true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (!centerExists) {
                String sql = "INSERT INTO impfzentrum (name, ort, strasse, hausnummer, plz, number_of_cabins) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                try (Connection conn = getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, ort);
                    pstmt.setString(3, strasse);
                    pstmt.setString(4, hausnummer);
                    pstmt.setString(5, plz);
                    pstmt.setInt(6, cabins);
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int centerId = rs.getInt(1);
                            System.out.println("Center inserted: " + name + " with ID: " + centerId);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Center already exists: " + name);
            }

            index++;
            nameKey = "centers[" + index + "].name";
        }
    }

    private static void initializeAdminUsersFromArray(Properties props) {
        int index = 0;
        String emailKey = "admins[" + index + "].email";

        while (props.containsKey(emailKey)) {
            String email = props.getProperty(emailKey);
            String password = props.getProperty("admins[" + index + "].password");
            String centerIdStr = props.getProperty("admins[" + index + "].center");

            if (email == null || password == null || centerIdStr == null) {
                System.out.println("Admin user data not fully defined for index: " + index);
                index++;
                emailKey = "admins[" + index + "].email";
                continue;
            }

            int centerId;
            try {
                centerId = Integer.parseInt(centerIdStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid center ID for admin user: " + centerIdStr);
                index++;
                emailKey = "admins[" + index + "].email";
                continue;
            }

            if (userExists(email)) {
                System.out.println("Admin user already exists: " + email);
                index++;
                emailKey = "admins[" + index + "].email";
                continue;
            }

            boolean centerExists = false;
            String checkSql = "SELECT COUNT(*) FROM impfzentrum WHERE id = ?";
            try (Connection conn = getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, centerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        centerExists = true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (!centerExists) {
                System.out.println(
                        "Center with ID " + centerId + " does not exist. Admin user " + email + " not created.");
                index++;
                emailKey = "admins[" + index + "].email";
                continue;
            }

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

            String sql = "INSERT INTO benutzer (email, password, rolle_id, impfzentrum_id) " +
                    "VALUES (?, ?, 2, ?)";
            try (Connection conn = getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                pstmt.setString(2, hashed);
                pstmt.setInt(3, centerId);
                pstmt.executeUpdate();
                System.out.println("Admin user inserted: " + email + " for center ID: " + centerId);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            index++;
            emailKey = "admins[" + index + "].email";
        }
    }

    private static boolean userExists(String email) {
        String checkSql = "SELECT COUNT(*) FROM benutzer WHERE email = ?";
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void closePool() {
        dataSource.close();
    }
}