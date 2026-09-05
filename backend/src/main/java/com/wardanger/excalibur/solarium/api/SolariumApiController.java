package com.wardanger.excalibur.solarium.api;

import com.wardanger.excalibur.generated.api.SolariumApi;
import com.wardanger.excalibur.generated.model.SolariumAccount;
import com.wardanger.excalibur.generated.model.SolariumPaymentMethod;
import com.wardanger.excalibur.generated.model.SolariumProduct;
import com.wardanger.excalibur.generated.model.SolariumTransaction;
import com.wardanger.excalibur.generated.model.UpdateSolariumProductRequest;
import com.wardanger.excalibur.generated.model.PurchaseSolariumRequest;
import com.wardanger.excalibur.generated.model.UseSolariumMinutesRequest;
import com.wardanger.excalibur.guest.application.GuestService;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import com.wardanger.excalibur.shared.error.ApiException;
import com.wardanger.excalibur.solarium.application.SolariumClient;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SolariumApiController implements SolariumApi {
    private final SolariumClient solarium; private final GuestService guests; private final SessionAuthenticationService sessions;
    @Override public ResponseEntity<List<SolariumProduct>> listSolariumProducts() { return ResponseEntity.ok(solarium.products().stream().map(SolariumApiController::map).toList()); }
    @Override public ResponseEntity<SolariumAccount> getSolariumAccount(UUID guestId) { var guest = guests.getGuest(guestId).guest(); return ResponseEntity.ok(map(solarium.account(guest.id(), guest.fullName()))); }
    @Override public ResponseEntity<SolariumAccount> purchaseSolariumMinutes(PurchaseSolariumRequest request) { var guest=guests.getGuest(request.getGuestId()).guest(); return ResponseEntity.status(HttpStatus.CREATED).body(map(solarium.purchase(guest.id(),guest.fullName(),request.getProductId(),request.getQuantity(),request.getPaymentMethod().getValue(),employee()))); }
    @Override public ResponseEntity<SolariumAccount> useSolariumMinutes(UseSolariumMinutesRequest request) { var guest=guests.getGuest(request.getGuestId()).guest(); return ResponseEntity.status(HttpStatus.CREATED).body(map(solarium.use(guest.id(),guest.fullName(),request.getMinutes(),employee()))); }
    @Override public ResponseEntity<SolariumProduct> updateSolariumProduct(UUID productId, UpdateSolariumProductRequest request) { return ResponseEntity.ok(map(solarium.updateProduct(productId,request.getName(),request.getDefaultPrice(),request.getActive(),employee()))); }
    private GymUserPrincipal employee(){return sessions.currentPrincipal().orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","A művelethez bejelentkezés szükséges."));}
    private static SolariumProduct map(SolariumClient.Product p){return new SolariumProduct(p.id(),p.code(),p.name(),p.minutesPerUnit(),p.defaultPrice(),p.active(),p.updatedAt());}
    private static SolariumAccount map(SolariumClient.Account a){return new SolariumAccount(a.guestId(),a.guestName(),a.remainingMinutes(),a.transactions().stream().<SolariumTransaction>map(t->{var m=new SolariumTransaction(t.id(),SolariumTransaction.TypeEnum.valueOf(t.type()),t.minutesDelta(),t.balanceAfter(),t.occurredAt(),t.employeeName());m.setProductName(t.productName());m.setQuantity(t.quantity());m.setPricePaid(t.pricePaid());if(t.paymentMethod()!=null)m.setPaymentMethod(SolariumPaymentMethod.valueOf(t.paymentMethod()));return m;}).toList());}
}