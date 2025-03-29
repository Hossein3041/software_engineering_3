package com.example.app.servlets;

import com.example.app.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(urlPatterns = { "/api/logout" })
public class LogoutServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        objectMapper.writeValue(
                response.getWriter(),
                new AuthResponse(true, "Logout successful"));
    }
}
