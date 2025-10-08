package com.builder.portfolio.util;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

class DBConnectionTest {

    @Test
    void testDatabaseConnection() {
        try (Connection connection = DBConnectionUtil.getConnection()) {
            // The test passes if connection is successfully established
            assertNotNull(connection, "Database connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");
        } catch (Exception e) {
            // If an exception occurs, the test fails
            fail("Database connection failed: " + e.getMessage());
        }
    }
}
