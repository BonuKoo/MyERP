package com.jinbo.myerp.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificationRequest(
        @NotBlank @Size(max = 50) String name
) {
}
