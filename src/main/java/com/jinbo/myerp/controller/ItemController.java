package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemRequest;
import com.jinbo.myerp.controller.dto.ItemResponse;
import com.jinbo.myerp.controller.dto.ItemSpecRequest;
import com.jinbo.myerp.controller.dto.ItemSpecResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.ItemService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "품목", description = "품목 등록/조회 및 품목의 규격(ItemSpec) 등록/조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ItemSpecService itemSpecService;

    @Operation(summary = "품목 등록", description = "categorySubId가 가리키는 중분류가 존재해야 하며, " +
            "certificationIds로 인증정보를 다대다로 함께 연결할 수 있다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 중분류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ItemResponse> register(@Valid @RequestBody ItemRequest request) {
        Item saved = itemService.register(request.toDomain(), request.certificationIdsOrEmpty());
        List<com.jinbo.myerp.domain.Certification> certifications = itemService.findCertifications(saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.from(saved, certifications));
    }

    @Operation(summary = "품목 단건 조회", description = "연결된 인증정보 목록을 함께 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 품목",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> findById(@PathVariable Long id) {
        Item item = itemService.findById(id);
        List<com.jinbo.myerp.domain.Certification> certifications = itemService.findCertifications(id);
        return ResponseEntity.ok(ItemResponse.from(item, certifications));
    }

    @Operation(summary = "품목 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<ItemResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(itemService.findAll(page, size),
                item -> ItemResponse.from(item, itemService.findCertifications(item.getId()))));
    }

    @Operation(summary = "품목 규격 등록", description = "등록 직후 현재재고(currentStock)는 항상 0으로 시작한다. " +
            "재고를 채우려면 매입 전표 등록 또는 재고 수동조정 API를 사용해야 한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 품목",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{itemId}/specs")
    public ResponseEntity<ItemSpecResponse> registerSpec(@PathVariable Long itemId, @Valid @RequestBody ItemSpecRequest request) {
        ItemSpec spec = request.toDomain();
        spec.setItemId(itemId);
        ItemSpec saved = itemSpecService.register(spec);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemSpecResponse.from(saved));
    }

    @Operation(summary = "품목의 규격 목록 조회")
    @GetMapping("/{itemId}/specs")
    public ResponseEntity<List<ItemSpecResponse>> findSpecsByItemId(@PathVariable Long itemId) {
        List<ItemSpecResponse> response = itemSpecService.findByItemId(itemId).stream()
                .map(ItemSpecResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
