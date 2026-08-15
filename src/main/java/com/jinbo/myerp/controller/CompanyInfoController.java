package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CompanyInfoRequest;
import com.jinbo.myerp.controller.dto.CompanyInfoResponse;
import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.service.CompanyInfoService;
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

@RestController
@RequestMapping("/api/company-info")
@RequiredArgsConstructor
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;

    @PostMapping
    public ResponseEntity<CompanyInfoResponse> register(@Valid @RequestBody CompanyInfoRequest request) {
        CompanyInfo saved = companyInfoService.register(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyInfoResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<CompanyInfoResponse>> findAll() {
        List<CompanyInfoResponse> response = companyInfoService.findAll().stream()
                .map(CompanyInfoResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
