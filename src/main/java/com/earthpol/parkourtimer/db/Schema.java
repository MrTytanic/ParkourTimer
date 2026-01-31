package com.earthpol.parkourtimer.db;

import java.sql.Connection;
import java.sql.Statement;

public class Schema {

    public static void init(Database database) {
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS time_records (
                    uuid CHAR(36) NOT NULL PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    time_ms BIGINT NOT NULL,
                    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_time_ms (time_ms)
                );
            """);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}