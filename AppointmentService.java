package com.example.app.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Base64;

import com.example.app.Exception.BookingException;
import com.example.app.dto.BookingRequest;
import com.example.app.dto.PatientDTO;
import com.example.app.dto.TimeSlotDTO;
import com.example.app.dto.UserDto;
import com.example.app.utils.DataBaseUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AppointmentService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_BOOKINGS_PER_USER = 5;

    public static boolean bookAppointment(BookingRequest bookingRequest, HttpServletRequest httpRequest)
            throws SQLException, BookingException {
        Connection conn = null;
        try {
            conn = DataBaseUtil.getConnection();
            conn.setAutoCommit(false);

            HttpSession session = validateSession(httpRequest);
            String email = (String) session.getAttribute("user");
            UserDto user = getUserByEmail(conn, email);

            validateSlotCenterMatch(conn, bookingRequest.getSlotId(), bookingRequest.getCenterId());

            int currentBookingCount = getUserBookingCount(conn, user.getId());
            int requestedBookings = bookingRequest.getBookingFor().equalsIgnoreCase("self") ? 1
                    : (bookingRequest.getPatientCount() == 0 ? 1 : bookingRequest.getPatientCount());

            if (currentBookingCount + requestedBookings > MAX_BOOKINGS_PER_USER) {
                throw new BookingException("Maximum booking limit exceeded. You can only have up to "
                        + MAX_BOOKINGS_PER_USER + " active bookings.");
            }

            TimeSlotDTO timeSlot = getAndLockTimeSlot(conn, bookingRequest.getSlotId());
            validateSlotCapacity(timeSlot, requestedBookings);

            List<PatientDTO> patients = createRequiredPatients(conn, user, requestedBookings);

            reserveVaccines(conn, bookingRequest.getCenterId(), bookingRequest.getVaccineId(), patients.size());

            createAppointments(conn, patients, bookingRequest, timeSlot);

            updateTimeSlotBooking(conn, bookingRequest.getSlotId(), patients.size());

            conn.commit();
            return true;

        } catch (SQLException e) {
            rollbackTransaction(conn);
            throw new BookingException("Database error during booking", e);
        } catch (BookingException e) {
            rollbackTransaction(conn);
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    private static void validateSlotCenterMatch(Connection conn, int slotId, int centerId)
            throws SQLException, BookingException {
        String sql = "SELECT impfzentrum_id FROM zeitslot WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new BookingException("Time slot not found");
                }
                int slotCenterId = rs.getInt("impfzentrum_id");
                if (slotCenterId != centerId) {
                    throw new BookingException("Time slot does not belong to the selected center");
                }
            }
        }
    }

    private static int getUserBookingCount(Connection conn, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM termin t " +
                "JOIN patient p ON t.patient_id = p.id " +
                "WHERE p.benutzer_id = ? AND t.status = 'BOOKED'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    private static HttpSession validateSession(HttpServletRequest request) throws BookingException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            throw new BookingException("User not authenticated");
        }
        return session;
    }

    private static TimeSlotDTO getAndLockTimeSlot(Connection conn, int slotId) throws SQLException {
        String sql = "SELECT * FROM zeitslot WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, slotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Time slot not found");
                }
                return mapTimeSlot(rs);
            }
        }
    }

    private static TimeSlotDTO mapTimeSlot(ResultSet rs) throws SQLException {
        TimeSlotDTO timeSlot = new TimeSlotDTO();
        timeSlot.setId(rs.getInt("id"));
        timeSlot.setCenterId(rs.getInt("impfzentrum_id"));
        timeSlot.setStartTime(rs.getTimestamp("start_time"));
        timeSlot.setEndTime(rs.getTimestamp("end_time"));
        timeSlot.setCapacity(rs.getInt("capacity"));
        timeSlot.setBookedCount(rs.getInt("booked_count"));
        return timeSlot;
    }

    private static void validateSlotCapacity(TimeSlotDTO slot, int requestedSlots) throws BookingException {
        if (slot.getBookedCount() + requestedSlots > slot.getCapacity()) {
            throw new BookingException("Not enough available slots");
        }
    }

    private static List<PatientDTO> createRequiredPatients(Connection conn, UserDto user, int count)
            throws SQLException, BookingException {
        List<PatientDTO> patients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PatientDTO patient = createPatient(conn, user.getId());
            patients.add(patient);
        }
        return patients;
    }

    private static PatientDTO createPatient(Connection conn, int userId) throws SQLException {
        String sql = "INSERT INTO patient (benutzer_id) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating patient failed, no rows affected.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    PatientDTO patient = new PatientDTO();
                    patient.setId(generatedKeys.getInt(1));
                    patient.setBenutzerId(userId);
                    return patient;
                } else {
                    throw new SQLException("Creating patient failed, no ID obtained.");
                }
            }
        }
    }

    private static void reserveVaccines(Connection conn, int centerId, int vaccineId, int quantity)
            throws SQLException, BookingException {
        String sql = "UPDATE impfzentrum_impfstoff " +
                "SET quantity = quantity - ? " +
                "WHERE impfzentrum_id = ? AND impfstoff_id = ? " +
                "AND quantity >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, centerId);
            stmt.setInt(3, vaccineId);
            stmt.setInt(4, quantity);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new BookingException("Insufficient vaccine stock");
            }
        }
    }

    private static void createAppointments(Connection conn, List<PatientDTO> patients, BookingRequest request,
            TimeSlotDTO timeSlot)
            throws SQLException {
        String sql = "INSERT INTO termin (patient_id, zeitslot_id, impfstoff_id, status, qr_code) " +
                "VALUES (?, ?, ?, ?, ?)";

        String verifySql = "SELECT impfzentrum_id FROM zeitslot WHERE id = ?";
        try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
            verifyStmt.setInt(1, request.getSlotId());

            try (ResultSet rs = verifyStmt.executeQuery()) {
                if (!rs.next() || rs.getInt("impfzentrum_id") != request.getCenterId()) {
                    throw new SQLException("Time slot does not belong to the specified center");
                }
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (PatientDTO patient : patients) {
                String qrCodeData = QRCodeService.generateQRCodeData(patient, request, timeSlot);

                stmt.setInt(1, patient.getId());
                stmt.setInt(2, request.getSlotId());
                stmt.setInt(3, request.getVaccineId());
                stmt.setString(4, "BOOKED");
                stmt.setString(5, qrCodeData);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private static void updateTimeSlotBooking(Connection conn, int slotId, int increment) throws SQLException {
        String sql = "UPDATE zeitslot SET booked_count = booked_count + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, increment);
            stmt.setInt(2, slotId);
            stmt.executeUpdate();
        }
    }

    private static void rollbackTransaction(Connection conn) {
        try {
            if (conn != null && !conn.getAutoCommit()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void closeConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static UserDto getUserByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT * FROM benutzer WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserDto user = new UserDto();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    int rolleId = rs.getInt("rolle_id");
                    user.setRolleId(rs.wasNull() ? null : rolleId);
                    int impfzentrumId = rs.getInt("impfzentrum_id");
                    user.setImpfzentrumId(rs.wasNull() ? null : impfzentrumId);
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
                throw new SQLException("User not found with email: " + email);
            }
        }
    }
}