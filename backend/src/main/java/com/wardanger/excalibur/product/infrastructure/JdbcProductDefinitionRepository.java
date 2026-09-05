package com.wardanger.excalibur.product.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.product.application.ProductDefinitionRepository;
import com.wardanger.excalibur.product.domain.ProductDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcProductDefinitionRepository implements ProductDefinitionRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ProductDefinition> findActiveById(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM product_definition WHERE id = ? AND active = TRUE",
                JdbcProductDefinitionRepository::mapProduct,
                id.toString()).stream().findFirst();
    }

    @Override
    public Optional<ProductDefinition> findById(UUID id) {
        return jdbcTemplate.query(
                "SELECT * FROM product_definition WHERE id = ? AND service_type = 'GYM'",
                JdbcProductDefinitionRepository::mapProduct,
                id.toString()).stream().findFirst();
    }

    @Override
    public List<ProductDefinition> findActiveGymPasses() {
        return jdbcTemplate.query("""
                        SELECT * FROM product_definition
                        WHERE service_type = 'GYM' AND active = TRUE
                        ORDER BY code
                        """,
                JdbcProductDefinitionRepository::mapProduct);
    }

    @Override
    public List<ProductDefinition> findAllGymPasses() {
        return jdbcTemplate.query("""
                        SELECT * FROM product_definition
                        WHERE service_type = 'GYM'
                        ORDER BY active DESC, LOWER(name)
                        """,
                JdbcProductDefinitionRepository::mapProduct);
    }

    @Override
    public void insert(ProductDefinition product) {
        jdbcTemplate.update("""
                INSERT INTO product_definition
                    (id, code, name, service_type, validity_days, entry_limit, default_price, active)
                VALUES (?, ?, ?, 'GYM', ?, ?, ?, ?)
                """,
                product.id().toString(), product.code(), product.name(), product.validityDays(),
                product.entryLimit(), product.defaultPrice(), product.active());
    }

    @Override
    public void update(ProductDefinition product) {
        jdbcTemplate.update("""
                UPDATE product_definition
                SET name = ?, validity_days = ?, entry_limit = ?, default_price = ?, active = ?
                WHERE id = ? AND service_type = 'GYM'
                """,
                product.name(), product.validityDays(), product.entryLimit(), product.defaultPrice(),
                product.active(), product.id().toString());
    }

    @Override
    public boolean deactivate(UUID id) {
        return jdbcTemplate.update("""
                UPDATE product_definition SET active = FALSE
                WHERE id = ? AND service_type = 'GYM' AND active = TRUE
                """, id.toString()) > 0;
    }

    private static ProductDefinition mapProduct(ResultSet resultSet, int rowNumber) throws SQLException {
        var entryLimitValue = resultSet.getObject("entry_limit");
        return new ProductDefinition(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getInt("validity_days"),
                entryLimitValue == null ? null : resultSet.getInt("entry_limit"),
                resultSet.getLong("default_price"),
                resultSet.getBoolean("active"));
    }
}
