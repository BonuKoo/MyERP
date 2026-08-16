package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.LedgerEntryResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "거래처 원장", description = "거래처별 미수금/미지급금 증감 이력 조회. 매입/매출/결제 전표 API가 자동으로 기록한다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/partners/{partnerId}/ledger")
@RequiredArgsConstructor
public class PartnerLedgerController {

    private final LedgerService ledgerService;

    @Operation(summary = "거래처 원장 이력 조회 (페이징)", description = "매출/매입 확정·취소(SALE_*, PURCHASE_*) 및 " +
            "결제 확정·취소(PAYMENT_*) 이력을 최신순으로 반환한다.")
    @GetMapping
    public ResponseEntity<PageResponse<LedgerEntryResponse>> history(@PathVariable Long partnerId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(ledgerService.findByPartnerId(partnerId, page, size), LedgerEntryResponse::from));
    }
}
