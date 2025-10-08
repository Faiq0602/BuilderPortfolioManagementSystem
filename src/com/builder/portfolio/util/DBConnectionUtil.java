package com.builder.portfolio.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnectionUtil {
    private static final String URL = "jdbc:postgresql://localhost:5432/builder_portfolio_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "faique";

    private DBConnectionUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
