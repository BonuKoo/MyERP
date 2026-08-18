package com.jinbo.myerp.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 품목에 첨부된 사진 한 장.
 *
 * <p>한 품목이 여러 장을 가질 수 있고(1:N) 상세 화면에서 순서대로 넘겨 보므로
 * 컬렉션은 순서를 가진 List로 다룬다 — displayOrder가 그 순서다. 목록 카드에 쓸
 * 대표 사진은 컬렉션의 키가 아니라 각 사진의 속성이라 primary 플래그로 표현한다.
 *
 * <p>MyBatis라 JPA처럼 연관관계가 자동으로 채워지지 않는다. Item과의 조립은
 * Service 계층이 명시적으로 한다(ItemService.findCertifications와 같은 방식).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemImage {

    private Long id;
    private Long itemId;
    private String uploadFileName;
    private String storeFileName;
    private String filePath;
    private String fileType;
    private long fileSize;
    private int displayOrder;
    private boolean primary;
    private LocalDateTime createdAt;
}
