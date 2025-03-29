package com.example.app.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.app.dto.VaccineDTO;
import com.example.app.utils.DataBaseUtil;

public class VaccineService {
    public static List<VaccineDTO> getAvailableVaccines(int centerId) throws SQLException {
        String sql = "SELECT i.*, z.quantity " +
                "FROM impfstoff i " +
                "JOIN impfzentrum_impfstoff z ON i.id = z.impfstoff_id " +
                "WHERE z.impfzentrum_id = ? AND z.quantity > 0";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, centerId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<VaccineDTO> vaccines = new ArrayList<>();
                while (rs.next()) {
                    VaccineDTO vaccine = new VaccineDTO();
                    vaccine.setId(rs.getInt("id"));
                    vaccine.setName(rs.getString("name"));
                    vaccine.setDetails(rs.getString("details"));
                    vaccine.setStock(rs.getInt("quantity"));
                    vaccines.add(vaccine);
                }
                return vaccines;
            }
        }
    }
}
