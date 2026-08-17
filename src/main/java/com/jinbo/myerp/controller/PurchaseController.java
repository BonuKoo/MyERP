package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.PurchaseItemRequest;
import com.jinbo.myerp.controller.dto.PurchaseRequest;
import com.jinbo.myerp.controller.dto.PurchaseResponse;
import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "매입 전표", description = "매입 등록 시 규격별 재고가 자동 증가하고, 취소 시 자동으로 반환된다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Operation(summary = "매입 전표 등록", description = "등록과 동시에 상태는 CONFIRMED로 확정되며, " +
            "품목별 규격의 현재재고가 즉시 증가하고 재고이력(PURCHASE_IN)이 함께 기록된다. " +
            "전표번호(purchaseNo)는 서버가 자동 생성한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패(품목 목록이 비어있는 경우 포함)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 거래처/회사정보/규격",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PurchaseResponse> register(@Valid @RequestBody PurchaseRequest request,
                                                       @AuthenticationPrincipal Long currentUserId) {
        Purchase saved = purchaseService.register(
                request.toDomain(),
                request.items().stream().map(PurchaseItemRequest::toDomain).toList(),
                currentUserId);
        return ResponseEntity.created(URI.create("/api/purchases/" + saved.getId()))
                .body(PurchaseResponse.from(saved, purchaseService.findItemsByPurchaseId(saved.getId())));
    }

    @Operation(summary = "매입 전표 단건 조회", description = "매입 품목 목록을 함께 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매입 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> findById(@PathVariable Long id) {
        Purchase purchase = purchaseService.findById(id);
        return ResponseEntity.ok(PurchaseResponse.from(purchase, purchaseService.findItemsByPurchaseId(id)));
    }

    @Operation(summary = "매입 전표 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<PurchaseResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(purchaseService.findAll(page, size),
                purchase -> PurchaseResponse.from(purchase, purchaseService.findItemsByPurchaseId(purchase.getId()))));
    }

    @Operation(summary = "매입 전표 취소", description = "취소 시 등록 때 증가시킨 재고를 다시 차감하고 " +
            "재고이력(ADJUST, 원본 전표 참조)을 기록한다. 이미 취소된 전표는 다시 취소할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매입 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 취소된 전표이거나, " +
                    "취소로 인해 재고가 음수가 되는 경우(취소 이후 다른 출고로 재고가 부족해진 경우)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseResponse> cancel(@PathVariable Long id,
                                                     @AuthenticationPrincipal Long currentUserId) {
        Purchase canceled = purchaseService.cancel(id, currentUserId);
        return ResponseEntity.ok(PurchaseResponse.from(canceled, purchaseService.findItemsByPurchaseId(id)));
    }
}
