package com.example.app.auth;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebFilter(filterName = "AuthFilter", urlPatterns = {
        "/api/slots/",
        "/api/logout",
        "/api/centers",
        "/api/my-bookings/*",
        "/api/bookAppointment"
}, initParams = {
        @WebInitParam(name = "exclusions", value = "/api/login,/api/register,/static/,/api/vt/login")
}, asyncSupported = true)
public class AuthenticationFilter implements Filter {

    private ObjectMapper objectMapper;
    private List<String> excludedPaths;

    @Override
    public void init(FilterConfig filterConfig) {
        objectMapper = new ObjectMapper();

        String exclusions = filterConfig.getInitParameter("exclusions");
        excludedPaths = Arrays.asList(exclusions.split(","));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        System.out.println("logging request" + req.getRequestURI());

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isExcludedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            sendUnauthorizedResponse(res);
            return;
        }

        System.out.println("logging session " + session.getAttribute("user") + session.toString());
        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        return excludedPaths.stream().anyMatch(excluded -> path.equals(excluded) || path.startsWith(excluded + "/"));
    }

    private void sendUnauthorizedResponse(HttpServletResponse response)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(
                response.getWriter(),
                Collections.singletonMap("error", "Unauthorized"));
    }
}