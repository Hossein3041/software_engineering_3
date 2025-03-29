package com.example.app.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.UUID;

import com.example.app.dto.BookingRequest;
import com.example.app.dto.PatientDTO;
import com.example.app.dto.TimeSlotDTO;
import com.example.app.utils.DataBaseUtil;

public class QRCodeService {
    public static String generateQRCodeData(PatientDTO patient, BookingRequest request, TimeSlotDTO timeSlot) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

            String startTime = timeFormat.format(timeSlot.getStartTime());
            String endTime = timeFormat.format(timeSlot.getEndTime());
            String appointmentDate = request.getAppointmentDate();

            String[] centerInfo = getCenterName(request.getCenterId());
            String vaccineName = getVaccineName(request.getVaccineId());
            String patientEmail = getPatientEmail(patient.getBenutzerId());

            String confirmationCode = generateConfirmationCode(patient.getId(), timeSlot.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("IMPFTERMIN\n");
            sb.append("===========\n");
            sb.append("Termin-Nr: ").append(confirmationCode).append("\n");
            sb.append("Datum: ").append(appointmentDate).append("\n");
            sb.append("Zeit: ").append(startTime).append(" - ").append(endTime).append("\n\n");

            sb.append("Zentrum: ").append(centerInfo[0]).append("\n");
            sb.append("Adresse: ").append(centerInfo[1]).append("\n\n");

            sb.append("Impfstoff: ").append(vaccineName).append("\n");
            sb.append("Patient: ").append(patientEmail).append("\n\n");

            sb.append("Bitte bringen Sie diesen QR-Code und einen Ausweis mit.");

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Impftermin #" + UUID.randomUUID().toString().substring(0, 8) +
                    " am " + request.getAppointmentDate();
        }
    }

    private static String generateConfirmationCode(int patientId, int timeSlotId) {
        String uuid = UUID.nameUUIDFromBytes((patientId + "-" + timeSlotId).getBytes()).toString();
        return "APT-" + uuid.substring(0, 6).toUpperCase();
    }

    private static String[] getCenterName(int centerId) {
        try (Connection conn = DataBaseUtil.getConnection()) {
            String sql = "SELECT name, ort, strasse, hausnummer, plz FROM impfzentrum WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, centerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        String address = String.format("%s %s, %s %s",
                                rs.getString("strasse"),
                                rs.getString("hausnummer"),
                                rs.getString("plz"),
                                rs.getString("ort"));
                        return new String[] { name, address };
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[] { "Unbekanntes Zentrum", "Keine Adresse verfügbar" };
    }

    private static String getVaccineName(int vaccineId) {
        try (Connection conn = DataBaseUtil.getConnection()) {
            String sql = "SELECT name FROM impfstoff WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, vaccineId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("name");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unbekannter Impfstoff";
    }

    private static String getPatientEmail(int userId) {
        try (Connection conn = DataBaseUtil.getConnection()) {
            String sql = "SELECT email FROM benutzer WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("email");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unbekannt";
    }
}