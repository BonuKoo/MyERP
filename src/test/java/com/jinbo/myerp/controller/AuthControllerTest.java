package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.CompanyUser;
import com.jinbo.myerp.domain.UserRole;
import com.jinbo.myerp.exception.EmailAlreadyExistsException;
import com.jinbo.myerp.exception.InvalidCredentialsException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void register_success_returns201() throws Exception {
        CompanyUser saved = CompanyUser.builder()
                .id(1L).email("owner@myerp.com").name("Kim Jinbo").role(UserRole.OWNER)
                .active(true).createdAt(LocalDateTime.now()).build();
        given(authService.register(anyString(), anyString(), anyString(), any(UserRole.class))).willReturn(saved);

        String body = """
                {"email":"owner@myerp.com","password":"Password123!","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("owner@myerp.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        String body = """
                {"email":"not-an-email","password":"Password123!","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        String body = """
                {"email":"owner@myerp.com","password":"short","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordMissingUppercase_returns400() throws Exception {
        String body = """
                {"email":"owner@myerp.com","password":"password123!","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordMissingLowercase_returns400() throws Exception {
        String body = """
                {"email":"owner@myerp.com","password":"PASSWORD123!","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordMissingDigit_returns400() throws Exception {
        String body = """
                {"email":"owner@myerp.com","password":"Password!!!","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordMissingSpecialChar_returns400() throws Exception {
        String body = """
                {"email":"owner@myerp.com","password":"Password123","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        given(authService.register(anyString(), anyString(), anyString(), any(UserRole.class)))
                .willThrow(new EmailAlreadyExistsException("owner@myerp.com"));

        String body = """
                {"email":"owner@myerp.com","password":"Password123!","name":"Kim Jinbo","role":"OWNER"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_success_returnsAccessToken() throws Exception {
        CompanyUser user = CompanyUser.builder()
                .id(1L).email("owner@myerp.com").name("Kim Jinbo").role(UserRole.OWNER).active(true).build();
        given(authService.login("owner@myerp.com", "password123")).willReturn(user);

        String body = """
                {"email":"owner@myerp.com","password":"password123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void login_wrongCredentials_returns401() throws Exception {
        given(authService.login(anyString(), anyString())).willThrow(new InvalidCredentialsException());

        String body = """
                {"email":"owner@myerp.com","password":"wrong-password"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
