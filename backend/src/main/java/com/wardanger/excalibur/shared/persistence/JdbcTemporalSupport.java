package com.wardanger.excalibur.shared.persistence;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

public final class JdbcTemporalSupport {

    private JdbcTemporalSupport() {
    }

    public static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    public static Date date(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    public static Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        var value = resultSet.getTimestamp(columnName);
        return value == null ? null : value.toInstant();
    }

    public static LocalDate localDate(ResultSet resultSet, String columnName) throws SQLException {
        var value = resultSet.getDate(columnName);
        return value == null ? null : value.toLocalDate();
    }
}
