package com.wardanger.excalibur.product.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.wardanger.excalibur.product.domain.ProductDefinition;

public interface ProductDefinitionRepository {

    Optional<ProductDefinition> findActiveById(UUID id);

    Optional<ProductDefinition> findById(UUID id);

    List<ProductDefinition> findActiveGymPasses();

    List<ProductDefinition> findAllGymPasses();

    void insert(ProductDefinition product);

    void update(ProductDefinition product);

    boolean deactivate(UUID id);
}
