package com.example.app.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

public class AppointmentUtil {

    public static boolean canCancelAppointment(int appointmentId) throws SQLException {
        String sql = "SELECT z.start_time FROM termin t " +
                "JOIN zeitslot z ON t.zeitslot_id = z.id " +
                "WHERE t.id = ?";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, appointmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp appointmentTime = rs.getTimestamp("start_time");
                    return isAppointmentCancellable(appointmentTime);
                }
            }
        }

        return false;
    }

    public static boolean isAppointmentCancellable(Timestamp appointmentTime) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now.getTime());
        cal.add(Calendar.HOUR_OF_DAY, 1);
        Timestamp oneHourFromNow = new Timestamp(cal.getTimeInMillis());

        return appointmentTime.after(oneHourFromNow);
    }
}