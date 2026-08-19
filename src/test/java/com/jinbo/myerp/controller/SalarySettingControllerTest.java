package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.SalarySetting;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.SalarySettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalarySettingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class SalarySettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SalarySettingService salarySettingService;

    private String staffToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        staffToken = "Bearer " + jwtTokenProvider.createToken(9L, "staff@myerp.com", "STAFF");
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void get_withStaffToken_returns200() throws Exception {
        given(salarySettingService.get()).willReturn(
                SalarySetting.builder().id(1L).dailyWage(new BigDecimal("100000")).build());

        mockMvc.perform(get("/api/salary-settings")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWage").value(100000));
    }

    @Test
    void update_withOwnerToken_returns200() throws Exception {
        given(salarySettingService.updateDailyWage(any(BigDecimal.class))).willReturn(
                SalarySetting.builder().id(1L).dailyWage(new BigDecimal("120000")).build());

        mockMvc.perform(put("/api/salary-settings")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyWage\":120000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWage").value(120000));
    }

    @Test
    void update_withStaffToken_returns403() throws Exception {
        mockMvc.perform(put("/api/salary-settings")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyWage\":120000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_negativeWage_returns400() throws Exception {
        mockMvc.perform(put("/api/salary-settings")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyWage\":-1}"))
                .andExpect(status().isBadRequest());
    }
}
