package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.Certification;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemImage;
import com.jinbo.myerp.domain.ItemSpec;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.ItemImageService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @MockBean
    private ItemImageService itemImageService;

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
                .andExpect(header().string("Location", "/api/items/1"))
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
        given(itemService.findAll(0, 20, null, null)).willReturn(new PageResult<>(List.of(item), 1, 0, 20));

        mockMvc.perform(get("/api/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void findAll_withCategoryFilterParams_passesToService() throws Exception {
        given(itemService.findAll(0, 20, 1L, 2L)).willReturn(new PageResult<>(List.of(), 0, 0, 20));

        mockMvc.perform(get("/api/items")
                        .param("categoryMainId", "1")
                        .param("categorySubId", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk());

        verify(itemService).findAll(0, 20, 1L, 2L);
    }

    /**
     * ItemService.update()는 구현·테스트까지 되어 있었지만 이를 호출하는 엔드포인트가
     * 없어 실제로는 쓸 수 없는 죽은 코드였다. 품목 사진을 수정 화면에서 붙이려면
     * 품목 수정 자체가 먼저 가능해야 해서 이번에 노출한다.
     */
    @Test
    void update_returns200WithUpdatedItem() throws Exception {
        Item updated = Item.builder().id(1L).categorySubId(2L).name("세라픽스 PC-8000P")
                .ksStandard("KS L 1593").active(true).updatedAt(LocalDateTime.now()).build();
        given(itemService.update(eq(1L), any(Item.class), any())).willReturn(updated);
        given(itemService.findCertifications(1L)).willReturn(List.of());

        String body = """
                {"categorySubId":2,"name":"세라픽스 PC-8000P","ksStandard":"KS L 1593","certificationIds":[]}
                """;

        mockMvc.perform(put("/api/items/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("세라픽스 PC-8000P"))
                .andExpect(jsonPath("$.categorySubId").value(2));
    }

    @Test
    void update_whenItemMissing_returns404() throws Exception {
        given(itemService.update(eq(99L), any(Item.class), any())).willThrow(new ItemNotFoundException(99L));

        String body = """
                {"categorySubId":2,"name":"없는 품목"}
                """;

        mockMvc.perform(put("/api/items/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_includesAllImages() throws Exception {
        Item item = Item.builder().id(1L).categorySubId(1L).name("세라픽스").active(true).build();
        given(itemService.findById(1L)).willReturn(item);
        given(itemService.findCertifications(1L)).willReturn(List.of());
        given(itemImageService.findByItemId(1L)).willReturn(List.of(
                ItemImage.builder().id(10L).itemId(1L).uploadFileName("a.png").displayOrder(0).primary(true).build(),
                ItemImage.builder().id(11L).itemId(1L).uploadFileName("b.png").displayOrder(1).build()));

        mockMvc.perform(get("/api/items/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].id").value(10));
    }

    /**
     * 목록은 품목마다 사진을 따로 조회하면 N+1이 된다(이미 인증정보 조회가 그런 상태라
     * 더 악화시키지 않아야 한다). 배치 조회 한 번으로 끝나는지 검증한다.
     */
    @Test
    void findAll_loadsPrimaryImagesInOneBatchQuery() throws Exception {
        Item first = Item.builder().id(1L).name("품목1").build();
        Item second = Item.builder().id(2L).name("품목2").build();
        given(itemService.findAll(0, 20, null, null))
                .willReturn(new PageResult<>(List.of(first, second), 2, 0, 20));
        given(itemImageService.findPrimaryByItemIds(List.of(1L, 2L))).willReturn(List.of(
                ItemImage.builder().id(10L).itemId(1L).uploadFileName("a.png").primary(true).build()));

        mockMvc.perform(get("/api/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].images[0].id").value(10))
                .andExpect(jsonPath("$.content[1].images.length()").value(0));

        verify(itemImageService).findPrimaryByItemIds(List.of(1L, 2L));
        verify(itemImageService, never()).findByItemId(anyLong());
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
