package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Sale;
import com.jinbo.myerp.domain.SaleItem;
import com.jinbo.myerp.domain.SaleStatus;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.OptimisticLockConflictException;
import com.jinbo.myerp.exception.SaleNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.SaleService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean(name = "optimisticLockSaleService")
    private SaleService saleService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(42L, "owner@myerp.com", "OWNER");
    }

    private Sale sampleSale() {
        return Sale.builder().id(1L).saleNo("SO123").partnerId(1L).companyInfoId(1L)
                .saleDate(LocalDate.now()).totalAmount(new BigDecimal("400000"))
                .status(SaleStatus.CONFIRMED).createdBy(42L).build();
    }

    @Test
    void register_returns201AndUsesAuthenticatedUserAsCreator() throws Exception {
        given(saleService.register(any(Sale.class), anyList(), eq(42L))).willReturn(sampleSale());
        given(saleService.findItemsBySaleId(1L)).willReturn(List.of());

        String body = """
                {"partnerId":1,"companyInfoId":1,"saleDate":"2026-08-15","items":[{"itemSpecId":1,"quantity":20,"unitPrice":20000}]}
                """;

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/sales/1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        String body = """
                {"partnerId":1,"companyInfoId":1,"saleDate":"2026-08-15","items":[{"itemSpecId":1,"quantity":20,"unitPrice":20000}]}
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_emptyItems_returns400() throws Exception {
        String body = """
                {"partnerId":1,"companyInfoId":1,"saleDate":"2026-08-15","items":[]}
                """;

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_insufficientStock_returns409() throws Exception {
        given(saleService.register(any(Sale.class), anyList(), eq(42L)))
                .willThrow(new InsufficientStockException(1L, 5, -20));

        String body = """
                {"partnerId":1,"companyInfoId":1,"saleDate":"2026-08-15","items":[{"itemSpecId":1,"quantity":20,"unitPrice":20000}]}
                """;

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_optimisticLockConflict_returns409() throws Exception {
        given(saleService.register(any(Sale.class), anyList(), eq(42L)))
                .willThrow(new OptimisticLockConflictException(1L));

        String body = """
                {"partnerId":1,"companyInfoId":1,"saleDate":"2026-08-15","items":[{"itemSpecId":1,"quantity":20,"unitPrice":20000}]}
                """;

        mockMvc.perform(post("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(saleService.findById(anyLong())).willThrow(new SaleNotFoundException(99L));

        mockMvc.perform(get("/api/sales/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returnsSaleWithItems() throws Exception {
        given(saleService.findById(1L)).willReturn(sampleSale());
        given(saleService.findItemsBySaleId(1L)).willReturn(
                List.of(SaleItem.builder().id(1L).itemSpecId(1L).quantity(20)
                        .unitPrice(new BigDecimal("20000")).amount(new BigDecimal("400000")).build()));

        mockMvc.perform(get("/api/sales/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleNo").value("SO123"))
                .andExpect(jsonPath("$.items[0].quantity").value(20));
    }

    @Test
    void findAll_returnsPagedResponse() throws Exception {
        given(saleService.findAll(0, 20)).willReturn(new PageResult<>(List.of(sampleSale()), 1, 0, 20));
        given(saleService.findItemsBySaleId(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void cancel_success_returns200() throws Exception {
        Sale canceled = sampleSale();
        canceled.setStatus(SaleStatus.CANCELED);
        given(saleService.cancel(1L, 42L)).willReturn(canceled);
        given(saleService.findItemsBySaleId(1L)).willReturn(List.of());

        mockMvc.perform(post("/api/sales/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancel_alreadyCanceled_returns409() throws Exception {
        given(saleService.cancel(anyLong(), anyLong()))
                .willThrow(new InvalidStatusTransitionException("이미 취소된 매출 전표입니다"));

        mockMvc.perform(post("/api/sales/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict());
    }
}
