package com.example.app.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.app.utils.DataBaseUtil;

public class AuthHelper {

    public static boolean isUserAdmin(String email) throws SQLException {
        String sql = "SELECT r.name FROM benutzer b JOIN rolle r ON b.rolle_id = r.id WHERE b.email = ?";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "ADMIN".equals(rs.getString("name"));
                }
                return false;
            }
        }
    }

    public static int getAdminCenterId(String email) throws SQLException {
        String sql = "SELECT impfzentrum_id FROM benutzer WHERE email = ?";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("impfzentrum_id");
                }
                throw new SQLException("Center ID not found for admin: " + email);
            }
        }
    }
}