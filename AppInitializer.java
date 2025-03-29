package com.example.app.auth;

import com.example.app.utils.DataBaseUtil;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            DataBaseUtil.initializeDatabaseFromSQLFile();
            DataBaseUtil.initializeDatabase();
            System.out.println("db is getting initilized");
        } catch (Exception e) {
            throw new RuntimeException("Database init failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DataBaseUtil.closePool();
    }
}