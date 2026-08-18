package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;
import com.jinbo.myerp.exception.EmailAlreadyExistsException;
import com.jinbo.myerp.exception.InvalidCredentialsException;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyUserMapper companyUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CompanyUser register(String email, String rawPassword, String name, UserRole role) {
        verifyCanCreateAccount();

        companyUserMapper.findByEmail(email).ifPresent(user -> {
            throw new EmailAlreadyExistsException(email);
        });

        LocalDateTime now = LocalDateTime.now();
        CompanyUser companyUser = CompanyUser.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(role)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        companyUserMapper.insert(companyUser);
        return companyUser;
    }

    /**
     * 계정 생성 권한 검사.
     *
     * <p>사내 ERP라 계정은 사업주(OWNER)가 만들어준다. 다만 그 규칙만 적용하면 최초
     * OWNER를 아무도 만들 수 없는 닭이알 문제가 생기므로, 사용자가 한 명도 없을 때만
     * 예외적으로 인증 없이 허용한다(부트스트랩).
     *
     * <p>이 조건은 런타임 DB 상태에 달려 있어 SecurityConfig의 URL 패턴으로는 표현할
     * 수 없다. 그래서 /api/auth/**는 permitAll로 두고 여기서 판단한다.
     */
    private void verifyCanCreateAccount() {
        if (companyUserMapper.countAll() == 0) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isOwner = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_OWNER".equals(authority.getAuthority()));

        if (!isOwner) {
            throw new AccessDeniedException("계정 생성은 사업주(OWNER)만 할 수 있습니다.");
        }
    }

    public CompanyUser login(String email, String rawPassword) {
        CompanyUser companyUser = companyUserMapper.findByEmail(email)
                .filter(CompanyUser::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, companyUser.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return companyUser;
    }
}
