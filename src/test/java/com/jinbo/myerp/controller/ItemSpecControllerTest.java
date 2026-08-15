package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ItemSpecNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.ItemSpecService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemSpecController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class ItemSpecControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ItemSpecService itemSpecService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void update_returns200() throws Exception {
        ItemSpec updated = ItemSpec.builder().id(1L).itemId(1L).specName("20kg").unit("BOX")
                .costPrice(new BigDecimal("16000")).salePrice(new BigDecimal("21000")).safetyStock(15).active(true).build();
        given(itemSpecService.update(any(Long.class), any(ItemSpec.class))).willReturn(updated);

        String body = """
                {"specName":"20kg","unit":"BOX","costPrice":16000,"salePrice":21000,"safetyStock":15}
                """;

        mockMvc.perform(put("/api/item-specs/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salePrice").value(21000));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        given(itemSpecService.update(any(Long.class), any(ItemSpec.class))).willThrow(new ItemSpecNotFoundException(99L));

        String body = """
                {"specName":"20kg","unit":"BOX","costPrice":16000,"salePrice":21000,"safetyStock":15}
                """;

        mockMvc.perform(put("/api/item-specs/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
