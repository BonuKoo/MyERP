package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.LeaveRequest;
import com.jinbo.myerp.domain.LeaveStatus;
import com.jinbo.myerp.domain.LeaveType;
import com.jinbo.myerp.exception.LeaveRequestNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveRequestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private LeaveRequestService leaveRequestService;

    private String staffToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        staffToken = "Bearer " + jwtTokenProvider.createToken(9L, "staff@myerp.com", "STAFF");
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    private LeaveRequest sample() {
        return LeaveRequest.builder().id(1L).employeeId(1L).leaveType(LeaveType.FULL_DAY)
                .startDate(LocalDate.of(2026, 8, 10)).endDate(LocalDate.of(2026, 8, 10))
                .leaveDays(new BigDecimal("1")).status(LeaveStatus.PENDING).build();
    }

    @Test
    void apply_withStaffToken_returns201() throws Exception {
        given(leaveRequestService.apply(eq(1L), eq(LeaveType.FULL_DAY), any(), any(), any())).willReturn(sample());

        mockMvc.perform(post("/api/employees/1/leave-requests")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaveType\":\"FULL_DAY\",\"startDate\":\"2026-08-10\",\"endDate\":\"2026-08-10\",\"reason\":\"개인사유\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void apply_forOtherEmployee_returns403() throws Exception {
        given(leaveRequestService.apply(eq(2L), any(), any(), any(), any()))
                .willThrow(new AccessDeniedException("본인 명의로만 휴가를 신청할 수 있습니다."));

        mockMvc.perform(post("/api/employees/2/leave-requests")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaveType\":\"FULL_DAY\",\"startDate\":\"2026-08-10\",\"endDate\":\"2026-08-10\",\"reason\":\"개인사유\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByEmployeeId_selfStaff_returns200() throws Exception {
        given(leaveRequestService.findByEmployeeId(1L)).willReturn(List.of(sample()));

        mockMvc.perform(get("/api/employees/1/leave-requests")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approve_withOwnerToken_returns204() throws Exception {
        mockMvc.perform(patch("/api/leave-requests/1/approve")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());

        verify(leaveRequestService).approve(1L);
    }

    @Test
    void approve_withStaffToken_returns403() throws Exception {
        mockMvc.perform(patch("/api/leave-requests/1/approve")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());

        verify(leaveRequestService, org.mockito.Mockito.never()).approve(anyLong());
    }

    @Test
    void reject_withOwnerToken_returns204() throws Exception {
        mockMvc.perform(patch("/api/leave-requests/1/reject")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());

        verify(leaveRequestService).reject(1L);
    }

    @Test
    void approve_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new LeaveRequestNotFoundException(99L)).when(leaveRequestService).approve(99L);

        mockMvc.perform(patch("/api/leave-requests/99/approve")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNotFound());
    }
}
