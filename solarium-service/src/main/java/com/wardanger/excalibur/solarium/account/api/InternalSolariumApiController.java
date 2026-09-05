package com.wardanger.excalibur.solarium.account.api;

import com.wardanger.excalibur.solarium.account.application.SolariumAccountService;
import com.wardanger.excalibur.solarium.account.domain.SolariumAccount;
import com.wardanger.excalibur.solarium.account.domain.SolariumProduct;
import com.wardanger.excalibur.solarium.account.domain.SolariumTransaction;
import com.wardanger.excalibur.solarium.generated.api.InternalSolariumApi;
import com.wardanger.excalibur.solarium.generated.model.InternalCreateSolariumPurchaseRequest;
import com.wardanger.excalibur.solarium.generated.model.InternalCreateSolariumUsageRequest;
import com.wardanger.excalibur.solarium.generated.model.InternalPaymentMethod;
import com.wardanger.excalibur.solarium.generated.model.InternalSolariumAccount;
import com.wardanger.excalibur.solarium.generated.model.InternalSolariumProduct;
import com.wardanger.excalibur.solarium.generated.model.InternalSolariumTransaction;
import com.wardanger.excalibur.solarium.generated.model.InternalSolariumTransactionType;
import com.wardanger.excalibur.solarium.generated.model.InternalUpdateSolariumProductRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalSolariumApiController implements InternalSolariumApi {
    private final SolariumAccountService service; private final Clock clock; private final byte[] expectedApiKey;
    public InternalSolariumApiController(SolariumAccountService service, Clock clock, @Value("${excalibur.internal-api-key}") String apiKey) { this.service = service; this.clock = clock; this.expectedApiKey = apiKey.getBytes(StandardCharsets.UTF_8); }
    @Override public ResponseEntity<List<InternalSolariumProduct>> listInternalSolariumProducts(String apiKey, Boolean includeInactive) { if (!authorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); return ResponseEntity.ok(service.products(Boolean.TRUE.equals(includeInactive)).stream().map(this::map).toList()); }
    @Override public ResponseEntity<InternalSolariumProduct> updateInternalSolariumProduct(String apiKey, UUID productId, InternalUpdateSolariumProductRequest request) { if (!authorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); return ResponseEntity.ok(map(service.updateProduct(productId, request.getName(), request.getDefaultPrice(), request.getActive(), request.getEmployeeId(), request.getEmployeeName()))); }
    @Override public ResponseEntity<InternalSolariumAccount> getInternalSolariumAccount(String apiKey, UUID guestId) { if (!authorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); return ResponseEntity.ok(map(service.account(guestId, "Vendég"))); }
    @Override public ResponseEntity<InternalSolariumAccount> createInternalSolariumPurchase(String apiKey, InternalCreateSolariumPurchaseRequest request) { if (!authorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); return ResponseEntity.status(HttpStatus.CREATED).body(map(service.purchase(request.getGuestId(), request.getGuestName(), request.getProductId(), request.getQuantity(), request.getPaymentMethod().getValue(), request.getEmployeeId(), request.getEmployeeName()))); }
    @Override public ResponseEntity<InternalSolariumAccount> createInternalSolariumUsage(String apiKey, InternalCreateSolariumUsageRequest request) { if (!authorized(apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); return ResponseEntity.status(HttpStatus.CREATED).body(map(service.use(request.getGuestId(), request.getGuestName(), request.getMinutes(), request.getEmployeeId(), request.getEmployeeName()))); }
    private boolean authorized(String key) { return key != null && MessageDigest.isEqual(expectedApiKey, key.getBytes(StandardCharsets.UTF_8)); }
    private InternalSolariumProduct map(SolariumProduct p) { return new InternalSolariumProduct(p.id(), p.code(), p.name(), p.minutesPerUnit(), p.defaultPrice(), p.active(), OffsetDateTime.ofInstant(p.updatedAt(), clock.getZone())); }
    private InternalSolariumAccount map(SolariumAccount a) { return new InternalSolariumAccount(a.guestId(), a.guestName(), a.remainingMinutes(), a.transactions().stream().map(this::map).toList()); }
    private InternalSolariumTransaction map(SolariumTransaction t) { var m = new InternalSolariumTransaction(t.id(), InternalSolariumTransactionType.valueOf(t.type().name()), t.minutesDelta(), t.balanceAfter(), OffsetDateTime.ofInstant(t.occurredAt(), clock.getZone()), t.employeeName()); if (t.productName() != null) m.setProductName(t.productName()); if (t.quantity() != null) m.setQuantity(t.quantity()); if (t.pricePaid() != null) m.setPricePaid(t.pricePaid()); if (t.paymentMethod() != null) m.setPaymentMethod(InternalPaymentMethod.valueOf(t.paymentMethod())); return m; }
}