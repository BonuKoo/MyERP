package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.ItemService;
import com.jinbo.myerp.service.ItemSpecService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ItemService itemService;

    @MockBean
    private ItemSpecService itemSpecService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void register_returns201() throws Exception {
        Item saved = Item.builder().id(1L).categorySubId(1L).name("Ceramic Fix PC-7000D")
                .active(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        given(itemService.register(any(Item.class), any())).willReturn(saved);

        String body = """
                {"categorySubId":1,"name":"Ceramic Fix PC-7000D","certificationIds":[1,2]}
                """;

        mockMvc.perform(post("/api/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ceramic Fix PC-7000D"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(itemService.findById(anyLong())).willThrow(new ItemNotFoundException(99L));

        mockMvc.perform(get("/api/items/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returnsItemWithCertifications() throws Exception {
        Item item = Item.builder().id(1L).categorySubId(1L).name("Ceramic Fix PC-7000D").active(true).build();
        given(itemService.findById(1L)).willReturn(item);
        given(itemService.findCertifications(1L)).willReturn(List.of(Certification.builder().id(1L).name("KS").build()));

        mockMvc.perform(get("/api/items/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certifications[0].name").value("KS"));
    }

    @Test
    void findAll_returnsPagedResponse() throws Exception {
        Item item = Item.builder().id(1L).name("a").build();
        given(itemService.findAll(0, 20)).willReturn(new PageResult<>(List.of(item), 1, 0, 20));

        mockMvc.perform(get("/api/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void registerSpec_returns201() throws Exception {
        ItemSpec saved = ItemSpec.builder().id(1L).itemId(1L).specName("20kg").unit("BOX")
                .costPrice(new BigDecimal("15000")).salePrice(new BigDecimal("20000")).active(true).build();
        given(itemSpecService.register(any(ItemSpec.class))).willReturn(saved);

        String body = """
                {"specName":"20kg","unit":"BOX","costPrice":15000,"salePrice":20000,"safetyStock":10}
                """;

        mockMvc.perform(post("/api/items/1/specs")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specName").value("20kg"));
    }

    @Test
    void findSpecsByItemId_returns200() throws Exception {
        given(itemSpecService.findByItemId(1L)).willReturn(
                List.of(ItemSpec.builder().id(1L).itemId(1L).specName("20kg").build()));

        mockMvc.perform(get("/api/items/1/specs")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specName").value("20kg"));
    }
}
