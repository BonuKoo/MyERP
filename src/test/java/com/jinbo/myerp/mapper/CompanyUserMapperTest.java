package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class CompanyUserMapperTest {

    @Autowired
    private CompanyUserMapper companyUserMapper;

    private CompanyUser newUser(String email) {
        LocalDateTime now = LocalDateTime.now();
        return CompanyUser.builder()
                .email(email)
                .password("encoded-password")
                .name("테스트")
                .role(UserRole.STAFF)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void insertAndFindByEmail() {
        CompanyUser user = newUser("test@myerp.com");

        companyUserMapper.insert(user);
        assertThat(user.getId()).isNotNull();

        Optional<CompanyUser> found = companyUserMapper.findByEmail("test@myerp.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트");
        assertThat(found.get().getRole()).isEqualTo(UserRole.STAFF);
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findByEmail_notFound_returnsEmpty() {
        assertThat(companyUserMapper.findByEmail("none@myerp.com")).isEmpty();
    }

    @Test
    void update_changesNameAndRole() {
        CompanyUser user = newUser("update@myerp.com");
        companyUserMapper.insert(user);

        user.setName("변경된이름");
        user.setRole(UserRole.OWNER);
        companyUserMapper.update(user);

        CompanyUser updated = companyUserMapper.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("변경된이름");
        assertThat(updated.getRole()).isEqualTo(UserRole.OWNER);
    }
}
