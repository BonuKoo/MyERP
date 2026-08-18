package com.jinbo.myerp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 역할(OWNER/STAFF)별 접근 정책을 그대로 옮긴 테스트.
 *
 * <p>인가는 "막아야 할 걸 막았는가"만큼 "열어둬야 할 걸 잠그지 않았는가"도 중요하다.
 * 과잉 차단은 조용히 업무를 마비시키므로 STAFF가 통과해야 하는 경로도 함께 검증한다.
 *
 * <p>여기서는 인가 계층만 보므로 응답 본문의 정상 동작(200/201)까지는 따지지 않는다.
 * 서비스 계층이 404/400을 내는 것도 "인가는 통과했다"는 뜻이라 성공으로 취급한다 —
 * 403만 아니면 된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationPolicyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String ownerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        ownerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
        staffToken = "Bearer " + jwtTokenProvider.createToken(2L, "staff@myerp.com", "STAFF");
    }

    private boolean isForbidden(int status) {
        return status == 403;
    }

    @Nested
    @DisplayName("OWNER 전용 - STAFF는 403")
    class OwnerOnly {

        @Test
        void staff_cannotRegisterPayment() throws Exception {
            mockMvc.perform(post("/api/payments")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"partnerId\":1,\"paymentType\":\"RECEIPT\",\"amount\":1000,\"paymentDate\":\"2026-08-18\"}"))
                    .andExpect(status().isForbidden())
                    // 기본 403은 빈 본문이라, 다른 에러와 같은 ErrorResponse JSON인지 확인한다.
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void staff_cannotCancelPayment() throws Exception {
            mockMvc.perform(post("/api/payments/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void staff_cannotCancelSale() throws Exception {
            mockMvc.perform(post("/api/sales/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void staff_cannotCancelPurchase() throws Exception {
            mockMvc.perform(post("/api/purchases/1/cancel")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void staff_cannotRegisterCompanyInfo() throws Exception {
            mockMvc.perform(post("/api/company-info")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"companyName\":\"테스트\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void staff_cannotCreateCategoryMain() throws Exception {
            mockMvc.perform(post("/api/categories/main")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"신규분류\",\"displayOrder\":1}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void staff_cannotCreateCertification() throws Exception {
            mockMvc.perform(post("/api/certifications")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"신규인증\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void staff_cannotDeactivatePartner() throws Exception {
            mockMvc.perform(delete("/api/partners/1")
                            .header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("OWNER는 위 경로에서 403을 받지 않는다")
    class OwnerAllowed {

        @Test
        void owner_isNotForbiddenOnOwnerOnlyEndpoints() throws Exception {
            int categoryStatus = mockMvc.perform(post("/api/categories/main")
                            .header(HttpHeaders.AUTHORIZATION, ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"인가테스트분류\",\"displayOrder\":99}"))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(categoryStatus)).isFalse();

            int cancelStatus = mockMvc.perform(post("/api/sales/999999/cancel")
                            .header(HttpHeaders.AUTHORIZATION, ownerToken))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(cancelStatus)).isFalse();

            int partnerStatus = mockMvc.perform(delete("/api/partners/999999")
                            .header(HttpHeaders.AUTHORIZATION, ownerToken))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(partnerStatus)).isFalse();
        }
    }

    @Nested
    @DisplayName("STAFF도 가능해야 하는 일상 업무 - 과잉 차단 방지")
    class StaffAllowed {

        @Test
        void staff_canReadLists() throws Exception {
            mockMvc.perform(get("/api/items").header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/partners").header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/payments").header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/company-info").header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/categories/main").header(HttpHeaders.AUTHORIZATION, staffToken))
                    .andExpect(status().isOk());
        }

        @Test
        void staff_isNotForbiddenOnDailyWork() throws Exception {
            // 거래처 등록
            int partnerStatus = mockMvc.perform(post("/api/partners")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"인가테스트거래처\",\"partnerType\":\"CUSTOMER\"}"))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(partnerStatus)).isFalse();

            // 매출 전표 등록(취소와 달리 STAFF도 가능해야 한다)
            int saleStatus = mockMvc.perform(post("/api/sales")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"partnerId\":1,\"companyInfoId\":1,\"saleDate\":\"2026-08-18\",\"items\":[]}"))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(saleStatus)).isFalse();

            // 품목 등록
            int itemStatus = mockMvc.perform(post("/api/items")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"categorySubId\":1,\"name\":\"인가테스트품목\"}"))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(itemStatus)).isFalse();

            // 재고 수동조정
            int stockStatus = mockMvc.perform(post("/api/item-specs/1/stock/adjust")
                            .header(HttpHeaders.AUTHORIZATION, staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantityDelta\":10}"))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(isForbidden(stockStatus)).isFalse();
        }
    }

    @Nested
    @DisplayName("인증 자체가 없으면 403이 아니라 401")
    class Unauthenticated {

        @Test
        void noToken_returns401NotForbidden() throws Exception {
            mockMvc.perform(get("/api/items"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 401도 다른 에러와 같은 ErrorResponse JSON이어야 한다. 기존에는
         * response.sendError()를 써서 서블릿 컨테이너 HTML이 나갔다.
         */
        @Test
        void noToken_returnsErrorResponseJson() throws Exception {
            mockMvc.perform(get("/api/items"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }
}
