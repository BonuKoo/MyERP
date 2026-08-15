package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemSpecRequest;
import com.jinbo.myerp.controller.dto.ItemSpecResponse;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.ItemSpecService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "규격", description = "품목 규격(단가/안전재고 등) 수정. 재고 수량 자체는 이 API로 바꿀 수 없다(재고 태그 참고).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/item-specs")
@RequiredArgsConstructor
public class ItemSpecController {

    private final ItemSpecService itemSpecService;

    @Operation(summary = "규격 정보 수정", description = "규격명/단위/매입가/매출가/안전재고를 수정한다. currentStock(현재재고)은 이 API로 변경되지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 규격",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ItemSpecResponse> update(@PathVariable Long id, @Valid @RequestBody ItemSpecRequest request) {
        ItemSpec updated = itemSpecService.update(id, request.toDomain());
        return ResponseEntity.ok(ItemSpecResponse.from(updated));
    }
}
