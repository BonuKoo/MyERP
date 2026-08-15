package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Purchase;
import com.jinbo.myerp.domain.PurchaseItem;
import com.jinbo.myerp.domain.PurchaseStatus;
import com.jinbo.myerp.exception.InsufficientStockException;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.PurchaseNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.PurchaseService;
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

@WebMvcTest(PurchaseController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PurchaseService purchaseService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(42L, "owner@myerp.com", "OWNER");
    }

    private Purchase samplePurchase() {
        return Purchase.builder().id(1L).purchaseNo("PO123").partnerId(1L).companyInfoId(1L)
                .purchaseDate(LocalDate.now()).totalAmount(new BigDecimal("300000"))
                .status(PurchaseStatus.CONFIRMED).createdBy(42L).build();
    }

    @Test
    void register_returns201AndUsesAuthenticatedUserAsCreator() throws Exception {
        given(purchaseService.register(any(Purchase.class), anyList(), eq(42L))).willReturn(samplePurchase());
        given(purchaseService.findItemsByPurchaseId(1L)).willReturn(List.of());

        String body = """
                {"partnerId":1,"companyInfoId":1,"purchaseDate":"2026-08-15","items":[{"itemSpecId":1,"quantity":20,"unitPrice":15000}]}
                """;

        mockMvc.perform(post("/api/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/purchases/1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        String body = """
                {"partnerId":1,"companyInfoId":1,"purchaseDate":"2026-08-15","items":[{"itemSpecId":1,"quantity":20,"unitPrice":15000}]}
                """;

        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_emptyItems_returns400() throws Exception {
        String body = """
                {"partnerId":1,"companyInfoId":1,"purchaseDate":"2026-08-15","items":[]}
                """;

        mockMvc.perform(post("/api/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(purchaseService.findById(anyLong())).willThrow(new PurchaseNotFoundException(99L));

        mockMvc.perform(get("/api/purchases/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returnsPurchaseWithItems() throws Exception {
        given(purchaseService.findById(1L)).willReturn(samplePurchase());
        given(purchaseService.findItemsByPurchaseId(1L)).willReturn(
                List.of(PurchaseItem.builder().id(1L).itemSpecId(1L).quantity(20)
                        .unitPrice(new BigDecimal("15000")).amount(new BigDecimal("300000")).build()));

        mockMvc.perform(get("/api/purchases/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseNo").value("PO123"))
                .andExpect(jsonPath("$.items[0].quantity").value(20));
    }

    @Test
    void findAll_returnsPagedResponse() throws Exception {
        given(purchaseService.findAll(0, 20)).willReturn(new PageResult<>(List.of(samplePurchase()), 1, 0, 20));
        given(purchaseService.findItemsByPurchaseId(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/purchases")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void cancel_success_returns200() throws Exception {
        Purchase canceled = samplePurchase();
        canceled.setStatus(PurchaseStatus.CANCELED);
        given(purchaseService.cancel(1L, 42L)).willReturn(canceled);
        given(purchaseService.findItemsByPurchaseId(1L)).willReturn(List.of());

        mockMvc.perform(post("/api/purchases/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancel_alreadyCanceled_returns409() throws Exception {
        given(purchaseService.cancel(anyLong(), anyLong()))
                .willThrow(new InvalidStatusTransitionException("이미 취소된 매입 전표입니다"));

        mockMvc.perform(post("/api/purchases/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_insufficientStock_returns409() throws Exception {
        given(purchaseService.cancel(anyLong(), anyLong()))
                .willThrow(new InsufficientStockException(1L, 5, -20));

        mockMvc.perform(post("/api/purchases/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict());
    }
}
