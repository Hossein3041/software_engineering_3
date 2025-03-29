package com.example.app;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.example.app.utils.DataBaseUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");

        try (Connection conn = DataBaseUtil.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT message FROM greetings WHERE id = 1")) {

            if (rs.next()) {
                String message = rs.getString("message");
                resp.getWriter().println(message);
            } else {
                resp.getWriter().println("No greeting found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Database error: " + e.getMessage());
        }
    }
}
