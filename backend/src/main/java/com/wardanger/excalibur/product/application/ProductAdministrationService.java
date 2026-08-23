package com.wardanger.excalibur.product.application;

import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.product.domain.ProductDefinition;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductAdministrationService {

    private final ProductDefinitionRepository products;
    private final AuditLogService auditLog;

    public List<ProductDefinition> listAll() {
        return products.findAllGymPasses();
    }

    @Transactional
    public ProductDefinition create(
            String rawName,
            int validityDays,
            Integer entryLimit,
            long defaultPrice,
            GymUserPrincipal employee) {
        var product = new ProductDefinition(
                UUID.randomUUID(),
                "GYM_CUSTOM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                validateName(rawName),
                validateValidity(validityDays),
                validateEntryLimit(entryLimit),
                validatePrice(defaultPrice),
                true);
        products.insert(product);
        auditLog.record(employee, "PRODUCT_CREATED", "PRODUCT", product.id(), "Bérlettípus létrehozva: " + product.name());
        return product;
    }

    @Transactional
    public ProductDefinition update(
            UUID productId,
            String rawName,
            int validityDays,
            Integer entryLimit,
            long defaultPrice,
            GymUserPrincipal employee) {
        var existing = requireProduct(productId);
        var updated = new ProductDefinition(
                existing.id(),
                existing.code(),
                validateName(rawName),
                validateValidity(validityDays),
                validateEntryLimit(entryLimit),
                validatePrice(defaultPrice),
                existing.active());
        products.update(updated);
        auditLog.record(employee, "PRODUCT_UPDATED", "PRODUCT", updated.id(), "Bérlettípus módosítva: " + updated.name());
        return updated;
    }

    @Transactional
    public void deactivate(UUID productId, GymUserPrincipal employee) {
        var product = requireProduct(productId);
        if (!products.deactivate(productId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PRODUCT_ALREADY_INACTIVE",
                    "A bérlettípus már inaktív.");
        }
        auditLog.record(employee, "PRODUCT_DEACTIVATED", "PRODUCT", product.id(), "Bérlettípus inaktiválva: " + product.name());
    }

    private ProductDefinition requireProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "A bérlettípus nem található."));
    }

    private static String validateName(String rawName) {
        var name = rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_NAME", "A név 2–120 karakter legyen.");
        }
        return name;
    }

    private static int validateValidity(int validityDays) {
        if (validityDays < 1 || validityDays > 3650) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VALIDITY", "Az érvényesség 1–3650 nap lehet.");
        }
        return validityDays;
    }

    private static Integer validateEntryLimit(Integer entryLimit) {
        if (entryLimit != null && (entryLimit < 1 || entryLimit > 1000)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ENTRY_LIMIT", "Az alkalmak száma 1–1000 lehet.");
        }
        return entryLimit;
    }

    private static long validatePrice(long defaultPrice) {
        if (defaultPrice < 0 || defaultPrice > 10_000_000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICE", "Az ár 0 és 10 000 000 Ft között lehet.");
        }
        return defaultPrice;
    }
}
