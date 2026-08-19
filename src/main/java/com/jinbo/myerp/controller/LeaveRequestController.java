package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.LeaveRequestApplyRequest;
import com.jinbo.myerp.controller.dto.LeaveRequestResponse;
import com.jinbo.myerp.domain.LeaveRequest;
import com.jinbo.myerp.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "휴가", description = "휴가 신청/승인/반려. 신청·조회는 본인만(OWNER는 전체 조회 가능), 승인/반려는 OWNER 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @Operation(summary = "휴가 신청", description = "본인 명의로만 신청할 수 있다. 반차는 시작일=종료일이어야 한다.")
    @PostMapping("/api/employees/{employeeId}/leave-requests")
    public ResponseEntity<LeaveRequestResponse> apply(
            @PathVariable Long employeeId, @Valid @RequestBody LeaveRequestApplyRequest request) {
        LeaveRequest saved = leaveRequestService.apply(
                employeeId, request.leaveType(), request.startDate(), request.endDate(), request.reason());
        return ResponseEntity.created(URI.create("/api/leave-requests/" + saved.getId()))
                .body(LeaveRequestResponse.from(saved));
    }

    @Operation(summary = "사원별 휴가 신청 목록", description = "STAFF는 본인 것만, OWNER는 누구든 조회 가능.")
    @GetMapping("/api/employees/{employeeId}/leave-requests")
    public ResponseEntity<List<LeaveRequestResponse>> findByEmployeeId(@PathVariable Long employeeId) {
        List<LeaveRequestResponse> responses = leaveRequestService.findByEmployeeId(employeeId).stream()
                .map(LeaveRequestResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "휴가 승인")
    @PatchMapping("/api/leave-requests/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        leaveRequestService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "휴가 반려")
    @PatchMapping("/api/leave-requests/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        leaveRequestService.reject(id);
        return ResponseEntity.noContent().build();
    }
}
