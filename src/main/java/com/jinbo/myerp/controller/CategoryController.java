package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CategoryMainRequest;
import com.jinbo.myerp.controller.dto.CategoryMainResponse;
import com.jinbo.myerp.controller.dto.CategorySubRequest;
import com.jinbo.myerp.controller.dto.CategorySubResponse;
import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.CategoryService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "카테고리", description = "품목 대분류(main)/중분류(sub) 관리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "대분류 등록", description = "대분류명은 DB에 UNIQUE 제약이 있으나 " +
            "별도 예외 처리는 없어 중복 시 500으로 응답한다(추후 개선 예정).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/main")
    public ResponseEntity<CategoryMainResponse> registerMain(@Valid @RequestBody CategoryMainRequest request) {
        CategoryMain saved = categoryService.registerMain(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryMainResponse.from(saved));
    }

    @Operation(summary = "대분류 전체 조회")
    @GetMapping("/main")
    public ResponseEntity<List<CategoryMainResponse>> findAllMain() {
        List<CategoryMainResponse> response = categoryService.findAllMain().stream()
                .map(CategoryMainResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "중분류 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 대분류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/sub")
    public ResponseEntity<CategorySubResponse> registerSub(@Valid @RequestBody CategorySubRequest request) {
        CategorySub saved = categoryService.registerSub(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategorySubResponse.from(saved));
    }

    @Operation(summary = "특정 대분류의 중분류 목록 조회")
    @GetMapping("/main/{mainId}/sub")
    public ResponseEntity<List<CategorySubResponse>> findSubsByMainId(@PathVariable Long mainId) {
        List<CategorySubResponse> response = categoryService.findSubsByMainId(mainId).stream()
                .map(CategorySubResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
