package com.jinbo.myerp.controller.dto;

import com.jinbo.myerp.domain.Certification;

public record CertificationResponse(Long id, String name) {
    public static CertificationResponse from(Certification certification) {
        return new CertificationResponse(certification.getId(), certification.getName());
    }
}
