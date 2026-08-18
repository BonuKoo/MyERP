package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.exception.CategoryMainNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.CategoryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CategoryService categoryService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    @Test
    void registerMain_returns201() throws Exception {
        CategoryMain saved = CategoryMain.builder().id(1L).name("Tile Adhesive").displayOrder(1).active(true).build();
        given(categoryService.registerMain(any(CategoryMain.class))).willReturn(saved);

        String body = """
                {"name":"Tile Adhesive","displayOrder":1}
                """;

        mockMvc.perform(post("/api/categories/main")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tile Adhesive"));
    }

    @Test
    void findAllMain_returns200() throws Exception {
        given(categoryService.findAllMain()).willReturn(
                List.of(CategoryMain.builder().id(1L).name("Tile Adhesive").displayOrder(1).active(true).build()));

        mockMvc.perform(get("/api/categories/main")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tile Adhesive"));
    }

    @Test
    void registerSub_whenMainNotFound_returns404() throws Exception {
        given(categoryService.registerSub(any(CategorySub.class))).willThrow(new CategoryMainNotFoundException(99L));

        String body = """
                {"categoryMainId":99,"name":"Interior Tile Adhesive","displayOrder":1}
                """;

        mockMvc.perform(post("/api/categories/sub")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void findSubsByMainId_returns200() throws Exception {
        given(categoryService.findSubsByMainId(1L)).willReturn(
                List.of(CategorySub.builder().id(1L).categoryMainId(1L).name("Interior Tile Adhesive").active(true).build()));

        mockMvc.perform(get("/api/categories/main/1/sub")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Interior Tile Adhesive"));
    }

    @Test
    void findAllSub_returns200() throws Exception {
        given(categoryService.findAllSub()).willReturn(List.of(
                CategorySub.builder().id(1L).categoryMainId(1L).name("Interior Tile Adhesive").active(true).build(),
                CategorySub.builder().id(2L).categoryMainId(2L).name("Wood Adhesive").active(true).build()));

        mockMvc.perform(get("/api/categories/sub")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
