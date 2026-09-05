package com.wardanger.excalibur.retail.api;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.wardanger.excalibur.generated.api.RetailApi;
import com.wardanger.excalibur.generated.model.BalanceTransactionType;
import com.wardanger.excalibur.generated.model.CreateBalanceDepositRequest;
import com.wardanger.excalibur.generated.model.CreateRetailSaleRequest;
import com.wardanger.excalibur.generated.model.DepositPaymentMethod;
import com.wardanger.excalibur.generated.model.GuestBalanceAccount;
import com.wardanger.excalibur.generated.model.GuestBalanceTransaction;
import com.wardanger.excalibur.generated.model.RetailProduct;
import com.wardanger.excalibur.generated.model.RetailSale;
import com.wardanger.excalibur.generated.model.RetailSaleItem;
import com.wardanger.excalibur.generated.model.UpsertRetailProductRequest;
import com.wardanger.excalibur.pass.domain.PaymentMethod;
import com.wardanger.excalibur.retail.application.RetailService;
import com.wardanger.excalibur.security.GymUserPrincipal;
import com.wardanger.excalibur.security.SessionAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RetailApiController implements RetailApi {

    private final RetailService service;
    private final SessionAuthenticationService authentication;

    @Override
    public ResponseEntity<List<RetailProduct>> listRetailProducts() {
        return ResponseEntity.ok(service.listActiveProducts().stream().map(this::toProduct).toList());
    }

    @Override
    public ResponseEntity<List<RetailProduct>> listAllRetailProducts() {
        return ResponseEntity.ok(service.listAllProducts().stream().map(this::toProduct).toList());
    }

    @Override
    public ResponseEntity<RetailProduct> createRetailProduct(UpsertRetailProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toProduct(service.createProduct(request.getName(), request.getDefaultPrice(), employee())));
    }

    @Override
    public ResponseEntity<RetailProduct> updateRetailProduct(UUID productId, UpsertRetailProductRequest request) {
        return ResponseEntity.ok(toProduct(
                service.updateProduct(productId, request.getName(), request.getDefaultPrice(), employee())));
    }

    @Override
    public ResponseEntity<Void> deactivateRetailProduct(UUID productId) {
        service.deactivateProduct(productId, employee());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GuestBalanceAccount> getGuestBalance(UUID guestId) {
        return ResponseEntity.ok(toBalance(service.getBalance(guestId)));
    }

    @Override
    public ResponseEntity<GuestBalanceAccount> createGuestBalanceDeposit(
            UUID guestId,
            CreateBalanceDepositRequest request) {
        var result = service.deposit(guestId, request.getAmount(),
                PaymentMethod.valueOf(request.getPaymentMethod().name()), employee());
        return ResponseEntity.status(HttpStatus.CREATED).body(toBalance(result));
    }

    @Override
    public ResponseEntity<RetailSale> createRetailSale(CreateRetailSaleRequest request) {
        var lines = request.getItems().stream()
                .map(item -> new RetailService.SaleLineRequest(item.getProductId(), item.getQuantity()))
                .toList();
        var sale = service.sell(request.getGuestId(), PaymentMethod.valueOf(request.getPaymentMethod().name()),
                lines, employee());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSale(sale));
    }

    private RetailProduct toProduct(com.wardanger.excalibur.retail.domain.RetailProduct product) {
        return new RetailProduct(product.id(), product.code(), product.name(), product.defaultPrice(), product.active());
    }

    private GuestBalanceAccount toBalance(RetailService.GuestBalanceView balance) {
        return new GuestBalanceAccount(balance.guestId(), balance.guestName(), balance.balance(),
                balance.transactions().stream().map(transaction -> {
                    var response = new GuestBalanceTransaction(
                            transaction.id(),
                            BalanceTransactionType.valueOf(transaction.type().name()),
                            transaction.amountDelta(),
                            transaction.balanceAfter(),
                            transaction.employeeName(),
                            transaction.occurredAt().atOffset(ZoneOffset.UTC));
                    if (transaction.paymentMethod() != null) {
                        response.setPaymentMethod(DepositPaymentMethod.valueOf(transaction.paymentMethod().name()));
                    }
                    response.setReferenceId(transaction.referenceId());
                    return response;
                }).toList());
    }

    private RetailSale toSale(com.wardanger.excalibur.retail.domain.RetailSale sale) {
        var items = sale.items().stream()
                .map(item -> new RetailSaleItem(item.id(), item.productId(), item.productName(), item.unitPrice(),
                        item.quantity(), item.lineTotal()))
                .toList();
        return new RetailSale(
                sale.id(),
                sale.totalPrice(),
                com.wardanger.excalibur.generated.model.RetailPaymentMethod.valueOf(sale.paymentMethod().name()),
                sale.employeeName(),
                sale.occurredAt().atOffset(ZoneOffset.UTC),
                items)
                .guestId(sale.guestId())
                .guestName(sale.guestName());
    }

    private GymUserPrincipal employee() {
        return authentication.currentPrincipal().orElseThrow();
    }
}