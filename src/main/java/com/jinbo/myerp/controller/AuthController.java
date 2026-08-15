package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CompanyUserResponse;
import com.jinbo.myerp.controller.dto.LoginRequest;
import com.jinbo.myerp.controller.dto.LoginResponse;
import com.jinbo.myerp.controller.dto.RegisterRequest;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<CompanyUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        CompanyUser user = authService.register(request.email(), request.password(), request.name(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyUserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        CompanyUser user = authService.login(request.email(), request.password());
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", user.getId(), user.getEmail(), user.getName(), user.getRole()));
    }
}
