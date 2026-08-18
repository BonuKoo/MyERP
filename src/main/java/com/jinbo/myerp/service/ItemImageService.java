package com.jinbo.myerp.service;

import com.jinbo.myerp.domain.ItemImage;
import com.jinbo.myerp.exception.InvalidImageFileException;
import com.jinbo.myerp.exception.ItemImageNotFoundException;
import com.jinbo.myerp.exception.ItemNotFoundException;
import com.jinbo.myerp.file.FileStore;
import com.jinbo.myerp.file.StoredFile;
import com.jinbo.myerp.mapper.ItemImageMapper;
import com.jinbo.myerp.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemImageService {

    private final ItemImageMapper itemImageMapper;
    private final ItemMapper itemMapper;
    private final FileStore fileStore;

    /**
     * 사진을 저장한다.
     *
     * <p>디스크 쓰기를 트랜잭션 안에 넣지 않는다. 파일 I/O가 걸리는 내내 DB 커넥션과
     * 트랜잭션을 붙잡고 있으면 풀(기본 10)이 금방 마른다 — 부하 테스트에서 커넥션
     * 대기가 병목이었던 전례가 있다. 대신 "파일 먼저 저장 → 짧은 트랜잭션으로 insert"
     * 순서로 가고, insert가 실패하면 이미 쓴 파일을 지우는 보상 처리를 한다.
     */
    public List<ItemImage> upload(Long itemId, List<MultipartFile> files) {
        itemMapper.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        if (files == null || files.isEmpty()) {
            throw new InvalidImageFileException("업로드할 파일이 없습니다.");
        }

        Integer maxOrder = itemImageMapper.findMaxDisplayOrder(itemId);
        // 사진이 하나도 없던 품목이면 이번에 올리는 첫 장이 대표가 된다.
        boolean itemHasNoImageYet = (maxOrder == null);
        int nextOrder = itemHasNoImageYet ? 0 : maxOrder + 1;

        List<ItemImage> saved = new ArrayList<>();
        List<String> storedFileNames = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                StoredFile stored = fileStore.store(file);
                storedFileNames.add(stored.storeFileName());

                ItemImage image = ItemImage.builder()
                        .itemId(itemId)
                        .uploadFileName(stored.uploadFileName())
                        .storeFileName(stored.storeFileName())
                        .filePath(stored.filePath())
                        .fileType(stored.fileType())
                        .fileSize(stored.fileSize())
                        .displayOrder(nextOrder)
                        .primary(itemHasNoImageYet && nextOrder == 0)
                        .createdAt(LocalDateTime.now())
                        .build();
                itemImageMapper.insert(image);
                saved.add(image);
                nextOrder++;
            }
        } catch (IOException e) {
            deleteQuietly(storedFileNames);
            throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
        } catch (RuntimeException e) {
            deleteQuietly(storedFileNames);
            throw e;
        }

        return saved;
    }

    private void deleteQuietly(List<String> storeFileNames) {
        storeFileNames.forEach(fileStore::delete);
    }

    public List<ItemImage> findByItemId(Long itemId) {
        return itemImageMapper.findByItemId(itemId);
    }

    public List<ItemImage> findPrimaryByItemIds(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }
        return itemImageMapper.findPrimaryByItemIds(itemIds);
    }

    public ItemImage findById(Long id) {
        return itemImageMapper.findById(id).orElseThrow(() -> new ItemImageNotFoundException(id));
    }

    /**
     * 파일을 응답 본문으로 내보낼 수 있는 형태로 읽는다. ResponseEntity를 만들지 않는
     * 이유는 계층 설계 원칙(Service는 도메인만 다루고 웹 관심사는 Controller가 맡는다)
     * 때문이다 — 참고한 원본 코드는 Service가 ResponseEntity를 반환했다.
     */
    public Resource loadAsResource(Long id, boolean thumbnail) {
        ItemImage image = findById(id);
        Path path = thumbnail
                ? fileStore.resolveThumbnail(image.getStoreFileName())
                : fileStore.resolve(image.getStoreFileName());
        try {
            UrlResource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ItemImageNotFoundException(id);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ItemImageNotFoundException(id);
        }
    }

    @Transactional
    public void delete(Long id) {
        ItemImage image = findById(id);
        itemImageMapper.deleteById(id);

        // 대표를 지웠으면 목록 카드가 빈칸이 되지 않도록 남은 첫 장을 대표로 승격한다.
        if (image.isPrimary()) {
            List<ItemImage> remaining = itemImageMapper.findByItemId(image.getItemId());
            if (!remaining.isEmpty()) {
                itemImageMapper.markPrimary(remaining.get(0).getId());
            }
        }

        fileStore.delete(image.getStoreFileName());
    }

    @Transactional
    public void setPrimary(Long id) {
        ItemImage image = findById(id);
        // 먼저 비우고 나중에 지정해야 한다. 순서가 뒤집히면 방금 지정한 대표까지 지워진다.
        itemImageMapper.clearPrimary(image.getItemId());
        itemImageMapper.markPrimary(id);
    }

    @Transactional
    public void deleteByItemId(Long itemId) {
        List<ItemImage> images = itemImageMapper.findByItemId(itemId);
        itemImageMapper.deleteByItemId(itemId);
        images.forEach(image -> fileStore.delete(image.getStoreFileName()));
    }
}
