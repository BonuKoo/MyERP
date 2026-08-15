package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.exception.CompanyInfoNotFoundException;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyInfoService {

    private final CompanyInfoMapper companyInfoMapper;

    @Transactional
    public CompanyInfo register(CompanyInfo companyInfo) {
        companyInfo.setCreatedAt(LocalDateTime.now());
        companyInfoMapper.insert(companyInfo);
        return companyInfo;
    }

    public CompanyInfo findById(Long id) {
        return companyInfoMapper.findById(id)
                .orElseThrow(() -> new CompanyInfoNotFoundException(id));
    }

    public List<CompanyInfo> findAll() {
        return companyInfoMapper.findAll();
    }
}
