package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.SalarySettingRequest;
import com.jinbo.myerp.controller.dto.SalarySettingResponse;
import com.jinbo.myerp.service.SalarySettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "급여 설정", description = "일당 등 급여 계산 설정값. 조회는 인증만 있으면 되고, 변경은 OWNER 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/salary-settings")
@RequiredArgsConstructor
public class SalarySettingController {

    private final SalarySettingService salarySettingService;

    @Operation(summary = "급여 설정 조회", description = "설정이 없으면 기본값(일당 10만원)으로 생성해서 반환한다.")
    @GetMapping
    public ResponseEntity<SalarySettingResponse> get() {
        return ResponseEntity.ok(SalarySettingResponse.from(salarySettingService.get()));
    }

    @Operation(summary = "일당 변경")
    @PutMapping
    public ResponseEntity<SalarySettingResponse> update(@Valid @RequestBody SalarySettingRequest request) {
        return ResponseEntity.ok(SalarySettingResponse.from(salarySettingService.updateDailyWage(request.dailyWage())));
    }
}
