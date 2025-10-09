package com.builder.portfolio.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DBConnectionUtil {
    private static final Logger LOGGER = Logger.getLogger(DBConnectionUtil.class.getName());


    private static final String URL = "jdbc:postgresql://localhost:5432/builder_portfolio_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "faique";

    private DBConnectionUtil() {
    }

    public static Connection getConnection() throws SQLException {

        // Try to establish DB Connection
        try {
            LOGGER.fine("Opening database connection");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Unable to obtain database connection", ex);
            throw ex;
        }
    }
}
