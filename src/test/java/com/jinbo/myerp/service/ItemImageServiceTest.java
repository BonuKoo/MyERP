package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.Item;
import com.jinbo.myerp.domain.ItemImage;
import com.jinbo.myerp.exception.ItemImageNotFoundException;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.file.FileStore;
import com.jinbo.myerp.file.StoredFile;
import com.jinbo.myerp.mapper.ItemImageMapper;
import com.jinbo.myerp.mapper.ItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemImageServiceTest {

    @Mock
    private ItemImageMapper itemImageMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private FileStore fileStore;

    @InjectMocks
    private ItemImageService itemImageService;

    private MultipartFile file(String name) {
        return new MockMultipartFile("files", name, "image/png", new byte[]{1, 2, 3});
    }

    private StoredFile storedFile(String uploadName) {
        return new StoredFile(uploadName, "uuid-" + uploadName, "/uploads/uuid-" + uploadName, "image/png", 3L);
    }

    @Test
    void upload_whenItemMissing_throwsAndDoesNotTouchDisk() throws IOException {
        given(itemMapper.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemImageService.upload(99L, List.of(file("a.png"))))
                .isInstanceOf(ItemNotFoundException.class);

        verify(fileStore, never()).store(any());
    }

    @Test
    void upload_firstImageOfItem_becomesPrimary() throws IOException {
        given(itemMapper.findById(1L)).willReturn(Optional.of(Item.builder().id(1L).build()));
        given(itemImageMapper.findMaxDisplayOrder(1L)).willReturn(null);
        given(fileStore.store(any())).willReturn(storedFile("first.png"));

        itemImageService.upload(1L, List.of(file("first.png")));

        ArgumentCaptor<ItemImage> captor = ArgumentCaptor.forClass(ItemImage.class);
        verify(itemImageMapper).insert(captor.capture());
        ItemImage saved = captor.getValue();
        assertThat(saved.isPrimary()).isTrue();
        assertThat(saved.getDisplayOrder()).isZero();
        assertThat(saved.getItemId()).isEqualTo(1L);
        assertThat(saved.getStoreFileName()).isEqualTo("uuid-first.png");
    }

    @Test
    void upload_whenItemAlreadyHasImages_doesNotStealPrimary() throws IOException {
        given(itemMapper.findById(1L)).willReturn(Optional.of(Item.builder().id(1L).build()));
        given(itemImageMapper.findMaxDisplayOrder(1L)).willReturn(2);
        given(fileStore.store(any())).willReturn(storedFile("another.png"));

        itemImageService.upload(1L, List.of(file("another.png")));

        ArgumentCaptor<ItemImage> captor = ArgumentCaptor.forClass(ItemImage.class);
        verify(itemImageMapper).insert(captor.capture());
        assertThat(captor.getValue().isPrimary()).isFalse();
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void upload_multipleFiles_getSequentialDisplayOrders_andOnlyFirstIsPrimary() throws IOException {
        given(itemMapper.findById(1L)).willReturn(Optional.of(Item.builder().id(1L).build()));
        given(itemImageMapper.findMaxDisplayOrder(1L)).willReturn(null);
        given(fileStore.store(any())).willReturn(storedFile("a.png"), storedFile("b.png"));

        itemImageService.upload(1L, List.of(file("a.png"), file("b.png")));

        ArgumentCaptor<ItemImage> captor = ArgumentCaptor.forClass(ItemImage.class);
        verify(itemImageMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<ItemImage> saved = captor.getAllValues();
        assertThat(saved).extracting(ItemImage::getDisplayOrder).containsExactly(0, 1);
        assertThat(saved).extracting(ItemImage::isPrimary).containsExactly(true, false);
    }

    /**
     * 디스크에 파일을 쓴 뒤 DB insert가 실패하면 아무도 참조하지 않는 파일이 남는다.
     * 그런 고아 파일이 쌓이면 디스크만 먹고 지울 근거도 사라지므로 보상 삭제가 필요하다.
     */
    @Test
    void upload_whenDbInsertFails_deletesAlreadyStoredFile() throws IOException {
        given(itemMapper.findById(1L)).willReturn(Optional.of(Item.builder().id(1L).build()));
        given(itemImageMapper.findMaxDisplayOrder(1L)).willReturn(null);
        given(fileStore.store(any())).willReturn(storedFile("a.png"));
        willThrow(new RuntimeException("DB 장애")).given(itemImageMapper).insert(any());

        assertThatThrownBy(() -> itemImageService.upload(1L, List.of(file("a.png"))))
                .isInstanceOf(RuntimeException.class);

        verify(fileStore).delete("uuid-a.png");
    }

    @Test
    void findByItemId_delegatesToMapper() {
        List<ItemImage> images = List.of(ItemImage.builder().id(1L).build());
        given(itemImageMapper.findByItemId(1L)).willReturn(images);

        assertThat(itemImageService.findByItemId(1L)).isEqualTo(images);
    }

    @Test
    void findPrimaryByItemIds_whenNoItems_returnsEmptyWithoutQuerying() {
        assertThat(itemImageService.findPrimaryByItemIds(List.of())).isEmpty();

        verify(itemImageMapper, never()).findPrimaryByItemIds(any());
    }

    @Test
    void delete_removesRowAndFile() {
        ItemImage image = ItemImage.builder().id(5L).itemId(1L).storeFileName("uuid-a.png").build();
        given(itemImageMapper.findById(5L)).willReturn(Optional.of(image));

        itemImageService.delete(5L);

        verify(itemImageMapper).deleteById(5L);
        verify(fileStore).delete("uuid-a.png");
    }

    @Test
    void delete_whenImageMissing_throws() {
        given(itemImageMapper.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> itemImageService.delete(5L))
                .isInstanceOf(ItemImageNotFoundException.class);
    }

    /**
     * 대표를 지운 경우 그 품목엔 대표 사진이 없어진다. 목록 카드가 빈칸이 되지 않도록
     * 남은 사진 중 첫 번째를 자동으로 승격시킨다.
     */
    @Test
    void delete_whenPrimaryDeleted_promotesNextImageToPrimary() {
        ItemImage primary = ItemImage.builder().id(5L).itemId(1L).storeFileName("uuid-a.png").primary(true).build();
        ItemImage remaining = ItemImage.builder().id(6L).itemId(1L).storeFileName("uuid-b.png").build();
        given(itemImageMapper.findById(5L)).willReturn(Optional.of(primary));
        given(itemImageMapper.findByItemId(1L)).willReturn(List.of(remaining));

        itemImageService.delete(5L);

        verify(itemImageMapper).markPrimary(6L);
    }

    @Test
    void delete_whenPrimaryDeletedAndNoImagesLeft_doesNotPromote() {
        ItemImage primary = ItemImage.builder().id(5L).itemId(1L).storeFileName("uuid-a.png").primary(true).build();
        given(itemImageMapper.findById(5L)).willReturn(Optional.of(primary));
        given(itemImageMapper.findByItemId(1L)).willReturn(List.of());

        itemImageService.delete(5L);

        verify(itemImageMapper, never()).markPrimary(any());
    }

    @Test
    void setPrimary_clearsOthersThenMarksTarget() {
        ItemImage image = ItemImage.builder().id(6L).itemId(1L).build();
        given(itemImageMapper.findById(6L)).willReturn(Optional.of(image));

        itemImageService.setPrimary(6L);

        // 순서가 중요하다 — 먼저 지정하고 나중에 전체를 지우면 대표가 사라진다.
        var inOrder = org.mockito.Mockito.inOrder(itemImageMapper);
        inOrder.verify(itemImageMapper).clearPrimary(1L);
        inOrder.verify(itemImageMapper).markPrimary(6L);
    }
}
