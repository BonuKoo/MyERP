package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;
import com.jinbo.myerp.exception.EmailAlreadyExistsException;
import com.jinbo.myerp.exception.InvalidCredentialsException;
import com.jinbo.myerp.mapper.CompanyUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyUserMapper companyUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CompanyUser register(String email, String rawPassword, String name, UserRole role) {
        companyUserMapper.findByEmail(email).ifPresent(user -> {
            throw new EmailAlreadyExistsException(email);
        });

        CompanyUser companyUser = CompanyUser.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(role)
                .active(true)
                .build();

        companyUserMapper.insert(companyUser);
        return companyUser;
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
