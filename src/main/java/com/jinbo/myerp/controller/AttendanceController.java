package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.AttendanceResponse;
import com.jinbo.myerp.controller.dto.AttendanceSummaryResponse;
import com.jinbo.myerp.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "근태", description = "출퇴근 기록 및 월별 집계. 출퇴근은 본인만, 월별 집계는 본인 또는 OWNER만 조회 가능")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "출근", description = "로그인 계정에 연결된 본인 사원의 오늘 출근을 기록한다. 이미 출근했으면 409.")
    @PostMapping("/api/attendances/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn() {
        return ResponseEntity.ok(AttendanceResponse.from(attendanceService.clockIn()));
    }

    @Operation(summary = "퇴근", description = "본인 사원의 오늘 퇴근을 기록한다. 출근 기록이 없거나 이미 퇴근했으면 409.")
    @PostMapping("/api/attendances/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut() {
        return ResponseEntity.ok(AttendanceResponse.from(attendanceService.clockOut()));
    }

    @Operation(summary = "월별 근태 집계", description = "지각/조퇴/초과근무를 월 단위로 집계한다. STAFF는 본인 것만 조회 가능(타인 조회 시 403).")
    @GetMapping("/api/employees/{employeeId}/attendances")
    public ResponseEntity<AttendanceSummaryResponse> monthlySummary(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        return ResponseEntity.ok(AttendanceSummaryResponse.from(attendanceService.monthlySummary(employeeId, yearMonth)));
    }
}
