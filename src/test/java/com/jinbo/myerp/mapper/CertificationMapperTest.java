package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.Certification;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class CertificationMapperTest {

    @Autowired
    private CertificationMapper certificationMapper;

    @Test
    void insertAndFindById() {
        Certification certification = Certification.builder().name("KS인증").build();

        certificationMapper.insert(certification);
        assertThat(certification.getId()).isNotNull();

        Optional<Certification> found = certificationMapper.findById(certification.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("KS인증");
    }

    @Test
    void findByName_returnsMatching() {
        certificationMapper.insert(Certification.builder().name("KS인증").build());

        Optional<Certification> found = certificationMapper.findByName("KS인증");

        assertThat(found).isPresent();
    }

    @Test
    void findAll_returnsAll() {
        certificationMapper.insert(Certification.builder().name("KS인증").build());
        certificationMapper.insert(Certification.builder().name("친환경인증").build());

        List<Certification> all = certificationMapper.findAll();

        assertThat(all).hasSize(2);
    }
}
