package com.example.app.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

@WebServlet(urlPatterns = { "/*" })
public class FrontendServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        System.out.println("Requested path: " + path);

        if (path.startsWith("/api/")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "API endpoint not found");
            return;
        }

        if (path.startsWith("/static/")) {
            req.getRequestDispatcher(path).forward(req, resp);
            return;
        }

        serveIndexHtml(resp);
    }

    private void serveIndexHtml(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");

        InputStream inputStream = getServletContext().getResourceAsStream("/index.html");

        if (inputStream != null) {
            byte[] buffer = new byte[10240];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                resp.getOutputStream().write(buffer, 0, len);
            }
            inputStream.close();
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "index.html not found");
        }
    }
}