package com.example.app.servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.example.app.auth.AuthHelper;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/session/admin" })
public class AdminSessionServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Map<String, Object> result = new HashMap<>();

        result.put("isAdmin", false);
        result.put("centerId", null);

        if (session != null && session.getAttribute("user") != null) {
            String email = (String) session.getAttribute("user");

            try {
                boolean isAdmin = AuthHelper.isUserAdmin(email);
                result.put("isAdmin", isAdmin);

                if (isAdmin) {
                    int centerId = AuthHelper.getAdminCenterId(email);
                    result.put("centerId", centerId);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), result);
    }
}
