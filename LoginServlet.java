package com.example.app.servlets;

import com.example.app.auth.AuthHelper;
import com.example.app.dto.AuthResponse;
import com.example.app.dto.LoginRequest;
import com.example.app.services.UserService;
import com.example.app.utils.SimpleDbLatency;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet(urlPatterns = { "/api/login" })
public class LoginServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");

        long startTime = System.currentTimeMillis();

        try {
            LoginRequest loginRequest = objectMapper.readValue(
                    request.getInputStream(),
                    LoginRequest.class);
            System.out.println("Logging in user: " + loginRequest.getEmail());

            boolean isValid = validateUserWithLatency(loginRequest.getEmail(), loginRequest.getPassword());

            Map<String, Object> result = new HashMap<>();

            if (!isValid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                result.put("success", false);
                result.put("message", "Invalid credentials");
            } else {
                HttpSession session = request.getSession();
                session.setAttribute("user", loginRequest.getEmail());

                result.put("success", true);
                result.put("message", "Login successful");

                try {
                    SimpleDbLatency.simulateLatency();
                    boolean isAdmin = AuthHelper.isUserAdmin(loginRequest.getEmail());
                    result.put("isAdmin", isAdmin);

                    if (isAdmin) {
                        SimpleDbLatency.simulateLatency();
                        int centerId = AuthHelper.getAdminCenterId(loginRequest.getEmail());
                        result.put("centerId", centerId);
                    }
                } catch (SQLException e) {
                    System.err.println("Error checking admin status: " + e.getMessage());
                }
            }

            long responseTime = System.currentTimeMillis() - startTime;
            result.put("responseTimeMs", responseTime);

            objectMapper.writeValue(response.getWriter(), result);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(
                    response.getWriter(),
                    Map.of(
                            "success", false,
                            "message", "Server error: " + e.getMessage()));
        }
    }

    private boolean validateUserWithLatency(String email, String password) throws SQLException {
        String sql = "SELECT password FROM benutzer WHERE email = ?";

        SimpleDbLatency.simulateLatency();

        try (Connection conn = com.example.app.utils.DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");
                    return BCrypt.checkpw(password, hashedPassword);
                }
            }
        }

        return false;
    }
}