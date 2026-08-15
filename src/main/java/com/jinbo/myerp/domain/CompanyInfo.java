package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyInfo {

    private Long id;
    private String companyName;
    private String businessNumber;
    private String ceoName;
    private String address;
    private String phone;
    private LocalDateTime createdAt;
}
