package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Partner;
import com.jinbo.myerp.domain.PartnerType;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.PartnerService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartnerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class PartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PartnerService partnerService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        String body = """
                {"name":"Dongyang Trading","partnerType":"CUSTOMER"}
                """;

        mockMvc.perform(post("/api/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withToken_returns201() throws Exception {
        Partner saved = Partner.builder()
                .id(1L).name("Dongyang Trading").partnerType(PartnerType.CUSTOMER)
                .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        given(partnerService.register(any(Partner.class))).willReturn(saved);

        String body = """
                {"name":"Dongyang Trading","partnerType":"CUSTOMER"}
                """;

        mockMvc.perform(post("/api/partners")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/partners/1"))
                .andExpect(jsonPath("$.name").value("Dongyang Trading"))
                .andExpect(jsonPath("$.partnerType").value("CUSTOMER"));
    }

    @Test
    void register_missingRequiredField_returns400() throws Exception {
        String body = """
                {"businessNumber":"123-45-67890"}
                """;

        mockMvc.perform(post("/api/partners")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_includesLedgerBalances() throws Exception {
        Partner partner = Partner.builder().id(1L).name("Dongyang Trading").partnerType(PartnerType.CUSTOMER)
                .active(true).receivableBalance(new BigDecimal("300000.00")).payableBalance(BigDecimal.ZERO).build();
        given(partnerService.findById(1L)).willReturn(partner);

        mockMvc.perform(get("/api/partners/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivableBalance").value(300000.00))
                .andExpect(jsonPath("$.payableBalance").value(0));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(partnerService.findById(anyLong())).willThrow(new PartnerNotFoundException(99L));

        mockMvc.perform(get("/api/partners/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_returnsPagedResponse() throws Exception {
        Partner partner = Partner.builder().id(1L).name("a").partnerType(PartnerType.CUSTOMER).active(true).build();
        given(partnerService.findAll(0, 20)).willReturn(new PageResult<>(List.of(partner), 1, 0, 20));

        mockMvc.perform(get("/api/partners")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.content[0].name").value("a"));
    }
}
