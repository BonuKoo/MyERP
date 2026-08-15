package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemSpecResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.StockAdjustRequest;
import com.jinbo.myerp.controller.dto.StockHistoryResponse;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.StockService;
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

@Tag(name = "재고", description = "수동 재고조정 및 재고변동 이력 조회. 매입/매출 전표에 의한 자동 증감은 각 전표 API가 처리한다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/item-specs/{itemSpecId}/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @Operation(summary = "재고 수동 조정", description = "quantityDelta는 증감량이다(양수=증가, 음수=감소). " +
            "조정 결과 재고가 음수가 되면 실패한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 규격",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "조정 후 재고가 음수가 되는 경우",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/adjust")
    public ResponseEntity<ItemSpecResponse> adjust(@PathVariable Long itemSpecId,
                                                     @Valid @RequestBody StockAdjustRequest request,
                                                     @AuthenticationPrincipal Long currentUserId) {
        ItemSpec updated = stockService.adjustStock(itemSpecId, request.quantityDelta(), currentUserId);
        return ResponseEntity.ok(ItemSpecResponse.from(updated));
    }

    @Operation(summary = "재고변동 이력 조회 (페이징)", description = "매입 입고(PURCHASE_IN)/매출 출고(SALE_OUT)/수동조정 및 " +
            "취소 반영(ADJUST) 이력을 최신순으로 반환한다.")
    @GetMapping("/history")
    public ResponseEntity<PageResponse<StockHistoryResponse>> history(@PathVariable Long itemSpecId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(stockService.findHistoryByItemSpecId(itemSpecId, page, size), StockHistoryResponse::from));
    }
}
