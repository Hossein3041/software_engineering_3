package com.example.app.servlets;

import com.example.app.dto.AuthResponse;
import com.example.app.dto.RegisterRequest;
import com.example.app.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = { "/api/register" })
public class RegisterServlet extends HttpServlet {
        private final ObjectMapper objectMapper = new ObjectMapper();

        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                        throws IOException {
                response.setContentType("application/json");

                try {
                        RegisterRequest registerRequest = objectMapper.readValue(
                                        request.getInputStream(),
                                        RegisterRequest.class);

                        UserService.registerUser(
                                        registerRequest.getEmail(),
                                        registerRequest.getPassword());

                        objectMapper.writeValue(
                                        response.getWriter(),
                                        new AuthResponse(true, "Registration successful"));
                } catch (Exception e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        objectMapper.writeValue(
                                        response.getWriter(),
                                        new AuthResponse(false, "Registration failed: " + e.getMessage()));
                }
        }
}