package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.PurchaseItemRequest;
import com.jinbo.myerp.controller.dto.PurchaseRequest;
import com.jinbo.myerp.controller.dto.PurchaseResponse;
import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<PurchaseResponse> register(@Valid @RequestBody PurchaseRequest request,
                                                       @AuthenticationPrincipal Long currentUserId) {
        Purchase saved = purchaseService.register(
                request.toDomain(),
                request.items().stream().map(PurchaseItemRequest::toDomain).toList(),
                currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PurchaseResponse.from(saved, purchaseService.findItemsByPurchaseId(saved.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> findById(@PathVariable Long id) {
        Purchase purchase = purchaseService.findById(id);
        return ResponseEntity.ok(PurchaseResponse.from(purchase, purchaseService.findItemsByPurchaseId(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PurchaseResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(purchaseService.findAll(page, size),
                purchase -> PurchaseResponse.from(purchase, purchaseService.findItemsByPurchaseId(purchase.getId()))));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseResponse> cancel(@PathVariable Long id,
                                                     @AuthenticationPrincipal Long currentUserId) {
        Purchase canceled = purchaseService.cancel(id, currentUserId);
        return ResponseEntity.ok(PurchaseResponse.from(canceled, purchaseService.findItemsByPurchaseId(id)));
    }
}
