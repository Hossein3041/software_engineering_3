package com.example.app.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import com.example.app.auth.AuthHelper;
import com.example.app.dto.ErrorResponse;
import com.example.app.utils.DataBaseUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { "/api/admin/vaccines" })
public class AdminVaccineManagementServlet extends HttpServlet {
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

            String action = request.getParameter("action");
            if ("new".equals(action)) {
                Map<String, Object> vaccineData = objectMapper.readValue(
                        request.getInputStream(),
                        new TypeReference<Map<String, Object>>() {
                        });

                String name = (String) vaccineData.get("name");
                String details = (String) vaccineData.get("details");
                System.out.println("data incoming" + vaccineData);
                int initialQuantity = Integer.parseInt(vaccineData.get("quantity").toString());

                int vaccineId = createNewVaccine(name, details, centerId, initialQuantity);
                System.out.println("inside vaccineid" + vaccineId);

                if (vaccineId > 0) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Neuer Impfstoff erfolgreich hinzugefügt");
                    result.put("vaccineId", vaccineId);
                    objectMapper.writeValue(response.getWriter(), result);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    objectMapper.writeValue(response.getWriter(),
                            new ErrorResponse("Fehler beim Hinzufügen des Impfstoffs"));
                }
            } else {
                Map<String, Object> vaccineUpdate = objectMapper.readValue(
                        request.getInputStream(),
                        new TypeReference<Map<String, Object>>() {
                        });
                int vaccineId = Integer.parseInt(vaccineUpdate.get("vaccineId").toString());
                int newQuantity = Integer.parseInt(vaccineUpdate.get("quantity").toString());

                boolean success = updateVaccineQuantity(centerId, vaccineId, newQuantity);

                if (success) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Impfstoffmenge erfolgreich aktualisiert");
                    objectMapper.writeValue(response.getWriter(), result);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    objectMapper.writeValue(response.getWriter(),
                            new ErrorResponse("Fehler beim Aktualisieren der Impfstoffmenge"));
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Datenbankfehler: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Fehler: " + e.getMessage()));
        }
    }

    private int createNewVaccine(String name, String details, int centerId, int initialQuantity) throws SQLException {
        Connection conn = null;
        int vaccineId = -1;

        try {
            conn = DataBaseUtil.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT id FROM impfstoff WHERE name = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, name);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        vaccineId = rs.getInt("id");

                        String checkInventorySql = "SELECT COUNT(*) FROM impfzentrum_impfstoff " +
                                "WHERE impfzentrum_id = ? AND impfstoff_id = ?";
                        try (PreparedStatement inventoryStmt = conn.prepareStatement(checkInventorySql)) {
                            inventoryStmt.setInt(1, centerId);
                            inventoryStmt.setInt(2, vaccineId);
                            try (ResultSet inventoryRs = inventoryStmt.executeQuery()) {
                                if (inventoryRs.next() && inventoryRs.getInt(1) > 0) {
                                    conn.rollback();
                                    conn.setAutoCommit(true);
                                    return updateVaccineQuantity(centerId, vaccineId, initialQuantity) ? vaccineId : -1;
                                }
                            }
                        }

                        String addInventorySql = "INSERT INTO impfzentrum_impfstoff (impfzentrum_id, impfstoff_id, quantity) "
                                +
                                "VALUES (?, ?, ?)";
                        try (PreparedStatement inventoryStmt = conn.prepareStatement(addInventorySql)) {
                            inventoryStmt.setInt(1, centerId);
                            inventoryStmt.setInt(2, vaccineId);
                            inventoryStmt.setInt(3, initialQuantity);
                            inventoryStmt.executeUpdate();
                        }

                        conn.commit();
                        return vaccineId;
                    }
                }
            }

            String createVaccineSql = "INSERT INTO impfstoff (name, details) VALUES (?, ?)";
            try (PreparedStatement vaccineStmt = conn.prepareStatement(createVaccineSql,
                    Statement.RETURN_GENERATED_KEYS)) {
                vaccineStmt.setString(1, name);
                vaccineStmt.setString(2, details);

                int rowsAffected = vaccineStmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Creating vaccine failed, no rows affected.");
                }

                try (ResultSet generatedKeys = vaccineStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        vaccineId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating vaccine failed, no ID obtained.");
                    }
                }

                String addInventorySql = "INSERT INTO impfzentrum_impfstoff (impfzentrum_id, impfstoff_id, quantity) " +
                        "VALUES (?, ?, ?)";
                try (PreparedStatement inventoryStmt = conn.prepareStatement(addInventorySql)) {
                    inventoryStmt.setInt(1, centerId);
                    inventoryStmt.setInt(2, vaccineId);
                    inventoryStmt.setInt(3, initialQuantity);
                    inventoryStmt.executeUpdate();
                }

                conn.commit();
                return vaccineId;
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
    }

    private boolean updateVaccineQuantity(int centerId, int vaccineId, int newQuantity) throws SQLException {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Die Menge darf nicht negativ sein");
        }

        String sql = "UPDATE impfzentrum_impfstoff SET quantity = ? WHERE impfzentrum_id = ? AND impfstoff_id = ?";

        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newQuantity);
            stmt.setInt(2, centerId);
            stmt.setInt(3, vaccineId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}