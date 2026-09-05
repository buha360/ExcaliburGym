package com.wardanger.excalibur.retail.domain;

import java.util.UUID;

public record RetailSaleItem(
        UUID id,
        UUID saleId,
        UUID productId,
        String productName,
        long unitPrice,
        int quantity,
        long lineTotal) {
}