package com.jinbo.myerp.controller;

import com.jinbo.myerp.domain.ItemImage;
import com.jinbo.myerp.exception.InvalidImageFileException;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.security.JwtAuthenticationFilter;
import com.jinbo.myerp.security.JwtTokenProvider;
import com.jinbo.myerp.security.SecurityConfig;
import com.jinbo.myerp.service.ItemImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemImageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class ItemImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ItemImageService itemImageService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.createToken(1L, "owner@myerp.com", "OWNER");
    }

    private MockMultipartFile pngPart(String filename) {
        return new MockMultipartFile("files", filename, "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void upload_returns201WithSavedImages() throws Exception {
        ItemImage saved = ItemImage.builder().id(10L).itemId(1L)
                .uploadFileName("front.png").storeFileName("uuid.png")
                .fileType("image/png").fileSize(3L).displayOrder(0).primary(true).build();
        given(itemImageService.upload(eq(1L), any())).willReturn(List.of(saved));

        mockMvc.perform(multipart("/api/items/1/images")
                        .file(pngPart("front.png"))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].uploadFileName").value("front.png"))
                .andExpect(jsonPath("$[0].primary").value(true));
    }

    @Test
    void upload_whenItemMissing_returns404() throws Exception {
        willThrow(new ItemNotFoundException(99L)).given(itemImageService).upload(eq(99L), any());

        mockMvc.perform(multipart("/api/items/99/images")
                        .file(pngPart("front.png"))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNotFound());
    }

    /**
     * 커스텀 예외를 만들어놓고 GlobalExceptionHandler에 등록하지 않으면 Exception.class
     * catch-all에 잡혀 500이 된다 — 사용자 입력 문제가 서버 결함으로 둔갑한다.
     * 실제 서버에 curl로 잘못된 파일을 올렸다가 발견해서 추가한 테스트다.
     */
    @Test
    void upload_whenFileRejected_returns400NotServerError() throws Exception {
        willThrow(new InvalidImageFileException("지원하지 않는 확장자입니다: exe"))
                .given(itemImageService).upload(eq(1L), any());

        mockMvc.perform(multipart("/api/items/1/images")
                        .file(new MockMultipartFile("files", "악성.exe", "image/png", new byte[]{1}))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void upload_withoutToken_returns401() throws Exception {
        mockMvc.perform(multipart("/api/items/1/images").file(pngPart("front.png")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findByItemId_returnsMetadataList() throws Exception {
        given(itemImageService.findByItemId(1L)).willReturn(List.of(
                ItemImage.builder().id(10L).itemId(1L).uploadFileName("a.png").displayOrder(0).primary(true).build()));

        mockMvc.perform(get("/api/items/1/images")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uploadFileName").value("a.png"));
    }

    /**
     * 메타 목록은 바이너리와 달리 인증을 유지해야 한다. SecurityConfig의 permitAll
     * 패턴이 이 경로까지 열어버리면 안 된다는 걸 못박아두는 테스트다.
     */
    @Test
    void findByItemId_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/items/1/images"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_returnsBinaryInlineWithContentType() throws Exception {
        given(itemImageService.findById(10L)).willReturn(
                ItemImage.builder().id(10L).itemId(1L).uploadFileName("front.png").fileType("image/png").build());
        given(itemImageService.loadAsResource(10L, false)).willReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/items/1/images/10")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                // attachment면 브라우저가 다운로드 창을 띄운다. <img>로 그리려면 inline이어야 한다.
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline")));
    }

    /**
     * <img src>는 Authorization 헤더를 붙일 수 없다. 바이너리 조회만 permitAll로
     * 열어두지 않으면 프론트에서 사진이 전부 깨진다 — 이 기능의 핵심 전제다.
     */
    @Test
    void download_withoutToken_returns200() throws Exception {
        given(itemImageService.findById(10L)).willReturn(
                ItemImage.builder().id(10L).itemId(1L).fileType("image/png").build());
        given(itemImageService.loadAsResource(10L, false)).willReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/items/1/images/10"))
                .andExpect(status().isOk());
    }

    @Test
    void download_withThumbSizeParam_loadsThumbnail() throws Exception {
        given(itemImageService.findById(10L)).willReturn(
                ItemImage.builder().id(10L).itemId(1L).fileType("image/png").build());
        given(itemImageService.loadAsResource(10L, true)).willReturn(new ByteArrayResource(new byte[]{1}));

        mockMvc.perform(get("/api/items/1/images/10").param("size", "thumb"))
                .andExpect(status().isOk());

        verify(itemImageService).loadAsResource(10L, true);
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/items/1/images/10")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent());

        verify(itemImageService).delete(10L);
    }

    /**
     * 삭제는 permitAll 대상이 아니다. GET만 여는 설정이 실제로 메서드 단위로
     * 동작하는지 확인한다.
     */
    @Test
    void delete_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/items/1/images/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setPrimary_returns204() throws Exception {
        mockMvc.perform(patch("/api/items/1/images/10/primary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken))
                .andExpect(status().isNoContent());

        verify(itemImageService).setPrimary(10L);
    }

    @Test
    void setPrimary_withoutToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/items/1/images/10/primary"))
                .andExpect(status().isUnauthorized());
    }
}
