package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.SaleItemRequest;
import com.jinbo.myerp.controller.dto.SaleRequest;
import com.jinbo.myerp.controller.dto.SaleResponse;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
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
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(@Qualifier("optimisticLockSaleService") SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> register(@Valid @RequestBody SaleRequest request,
                                                   @AuthenticationPrincipal Long currentUserId) {
        Sale saved = saleService.register(
                request.toDomain(),
                request.items().stream().map(SaleItemRequest::toDomain).toList(),
                currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SaleResponse.from(saved, saleService.findItemsBySaleId(saved.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> findById(@PathVariable Long id) {
        Sale sale = saleService.findById(id);
        return ResponseEntity.ok(SaleResponse.from(sale, saleService.findItemsBySaleId(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SaleResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(saleService.findAll(page, size),
                sale -> SaleResponse.from(sale, saleService.findItemsBySaleId(sale.getId()))));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleResponse> cancel(@PathVariable Long id,
                                                 @AuthenticationPrincipal Long currentUserId) {
        Sale canceled = saleService.cancel(id, currentUserId);
        return ResponseEntity.ok(SaleResponse.from(canceled, saleService.findItemsBySaleId(id)));
    }
}
