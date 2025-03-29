package com.example.app.services;

import org.mindrot.jbcrypt.BCrypt;

import com.example.app.utils.DataBaseUtil;

import java.sql.*;

public class UserService {
    public static void registerUser(String email, String password)
            throws SQLException {
        if (emailExists(email)) {
            throw new SQLException("Diese E-Mail-Adresse ist bereits registriert");
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO benutzer (email, password) VALUES (?, ?)";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, hashed);
            pstmt.executeUpdate();
        }
    }

    public static boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM benutzer WHERE email = ?";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static boolean validateUser(String email, String password) throws SQLException {
        String sql = "SELECT password FROM benutzer WHERE email = ?";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashed = rs.getString("password");
                    System.out.println("Logging in user: " + email + ", hashed password: " + hashed);
                    return BCrypt.checkpw(password, hashed);
                } else {
                    System.out.println("No user found for email: " + email);
                }
            }
        }
        return false;
    }
}