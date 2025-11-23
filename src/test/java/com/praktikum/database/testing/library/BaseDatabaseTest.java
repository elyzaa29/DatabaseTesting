package com.praktikum.database.testing.library;

import com.praktikum.database.testing.library.config.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class BaseDatabaseTest {
    protected static final Logger logger = Logger.getLogger(BaseDatabaseTest.class.getName());
    protected Connection connection;

    @BeforeAll
    static void setUpAll() {
        logger.info("===============================");
        logger.info(" Setting up Database Test Environment");
        logger.info("===============================");

        boolean connected = DatabaseConfig.testConnection();
        if (!connected) {
            throw new RuntimeException("Tidak bisa terkoneksi ke database. Tests tidak bisa dijalankan.");
        }

        DatabaseConfig.printDatabaseInfo();
    }

    @BeforeEach
    void setUp() throws SQLException {
        connection = DatabaseConfig.getConnection();
        connection.setAutoCommit(false);
        logger.info("Test connection established - Auto-commit: false");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
                logger.info(" All test changes rolled back");
            } catch (SQLException e) {
                logger.warning("Warning: Gagal rollback: " + e.getMessage());
            } finally {
                connection.close();
                logger.info(" Test connection closed");
            }
        }
    }

    protected void cleanUpTable(String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            int deletedRows = stmt.executeUpdate("DELETE FROM " + tableName);
            logger.info("Cleaned up table '" + tableName + "' - Deleted " + deletedRows + " rows");
        } catch (SQLException e) {
            logger.warning("Gagal clean up table '" + tableName + "': " + e.getMessage());
            throw e;
        }
    }

    protected int countRecords(String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {

            int count = rs.next() ? rs.getInt(1) : 0;
            logger.fine("Record count in '" + tableName + "': " + count);
            return count;
        }
    }

    protected void executeSQL(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.fine("Executed SQL: " + sql);
        }
    }

    protected void pause(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
            logger.fine("Paused for " + milliseconds + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Sleep interrupted: " + e.getMessage());
        }
    }
}