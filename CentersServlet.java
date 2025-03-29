package com.example.app.servlets;

import com.example.app.dto.Center;
import com.example.app.dto.ErrorResponse;
import com.example.app.dto.VaccineDTO;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns = { "/api/centers/*" })
public class CentersServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            getCenters(request, response);
        } else {
            String[] parts = pathInfo.substring(1).split("/");
            if (parts.length >= 2 && "vaccines".equalsIgnoreCase(parts[1])) {
                try {
                    int centerId = Integer.parseInt(parts[0]);
                    getVaccinesForCenter(centerId, request, response);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid centerId"));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                objectMapper.writeValue(response.getWriter(), new ErrorResponse("Invalid URL"));
            }
        }
    }

    private void getCenters(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Center> centers = new ArrayList<>();
        String sql = "SELECT id, name, ort, strasse, hausnummer, plz, number_of_cabins FROM impfzentrum";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Center center = new Center();
                center.setId(rs.getInt("id"));
                center.setName(rs.getString("name"));
                center.setOrt(rs.getString("ort"));
                center.setStrasse(rs.getString("strasse"));
                center.setHausnummer(rs.getString("hausnummer"));
                center.setPlz(rs.getString("plz"));
                center.setNumberOfCabins(rs.getInt("number_of_cabins"));
                centers.add(center);
            }
            objectMapper.writeValue(response.getWriter(), centers);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Error fetching centers: " + e.getMessage()));
        }
    }

    private void getVaccinesForCenter(int centerId, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<VaccineDTO> vaccines = new ArrayList<>();
        String sql = "SELECT i.id, i.name, i.details, z.quantity " +
                "FROM impfzentrum_impfstoff z " +
                "JOIN impfstoff i ON z.impfstoff_id = i.id " +
                "WHERE z.impfzentrum_id = ?";
        try (Connection conn = DataBaseUtil.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, centerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    VaccineDTO vaccine = new VaccineDTO();
                    vaccine.setId(rs.getInt("id"));
                    vaccine.setName(rs.getString("name"));
                    vaccine.setDetails(rs.getString("details"));
                    vaccine.setStock(rs.getInt("quantity"));
                    vaccines.add(vaccine);
                }
            }
            objectMapper.writeValue(response.getWriter(), vaccines);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Error fetching vaccines: " + e.getMessage()));
        }
    }
}