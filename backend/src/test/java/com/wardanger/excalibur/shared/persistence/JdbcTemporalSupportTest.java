package com.wardanger.excalibur.shared.persistence;

import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcTemporalSupportTest {

    @Test
    void roundTripsTypedTemporalValuesThroughTheLocalSqliteDriver() throws Exception {
        var expectedInstant = Instant.parse("2026-09-04T18:30:45Z");
        var expectedDate = LocalDate.of(2026, 9, 4);

        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.createStatement().execute("CREATE TABLE sample (occurred_at TIMESTAMP, valid_on DATE)");

            try (var insert = connection.prepareStatement(
                    "INSERT INTO sample (occurred_at, valid_on) VALUES (?, ?)")) {
                insert.setTimestamp(1, Timestamp.from(expectedInstant));
                insert.setDate(2, Date.valueOf(expectedDate));
                insert.executeUpdate();
            }

            try (var resultSet = connection.createStatement().executeQuery("SELECT * FROM sample")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(JdbcTemporalSupport.instant(resultSet, "occurred_at")).isEqualTo(expectedInstant);
                assertThat(JdbcTemporalSupport.localDate(resultSet, "valid_on")).isEqualTo(expectedDate);
            }
        }
    }
}
