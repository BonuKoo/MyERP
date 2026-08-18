package com.jinbo.myerp.mapper;

import com.jinbo.myerp.domain.CategoryMain;
import com.jinbo.myerp.domain.CategorySub;
import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemImage;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MybatisTest
class ItemImageMapperTest {

    @Autowired
    private CategoryMainMapper categoryMainMapper;

    @Autowired
    private CategorySubMapper categorySubMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ItemImageMapper itemImageMapper;

    private Long insertItem(String name) {
        CategoryMain main = CategoryMain.builder().name("대분류-" + name).displayOrder(1).active(true).build();
        categoryMainMapper.insert(main);
        CategorySub sub = CategorySub.builder().categoryMainId(main.getId()).name("중분류-" + name).displayOrder(1).active(true).build();
        categorySubMapper.insert(sub);
        LocalDateTime now = LocalDateTime.now();
        Item item = Item.builder().categorySubId(sub.getId()).name(name).active(true).createdAt(now).updatedAt(now).build();
        itemMapper.insert(item);
        return item.getId();
    }

    private ItemImage image(Long itemId, String uploadName, int displayOrder, boolean primary) {
        return ItemImage.builder()
                .itemId(itemId)
                .uploadFileName(uploadName)
                .storeFileName("uuid-" + uploadName)
                .filePath("/uploads/uuid-" + uploadName)
                .fileType("image/png")
                .fileSize(1024L)
                .displayOrder(displayOrder)
                .primary(primary)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void insertAndFindById() {
        Long itemId = insertItem("세라픽스 PC-7000D");
        ItemImage saved = image(itemId, "front.png", 0, true);

        itemImageMapper.insert(saved);

        assertThat(saved.getId()).isNotNull();
        Optional<ItemImage> found = itemImageMapper.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUploadFileName()).isEqualTo("front.png");
        assertThat(found.get().getStoreFileName()).isEqualTo("uuid-front.png");
        assertThat(found.get().getFileType()).isEqualTo("image/png");
        assertThat(found.get().getFileSize()).isEqualTo(1024L);
        // boolean 컬럼은 명시적 resultMap이 없으면 매핑되지 않는다(계층 설계 원칙 1).
        assertThat(found.get().isPrimary()).isTrue();
    }

    @Test
    void findByItemId_returnsImagesOrderedByDisplayOrder() {
        Long itemId = insertItem("세라픽스 PC-8000P");
        itemImageMapper.insert(image(itemId, "third.png", 2, false));
        itemImageMapper.insert(image(itemId, "first.png", 0, true));
        itemImageMapper.insert(image(itemId, "second.png", 1, false));

        List<ItemImage> images = itemImageMapper.findByItemId(itemId);

        assertThat(images).extracting(ItemImage::getUploadFileName)
                .containsExactly("first.png", "second.png", "third.png");
    }

    @Test
    void findByItemId_whenNoImages_returnsEmptyList() {
        Long itemId = insertItem("사진없는품목");

        assertThat(itemImageMapper.findByItemId(itemId)).isEmpty();
    }

    /**
     * 목록 화면은 품목마다 대표 사진 1장만 필요하다. 품목별로 따로 조회하면 20건 목록이
     * 20쿼리가 되므로 한 번에 가져올 수 있어야 한다.
     */
    @Test
    void findPrimaryByItemIds_returnsOnlyPrimaryOfEachItem() {
        Long itemA = insertItem("품목A");
        Long itemB = insertItem("품목B");
        Long itemC = insertItem("품목C-사진없음");

        itemImageMapper.insert(image(itemA, "a-primary.png", 0, true));
        itemImageMapper.insert(image(itemA, "a-second.png", 1, false));
        itemImageMapper.insert(image(itemB, "b-primary.png", 0, true));

        List<ItemImage> primaries = itemImageMapper.findPrimaryByItemIds(List.of(itemA, itemB, itemC));

        assertThat(primaries).hasSize(2);
        assertThat(primaries).extracting(ItemImage::getUploadFileName)
                .containsExactlyInAnyOrder("a-primary.png", "b-primary.png");
    }

    @Test
    void clearPrimary_unsetsPrimaryFlagOfThatItemOnly() {
        Long itemA = insertItem("품목A");
        Long itemB = insertItem("품목B");
        ItemImage aPrimary = image(itemA, "a.png", 0, true);
        ItemImage bPrimary = image(itemB, "b.png", 0, true);
        itemImageMapper.insert(aPrimary);
        itemImageMapper.insert(bPrimary);

        itemImageMapper.clearPrimary(itemA);

        assertThat(itemImageMapper.findById(aPrimary.getId()).orElseThrow().isPrimary()).isFalse();
        assertThat(itemImageMapper.findById(bPrimary.getId()).orElseThrow().isPrimary()).isTrue();
    }

    @Test
    void markPrimary_setsFlagOnGivenImage() {
        Long itemId = insertItem("품목A");
        ItemImage second = image(itemId, "second.png", 1, false);
        itemImageMapper.insert(second);

        itemImageMapper.markPrimary(second.getId());

        assertThat(itemImageMapper.findById(second.getId()).orElseThrow().isPrimary()).isTrue();
    }

    @Test
    void deleteById_removesOnlyThatImage() {
        Long itemId = insertItem("품목A");
        ItemImage first = image(itemId, "first.png", 0, true);
        ItemImage second = image(itemId, "second.png", 1, false);
        itemImageMapper.insert(first);
        itemImageMapper.insert(second);

        itemImageMapper.deleteById(first.getId());

        assertThat(itemImageMapper.findById(first.getId())).isEmpty();
        assertThat(itemImageMapper.findByItemId(itemId)).hasSize(1);
    }

    @Test
    void deleteByItemId_removesAllImagesOfItem() {
        Long itemId = insertItem("품목A");
        itemImageMapper.insert(image(itemId, "first.png", 0, true));
        itemImageMapper.insert(image(itemId, "second.png", 1, false));

        itemImageMapper.deleteByItemId(itemId);

        assertThat(itemImageMapper.findByItemId(itemId)).isEmpty();
    }

    @Test
    void findMaxDisplayOrder_whenNoImages_returnsNull() {
        Long itemId = insertItem("사진없는품목");

        assertThat(itemImageMapper.findMaxDisplayOrder(itemId)).isNull();
    }

    @Test
    void findMaxDisplayOrder_returnsHighestOrder() {
        Long itemId = insertItem("품목A");
        itemImageMapper.insert(image(itemId, "a.png", 0, true));
        itemImageMapper.insert(image(itemId, "b.png", 5, false));

        assertThat(itemImageMapper.findMaxDisplayOrder(itemId)).isEqualTo(5);
    }
}
