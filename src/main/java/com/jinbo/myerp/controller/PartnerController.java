package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.PageResponse;
import com.jinbo.myerp.controller.dto.PartnerRequest;
import com.jinbo.myerp.controller.dto.PartnerResponse;
import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    public ResponseEntity<PartnerResponse> register(@Valid @RequestBody PartnerRequest request) {
        Partner partner = partnerService.register(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(PartnerResponse.from(partner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(PartnerResponse.from(partnerService.findById(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PartnerResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(partnerService.findAll(page, size), PartnerResponse::from));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartnerResponse> update(@PathVariable Long id, @Valid @RequestBody PartnerRequest request) {
        Partner partner = partnerService.update(id, request.toDomain());
        return ResponseEntity.ok(PartnerResponse.from(partner));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        partnerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
