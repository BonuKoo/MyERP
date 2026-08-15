package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.CompanyUserResponse;
import com.jinbo.myerp.controller.dto.LoginRequest;
import com.jinbo.myerp.controller.dto.LoginResponse;
import com.jinbo.myerp.controller.dto.RegisterRequest;
import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입 및 로그인. 이 그룹의 엔드포인트는 인증 없이 호출한다.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "회원가입", description = "회사 사용자(OWNER/STAFF)를 등록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<CompanyUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        CompanyUser user = authService.register(request.email(), request.password(), request.name(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyUserResponse.from(user));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인해 JWT accessToken을 발급받는다. " +
            "이후 모든 인증 필요 API는 이 토큰을 `Authorization: Bearer {token}` 헤더로 전달해야 한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        CompanyUser user = authService.login(request.email(), request.password());
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", user.getId(), user.getEmail(), user.getName(), user.getRole()));
    }
}
