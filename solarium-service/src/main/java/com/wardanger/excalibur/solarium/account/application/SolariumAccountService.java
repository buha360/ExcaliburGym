package com.wardanger.excalibur.solarium.account.application;

import com.wardanger.excalibur.events.EventTopics;
import com.wardanger.excalibur.solarium.account.domain.SolariumAccount;
import com.wardanger.excalibur.solarium.account.domain.SolariumProduct;
import com.wardanger.excalibur.solarium.account.domain.SolariumTransaction;
import com.wardanger.excalibur.solarium.integration.IntegrationEventPublisher;
import com.wardanger.excalibur.solarium.shared.SolariumApiException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolariumAccountService {
    private final SolariumRepository repository;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public List<SolariumProduct> products(boolean includeInactive) { return repository.findProducts(includeInactive); }
    public SolariumAccount account(UUID guestId, String guestName) { return repository.getAccount(guestId, cleanName(guestName)); }

    @Transactional
    public SolariumAccount purchase(UUID guestId, String rawGuestName, UUID productId, int quantity, String paymentMethod, UUID employeeId, String rawEmployeeName) {
        if (quantity < 1 || quantity > 1000) throw bad("INVALID_QUANTITY", "A mennyiség 1–1000 között lehet.");
        var guestName = cleanName(rawGuestName); var employeeName = cleanName(rawEmployeeName);
        var product = repository.findProduct(productId);
        if (product == null || !product.active()) throw new SolariumApiException(HttpStatus.NOT_FOUND, "SOLARIUM_PRODUCT_NOT_FOUND", "A szoláriumtermék nem található vagy nem aktív.");
        if (product.defaultPrice() <= 0) throw new SolariumApiException(HttpStatus.CONFLICT, "SOLARIUM_PRICE_NOT_CONFIGURED", "Ehhez a termékhez még nincs beállítva ár.");
        var now = clock.instant(); var balance = repository.lockOrCreateAccount(guestId, guestName, now);
        var minutes = Math.multiplyExact(product.minutesPerUnit(), quantity);
        var price = Math.multiplyExact(product.defaultPrice(), quantity);
        var newBalance = Math.addExact(balance, minutes);
        repository.updateBalance(guestId, guestName, newBalance, now);
        var transactionId = UUID.randomUUID();
        repository.appendTransaction(transactionId, guestId, SolariumTransaction.Type.PURCHASE, minutes, newBalance, product.id(), product.name(), quantity, price, paymentMethod, now, employeeId, employeeName);
        events.publish(EventTopics.SOLARIUM_EVENTS, "SOLARIUM_MINUTES_PURCHASED", "SOLARIUM_ACCOUNT", guestId, employeeId, now,
                Map.of("transactionId", transactionId.toString(), "guestId", guestId.toString(), "minutes", minutes, "pricePaid", price, "paymentMethod", paymentMethod));
        return repository.getAccount(guestId, guestName);
    }

    @Transactional
    public SolariumAccount use(UUID guestId, String rawGuestName, int minutes, UUID employeeId, String rawEmployeeName) {
        if (minutes < 1 || minutes > 1000) throw bad("INVALID_MINUTES", "A felhasznált percek száma 1–1000 között lehet.");
        var guestName = cleanName(rawGuestName); var employeeName = cleanName(rawEmployeeName); var now = clock.instant();
        var balance = repository.lockOrCreateAccount(guestId, guestName, now);
        if (balance < minutes) throw new SolariumApiException(HttpStatus.CONFLICT, "INSUFFICIENT_SOLARIUM_MINUTES", "Nincs elegendő szolárium-perce a vendégnek.");
        var newBalance = balance - minutes;
        repository.updateBalance(guestId, guestName, newBalance, now);
        var transactionId = UUID.randomUUID();
        repository.appendTransaction(transactionId, guestId, SolariumTransaction.Type.USAGE, -minutes, newBalance, null, null, null, null, null, now, employeeId, employeeName);
        events.publish(EventTopics.SOLARIUM_EVENTS, "SOLARIUM_MINUTES_USED", "SOLARIUM_ACCOUNT", guestId, employeeId, now,
                Map.of("transactionId", transactionId.toString(), "guestId", guestId.toString(), "minutes", minutes));
        return repository.getAccount(guestId, guestName);
    }

    @Transactional
    public SolariumProduct updateProduct(UUID id, String rawName, long price, boolean active, UUID employeeId, String rawEmployeeName) {
        var name = cleanName(rawName); cleanName(rawEmployeeName);
        if (price < 0 || price > 10_000_000) throw bad("INVALID_PRICE", "Az ár 0 és 10 000 000 Ft között lehet.");
        if (repository.findProduct(id) == null) throw new SolariumApiException(HttpStatus.NOT_FOUND, "SOLARIUM_PRODUCT_NOT_FOUND", "A szoláriumtermék nem található.");
        return repository.updateProduct(id, name, price, active, clock.instant());
    }

    private static SolariumApiException bad(String code, String message) { return new SolariumApiException(HttpStatus.BAD_REQUEST, code, message); }
    private static String cleanName(String value) { var clean = value == null ? "" : value.strip().replaceAll("\\s+", " "); if (clean.length() < 2 || clean.length() > 160) throw bad("INVALID_NAME", "A név 2–160 karakter lehet."); return clean; }
}