package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Position;
import com.jinbo.myerp.exception.PositionNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.PositionService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(PositionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PositionService positionService;

    private String ownerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
        staffToken = "Bearer " + jwtTokenProvider.createToken(2L, "staff@myerp.com", "STAFF");
    }

    @Test
    void register_withOwnerToken_returns201() throws Exception {
        Position saved = Position.builder().id(1L).name("팀장").allowance(new BigDecimal("200000")).active(true).build();
        given(positionService.register(any(Position.class))).willReturn(saved);

        mockMvc.perform(post("/api/positions")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"팀장\",\"allowance\":200000}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/positions/1"))
                .andExpect(jsonPath("$.name").value("팀장"));
    }

    @Test
    void register_withStaffToken_returns403() throws Exception {
        mockMvc.perform(post("/api/positions")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"팀장\",\"allowance\":200000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"팀장\",\"allowance\":200000}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/positions")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"allowance\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_negativeAllowance_returns400() throws Exception {
        mockMvc.perform(post("/api/positions")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"팀장\",\"allowance\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_withStaffToken_returns200() throws Exception {
        Position position = Position.builder().id(1L).name("팀장").allowance(new BigDecimal("200000")).active(true).build();
        given(positionService.findAll(0, 20)).willReturn(new PageResult<>(List.of(position), 1, 0, 20));

        mockMvc.perform(get("/api/positions")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.content[0].name").value("팀장"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(positionService.findById(99L)).willThrow(new PositionNotFoundException(99L));

        mockMvc.perform(get("/api/positions/99")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withOwnerToken_returns200() throws Exception {
        Position updated = Position.builder().id(1L).name("변경후").allowance(new BigDecimal("150000")).active(true).build();
        given(positionService.update(eq(1L), anyString(), any(BigDecimal.class))).willReturn(updated);

        mockMvc.perform(put("/api/positions/1")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"변경후\",\"allowance\":150000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("변경후"));
    }

    @Test
    void update_withStaffToken_returns403() throws Exception {
        mockMvc.perform(put("/api/positions/1")
                        .header(HttpHeaders.AUTHORIZATION, staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"변경후\",\"allowance\":150000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_withOwnerToken_returns204() throws Exception {
        mockMvc.perform(delete("/api/positions/1")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isNoContent());

        verify(positionService).deactivate(1L);
    }

    @Test
    void deactivate_withStaffToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/positions/1")
                        .header(HttpHeaders.AUTHORIZATION, staffToken))
                .andExpect(status().isForbidden());

        verify(positionService, org.mockito.Mockito.never()).deactivate(anyLong());
    }
}
