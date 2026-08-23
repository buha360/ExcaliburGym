package com.wardanger.excalibur.employee.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.employee.application.EmployeeAccountRepository;
import com.wardanger.excalibur.employee.domain.EmployeeAccount;
import com.wardanger.excalibur.employee.domain.EmployeeRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEmployeeAccountRepository implements EmployeeAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEmployeeAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count() {
        var result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employee_account", Long.class);
        return result == null ? 0 : result;
    }

    @Override
    public boolean existsByDisplayNameIgnoreCase(String displayName) {
        var result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_account WHERE LOWER(display_name) = LOWER(?)",
                Long.class,
                displayName);
        return result != null && result > 0;
    }

    @Override
    public Optional<EmployeeAccount> findById(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM employee_account WHERE id = ?",
                JdbcEmployeeAccountRepository::mapAccount,
                id.toString()).stream().findFirst();
    }

    @Override
    public List<EmployeeAccount> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM employee_account ORDER BY role, LOWER(display_name)",
                JdbcEmployeeAccountRepository::mapAccount);
    }

    @Override
    public List<EmployeeAccount> findAllEnabled() {
        return jdbcTemplate.query(
                "SELECT * FROM employee_account WHERE enabled = TRUE ORDER BY role, LOWER(display_name)",
                JdbcEmployeeAccountRepository::mapAccount);
    }

    @Override
    public void insert(EmployeeAccount account) {
        jdbcTemplate.update("""
                INSERT INTO employee_account
                    (id, display_name, pin_hash, role, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                account.id().toString(),
                account.displayName(),
                account.pinHash(),
                account.role().name(),
                account.enabled(),
                account.createdAt().toString(),
                account.updatedAt().toString());
    }

    @Override
    public void updateEnabled(UUID id, boolean enabled, String updatedAt) {
        jdbcTemplate.update(
                "UPDATE employee_account SET enabled = ?, updated_at = ? WHERE id = ?",
                enabled,
                updatedAt,
                id.toString());
    }

    @Override
    public void updatePinHash(UUID id, String pinHash, String updatedAt) {
        jdbcTemplate.update(
                "UPDATE employee_account SET pin_hash = ?, updated_at = ? WHERE id = ?",
                pinHash,
                updatedAt,
                id.toString());
    }

    private static EmployeeAccount mapAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new EmployeeAccount(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("display_name"),
                resultSet.getString("pin_hash"),
                EmployeeRole.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("enabled"),
                Instant.parse(resultSet.getString("created_at")),
                Instant.parse(resultSet.getString("updated_at")));
    }
}
