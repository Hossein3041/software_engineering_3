package com.example.app.servlets;

import com.example.app.dto.ErrorResponse;
import com.example.app.dto.Slot;
import com.example.app.utils.DataBaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Date;

@WebServlet(urlPatterns = { "/api/slots" })
public class SlotsServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        System.out.println("Slot request received with parameters:");
        System.out.println("  centerId: " + request.getParameter("centerId"));
        System.out.println("  date: " + request.getParameter("date"));

        String centerIdParam = request.getParameter("centerId");
        if (centerIdParam == null || centerIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("centerId parameter is required"));
            return;
        }

        int centerId;
        try {
            centerId = Integer.parseInt(centerIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Invalid centerId format: " + centerIdParam));
            return;
        }

        String dateParam = request.getParameter("date");

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Current Java timestamp: " + sdf.format(now));

        List<Slot> slots = new ArrayList<>();
        String sql;

        try (Connection conn = DataBaseUtil.getConnection()) {
            try (PreparedStatement dbTimeStmt = conn.prepareStatement("SELECT CURRENT_TIMESTAMP")) {
                try (ResultSet rs = dbTimeStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Database CURRENT_TIMESTAMP: " + rs.getTimestamp(1));
                    }
                }
            }

            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM zeitslot WHERE impfzentrum_id = ?")) {
                checkStmt.setInt(1, centerId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Total slots for center " + centerId + ": " + rs.getInt(1));
                    }
                }
            }

            if (dateParam != null && !dateParam.isEmpty()) {
                sql = "SELECT id, impfzentrum_id, start_time, end_time, capacity, booked_count " +
                        "FROM zeitslot WHERE impfzentrum_id = ? " +
                        "AND DATE(start_time) = ? " +
                        "AND start_time > CURRENT_TIMESTAMP " +
                        "ORDER BY start_time ASC";

                System.out.println("Using SQL with date filter: " + sql);
                System.out.println("Date parameter: " + dateParam);
            } else {
                sql = "SELECT id, impfzentrum_id, start_time, end_time, capacity, booked_count " +
                        "FROM zeitslot WHERE impfzentrum_id = ? AND start_time > CURRENT_TIMESTAMP " +
                        "ORDER BY start_time ASC";

                System.out.println("Using SQL without date filter: " + sql);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, centerId);
                if (dateParam != null && !dateParam.isEmpty()) {
                    pstmt.setString(2, dateParam);
                }

                System.out.println("Executing query...");
                try (ResultSet rs = pstmt.executeQuery()) {
                    int rowCount = 0;
                    while (rs.next()) {
                        rowCount++;
                        Slot slot = new Slot();
                        slot.setId(rs.getInt("id"));
                        slot.setImpfzentrumId(rs.getInt("impfzentrum_id"));
                        slot.setStartTime(rs.getString("start_time"));
                        slot.setEndTime(rs.getString("end_time"));
                        slot.setCapacity(rs.getInt("capacity"));
                        slot.setBookedCount(rs.getInt("booked_count"));

                        System.out.println("Found slot: ID=" + slot.getId() +
                                ", Start=" + slot.getStartTime() +
                                ", End=" + slot.getEndTime() +
                                ", Capacity=" + slot.getCapacity() +
                                ", Booked=" + slot.getBookedCount());

                        if (slot.getBookedCount() < slot.getCapacity()) {
                            slots.add(slot);
                        } else {
                            System.out.println("Skipping slot " + slot.getId() + " as it's fully booked");
                        }
                    }
                    System.out.println("Total rows returned from query: " + rowCount);
                    System.out.println("Total available slots after capacity filtering: " + slots.size());
                }
            }

            slots.sort(Comparator.comparing(Slot::getStartTime));

            objectMapper.writeValue(response.getWriter(), slots);
        } catch (Exception e) {
            System.err.println("Error fetching slots: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Error fetching slots: " + e.getMessage()));
        }
    }
}