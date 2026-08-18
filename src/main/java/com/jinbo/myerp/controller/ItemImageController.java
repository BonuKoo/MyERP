package com.jinbo.myerp.controller;

import com.jinbo.myerp.controller.dto.ItemImageResponse;
import com.jinbo.myerp.domain.ItemImage;
import com.jinbo.myerp.exception.ErrorResponse;
import com.jinbo.myerp.service.ItemImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "품목 사진", description = "품목에 첨부되는 사진의 업로드/조회/삭제 및 대표 사진 지정")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/items/{itemId}/images")
@RequiredArgsConstructor
public class ItemImageController {

    private final ItemImageService itemImageService;

    @Operation(summary = "품목 사진 업로드",
            description = "여러 장을 한 번에 올릴 수 있다. 사진이 하나도 없던 품목이면 첫 장이 자동으로 대표 사진이 된다. " +
                    "jpg/jpeg/png/gif만 허용하며, 저장 시점에 목록용 썸네일도 함께 생성된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "빈 파일이거나 허용되지 않는 형식/확장자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 품목",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ItemImageResponse>> upload(@PathVariable Long itemId,
                                                          @RequestPart("files") List<MultipartFile> files) {
        List<ItemImageResponse> response = itemImageService.upload(itemId, files).stream()
                .map(ItemImageResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "품목 사진 목록 조회 (메타데이터)",
            description = "표시 순서대로 정렬된 사진 메타데이터를 반환한다. 실제 이미지는 개별 조회 API로 가져온다.")
    @GetMapping
    public ResponseEntity<List<ItemImageResponse>> findByItemId(@PathVariable Long itemId) {
        List<ItemImageResponse> response = itemImageService.findByItemId(itemId).stream()
                .map(ItemImageResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 이미지 바이너리를 내려준다.
     *
     * <p>이 엔드포인트만 SecurityConfig에서 permitAll이다. 브라우저의 {@code <img src>}는
     * Authorization 헤더를 붙일 수 없어서, 인증을 걸면 프론트의 모든 사진이 깨진다.
     * 파일명이 UUID라 URL을 알기 어렵고 내용도 제품 사진이라 공개 조회를 허용했다.
     *
     * <p>Content-Disposition은 inline이다 — 참고한 원본 코드는 다운로드 기능이라
     * attachment였지만, 그대로 쓰면 이미지가 그려지는 대신 다운로드 창이 뜬다.
     */
    @Operation(summary = "품목 사진 조회 (이미지 바이너리)",
            description = "size=thumb를 주면 목록 카드용 축소본을, 생략하면 원본을 반환한다. " +
                    "브라우저 <img> 태그가 Authorization 헤더를 보낼 수 없으므로 이 엔드포인트는 인증 없이 접근 가능하다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사진이거나 디스크에서 파일이 사라진 경우",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{imageId}")
    public ResponseEntity<Resource> download(@PathVariable Long itemId,
                                             @PathVariable Long imageId,
                                             @RequestParam(required = false) String size) {
        ItemImage image = itemImageService.findById(imageId);
        Resource resource = itemImageService.loadAsResource(imageId, "thumb".equals(size));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @Operation(summary = "품목 사진 삭제",
            description = "대표 사진을 지우면 남은 사진 중 첫 번째가 자동으로 대표가 된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사진",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable Long itemId, @PathVariable Long imageId) {
        itemImageService.delete(imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "대표 사진 지정",
            description = "품목 목록 카드에 표시될 사진을 바꾼다. 같은 품목의 기존 대표는 자동으로 해제된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "지정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사진",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<Void> setPrimary(@PathVariable Long itemId, @PathVariable Long imageId) {
        itemImageService.setPrimary(imageId);
        return ResponseEntity.noContent().build();
    }
}
