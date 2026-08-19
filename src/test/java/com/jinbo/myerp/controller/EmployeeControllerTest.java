package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Employee;
import com.jinbo.myerp.exception.EmployeeNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.EmployeeService;
import com.jinbo.myerp.service.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private EmployeeService employeeService;

    private String ownerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
        staffToken = "Bearer " + jwtTokenProvider.createToken(2L, "staff@myerp.com", "STAFF");
    }

    private Employee sampleEmployee() {
        return Employee.builder().id(1L).departmentId(1L).positionId(1L).name("김철수")
                .hireDate(LocalDate.of(2026, 1, 1)).active(true).build();
    }

    @Test
    void register_withOwnerToken_returns201() throws Exception {
        given(employeeService.register(any(Employee.class))).willReturn(sampleEmployee());

        mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1,\"positionId\":1,\"name\":\"김철수\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/employees/1"))
                .andExpect(jsonPath("$.name").value("김철수"));
    }

    @Test
    void register_withStaffToken_returns403() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1,\"positionId\":1,\"name\":\"김철수\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1,\"positionId\":1,\"name\":\"\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_withStaffToken_returns200() throws Exception {
        given(employeeService.findAll(0, 20, null, null, null))
                .willReturn(new PageResult<>(List.of(sampleEmployee()), 1, 0, 20));

        mockMvc.perform(get("/api/employees")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.content[0].name").value("김철수"));
    }

    @Test
    void findAll_withNameFilter_passesToService() throws Exception {
        given(employeeService.findAll(0, 20, null, null, "철수"))
                .willReturn(new PageResult<>(List.of(sampleEmployee()), 1, 0, 20));

        mockMvc.perform(get("/api/employees").param("name", "철수")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk());

        verify(employeeService).findAll(0, 20, null, null, "철수");
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(employeeService.findById(99L)).willThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/employees/99")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withOwnerToken_returns200() throws Exception {
        Employee updated = sampleEmployee();
        given(employeeService.update(eq(1L), eq(1L), eq(1L), anyString(), any(), any())).willReturn(updated);

        mockMvc.perform(put("/api/employees/1")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1,\"positionId\":1,\"name\":\"김철수\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김철수"));
    }

    @Test
    void update_withStaffToken_returns403() throws Exception {
        mockMvc.perform(put("/api/employees/1")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1,\"positionId\":1,\"name\":\"김철수\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resign_withOwnerToken_returns204() throws Exception {
        mockMvc.perform(patch("/api/employees/1/resign")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resignationDate\":\"2026-06-30\"}"))
                .andExpect(status().isNoContent());

        verify(employeeService).resign(eq(1L), eq(LocalDate.of(2026, 6, 30)));
    }

    @Test
    void resign_withStaffToken_returns403() throws Exception {
        mockMvc.perform(patch("/api/employees/1/resign")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resignationDate\":\"2026-06-30\"}"))
                .andExpect(status().isForbidden());

        verify(employeeService, org.mockito.Mockito.never()).resign(anyLong(), any());
    }
}
