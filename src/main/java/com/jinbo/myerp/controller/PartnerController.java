package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.PartnerRequest;
import com.jinbo.myerp.controller.dto.PartnerResponse;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.PartnerService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "거래처", description = "매입처(SUPPLIER)/매출처(CUSTOMER) 거래처 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "거래처 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PartnerResponse> register(@Valid @RequestBody PartnerRequest request) {
        Partner partner = partnerService.register(request.toDomain());
        return ResponseEntity.created(URI.create("/api/partners/" + partner.getId())).body(PartnerResponse.from(partner));
    }

    @Operation(summary = "거래처 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 거래처",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(PartnerResponse.from(partnerService.findById(id)));
    }

    @Operation(summary = "거래처 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<PartnerResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(partnerService.findAll(page, size), PartnerResponse::from));
    }

    @Operation(summary = "거래처 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 거래처",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(@PathVariable Long id, @Valid @RequestBody PartnerRequest request) {
        Partner partner = partnerService.update(id, request.toDomain());
        return ResponseEntity.ok(PartnerResponse.from(partner));
    }

    @Operation(summary = "거래처 비활성화", description = "실제로 삭제하지 않고 is_active를 false로 바꾼다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 거래처",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        partnerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
