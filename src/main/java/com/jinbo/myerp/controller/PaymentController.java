package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.PaymentRequest;
import com.jinbo.myerp.controller.dto.PaymentResponse;
import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.PaymentService;
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

@Tag(name = "결제(수금/지급)", description = "수금은 미수금을, 지급은 미지급금을 차감한다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 전표 등록", description = "등록과 동시에 CONFIRMED로 확정되며, " +
            "RECEIPT는 거래처 미수금을, DISBURSEMENT는 미지급금을 즉시 차감한다. 전표번호는 서버가 자동 생성한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 거래처",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> register(@Valid @RequestBody PaymentRequest request,
                                                      @AuthenticationPrincipal Long currentUserId) {
        Payment saved = paymentService.register(request.toDomain(), currentUserId);
        return ResponseEntity.created(URI.create("/api/payments/" + saved.getId()))
                .body(PaymentResponse.from(saved));
    }

    @Operation(summary = "결제 전표 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 결제 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.findById(id)));
    }

    @Operation(summary = "결제 전표 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(paymentService.findAll(page, size), PaymentResponse::from));
    }

    @Operation(summary = "결제 전표 취소", description = "취소 시 등록 때 차감했던 미수금/미지급금을 원복한다. " +
            "이미 취소된 전표는 다시 취소할 수 없다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 결제 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 취소된 전표",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancel(@PathVariable Long id,
                                                    @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.cancel(id, currentUserId)));
    }
}
