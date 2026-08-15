package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CertificationRequest;
import com.jinbo.myerp.controller.dto.CertificationResponse;
import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.service.CertificationService;
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
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @PostMapping
    public ResponseEntity<CertificationResponse> register(@Valid @RequestBody CertificationRequest request) {
        Certification saved = certificationService.register(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(CertificationResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<CertificationResponse>> findAll() {
        List<CertificationResponse> response = certificationService.findAll().stream()
                .map(CertificationResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
