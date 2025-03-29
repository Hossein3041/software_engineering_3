package com.example.app.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.app.dto.BookingRequest;
import com.example.app.services.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/api/bookAppointment" })
public class BookAppointmentServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        try {
            BookingRequest bookingRequest = objectMapper.readValue(request.getInputStream(), BookingRequest.class);

            Map<String, Object> resultData = new HashMap<>();
            boolean bookingSuccess = AppointmentService.bookAppointment(bookingRequest, request);

            if (bookingSuccess) {
                resultData.put("success", true);
                resultData.put("message", "Termin erfolgreich gebucht");

                objectMapper.writeValue(response.getWriter(), resultData);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resultData.put("success", false);
                resultData.put("message", "Buchung fehlgeschlagen");

                objectMapper.writeValue(response.getWriter(), resultData);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            Map<String, Object> errorData = new HashMap<>();
            errorData.put("success", false);
            errorData.put("message", "Server error: " + e.getMessage());

            objectMapper.writeValue(response.getWriter(), errorData);
        }
    }
}
