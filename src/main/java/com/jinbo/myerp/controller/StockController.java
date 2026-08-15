package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemSpecResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.StockAdjustRequest;
import com.jinbo.myerp.controller.dto.StockHistoryResponse;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.service.StockService;
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

@RestController
@RequestMapping("/api/item-specs/{itemSpecId}/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/adjust")
    public ResponseEntity<ItemSpecResponse> adjust(@PathVariable Long itemSpecId,
                                                     @Valid @RequestBody StockAdjustRequest request,
                                                     @AuthenticationPrincipal Long currentUserId) {
        ItemSpec updated = stockService.adjustStock(itemSpecId, request.quantityDelta(), currentUserId);
        return ResponseEntity.ok(ItemSpecResponse.from(updated));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<StockHistoryResponse>> history(@PathVariable Long itemSpecId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(stockService.findHistoryByItemSpecId(itemSpecId, page, size), StockHistoryResponse::from));
    }
}
