package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.SaleItemRequest;
import com.jinbo.myerp.controller.dto.SaleRequest;
import com.jinbo.myerp.controller.dto.SaleResponse;
import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.SaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "매출 전표", description = "매출 등록 시 규격별 재고가 자동 감소하고, 취소 시 자동으로 복원된다. " +
        "동시 요청에 대비해 재고 차감은 낙관적 락(버전 기반, 최대 3회 재시도)으로 처리한다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    // 어느 락 전략을 쓸지는 SaleLockStrategyConfig가 myerp.sale.lock-strategy 설정으로
    // 정한다(기본: 낙관적 락). 여기서 특정 구현으로 고정하면 두 전략을 같은 부하로
    // 비교할 수 없다 — k6/LOAD_TEST_PLAN.md A1 참고.
    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @Operation(summary = "매출 전표 등록", description = "등록과 동시에 상태는 CONFIRMED로 확정되며, " +
            "품목별 규격의 현재재고가 즉시 감소하고 재고이력(SALE_OUT)이 함께 기록된다. " +
            "전표번호(saleNo)는 서버가 자동 생성한다. 재고보다 많은 수량을 요청하면 실패한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패(품목 목록이 비어있는 경우 포함)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 거래처/회사정보/규격",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "재고 부족, 또는 낙관적 락 충돌이 반복되어 실패한 경우" +
                    "(후자는 클라이언트가 동일 요청을 재시도하면 해결될 수 있다)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SaleResponse> register(@Valid @RequestBody SaleRequest request,
                                                   @AuthenticationPrincipal Long currentUserId) {
        Sale saved = saleService.register(
                request.toDomain(),
                request.items().stream().map(SaleItemRequest::toDomain).toList(),
                currentUserId);
        return ResponseEntity.created(URI.create("/api/sales/" + saved.getId()))
                .body(SaleResponse.from(saved, saleService.findItemsBySaleId(saved.getId())));
    }

    @Operation(summary = "매출 전표 단건 조회", description = "매출 품목 목록을 함께 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매출 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> findById(@PathVariable Long id) {
        Sale sale = saleService.findById(id);
        return ResponseEntity.ok(SaleResponse.from(sale, saleService.findItemsBySaleId(id)));
    }

    @Operation(summary = "매출 전표 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<SaleResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(saleService.findAll(page, size),
                sale -> SaleResponse.from(sale, saleService.findItemsBySaleId(sale.getId()))));
    }

    @Operation(summary = "매출 전표 취소", description = "취소 시 등록 때 감소시킨 재고를 다시 복원하고 " +
            "재고이력(ADJUST, 원본 전표 참조)을 기록한다. 이미 취소된 전표는 다시 취소할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 매출 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 취소된 전표이거나 낙관적 락 충돌이 반복된 경우",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<SaleResponse> cancel(@PathVariable Long id,
                                                 @AuthenticationPrincipal Long currentUserId) {
        Sale canceled = saleService.cancel(id, currentUserId);
        return ResponseEntity.ok(SaleResponse.from(canceled, saleService.findItemsBySaleId(id)));
    }
}
