package com.wardanger.excalibur.retail.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.pass.domain.PaymentMethod;

public record RetailSale(
        UUID id,
        UUID guestId,
        String guestName,
        long totalPrice,
        PaymentMethod paymentMethod,
        UUID employeeId,
        String employeeName,
        Instant occurredAt,
        List<RetailSaleItem> items) {
}