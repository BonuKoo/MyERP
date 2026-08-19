package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Attendance;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.AttendanceService;
import com.jinbo.myerp.service.AttendanceSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AttendanceService attendanceService;

    private String staffToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        staffToken = "Bearer " + jwtTokenProvider.createToken(9L, "staff@myerp.com", "STAFF");
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void clockIn_withStaffToken_returns200() throws Exception {
        Attendance attendance = Attendance.builder().id(1L).employeeId(1L).workDate(LocalDate.now())
                .clockIn(LocalDateTime.now()).build();
        given(attendanceService.clockIn()).willReturn(attendance);

        mockMvc.perform(post("/api/attendances/clock-in")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    void clockIn_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/attendances/clock-in"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clockOut_withStaffToken_returns200() throws Exception {
        Attendance attendance = Attendance.builder().id(1L).employeeId(1L).workDate(LocalDate.now())
                .clockIn(LocalDateTime.now().minusHours(9)).clockOut(LocalDateTime.now()).build();
        given(attendanceService.clockOut()).willReturn(attendance);

        mockMvc.perform(post("/api/attendances/clock-out")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    void monthlySummary_withOwnerToken_returns200() throws Exception {
        AttendanceSummary summary = new AttendanceSummary(1L, "2026-08", 20, 30, 10, 60);
        given(attendanceService.monthlySummary(1L, YearMonth.of(2026, 8))).willReturn(summary);

        mockMvc.perform(get("/api/employees/1/attendances").param("yearMonth", "2026-08")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workDays").value(20));
    }

    @Test
    void monthlySummary_staffViewingOthers_returns403() throws Exception {
        given(attendanceService.monthlySummary(2L, YearMonth.of(2026, 8)))
                .willThrow(new AccessDeniedException("본인의 근태만 조회할 수 있습니다."));

        mockMvc.perform(get("/api/employees/2/attendances").param("yearMonth", "2026-08")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());
    }
}
