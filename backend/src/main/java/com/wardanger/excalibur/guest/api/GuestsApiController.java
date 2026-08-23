package com.wardanger.excalibur.guest.api;

import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.generated.api.GuestsApi;
import com.wardanger.excalibur.generated.model.CreateGuestRequest;
import com.wardanger.excalibur.generated.model.CheckInGuestRequest;
import com.wardanger.excalibur.generated.model.GuestCheckIn;
import com.wardanger.excalibur.generated.model.GuestDetails;
import com.wardanger.excalibur.generated.model.GuestPass;
import com.wardanger.excalibur.generated.model.GuestSummary;
import com.wardanger.excalibur.generated.model.SellPassRequest;
import com.wardanger.excalibur.generated.model.ReverseCheckInRequest;
import com.wardanger.excalibur.guest.application.GuestService;
import com.wardanger.excalibur.guest.domain.GuestRegistrationType;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import com.wardanger.excalibur.shared.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class GuestsApiController implements GuestsApi {

    private final GuestService guestService;
    private final GuestApiMapper mapper;
    private final SessionAuthenticationService sessions;

    @Override
    public ResponseEntity<GuestDetails> createGuest(CreateGuestRequest request) {
        var profile = guestService.createGuest(
                request.getFullName(),
                GuestRegistrationType.valueOf(request.getRegistrationType().name()),
                currentEmployee());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDetails(profile, guestService.today()));
    }

    @Override
    public ResponseEntity<GuestDetails> getGuest(UUID guestId) {
        return ResponseEntity.ok(mapper.toDetails(
                guestService.getGuest(guestId),
                guestService.today()));
    }

    @Override
    public ResponseEntity<List<GuestSummary>> listGuests(@Nullable String query) {
        var today = guestService.today();
        return ResponseEntity.ok(guestService.listGuests(query).stream()
                .map(profile -> mapper.toSummary(profile, today))
                .toList());
    }

    @Override
    public ResponseEntity<GuestPass> sellGuestPass(UUID guestId, SellPassRequest request) {
        var guestPass = guestService.sellPass(
                guestId,
                request.getProductId(),
                request.getValidFrom(),
                PaymentMethod.valueOf(request.getPaymentMethod().name()),
                currentEmployee());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toPass(guestPass, guestService.today()));
    }

    @Override
    public ResponseEntity<GuestCheckIn> checkInGuest(UUID guestId, CheckInGuestRequest request) {
        var checkIn = guestService.checkIn(
                guestId,
                request.getConfirmRepeatedToday(),
                currentEmployee());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCheckIn(checkIn));
    }

    @Override
    public ResponseEntity<GuestCheckIn> reverseGuestCheckIn(
            UUID guestId,
            UUID checkInId,
            ReverseCheckInRequest request) {
        return ResponseEntity.ok(mapper.toCheckIn(guestService.reverseCheckIn(
                guestId,
                checkInId,
                request.getReason(),
                currentEmployee())));
    }

    private com.wardanger.excalibur.security.GymUserPrincipal currentEmployee() {
        return sessions.currentPrincipal().orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "A művelethez bejelentkezés szükséges."));
    }
}
