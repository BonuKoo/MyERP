package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.LedgerChangeType;
import com.jinbo.myerp.domain.LedgerEntry;
import com.jinbo.myerp.domain.LedgerType;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.LedgerService;
import com.jinbo.myerp.service.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartnerLedgerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class PartnerLedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private LedgerService ledgerService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(42L, "owner@myerp.com", "OWNER");
    }

    @Test
    void history_returnsPagedResponse() throws Exception {
        LedgerEntry entry = LedgerEntry.builder().id(1L).partnerId(1L).ledgerType(LedgerType.RECEIVABLE)
                .changeType(LedgerChangeType.SALE_CONFIRMED).amount(new BigDecimal("300000.00"))
                .balanceAfter(new BigDecimal("300000.00")).build();
        given(ledgerService.findByPartnerId(1L, 0, 20)).willReturn(new PageResult<>(List.of(entry), 1, 0, 20));

        mockMvc.perform(get("/api/partners/1/ledger")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.content[0].changeType").value("SALE_CONFIRMED"));
    }

    @Test
    void history_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/partners/1/ledger"))
                .andExpect(status().isUnauthorized());
    }
}
