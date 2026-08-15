package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;
import com.jinbo.myerp.exception.EmailAlreadyExistsException;
import com.jinbo.myerp.exception.InvalidCredentialsException;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CompanyUserMapper companyUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        given(companyUserMapper.findByEmail("a@myerp.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");

        CompanyUser result = authService.register("a@myerp.com", "password123", "Tester", UserRole.STAFF);

        assertThat(result.getEmail()).isEqualTo("a@myerp.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.isActive()).isTrue();
        verify(companyUserMapper).insert(result);
    }

    @Test
    void register_duplicateEmail_throws() {
        given(companyUserMapper.findByEmail("a@myerp.com"))
                .willReturn(Optional.of(CompanyUser.builder().email("a@myerp.com").build()));

        assertThatThrownBy(() -> authService.register("a@myerp.com", "password123", "Tester", UserRole.STAFF))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void login_success() {
        CompanyUser user = CompanyUser.builder().email("a@myerp.com").password("encoded").active(true).build();
        given(companyUserMapper.findByEmail("a@myerp.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("raw", "encoded")).willReturn(true);

        CompanyUser result = authService.login("a@myerp.com", "raw");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void login_wrongPassword_throws() {
        CompanyUser user = CompanyUser.builder().email("a@myerp.com").password("encoded").active(true).build();
        given(companyUserMapper.findByEmail("a@myerp.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login("a@myerp.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_userNotFound_throws() {
        given(companyUserMapper.findByEmail("none@myerp.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("none@myerp.com", "raw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_inactiveUser_throws() {
        CompanyUser user = CompanyUser.builder().email("a@myerp.com").password("encoded").active(false).build();
        given(companyUserMapper.findByEmail("a@myerp.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("a@myerp.com", "raw"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
