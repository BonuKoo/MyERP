package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.CompanyInfo;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.CompanyInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyInfoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class CompanyInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CompanyInfoService companyInfoService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void register_returns201() throws Exception {
        given(companyInfoService.register(any(CompanyInfo.class)))
                .willReturn(CompanyInfo.builder().id(1L).companyName("Jinbo Trading").build());

        String body = """
                {"companyName":"Jinbo Trading"}
                """;

        mockMvc.perform(post("/api/company-info")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Jinbo Trading"));
    }

    @Test
    void findAll_returns200() throws Exception {
        given(companyInfoService.findAll())
                .willReturn(List.of(CompanyInfo.builder().id(1L).companyName("Jinbo Trading").build()));

        mockMvc.perform(get("/api/company-info")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Jinbo Trading"));
    }
}
