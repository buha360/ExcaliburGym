package com.wardanger.excalibur.retail.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wardanger.excalibur.audit.application.AuditLogService;
import com.wardanger.excalibur.events.EventTopics;
import com.wardanger.excalibur.guest.application.GuestRepository;
import com.wardanger.excalibur.guest.domain.Guest;
import com.wardanger.excalibur.integration.outbox.IntegrationEventPublisher;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.retail.domain.BalanceTransaction;
import com.wardanger.excalibur.retail.domain.RetailProduct;
import com.wardanger.excalibur.retail.domain.RetailSale;
import com.wardanger.excalibur.retail.domain.RetailSaleItem;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RetailService {

    private final RetailRepository retail;
    private final GuestRepository guests;
    private final AuditLogService auditLog;
    private final IntegrationEventPublisher integrationEvents;
    private final Clock clock;

    public List<RetailProduct> listActiveProducts() {
        return retail.findActiveProducts();
    }

    public List<RetailProduct> listAllProducts() {
        return retail.findAllProducts();
    }

    @Transactional
    public RetailProduct createProduct(String rawName, long defaultPrice, GymUserPrincipal employee) {
        var now = clock.instant();
        var product = new RetailProduct(
                UUID.randomUUID(),
                "RETAIL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                validName(rawName),
                validPrice(defaultPrice),
                true,
                now,
                now);
        retail.insertProduct(product);
        auditLog.record(employee, "RETAIL_PRODUCT_CREATED", "RETAIL_PRODUCT", product.id(),
                "Pénztári termék létrehozva: " + product.name());
        return product;
    }

    @Transactional
    public RetailProduct updateProduct(UUID productId, String rawName, long defaultPrice, GymUserPrincipal employee) {
        var existing = requireProduct(productId);
        var updated = new RetailProduct(existing.id(), existing.code(), validName(rawName), validPrice(defaultPrice),
                existing.active(), existing.createdAt(), clock.instant());
        retail.updateProduct(updated);
        auditLog.record(employee, "RETAIL_PRODUCT_UPDATED", "RETAIL_PRODUCT", updated.id(),
                "Pénztári termék módosítva: " + updated.name());
        return updated;
    }

    @Transactional
    public void deactivateProduct(UUID productId, GymUserPrincipal employee) {
        var product = requireProduct(productId);
        if (!retail.deactivateProduct(productId, clock.instant())) {
            throw conflict("RETAIL_PRODUCT_ALREADY_INACTIVE", "A termék már inaktív.");
        }
        auditLog.record(employee, "RETAIL_PRODUCT_DEACTIVATED", "RETAIL_PRODUCT", product.id(),
                "Pénztári termék inaktiválva: " + product.name());
    }

    public GuestBalanceView getBalance(UUID guestId) {
        var guest = requireGuest(guestId);
        return new GuestBalanceView(guest.id(), guest.fullName(), retail.balance(guestId),
                retail.findBalanceTransactions(guestId));
    }

    @Transactional
    public GuestBalanceView deposit(
            UUID guestId,
            long amount,
            PaymentMethod paymentMethod,
            GymUserPrincipal employee) {
        var guest = requireGuest(guestId);
        if (amount < 1 || amount > 10_000_000) {
            throw badRequest("INVALID_DEPOSIT_AMOUNT", "A feltöltés összege 1 és 10 000 000 Ft között lehet.");
        }
        if (paymentMethod == PaymentMethod.PREPAID_BALANCE) {
            throw badRequest("INVALID_DEPOSIT_PAYMENT_METHOD", "Egyenleget készpénzzel vagy bankkártyával lehet feltölteni.");
        }
        var now = clock.instant();
        retail.ensureBalanceAccount(guestId, now);
        retail.addBalance(guestId, amount, now);
        var balance = retail.balance(guestId);
        var transaction = new BalanceTransaction(
                UUID.randomUUID(), guest.id(), guest.fullName(), BalanceTransaction.Type.DEPOSIT, amount, balance,
                paymentMethod, null, employee.id(), employee.displayName(), now);
        retail.insertBalanceTransaction(transaction);
        auditLog.record(employee, "GUEST_BALANCE_DEPOSITED", "GUEST_BALANCE_TRANSACTION", transaction.id(),
                "Vendégegyenleg feltöltve: " + guest.fullName() + " – " + amount + " Ft");
        integrationEvents.publish(EventTopics.RETAIL_EVENTS, "GUEST_BALANCE_DEPOSITED",
                "GUEST_BALANCE_TRANSACTION", transaction.id(), employee.id(), now,
                Map.of("guestId", guest.id().toString(), "amount", amount, "paymentMethod", paymentMethod.name()));
        return new GuestBalanceView(guest.id(), guest.fullName(), balance, retail.findBalanceTransactions(guestId));
    }

    @Transactional
    public RetailSale sell(
            UUID guestId,
            PaymentMethod paymentMethod,
            List<SaleLineRequest> requestedItems,
            GymUserPrincipal employee) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw badRequest("EMPTY_RETAIL_SALE", "A vásárláshoz legalább egy terméket válassz.");
        }
        Guest guest = guestId == null ? null : requireGuest(guestId);
        if (paymentMethod == PaymentMethod.PREPAID_BALANCE && guest == null) {
            throw badRequest("GUEST_REQUIRED_FOR_BALANCE_PAYMENT", "Vendégegyenleges fizetéshez válassz vendéget.");
        }

        var saleId = UUID.randomUUID();
        var items = new ArrayList<RetailSaleItem>();
        long total = 0;
        for (var requested : requestedItems) {
            if (requested.quantity() < 1 || requested.quantity() > 100) {
                throw badRequest("INVALID_RETAIL_QUANTITY", "Egy termékből 1 és 100 közötti mennyiség adható el.");
            }
            var product = retail.findActiveProduct(requested.productId()).orElseThrow(() -> new ApiException(
                    HttpStatus.NOT_FOUND, "RETAIL_PRODUCT_NOT_FOUND", "A kiválasztott termék nem található vagy inaktív."));
            if (product.defaultPrice() <= 0) {
                throw conflict("RETAIL_PRODUCT_PRICE_MISSING", "A(z) " + product.name() + " termék ára nincs beállítva.");
            }
            long lineTotal;
            try {
                lineTotal = Math.multiplyExact(product.defaultPrice(), requested.quantity());
                total = Math.addExact(total, lineTotal);
            } catch (ArithmeticException exception) {
                throw badRequest("RETAIL_TOTAL_TOO_LARGE", "A vásárlás végösszege túl nagy.");
            }
            items.add(new RetailSaleItem(UUID.randomUUID(), saleId, product.id(), product.name(),
                    product.defaultPrice(), requested.quantity(), lineTotal));
        }

        var now = clock.instant();
        if (paymentMethod == PaymentMethod.PREPAID_BALANCE) {
            retail.ensureBalanceAccount(guest.id(), now);
            if (!retail.subtractBalance(guest.id(), total, now)) {
                throw conflict("INSUFFICIENT_GUEST_BALANCE", "A vendég egyenlege nem elegendő a vásárláshoz.");
            }
        }

        var sale = new RetailSale(saleId, guest == null ? null : guest.id(), guest == null ? null : guest.fullName(),
                total, paymentMethod, employee.id(), employee.displayName(), now, List.copyOf(items));
        retail.insertSale(sale);

        if (paymentMethod == PaymentMethod.PREPAID_BALANCE) {
            var balance = retail.balance(guest.id());
            retail.insertBalanceTransaction(new BalanceTransaction(
                    UUID.randomUUID(), guest.id(), guest.fullName(), BalanceTransaction.Type.PURCHASE, -total, balance,
                    null, sale.id(), employee.id(), employee.displayName(), now));
        }

        auditLog.record(employee, "RETAIL_SALE_COMPLETED", "RETAIL_SALE", sale.id(),
                "Pénztári vásárlás rögzítve: " + total + " Ft"
                        + (guest == null ? "" : " – " + guest.fullName()));
        var payload = new LinkedHashMap<String, Object>();
        payload.put("saleId", sale.id().toString());
        payload.put("totalPrice", total);
        payload.put("paymentMethod", paymentMethod.name());
        payload.put("itemCount", items.size());
        if (guest != null) {
            payload.put("guestId", guest.id().toString());
        }
        integrationEvents.publish(EventTopics.RETAIL_EVENTS, "RETAIL_SALE_COMPLETED",
                "RETAIL_SALE", sale.id(), employee.id(), now, payload);
        return sale;
    }

    private RetailProduct requireProduct(UUID productId) {
        return retail.findProduct(productId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "RETAIL_PRODUCT_NOT_FOUND", "A pénztári termék nem található."));
    }

    private Guest requireGuest(UUID guestId) {
        return guests.findById(guestId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "GUEST_NOT_FOUND", "A vendég nem található."));
    }

    private static String validName(String rawName) {
        var name = rawName == null ? "" : rawName.strip().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 120) {
            throw badRequest("INVALID_RETAIL_PRODUCT_NAME", "A termék neve 2 és 120 karakter között lehet.");
        }
        return name;
    }

    private static long validPrice(long price) {
        if (price < 0 || price > 10_000_000) {
            throw badRequest("INVALID_RETAIL_PRODUCT_PRICE", "Az ár 0 és 10 000 000 Ft között lehet.");
        }
        return price;
    }

    private static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public record SaleLineRequest(UUID productId, int quantity) {
    }

    public record GuestBalanceView(
            UUID guestId,
            String guestName,
            long balance,
            List<BalanceTransaction> transactions) {
    }
}