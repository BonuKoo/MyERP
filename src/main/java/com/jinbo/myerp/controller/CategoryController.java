package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CategoryMainRequest;
import com.jinbo.myerp.controller.dto.CategoryMainResponse;
import com.jinbo.myerp.controller.dto.CategorySubRequest;
import com.jinbo.myerp.controller.dto.CategorySubResponse;
import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.service.CategoryService;
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

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/main")
    public ResponseEntity<CategoryMainResponse> registerMain(@Valid @RequestBody CategoryMainRequest request) {
        CategoryMain saved = categoryService.registerMain(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryMainResponse.from(saved));
    }

    @GetMapping("/main")
    public ResponseEntity<List<CategoryMainResponse>> findAllMain() {
        List<CategoryMainResponse> response = categoryService.findAllMain().stream()
                .map(CategoryMainResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sub")
    public ResponseEntity<CategorySubResponse> registerSub(@Valid @RequestBody CategorySubRequest request) {
        CategorySub saved = categoryService.registerSub(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategorySubResponse.from(saved));
    }

    @GetMapping("/main/{mainId}/sub")
    public ResponseEntity<List<CategorySubResponse>> findSubsByMainId(@PathVariable Long mainId) {
        List<CategorySubResponse> response = categoryService.findSubsByMainId(mainId).stream()
                .map(CategorySubResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
