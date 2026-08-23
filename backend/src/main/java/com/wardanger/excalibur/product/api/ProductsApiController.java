package com.wardanger.excalibur.product.api;

import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.generated.api.ProductsApi;
import com.wardanger.excalibur.generated.model.ProductDefinition;
import com.wardanger.excalibur.generated.model.UpsertGymPassProductRequest;
import com.wardanger.excalibur.guest.api.GuestApiMapper;
import com.wardanger.excalibur.guest.application.GuestService;
import com.wardanger.excalibur.product.application.ProductAdministrationService;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductsApiController implements ProductsApi {

    private final GuestService guestService;
    private final GuestApiMapper mapper;
    private final ProductAdministrationService administration;
    private final SessionAuthenticationService authentication;

    @Override
    public ResponseEntity<List<ProductDefinition>> listGymPassProducts() {
        return ResponseEntity.ok(guestService.listGymPassProducts().stream()
                .map(mapper::toProduct)
                .toList());
    }

    @Override
    public ResponseEntity<List<ProductDefinition>> listAllGymPassProducts() {
        return ResponseEntity.ok(administration.listAll().stream().map(mapper::toProduct).toList());
    }

    @Override
    public ResponseEntity<ProductDefinition> createGymPassProduct(UpsertGymPassProductRequest request) {
        var created = administration.create(
                request.getName(), request.getValidityDays(), request.getEntryLimit(), request.getDefaultPrice(), employee());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toProduct(created));
    }

    @Override
    public ResponseEntity<ProductDefinition> updateGymPassProduct(
            UUID productId,
            UpsertGymPassProductRequest request) {
        var updated = administration.update(
                productId, request.getName(), request.getValidityDays(), request.getEntryLimit(), request.getDefaultPrice(), employee());
        return ResponseEntity.ok(mapper.toProduct(updated));
    }

    @Override
    public ResponseEntity<Void> deactivateGymPassProduct(UUID productId) {
        administration.deactivate(productId, employee());
        return ResponseEntity.noContent().build();
    }

    private GymUserPrincipal employee() {
        return authentication.currentPrincipal().orElseThrow();
    }
}
