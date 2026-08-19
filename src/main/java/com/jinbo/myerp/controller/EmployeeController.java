package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.EmployeeRequest;
import com.jinbo.myerp.controller.dto.EmployeeResignRequest;
import com.jinbo.myerp.controller.dto.EmployeeResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.EmployeeService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "사원", description = "인사관리 사원 마스터. 등록/수정/퇴사처리는 OWNER 전용, 조회는 STAFF도 가능")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "사원 등록", description = "부서/직책이 존재해야 하고, 로그인 계정을 연결하면 중복 연결(409)을 막는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부서/직책/계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 다른 사원과 연결된 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<EmployeeResponse> register(@Valid @RequestBody EmployeeRequest request) {
        Employee saved = employeeService.register(request.toDomain());
        return ResponseEntity.created(URI.create("/api/employees/" + saved.getId()))
                .body(EmployeeResponse.from(saved));
    }

    @Operation(summary = "내 사원 정보 조회", description = "로그인 계정에 연결된 사원 정보를 반환한다. 연결된 사원이 없으면 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "연결된 사원 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> findMe(@AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.findByCompanyUserId(currentUserId)));
    }

    @Operation(summary = "사원 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사원",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.findById(id)));
    }

    @Operation(summary = "사원 목록 조회 (페이징)", description = "부서/직책/이름(LIKE)으로 선택적으로 필터링한다.")
    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(PageResponse.of(
                employeeService.findAll(page, size, departmentId, positionId, name), EmployeeResponse::from));
    }

    @Operation(summary = "사원 정보 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사원/부서/직책",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        Employee updated = employeeService.update(
                id, request.departmentId(), request.positionId(), request.name(), request.phone(), request.email());
        return ResponseEntity.ok(EmployeeResponse.from(updated));
    }

    @Operation(summary = "퇴사 처리", description = "실제로 삭제하지 않고 퇴사일을 기록하고 is_active를 false로 바꾼다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사원",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/resign")
    public ResponseEntity<Void> resign(@PathVariable Long id, @Valid @RequestBody EmployeeResignRequest request) {
        employeeService.resign(id, request.resignationDate());
        return ResponseEntity.noContent().build();
    }
}
