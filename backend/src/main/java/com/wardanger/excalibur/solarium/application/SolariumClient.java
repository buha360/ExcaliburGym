package com.wardanger.excalibur.solarium.application;

import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.shared.error.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SolariumClient {
    private final RestClient restClient;

    public SolariumClient(@Qualifier("solariumRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    public List<Product> products() { try { var result = restClient.get().uri("/internal/v1/solarium/products").retrieve().body(Product[].class); return result == null ? List.of() : List.of(result); } catch (RestClientException e) { throw map(e); } }
    public Account account(UUID guestId, String guestName) { try { var result = restClient.get().uri("/internal/v1/solarium/accounts/{id}", guestId).retrieve().body(Account.class); return result == null ? new Account(guestId, guestName, 0, List.of()) : result; } catch (RestClientException e) { throw map(e); } }
    public Account purchase(UUID guestId, String guestName, UUID productId, int quantity, String paymentMethod, GymUserPrincipal employee) { try { var result = restClient.post().uri("/internal/v1/solarium/purchases").body(new Purchase(guestId, guestName, productId, quantity, paymentMethod, employee.id(), employee.displayName())).retrieve().body(Account.class); if (result == null) throw unavailable(); return result; } catch (RestClientException e) { throw map(e); } }
    public Account use(UUID guestId, String guestName, int minutes, GymUserPrincipal employee) { try { var result = restClient.post().uri("/internal/v1/solarium/usages").body(new Usage(guestId, guestName, minutes, employee.id(), employee.displayName())).retrieve().body(Account.class); if (result == null) throw unavailable(); return result; } catch (RestClientException e) { throw map(e); } }
    public Product updateProduct(UUID id, String name, long price, boolean active, GymUserPrincipal employee) { try { var result = restClient.put().uri("/internal/v1/solarium/products/{id}", id).body(new ProductUpdate(name, price, active, employee.id(), employee.displayName())).retrieve().body(Product.class); if (result == null) throw unavailable(); return result; } catch (RestClientException e) { throw map(e); } }
    private static ApiException map(RestClientException e) { if (e instanceof HttpClientErrorException c) { if (c.getStatusCode().value() == 404) return new ApiException(HttpStatus.NOT_FOUND, "SOLARIUM_NOT_FOUND", "A szoláriumadat nem található."); if (c.getStatusCode().value() == 409) return new ApiException(HttpStatus.CONFLICT, "SOLARIUM_CONFLICT", "A szolárium-egyenleg vagy termék művelete ütközött."); if (c.getStatusCode().value() == 400) return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SOLARIUM_REQUEST", "A szolárium művelet adatai érvénytelenek."); } return unavailable(); }
    private static ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SOLARIUM_SERVICE_UNAVAILABLE", "A szoláriumszolgáltatás átmenetileg nem érhető el."); }
    public record Product(UUID id, String code, String name, int minutesPerUnit, long defaultPrice, boolean active, OffsetDateTime updatedAt) {}
    public record Transaction(UUID id, String type, int minutesDelta, int balanceAfter, String productName, Integer quantity, Long pricePaid, String paymentMethod, OffsetDateTime occurredAt, String employeeName) {}
    public record Account(UUID guestId, String guestName, int remainingMinutes, List<Transaction> transactions) {}
    private record Purchase(UUID guestId, String guestName, UUID productId, int quantity, String paymentMethod, UUID employeeId, String employeeName) {}
    private record Usage(UUID guestId, String guestName, int minutes, UUID employeeId, String employeeName) {}
    private record ProductUpdate(String name, long defaultPrice, boolean active, UUID employeeId, String employeeName) {}
}