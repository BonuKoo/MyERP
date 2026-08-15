package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.exception.CertificationAlreadyExistsException;
import com.jinbo.myerp.mapper.CertificationMapper;
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
class CertificationServiceTest {

    @Mock
    private CertificationMapper certificationMapper;

    @InjectMocks
    private CertificationService certificationService;

    @Test
    void register_success() {
        given(certificationMapper.findByName("KS인증")).willReturn(Optional.empty());

        Certification result = certificationService.register("KS인증");

        assertThat(result.getName()).isEqualTo("KS인증");
        verify(certificationMapper).insert(result);
    }

    @Test
    void register_duplicateName_throws() {
        given(certificationMapper.findByName("KS인증"))
                .willReturn(Optional.of(Certification.builder().id(1L).name("KS인증").build()));

        assertThatThrownBy(() -> certificationService.register("KS인증"))
                .isInstanceOf(CertificationAlreadyExistsException.class);
    }

    @Test
    void findAll_delegatesToMapper() {
        List<Certification> certs = List.of(Certification.builder().id(1L).name("KS인증").build());
        given(certificationMapper.findAll()).willReturn(certs);

        List<Certification> result = certificationService.findAll();

        assertThat(result).isEqualTo(certs);
    }
}
