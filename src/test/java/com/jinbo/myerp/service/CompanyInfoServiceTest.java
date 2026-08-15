package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.exception.CompanyInfoNotFoundException;
import com.jinbo.myerp.mapper.CompanyInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyInfoServiceTest {

    @Mock
    private CompanyInfoMapper companyInfoMapper;

    @InjectMocks
    private CompanyInfoService companyInfoService;

    @Test
    void register_setsCreatedAt() {
        CompanyInfo companyInfo = CompanyInfo.builder().companyName("진보상사").build();

        CompanyInfo result = companyInfoService.register(companyInfo);

        assertThat(result.getCreatedAt()).isNotNull();
        verify(companyInfoMapper).insert(companyInfo);
    }

    @Test
    void findById_notFound_throws() {
        given(companyInfoMapper.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> companyInfoService.findById(1L))
                .isInstanceOf(CompanyInfoNotFoundException.class);
    }

    @Test
    void findAll_delegatesToMapper() {
        List<CompanyInfo> list = List.of(CompanyInfo.builder().id(1L).companyName("진보상사").build());
        given(companyInfoMapper.findAll()).willReturn(list);

        List<CompanyInfo> result = companyInfoService.findAll();

        assertThat(result).isEqualTo(list);
    }
}
