package com.example.app.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.app.auth.AuthHelper;
import com.example.app.dto.LoginRequest;
import com.example.app.utils.DataBaseUtil;
import com.example.app.utils.SimpleDbLatency;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet(urlPatterns = { "/api/vt/login" }, asyncSupported = true)
public class VTLoginWithLatency extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        long startTime = System.currentTimeMillis();

        final AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(30000);

        try {
            LoginRequest loginRequest = objectMapper.readValue(
                    request.getInputStream(),
                    LoginRequest.class);

            executor.submit(() -> {
                try {
                    boolean isValid = validateUserWithLatency(loginRequest.getEmail(), loginRequest.getPassword());

                    Map<String, Object> result = new HashMap<>();

                    if (!isValid) {
                        result.put("success", false);
                        result.put("message", "Invalid credentials");
                    } else {
                        result.put("success", true);
                        result.put("message", "Login successful");

                        HttpSession session = request.getSession();
                        session.setAttribute("user", loginRequest.getEmail());

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

                    HttpServletResponse asyncResponse = (HttpServletResponse) asyncContext.getResponse();
                    objectMapper.writeValue(asyncResponse.getWriter(), result);
                } catch (Exception e) {
                    handleError(asyncContext, e);
                } finally {
                    asyncContext.complete();
                }
            });
        } catch (Exception e) {
            handleError(asyncContext, e);
            asyncContext.complete();
        }
    }

    private void handleError(AsyncContext asyncContext, Exception e) {
        try {
            HttpServletResponse asyncResponse = (HttpServletResponse) asyncContext.getResponse();
            asyncResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(
                    asyncResponse.getWriter(),
                    Map.of(
                            "success", false,
                            "message", "Server error: " + e.getMessage()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean validateUserWithLatency(String email, String password) throws SQLException {
        String sql = "SELECT password FROM benutzer WHERE email = ?";

        SimpleDbLatency.simulateLatency();

        try (Connection conn = DataBaseUtil.getConnection();
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