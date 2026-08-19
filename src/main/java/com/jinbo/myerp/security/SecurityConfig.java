package com.jinbo.myerp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinbo.myerp.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인가 정책: 조회와 일상 업무(전표 등록, 품목/거래처 등록·수정, 재고 조정)는 STAFF도
     * 하고, 돈이 오가는 결제·되돌리는 취소·마스터 데이터·계정 생성만 OWNER로 제한한다.
     *
     * <p>베이스라인은 anyRequest().authenticated() 그대로 두고 OWNER 전용 규칙만 위에
     * 얹었다. GET을 hasAnyRole("OWNER","STAFF")로 좁히면 역할이 없는 인증 주체
     * (테스트의 @WithMockUser 등)가 403을 받게 되어, 막을 이유가 없는 것까지 막힌다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        // 회원가입은 permitAll로 두되 실제 허용 여부는 AuthService가 판단한다.
                        // "최초 사용자면 허용, 아니면 OWNER만"은 런타임 DB 상태에 달린
                        // 조건이라 URL 패턴으로 표현할 수 없다.
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 품목 사진 바이너리만 공개. 브라우저의 <img src>는 Authorization
                        // 헤더를 붙일 수 없어서 인증을 걸면 프론트의 사진이 전부 깨진다.
                        // 패턴에 세그먼트가 하나 더 있어야 매칭되므로 메타데이터 목록
                        // (/api/items/{id}/images)은 인증이 그대로 유지되고, GET으로
                        // 한정했으므로 업로드/삭제/대표지정도 인증이 필요하다.
                        .requestMatchers(HttpMethod.GET, "/api/items/*/images/*").permitAll()

                        // --- 여기부터 OWNER 전용 ---
                        // 돈: 수금/지급 등록과 취소
                        .requestMatchers(HttpMethod.POST, "/api/payments", "/api/payments/*/cancel").hasRole("OWNER")
                        // 되돌리기: 확정된 전표 취소(재고와 원장이 함께 되돌아간다)
                        .requestMatchers(HttpMethod.POST, "/api/sales/*/cancel", "/api/purchases/*/cancel").hasRole("OWNER")
                        // 마스터 데이터: 회사정보, 분류 체계, 인증 항목
                        .requestMatchers(HttpMethod.POST, "/api/company-info", "/api/categories/**", "/api/certifications").hasRole("OWNER")
                        // 거래처 비활성화
                        .requestMatchers(HttpMethod.DELETE, "/api/partners/*").hasRole("OWNER")
                        // 인사관리 마스터: 부서/직책/사원 등록/수정/비활성화·퇴사(조회는 STAFF도 가능)
                        .requestMatchers(HttpMethod.POST, "/api/departments", "/api/positions", "/api/employees").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/departments/*", "/api/positions/*", "/api/employees/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/departments/*", "/api/positions/*").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/employees/*/resign").hasRole("OWNER")
                        // 휴가 승인/반려: 신청·본인조회는 인증만 있으면 되고(Service에서 본인 확인),
                        // 승인 결정만 OWNER 전용
                        .requestMatchers(HttpMethod.PATCH, "/api/leave-requests/*/approve", "/api/leave-requests/*/reject").hasRole("OWNER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증 자체가 없을 때(401). 예전에는 response.sendError()를 써서 서블릿 컨테이너의
     * HTML 오류 페이지가 나갔는데, 나머지 API 오류는 전부 ErrorResponse JSON이라
     * 클라이언트가 응답을 일관되게 파싱할 수 없었다. 같은 형태로 맞춘다.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                writeError(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }

    /**
     * 인증은 됐지만 권한이 모자랄 때(403). URL 패턴 단계에서 거부되면 이 핸들러가
     * 응답을 쓴다 — 그 시점은 DispatcherServlet 바깥이라 @RestControllerAdvice가
     * 관여하지 못하기 때문이다(컨트롤러 안에서 던져진 AccessDeniedException은
     * GlobalExceptionHandler가 따로 잡는다).
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeError(response, HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다.");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(status, message));
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
