package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CertificationRequest;
import com.jinbo.myerp.controller.dto.CertificationResponse;
import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.CertificationService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "인증정보", description = "KS 등 품목 인증정보. 품목 등록 시 다대다로 연결된다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @Operation(summary = "인증정보 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 인증정보명",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CertificationResponse> register(@Valid @RequestBody CertificationRequest request) {
        Certification saved = certificationService.register(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(CertificationResponse.from(saved));
    }

    @Operation(summary = "인증정보 전체 조회")
    @GetMapping
    public ResponseEntity<List<CertificationResponse>> findAll() {
        List<CertificationResponse> response = certificationService.findAll().stream()
                .map(CertificationResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
