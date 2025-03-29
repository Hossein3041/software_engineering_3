package com.example.app.servlets;

import com.example.app.dto.ErrorResponse;
import com.example.app.utils.DataBaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
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
import java.util.Collections;

@WebServlet(urlPatterns = { "/api/my-bookings/count" })
public class BookingCountServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse("User not authenticated"));
            return;
        }

        String email = (String) session.getAttribute("user");
        try {
            int count = getActiveBookingsCount(email);
            objectMapper.writeValue(response.getWriter(), Collections.singletonMap("count", count));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Error fetching booking count: " + e.getMessage()));
        }
    }

    private int getActiveBookingsCount(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM termin t " +
                "JOIN patient p ON t.patient_id = p.id " +
                "JOIN benutzer b ON p.benutzer_id = b.id " +
                "WHERE b.email = ? AND t.status = 'BOOKED'";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }
}