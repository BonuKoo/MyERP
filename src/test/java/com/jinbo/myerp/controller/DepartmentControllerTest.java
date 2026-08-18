package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Department;
import com.jinbo.myerp.exception.DepartmentNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.DepartmentService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private DepartmentService departmentService;

    private String ownerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
        staffToken = "Bearer " + jwtTokenProvider.createToken(2L, "staff@myerp.com", "STAFF");
    }

    @Test
    void register_withOwnerToken_returns201() throws Exception {
        Department saved = Department.builder().id(1L).name("인사팀").active(true).createdAt(LocalDateTime.now()).build();
        given(departmentService.register(org.mockito.ArgumentMatchers.any(Department.class))).willReturn(saved);

        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"인사팀\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/departments/1"))
                .andExpect(jsonPath("$.name").value("인사팀"));
    }

    @Test
    void register_withStaffToken_returns403() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"인사팀\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"인사팀\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_withStaffToken_returns200() throws Exception {
        Department dept = Department.builder().id(1L).name("인사팀").active(true).build();
        given(departmentService.findAll(0, 20)).willReturn(new PageResult<>(List.of(dept), 1, 0, 20));

        mockMvc.perform(get("/api/departments")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.content[0].name").value("인사팀"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(departmentService.findById(99L)).willThrow(new DepartmentNotFoundException(99L));

        mockMvc.perform(get("/api/departments/99")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rename_withOwnerToken_returns200() throws Exception {
        Department renamed = Department.builder().id(1L).name("변경후").active(true).build();
        given(departmentService.rename(eq(1L), anyString())).willReturn(renamed);

        mockMvc.perform(put("/api/departments/1")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"변경후\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("변경후"));
    }

    @Test
    void rename_withStaffToken_returns403() throws Exception {
        mockMvc.perform(put("/api/departments/1")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"변경후\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_withOwnerToken_returns204() throws Exception {
        mockMvc.perform(delete("/api/departments/1")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());

        verify(departmentService).deactivate(1L);
    }

    @Test
    void deactivate_withStaffToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/departments/1")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());

        verify(departmentService, org.mockito.Mockito.never()).deactivate(anyLong());
    }
}
