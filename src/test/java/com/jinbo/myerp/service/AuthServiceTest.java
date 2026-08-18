package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;
import com.jinbo.myerp.exception.EmailAlreadyExistsException;
import com.jinbo.myerp.exception.InvalidCredentialsException;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CompanyUserMapper companyUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void register_success() {
        // 사용자가 아직 없는 최초 상태 = 부트스트랩 허용 경로
        given(companyUserMapper.countAll()).willReturn(0);
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
        given(companyUserMapper.countAll()).willReturn(0);
        given(companyUserMapper.findByEmail("a@myerp.com"))
                .willReturn(Optional.of(CompanyUser.builder().email("a@myerp.com").build()));

        assertThatThrownBy(() -> authService.register("a@myerp.com", "password123", "Tester", UserRole.STAFF))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    /**
     * 계정 생성은 OWNER만 할 수 있다. 그런데 그 규칙을 그대로 적용하면 최초 OWNER를
     * 아무도 만들 수 없는 닭이알 문제가 생기므로, 사용자가 0명일 때만 예외적으로 허용한다.
     */
    @Test
    void register_whenNoUserExists_allowsWithoutAuthentication() {
        given(companyUserMapper.countAll()).willReturn(0);
        given(companyUserMapper.findByEmail("first@myerp.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encoded");

        CompanyUser result = authService.register("first@myerp.com", "password123", "최초사업주", UserRole.OWNER);

        assertThat(result.getRole()).isEqualTo(UserRole.OWNER);
        verify(companyUserMapper).insert(result);
    }

    @Test
    void register_whenUsersExistAndAnonymous_throwsAccessDenied() {
        given(companyUserMapper.countAll()).willReturn(1);

        assertThatThrownBy(() -> authService.register("b@myerp.com", "password123", "침입자", UserRole.OWNER))
                .isInstanceOf(AccessDeniedException.class);

        verify(companyUserMapper, never()).insert(any());
    }

    @Test
    void register_whenUsersExistAndCallerIsStaff_throwsAccessDenied() {
        given(companyUserMapper.countAll()).willReturn(5);
        authenticateAs("STAFF");

        assertThatThrownBy(() -> authService.register("b@myerp.com", "password123", "직원", UserRole.STAFF))
                .isInstanceOf(AccessDeniedException.class);

        verify(companyUserMapper, never()).insert(any());
    }

    @Test
    void register_whenUsersExistAndCallerIsOwner_allows() {
        given(companyUserMapper.countAll()).willReturn(5);
        given(companyUserMapper.findByEmail("b@myerp.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        authenticateAs("OWNER");

        CompanyUser result = authService.register("b@myerp.com", "password123", "신규직원", UserRole.STAFF);

        verify(companyUserMapper).insert(result);
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
