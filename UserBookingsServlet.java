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

@WebServlet(urlPatterns = { "/api/my-bookings", "/api/my-bookings/*" })
public class UserBookingsServlet extends HttpServlet {
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
            List<AppointmentDTO> appointments = getUserAppointments(email);
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

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/cancel/")) {
            try {
                int appointmentId = Integer.parseInt(pathInfo.substring("/cancel/".length()));
                String email = (String) session.getAttribute("user");

                if (!AppointmentUtil.canCancelAppointment(appointmentId)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    objectMapper.writeValue(response.getWriter(),
                            new ErrorResponse("Termine können nur mindestens 1 Stunde vor Beginn storniert werden"));
                    return;
                }

                boolean success = cancelAppointment(appointmentId, email);
                if (success) {
                    objectMapper.writeValue(response.getWriter(),
                            Collections.singletonMap("success", "Appointment cancelled successfully"));
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    objectMapper.writeValue(response.getWriter(),
                            new ErrorResponse(
                                    "Could not cancel appointment. It might not exist or you don't have permission."));
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid appointment ID"));
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                objectMapper.writeValue(response.getWriter(),
                        new ErrorResponse("Error cancelling appointment: " + e.getMessage()));
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid request"));
        }
    }

    private List<AppointmentDTO> getUserAppointments(String email) throws SQLException {
        List<AppointmentDTO> appointments = new ArrayList<>();

        String sql = "SELECT t.id, t.patient_id, t.zeitslot_id, t.impfstoff_id, t.status, " +
                "t.created_at, t.cancelled_at, t.qr_code, z.start_time, z.end_time, " +
                "CONCAT('Patient ', p.id) AS patient_name, iz.name AS center_name, i.name AS vaccine_name " +
                "FROM termin t " +
                "JOIN patient p ON t.patient_id = p.id " +
                "JOIN zeitslot z ON t.zeitslot_id = z.id " +
                "JOIN impfzentrum iz ON z.impfzentrum_id = iz.id " +
                "JOIN impfstoff i ON t.impfstoff_id = i.id " +
                "JOIN benutzer b ON p.benutzer_id = b.id " +
                "WHERE b.email = ? " +
                "ORDER BY z.start_time DESC";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

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

                    appointment.setAdditionalInfo("startTime", rs.getTimestamp("start_time"));
                    appointment.setAdditionalInfo("endTime", rs.getTimestamp("end_time"));
                    appointment.setAdditionalInfo("patientName", rs.getString("patient_name"));
                    appointment.setAdditionalInfo("centerName", rs.getString("center_name"));
                    appointment.setAdditionalInfo("vaccineName", rs.getString("vaccine_name"));

                    appointments.add(appointment);
                }
            }
        }

        return appointments;
    }

    private boolean cancelAppointment(int appointmentId, String email) throws SQLException {
        Connection conn = null;
        boolean success = false;

        try {
            conn = DataBaseUtil.getConnection();
            conn.setAutoCommit(false);

            String verifySql = "SELECT t.id, t.status, t.zeitslot_id " +
                    "FROM termin t " +
                    "JOIN patient p ON t.patient_id = p.id " +
                    "JOIN benutzer b ON p.benutzer_id = b.id " +
                    "WHERE t.id = ? AND b.email = ? FOR UPDATE";

            try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                verifyStmt.setInt(1, appointmentId);
                verifyStmt.setString(2, email);

                try (ResultSet rs = verifyStmt.executeQuery()) {
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if (!"CANCELLED".equals(status)) {
                            int timeSlotId = rs.getInt("zeitslot_id");

                            String updateSql = "UPDATE termin SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP "
                                    +
                                    "WHERE id = ?";

                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setInt(1, appointmentId);
                                int updatedRows = updateStmt.executeUpdate();

                                if (updatedRows > 0) {
                                    String decrementSql = "UPDATE zeitslot SET booked_count = booked_count - 1 " +
                                            "WHERE id = ? AND booked_count > 0";

                                    try (PreparedStatement decrementStmt = conn.prepareStatement(decrementSql)) {
                                        decrementStmt.setInt(1, timeSlotId);
                                        decrementStmt.executeUpdate();

                                        String releaseSql = "UPDATE impfzentrum_impfstoff SET quantity = quantity + 1 "
                                                +
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