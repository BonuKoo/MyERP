package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CompanyInfoRequest;
import com.jinbo.myerp.controller.dto.CompanyInfoResponse;
import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.CompanyInfoService;
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

@Tag(name = "회사정보", description = "매입/매출 전표에서 발행 주체로 쓰이는 우리 회사 정보")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/company-info")
@RequiredArgsConstructor
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;

    @Operation(summary = "회사정보 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CompanyInfoResponse> register(@Valid @RequestBody CompanyInfoRequest request) {
        CompanyInfo saved = companyInfoService.register(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyInfoResponse.from(saved));
    }

    @Operation(summary = "회사정보 전체 조회", description = "페이징 없이 전체 목록을 반환한다.")
    @GetMapping
    public ResponseEntity<List<CompanyInfoResponse>> findAll() {
        List<CompanyInfoResponse> response = companyInfoService.findAll().stream()
                .map(CompanyInfoResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
