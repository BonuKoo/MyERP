package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Payment;
import com.jinbo.myerp.domain.PaymentStatus;
import com.jinbo.myerp.domain.PaymentType;
import com.jinbo.myerp.exception.InvalidStatusTransitionException;
import com.jinbo.myerp.exception.PartnerNotFoundException;
import com.jinbo.myerp.exception.PaymentNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.PageResult;
import com.jinbo.myerp.service.PaymentService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PaymentService paymentService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(42L, "owner@myerp.com", "OWNER");
    }

    private Payment samplePayment() {
        return Payment.builder().id(1L).paymentNo("PM123").partnerId(1L).paymentType(PaymentType.RECEIPT)
                .amount(new BigDecimal("100000")).paymentDate(LocalDate.now())
                .status(PaymentStatus.CONFIRMED).createdBy(42L).build();
    }

    @Test
    void register_returns201AndUsesAuthenticatedUserAsCreator() throws Exception {
        given(paymentService.register(any(Payment.class), eq(42L))).willReturn(samplePayment());

        String body = """
                {"partnerId":1,"paymentType":"RECEIPT","amount":100000,"paymentDate":"2026-08-16","method":"BANK_TRANSFER"}
                """;

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/payments/1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void register_withoutToken_returns401() throws Exception {
        String body = """
                {"partnerId":1,"paymentType":"RECEIPT","amount":100000,"paymentDate":"2026-08-16"}
                """;

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_zeroAmount_returns400() throws Exception {
        String body = """
                {"partnerId":1,"paymentType":"RECEIPT","amount":0,"paymentDate":"2026-08-16"}
                """;

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_partnerNotFound_returns404() throws Exception {
        given(paymentService.register(any(Payment.class), eq(42L))).willThrow(new PartnerNotFoundException(1L));

        String body = """
                {"partnerId":1,"paymentType":"RECEIPT","amount":100000,"paymentDate":"2026-08-16"}
                """;

        mockMvc.perform(post("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(paymentService.findById(anyLong())).willThrow(new PaymentNotFoundException(99L));

        mockMvc.perform(get("/api/payments/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returnsPayment() throws Exception {
        given(paymentService.findById(1L)).willReturn(samplePayment());

        mockMvc.perform(get("/api/payments/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNo").value("PM123"));
    }

    @Test
    void findAll_returnsPagedResponse() throws Exception {
        given(paymentService.findAll(0, 20)).willReturn(new PageResult<>(List.of(samplePayment()), 1, 0, 20));

        mockMvc.perform(get("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void cancel_success_returns200() throws Exception {
        Payment canceled = samplePayment();
        canceled.setStatus(PaymentStatus.CANCELED);
        given(paymentService.cancel(1L, 42L)).willReturn(canceled);

        mockMvc.perform(post("/api/payments/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancel_alreadyCanceled_returns409() throws Exception {
        given(paymentService.cancel(anyLong(), anyLong()))
                .willThrow(new InvalidStatusTransitionException("이미 취소된 결제 전표입니다"));

        mockMvc.perform(post("/api/payments/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isConflict());
    }
}
