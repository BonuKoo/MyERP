package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Salary;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.SalaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalaryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class SalaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SalaryService salaryService;

    private String staffToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        staffToken = "Bearer " + jwtTokenProvider.createToken(9L, "staff@myerp.com", "STAFF");
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    private Salary sample() {
        return Salary.builder().id(1L).employeeId(1L).payYearMonth("2026-08").workDays(20)
                .basePay(new BigDecimal("2000000")).positionAllowance(new BigDecimal("200000"))
                .overtimePay(BigDecimal.ZERO).grossPay(new BigDecimal("2200000"))
                .incomeTax(new BigDecimal("72600")).residentTax(new BigDecimal("7260"))
                .nationalPension(new BigDecimal("99000")).healthInsurance(new BigDecimal("72600"))
                .employmentInsurance(new BigDecimal("17600")).totalDeduction(new BigDecimal("269060"))
                .netPay(new BigDecimal("1930940")).build();
    }

    @Test
    void calculate_withOwnerToken_returns200() throws Exception {
        given(salaryService.calculate(YearMonth.of(2026, 8))).willReturn(List.of(sample()));

        mockMvc.perform(post("/api/salaries/calculate").param("yearMonth", "2026-08")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].netPay").value(1930940));
    }

    @Test
    void calculate_withStaffToken_returns403() throws Exception {
        mockMvc.perform(post("/api/salaries/calculate").param("yearMonth", "2026-08")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByPayYearMonth_withOwnerToken_returns200() throws Exception {
        given(salaryService.findByPayYearMonth("2026-08", 0, 20)).willReturn(new PageResult<>(List.of(sample()), 1, 0, 20));

        mockMvc.perform(get("/api/salaries").param("yearMonth", "2026-08")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void findByPayYearMonth_withStaffToken_returns403() throws Exception {
        mockMvc.perform(get("/api/salaries").param("yearMonth", "2026-08")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());
    }
}
