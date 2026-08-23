package com.wardanger.excalibur.guest.api;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.wardanger.excalibur.guest.application.GuestService.GuestProfile;
import com.wardanger.excalibur.pass.domain.GuestPass;
import com.wardanger.excalibur.product.domain.ProductDefinition;
import com.wardanger.excalibur.visit.domain.GuestCheckIn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuestApiMapper {

    private final Clock clock;

    public com.wardanger.excalibur.generated.model.GuestSummary toSummary(
            GuestProfile profile,
            LocalDate today) {
        var guest = profile.guest();
        var activePass = profile.currentActivePass(today);
        var response = new com.wardanger.excalibur.generated.model.GuestSummary(
                guest.id(),
                guest.fullName(),
                com.wardanger.excalibur.generated.model.GuestRegistrationType.valueOf(
                        guest.registrationType().name()),
                OffsetDateTime.ofInstant(guest.createdAt(), clock.getZone()),
                activePass != null,
                profile.checkedInToday(clock));
        if (activePass != null) {
            response.setCurrentPassName(activePass.productName());
            response.setCurrentPassValidUntil(activePass.validUntil());
        }
        return response;
    }

    public com.wardanger.excalibur.generated.model.GuestDetails toDetails(
            GuestProfile profile,
            LocalDate today) {
        var guest = profile.guest();
        var activePass = profile.currentActivePass(today);
        var response = new com.wardanger.excalibur.generated.model.GuestDetails(
                guest.id(),
                guest.fullName(),
                com.wardanger.excalibur.generated.model.GuestRegistrationType.valueOf(
                        guest.registrationType().name()),
                OffsetDateTime.ofInstant(guest.createdAt(), clock.getZone()),
                activePass != null,
                profile.checkedInToday(clock),
                profile.passes().stream().map(pass -> toPass(pass, today)).toList(),
                profile.checkIns().stream().map(this::toCheckIn).toList());
        if (activePass != null) {
            response.setCurrentPassName(activePass.productName());
            response.setCurrentPassValidUntil(activePass.validUntil());
        }
        return response;
    }

    public com.wardanger.excalibur.generated.model.GuestPass toPass(GuestPass pass, LocalDate today) {
        var response = new com.wardanger.excalibur.generated.model.GuestPass(
                pass.id(),
                pass.productName(),
                OffsetDateTime.ofInstant(pass.purchasedAt(), clock.getZone()),
                pass.validFrom(),
                pass.validUntil(),
                pass.pricePaid(),
                com.wardanger.excalibur.generated.model.PaymentMethod.valueOf(pass.paymentMethod().name()),
                pass.issuedByEmployeeName(),
                com.wardanger.excalibur.generated.model.GuestPass.StatusEnum.valueOf(pass.status(today).name()));
        response.setTotalEntries(pass.totalEntries());
        response.setRemainingEntries(pass.remainingEntries());
        return response;
    }

    public com.wardanger.excalibur.generated.model.ProductDefinition toProduct(ProductDefinition product) {
        var response = new com.wardanger.excalibur.generated.model.ProductDefinition(
                product.id(),
                product.code(),
                product.name(),
                product.validityDays(),
                product.defaultPrice(),
                product.active());
        response.setEntryLimit(product.entryLimit());
        return response;
    }

    public com.wardanger.excalibur.generated.model.GuestCheckIn toCheckIn(GuestCheckIn checkIn) {
        var response = new com.wardanger.excalibur.generated.model.GuestCheckIn(
                checkIn.id(),
                OffsetDateTime.ofInstant(checkIn.checkedInAt(), clock.getZone()),
                checkIn.passName(),
                checkIn.checkedInByEmployeeName(),
                checkIn.reversed());
        if (checkIn.reversedAt() != null) {
            response.setReversedAt(OffsetDateTime.ofInstant(checkIn.reversedAt(), clock.getZone()));
            response.setReversedByEmployeeName(checkIn.reversedByEmployeeName());
            response.setReversalReason(checkIn.reversalReason());
        }
        return response;
    }
}
