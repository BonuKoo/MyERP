package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.domain.StockChangeType;
import com.jinbo.myerp.domain.StockHistory;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.StockService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StockService stockService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(42L, "owner@myerp.com", "OWNER");
    }

    @Test
    void adjust_returns200AndUsesAuthenticatedUserAsCreatedBy() throws Exception {
        ItemSpec updated = ItemSpec.builder().id(1L).currentStock(30).version(1).build();
        given(stockService.adjustStock(eq(1L), anyInt(), eq(42L))).willReturn(updated);

        String body = """
                {"quantityDelta":20}
                """;

        mockMvc.perform(post("/api/item-specs/1/stock/adjust")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(30));
    }

    @Test
    void adjust_withoutToken_returns401() throws Exception {
        String body = """
                {"quantityDelta":20}
                """;

        mockMvc.perform(post("/api/item-specs/1/stock/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adjust_zeroDelta_returns400() throws Exception {
        String body = """
                {"quantityDelta":0}
                """;

        mockMvc.perform(post("/api/item-specs/1/stock/adjust")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjust_insufficientStock_returns409() throws Exception {
        given(stockService.adjustStock(anyLong(), anyInt(), anyLong()))
                .willThrow(new InsufficientStockException(1L, 5, -10));

        String body = """
                {"quantityDelta":-10}
                """;

        mockMvc.perform(post("/api/item-specs/1/stock/adjust")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void history_returnsPagedResponse() throws Exception {
        StockHistory history = StockHistory.builder().id(1L).itemSpecId(1L).changeType(StockChangeType.ADJUST)
                .quantity(20).beforeStock(10).afterStock(30).build();
        given(stockService.findHistoryByItemSpecId(1L, 0, 20)).willReturn(new PageResult<>(List.of(history), 1, 0, 20));

        mockMvc.perform(get("/api/item-specs/1/stock/history")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.content[0].changeType").value("ADJUST"));
    }
}
