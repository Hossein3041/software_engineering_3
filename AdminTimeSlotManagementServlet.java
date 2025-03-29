package com.example.app.servlets;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;

import com.example.app.auth.AuthHelper;
import com.example.app.dto.ErrorResponse;
import com.example.app.dto.TimeSlotDTO;
import com.example.app.utils.DataBaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/admin/timeslots", "/api/admin/timeslots/*" })
public class AdminTimeSlotManagementServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
            if (!AuthHelper.isUserAdmin(email)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Access denied: Not an admin"));
                return;
            }

            int centerId = AuthHelper.getAdminCenterId(email);

            try {
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = request.getReader().readLine()) != null) {
                    requestBody.append(line);
                }
                System.out.println("Raw time slot creation request: " + requestBody.toString());

                TimeSlotDTO timeSlot = objectMapper.readValue(requestBody.toString(), TimeSlotDTO.class);
                timeSlot.setCenterId(centerId);

                System.out.println("Parsed TimeSlotDTO: " + timeSlot.toString());
                System.out.println("  Center ID: " + timeSlot.getCenterId());
                System.out.println("  Start Time: " + timeSlot.getStartTime());
                System.out.println("  End Time: " + timeSlot.getEndTime());
                System.out.println("  Capacity: " + timeSlot.getCapacity());

                validateTimeSlot(timeSlot);

                int newSlotId = createTimeSlot(timeSlot);

                if (newSlotId > 0) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Zeitslot erfolgreich erstellt");
                    result.put("slotId", newSlotId);
                    objectMapper.writeValue(response.getWriter(), result);
                    System.out.println("Successfully created time slot with ID: " + newSlotId);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    objectMapper.writeValue(response.getWriter(),
                            new ErrorResponse("Fehler beim Erstellen des Zeitslots"));
                    System.out.println("Failed to create time slot - no ID returned");
                }
            } catch (Exception e) {
                System.err.println("Error parsing time slot request: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("Error parsing request: " + e.getMessage()));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse(e.getMessage()));
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Datenbankfehler: " + e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse("User not authenticated"));
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid time slot ID"));
            return;
        }

        int timeSlotId = Integer.parseInt(pathInfo.substring(1));
        String email = (String) session.getAttribute("user");

        try {
            if (!AuthHelper.isUserAdmin(email)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Access denied: Not an admin"));
                return;
            }

            int centerId = AuthHelper.getAdminCenterId(email);

            if (!isTimeSlotInCenter(timeSlotId, centerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("Access denied: Time slot does not belong to your center"));
                return;
            }

            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                requestBody.append(line);
            }
            System.out.println("Raw time slot update request: " + requestBody.toString());

            Map<String, Object> updates = objectMapper.readValue(requestBody.toString(),
                    new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {
                    });
            boolean success = updateTimeSlot(timeSlotId, updates);

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Zeitslot erfolgreich aktualisiert");
                objectMapper.writeValue(response.getWriter(), result);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("Fehler beim Aktualisieren des Zeitslots"));
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse(e.getMessage()));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Datenbankfehler: " + e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse("User not authenticated"));
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid time slot ID"));
            return;
        }

        int timeSlotId = Integer.parseInt(pathInfo.substring(1));
        String email = (String) session.getAttribute("user");

        try {
            if (!AuthHelper.isUserAdmin(email)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Access denied: Not an admin"));
                return;
            }

            int centerId = AuthHelper.getAdminCenterId(email);

            if (!isTimeSlotInCenter(timeSlotId, centerId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("Access denied: Time slot does not belong to your center"));
                return;
            }

            if (hasBookings(timeSlotId)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("Cannot delete time slot with existing bookings"));
                return;
            }

            boolean success = deleteTimeSlot(timeSlotId);

            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Zeitslot erfolgreich gelöscht");
                objectMapper.writeValue(response.getWriter(), result);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Fehler beim Löschen des Zeitslots"));
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Datenbankfehler: " + e.getMessage()));
        }
    }

    private void validateTimeSlot(TimeSlotDTO timeSlot) {
        System.out.println("Validating time slot...");

        if (timeSlot.getStartTime() == null || timeSlot.getEndTime() == null) {
            throw new IllegalArgumentException("Start- und Endzeit müssen angegeben werden");
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = formatter.format(timeSlot.getStartTime());
        String endTimeStr = formatter.format(timeSlot.getEndTime());

        System.out.println("Validating time slot - Start: " + startTimeStr + ", End: " + endTimeStr);

        if (timeSlot.getStartTime().after(timeSlot.getEndTime())) {
            throw new IllegalArgumentException("Die Startzeit muss vor der Endzeit liegen");
        }

        if (timeSlot.getCapacity() <= 0) {
            throw new IllegalArgumentException("Die Kapazität muss größer als 0 sein");
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (timeSlot.getStartTime().before(now)) {
            System.out.println("Warning: Start time is in the past. Current time: " +
                    formatter.format(now) + ", Start time: " + startTimeStr);
            throw new IllegalArgumentException("Die Startzeit muss in der Zukunft liegen");
        }

        try (Connection conn = DataBaseUtil.getConnection()) {
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP")) {
                if (rs.next()) {
                    System.out.println("Database CURRENT_TIMESTAMP: " + rs.getTimestamp(1));
                }
            }

            String sql = "SELECT id, start_time, end_time FROM zeitslot " +
                    "WHERE impfzentrum_id = ? AND " +
                    "((? BETWEEN start_time AND end_time) OR " +
                    "(? BETWEEN start_time AND end_time) OR " +
                    "(start_time BETWEEN ? AND ?) OR " +
                    "(end_time BETWEEN ? AND ?))";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, timeSlot.getCenterId());
                stmt.setTimestamp(2, timeSlot.getStartTime());
                stmt.setTimestamp(3, timeSlot.getEndTime());
                stmt.setTimestamp(4, timeSlot.getStartTime());
                stmt.setTimestamp(5, timeSlot.getEndTime());
                stmt.setTimestamp(6, timeSlot.getStartTime());
                stmt.setTimestamp(7, timeSlot.getEndTime());

                System.out.println("Executing overlap check query...");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int conflictingSlotId = rs.getInt("id");
                        Timestamp conflictStart = rs.getTimestamp("start_time");
                        Timestamp conflictEnd = rs.getTimestamp("end_time");

                        System.out.println("Found overlapping slot: ID=" + conflictingSlotId +
                                ", Start=" + formatter.format(conflictStart) +
                                ", End=" + formatter.format(conflictEnd));

                        throw new IllegalArgumentException(
                                "Der Zeitslot überschneidet sich mit bestehenden Zeitslots: " +
                                        formatter.format(conflictStart) + " - " + formatter.format(conflictEnd));
                    }
                }
            }

            System.out.println("Time slot validation successful");
        } catch (SQLException e) {
            System.err.println("Database error during time slot validation: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalArgumentException("Fehler bei der Validierung des Zeitslots: " + e.getMessage());
        }
    }

    private int createTimeSlot(TimeSlotDTO timeSlot) throws SQLException {
        System.out.println("Creating time slot in database...");
        String sql = "INSERT INTO zeitslot (impfzentrum_id, start_time, end_time, capacity, booked_count) VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, timeSlot.getCenterId());
            stmt.setTimestamp(2, timeSlot.getStartTime());
            stmt.setTimestamp(3, timeSlot.getEndTime());
            stmt.setInt(4, timeSlot.getCapacity());

            System.out.println("Executing insert statement:");
            System.out.println("  Center ID: " + timeSlot.getCenterId());
            System.out.println("  Start Time: " + timeSlot.getStartTime());
            System.out.println("  End Time: " + timeSlot.getEndTime());
            System.out.println("  Capacity: " + timeSlot.getCapacity());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        System.out.println("Successfully created time slot with ID: " + newId);
                        return newId;
                    }
                }
            }

            System.out.println("Failed to create time slot - no rows affected or no ID returned");
            return 0;
        } catch (SQLException e) {
            System.err.println("Database error creating time slot: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private boolean isTimeSlotInCenter(int timeSlotId, int centerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM zeitslot WHERE id = ? AND impfzentrum_id = ?";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, timeSlotId);
            stmt.setInt(2, centerId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean hasBookings(int timeSlotId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM termin WHERE zeitslot_id = ? AND status != 'CANCELLED'";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, timeSlotId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean updateTimeSlot(int timeSlotId, Map<String, Object> updates) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("UPDATE zeitslot SET ");
        boolean hasUpdates = false;

        if (updates.containsKey("capacity")) {
            int newCapacity = Integer.parseInt(updates.get("capacity").toString());

            String checkSql = "SELECT booked_count FROM zeitslot WHERE id = ?";
            try (Connection conn = DataBaseUtil.getConnection();
                    PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

                checkStmt.setInt(1, timeSlotId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int bookedCount = rs.getInt("booked_count");
                        if (newCapacity < bookedCount) {
                            throw new IllegalArgumentException(
                                    "Die neue Kapazität kann nicht kleiner als die aktuelle Buchungszahl sein");
                        }
                    }
                }
            }

            if (hasUpdates) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("capacity = ?");
            hasUpdates = true;
        }

        if (!hasUpdates) {
            return false;
        }

        sqlBuilder.append(" WHERE id = ?");

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            int paramIndex = 1;

            if (updates.containsKey("capacity")) {
                stmt.setInt(paramIndex++, Integer.parseInt(updates.get("capacity").toString()));
            }

            stmt.setInt(paramIndex, timeSlotId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    private boolean deleteTimeSlot(int timeSlotId) throws SQLException {
        String sql = "DELETE FROM zeitslot WHERE id = ?";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, timeSlotId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}