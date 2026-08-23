package com.wardanger.excalibur.guest.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.guest.domain.Guest;
import com.wardanger.excalibur.guest.domain.GuestRegistrationType;
import com.wardanger.excalibur.pass.application.GuestPassRepository;
import com.wardanger.excalibur.pass.domain.GuestPass;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.product.application.ProductDefinitionRepository;
import com.wardanger.excalibur.product.domain.ProductDefinition;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import com.wardanger.excalibur.visit.application.GuestCheckInRepository;
import com.wardanger.excalibur.visit.domain.GuestCheckIn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestService {

    private static final Locale HUNGARIAN = Locale.forLanguageTag("hu-HU");

    private final GuestRepository guests;
    private final ProductDefinitionRepository products;
    private final GuestPassRepository passes;
    private final GuestCheckInRepository checkIns;
    private final AuditLogService auditLog;
    private final Clock clock;

    @Transactional
    public GuestProfile createGuest(
            String rawFullName,
            GuestRegistrationType registrationType,
            GymUserPrincipal employee) {
        var fullName = rawFullName.strip().replaceAll("\\s+", " ");
        if (fullName.length() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GUEST_NAME",
                    "A vendég neve legalább 2 karakter legyen.");
        }
        var guest = new Guest(
                UUID.randomUUID(),
                fullName,
                registrationType,
                clock.instant(),
                employee.id());
        guests.insert(guest, normalize(fullName));
        auditLog.record(employee, "GUEST_CREATED", "GUEST", guest.id(), "Új vendég: " + fullName);
        return new GuestProfile(guest, List.of(), List.of());
    }

    public List<GuestProfile> listGuests(String query) {
        return guests.findQuickSearch(normalize(query == null ? "" : query.strip()), today()).stream()
                .map(guest -> new GuestProfile(
                        guest,
                        passes.findByGuestId(guest.id()),
                        checkIns.findByGuestId(guest.id())))
                .toList();
    }

    public GuestProfile getGuest(UUID guestId) {
        var guest = requireGuest(guestId);
        return new GuestProfile(guest, passes.findByGuestId(guestId), checkIns.findByGuestId(guestId));
    }

    public List<ProductDefinition> listGymPassProducts() {
        return products.findActiveGymPasses();
    }

    @Transactional
    public GuestPass sellPass(
            UUID guestId,
            UUID productId,
            LocalDate validFrom,
            PaymentMethod paymentMethod,
            GymUserPrincipal employee) {
        var guest = requireGuest(guestId);
        var product = products.findActiveById(productId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "A kiválasztott bérlettípus nem található vagy már nem aktív."));
        if (paymentMethod == PaymentMethod.PREPAID_BALANCE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PREPAID_BALANCE_NOT_AVAILABLE",
                    "Az egyenlegből fizetés az egyenlegmodul elkészülése után használható.");
        }
        var guestPass = new GuestPass(
                UUID.randomUUID(),
                guestId,
                product.id(),
                product.name(),
                clock.instant(),
                validFrom,
                validFrom.plusDays(product.validityDays() - 1L),
                product.entryLimit(),
                product.entryLimit(),
                product.defaultPrice(),
                paymentMethod,
                employee.id(),
                employee.displayName(),
                null);
        passes.insert(guestPass);
        auditLog.record(
                employee,
                "PASS_SOLD",
                "GUEST_PASS",
                guestPass.id(),
                "Bérlet kiállítva: " + product.name() + " – " + guest.fullName());
        return guestPass;
    }

    @Transactional
    public GuestCheckIn checkIn(UUID guestId, boolean confirmRepeatedToday, GymUserPrincipal employee) {
        var guest = requireGuest(guestId);
        var activePass = passes.findByGuestId(guestId).stream()
                .filter(pass -> pass.status(today()) == GuestPass.Status.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "NO_ACTIVE_GYM_PASS",
                        "A vendégnek nincs beléptetésre használható aktív kondibérlete."));
        if (!confirmRepeatedToday && checkIns.existsActiveForGuestSince(guestId, todayStart())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REPEATED_SAME_DAY_CHECK_IN",
                    "A vendég ma már volt beléptetve. Erősítsd meg az ismételt beléptetést.");
        }
        var consumedEntry = activePass.remainingEntries() != null;
        if (consumedEntry && !passes.consumeEntry(activePass.id())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PASS_ENTRY_NOT_AVAILABLE",
                    "A bérleten nincs felhasználható alkalom.");
        }
        var checkIn = new GuestCheckIn(
                UUID.randomUUID(),
                guestId,
                activePass.id(),
                activePass.productName(),
                clock.instant(),
                employee.id(),
                employee.displayName(),
                consumedEntry,
                null,
                null,
                null,
                null);
        checkIns.insert(checkIn);
        auditLog.record(employee, "GUEST_CHECKED_IN", "CHECK_IN", checkIn.id(), "Vendég beléptetve: " + guest.fullName());
        return checkIn;
    }

    @Transactional
    public GuestCheckIn reverseCheckIn(
            UUID guestId,
            UUID checkInId,
            String rawReason,
            GymUserPrincipal employee) {
        var guest = requireGuest(guestId);
        var reason = rawReason == null ? "" : rawReason.strip().replaceAll("\\s+", " ");
        if (reason.length() < 3 || reason.length() > 500) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REVERSAL_REASON",
                    "A visszavonás indoka 3–500 karakter lehet.");
        }
        var checkIn = checkIns.findByIdForGuest(checkInId, guestId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "CHECK_IN_NOT_FOUND",
                "A beléptetés nem található."));
        if (checkIn.reversed()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CHECK_IN_ALREADY_REVERSED",
                    "Ezt a beléptetést már visszavonták.");
        }
        if (!LocalDate.ofInstant(checkIn.checkedInAt(), clock.getZone()).equals(today())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CHECK_IN_REVERSAL_WINDOW_CLOSED",
                    "Csak a mai napon történt téves beléptetés vonható vissza.");
        }

        var reversedAt = clock.instant();
        if (!checkIns.reverse(checkInId, reversedAt, employee.id(), reason)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CHECK_IN_ALREADY_REVERSED",
                    "Ezt a beléptetést már visszavonták.");
        }
        if (checkIn.consumedEntry() && !passes.restoreEntry(checkIn.guestPassId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PASS_ENTRY_RESTORE_FAILED",
                    "A bérletalkalom visszaállítása nem sikerült; a visszavonás nem került mentésre.");
        }
        auditLog.record(
                employee,
                "CHECK_IN_REVERSED",
                "CHECK_IN",
                checkIn.id(),
                "Beléptetés visszavonva: " + guest.fullName());
        return new GuestCheckIn(
                checkIn.id(),
                checkIn.guestId(),
                checkIn.guestPassId(),
                checkIn.passName(),
                checkIn.checkedInAt(),
                checkIn.checkedInByEmployeeId(),
                checkIn.checkedInByEmployeeName(),
                checkIn.consumedEntry(),
                reversedAt,
                employee.id(),
                employee.displayName(),
                reason);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private Instant todayStart() {
        return today().atStartOfDay(clock.getZone()).toInstant();
    }

    private Guest requireGuest(UUID guestId) {
        return guests.findById(guestId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "GUEST_NOT_FOUND",
                "A vendég nem található."));
    }

    private static String normalize(String value) {
        return value.toLowerCase(HUNGARIAN);
    }

    public record GuestProfile(Guest guest, List<GuestPass> passes, List<GuestCheckIn> checkIns) {
        public GuestPass currentActivePass(LocalDate today) {
            return passes.stream()
                    .filter(pass -> pass.status(today) == GuestPass.Status.ACTIVE)
                    .findFirst()
                    .orElse(null);
        }

        public boolean checkedInToday(Clock clock) {
            var today = LocalDate.now(clock);
            return checkIns.stream().anyMatch(checkIn -> !checkIn.reversed() &&
                    LocalDate.ofInstant(checkIn.checkedInAt(), clock.getZone()).equals(today));
        }
    }
}
