package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.DepartmentRequest;
import com.jinbo.myerp.controller.dto.DepartmentResponse;
import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.DepartmentService;
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

@Tag(name = "부서", description = "인사관리 부서 마스터. 등록/수정/비활성화는 OWNER 전용, 조회는 STAFF도 가능")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "부서 등록", description = "부서명은 중복 등록할 수 없다(409).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 등록된 부서명",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DepartmentResponse> register(@Valid @RequestBody DepartmentRequest request) {
        Department saved = departmentService.register(request.toDomain());
        return ResponseEntity.created(URI.create("/api/departments/" + saved.getId()))
                .body(DepartmentResponse.from(saved));
    }

    @Operation(summary = "부서 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부서",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.findById(id)));
    }

    @Operation(summary = "부서 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<PageResponse<DepartmentResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(departmentService.findAll(page, size), DepartmentResponse::from));
    }

    @Operation(summary = "부서명 변경")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부서",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 등록된 부서명",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> rename(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        Department renamed = departmentService.rename(id, request.name());
        return ResponseEntity.ok(DepartmentResponse.from(renamed));
    }

    @Operation(summary = "부서 비활성화", description = "실제로 삭제하지 않고 is_active를 false로 바꾼다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부서",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        departmentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
