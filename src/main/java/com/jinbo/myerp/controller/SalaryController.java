package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.SalaryResponse;
import com.jinbo.myerp.service.SalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "급여", description = "월별 급여 계산/조회. 계산 실행과 조회 모두 OWNER 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/salaries")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @Operation(summary = "월별 급여 계산", description = "재직중인 전 직원의 급여를 계산해 저장한다. 이미 계산된 달이면 덮어쓴다(재계산).")
    @PostMapping("/calculate")
    public ResponseEntity<List<SalaryResponse>> calculate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        List<SalaryResponse> responses = salaryService.calculate(yearMonth).stream()
                .map(SalaryResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "월별 급여 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<SalaryResponse>> findByPayYearMonth(
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(salaryService.findByPayYearMonth(yearMonth, page, size), SalaryResponse::from));
    }
}
