package com.example.app.servlets;

import com.example.app.dto.AppointmentDTO;
import com.example.app.dto.ErrorResponse;
import com.example.app.utils.AppointmentUtil;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(urlPatterns = { "/api/admin/appointments", "/api/admin/appointments/*" })
public class AdminAppointmentsServlet extends HttpServlet {
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

            if (!isUserAdmin(email)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Access denied: Not an admin"));
                return;
            }

            int centerId = getAdminCenterId(email);
            List<AppointmentDTO> appointments = getCenterAppointments(centerId);
            objectMapper.writeValue(response.getWriter(), appointments);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Error fetching appointments: " + e.getMessage()));
        }
    }

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
            if (!isUserAdmin(email)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Access denied: Not an admin"));
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith("/cancel/")) {
                try {
                    int appointmentId = Integer.parseInt(pathInfo.substring("/cancel/".length()));
                    int centerId = getAdminCenterId(email);

                    if (!isAppointmentInCenter(appointmentId, centerId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        objectMapper.writeValue(response.getWriter(),
                                new ErrorResponse("Access denied: Appointment not in your center"));
                        return;
                    }

                    if (!AppointmentUtil.canCancelAppointment(appointmentId)) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        objectMapper.writeValue(response.getWriter(),
                                new ErrorResponse(
                                        "Termine können nur mindestens 1 Stunde vor Beginn storniert werden"));
                        return;
                    }

                    boolean success = cancelAppointment(appointmentId);
                    if (success) {
                        objectMapper.writeValue(response.getWriter(),
                                Collections.singletonMap("success", "Appointment cancelled successfully"));
                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        objectMapper.writeValue(response.getWriter(),
                                new ErrorResponse("Could not cancel appointment."));
                    }
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid appointment ID"));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid request"));
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Error processing request: " + e.getMessage()));
        }
    }

    private boolean isUserAdmin(String email) throws SQLException {
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

    private int getAdminCenterId(String email) throws SQLException {
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

    private boolean isAppointmentInCenter(int appointmentId, int centerId) throws SQLException {
        String sql = "SELECT 1 FROM termin t " +
                "JOIN zeitslot z ON t.zeitslot_id = z.id " +
                "WHERE t.id = ? AND z.impfzentrum_id = ?";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, appointmentId);
            stmt.setInt(2, centerId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<AppointmentDTO> getCenterAppointments(int centerId) throws SQLException {
        List<AppointmentDTO> appointments = new ArrayList<>();

        String sql = "SELECT t.id, t.patient_id, t.zeitslot_id, t.impfstoff_id, t.status, " +
                "t.created_at, t.cancelled_at, t.qr_code, z.start_time, z.end_time, " +
                "b.email as patient_email, iz.name AS center_name, i.name AS vaccine_name " +
                "FROM termin t " +
                "JOIN patient p ON t.patient_id = p.id " +
                "JOIN benutzer b ON p.benutzer_id = b.id " +
                "JOIN zeitslot z ON t.zeitslot_id = z.id " +
                "JOIN impfzentrum iz ON z.impfzentrum_id = iz.id " +
                "JOIN impfstoff i ON t.impfstoff_id = i.id " +
                "WHERE z.impfzentrum_id = ? " +
                "ORDER BY z.start_time DESC";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, centerId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AppointmentDTO appointment = new AppointmentDTO();
                    appointment.setId(rs.getInt("id"));
                    appointment.setPatientId(rs.getInt("patient_id"));
                    appointment.setTimeSlotId(rs.getInt("zeitslot_id"));
                    appointment.setVaccineId(rs.getInt("impfstoff_id"));
                    appointment.setStatus(rs.getString("status"));
                    appointment.setCreatedAt(rs.getTimestamp("created_at"));
                    appointment.setCancelledAt(rs.getTimestamp("cancelled_at"));
                    appointment.setQrCode(rs.getString("qr_code"));

                    appointment.setStartTime(rs.getTimestamp("start_time"));
                    appointment.setEndTime(rs.getTimestamp("end_time"));
                    appointment.setAdditionalInfo("patientEmail", rs.getString("patient_email"));
                    appointment.setCenterName(rs.getString("center_name"));
                    appointment.setVaccineName(rs.getString("vaccine_name"));

                    appointments.add(appointment);
                }
            }
        }

        return appointments;
    }

    private boolean cancelAppointment(int appointmentId) throws SQLException {
        Connection conn = null;
        boolean success = false;

        try {
            conn = DataBaseUtil.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT status, zeitslot_id FROM termin WHERE id = ? FOR UPDATE";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, appointmentId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if ("CANCELLED".equals(status)) {
                            conn.commit();
                            return true;
                        }

                        int timeSlotId = rs.getInt("zeitslot_id");

                        String updateSql = "UPDATE termin SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, appointmentId);
                            int updatedRows = updateStmt.executeUpdate();

                            if (updatedRows > 0) {
                                String decrementSql = "UPDATE zeitslot SET booked_count = booked_count - 1 WHERE id = ? AND booked_count > 0";
                                try (PreparedStatement decrementStmt = conn.prepareStatement(decrementSql)) {
                                    decrementStmt.setInt(1, timeSlotId);
                                    decrementStmt.executeUpdate();

                                    String releaseSql = "UPDATE impfzentrum_impfstoff SET quantity = quantity + 1 " +
                                            "WHERE (impfzentrum_id, impfstoff_id) IN " +
                                            "(SELECT z.impfzentrum_id, t.impfstoff_id " +
                                            "FROM termin t JOIN zeitslot z ON t.zeitslot_id = z.id " +
                                            "WHERE t.id = ?)";

                                    try (PreparedStatement releaseStmt = conn.prepareStatement(releaseSql)) {
                                        releaseStmt.setInt(1, appointmentId);
                                        releaseStmt.executeUpdate();
                                        success = true;
                                    } catch (SQLException e) {
                                        System.err.println("Warning: Failed to release vaccine: " + e.getMessage());
                                        success = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return success;
    }
}
