package com.jinbo.myerp.file;

import com.jinbo.myerp.exception.InvalidImageFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 디스크에 쓰는 컴포넌트라 @TempDir로 테스트마다 격리된 디렉터리를 준다
 * (운영/개발 업로드 폴더를 테스트가 오염시키면 안 된다).
 */
class FileStoreTest {

    @TempDir
    Path tempDir;

    private FileStore fileStore;

    @BeforeEach
    void setUp() {
        fileStore = new FileStore();
        ReflectionTestUtils.setField(fileStore, "fileDir", tempDir.toString());
        fileStore.init();
    }

    private byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void store_savesOriginalUnderUuidName_andKeepsOriginalNameAsMetadata() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "files", "세라픽스 사진.png", "image/png", pngBytes(100, 80));

        StoredFile stored = fileStore.store(file);

        assertThat(stored.uploadFileName()).isEqualTo("세라픽스 사진.png");
        // 사용자가 준 이름을 경로에 쓰면 경로 조작·중복 위험이 있어 UUID로만 저장한다.
        assertThat(stored.storeFileName()).endsWith(".png").doesNotContain("세라픽스");
        assertThat(stored.fileType()).isEqualTo("image/png");
        assertThat(stored.fileSize()).isEqualTo(file.getSize());
        assertThat(Files.exists(tempDir.resolve(stored.storeFileName()))).isTrue();
    }

    @Test
    void store_alsoGeneratesSmallerThumbnail() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "files", "big.png", "image/png", pngBytes(1600, 1200));

        StoredFile stored = fileStore.store(file);

        Path thumbnail = fileStore.resolveThumbnail(stored.storeFileName());
        assertThat(Files.exists(thumbnail)).isTrue();

        BufferedImage thumbImage = ImageIO.read(thumbnail.toFile());
        assertThat(thumbImage.getWidth()).isEqualTo(FileStore.THUMBNAIL_MAX_EDGE);
        // 1600x1200 비율(4:3)이 유지되어야 한다.
        assertThat(thumbImage.getHeight()).isEqualTo(FileStore.THUMBNAIL_MAX_EDGE * 1200 / 1600);
    }

    @Test
    void store_whenImageSmallerThanThumbnailSize_doesNotUpscale() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "files", "small.png", "image/png", pngBytes(120, 90));

        StoredFile stored = fileStore.store(file);

        BufferedImage thumbImage = ImageIO.read(fileStore.resolveThumbnail(stored.storeFileName()).toFile());
        assertThat(thumbImage.getWidth()).isEqualTo(120);
        assertThat(thumbImage.getHeight()).isEqualTo(90);
    }

    @Test
    void store_whenFileHasNoExtension_rejects() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "files", "확장자없음", "image/png", pngBytes(10, 10));

        assertThatThrownBy(() -> fileStore.store(file))
                .isInstanceOf(InvalidImageFileException.class);
    }

    @Test
    void store_whenNotAnImageContentType_rejects() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "악성.exe", "application/octet-stream", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileStore.store(file))
                .isInstanceOf(InvalidImageFileException.class);
    }

    @Test
    void store_whenExtensionNotAllowed_rejects() {
        // Content-Type만 위조하고 확장자는 실행파일인 경우도 막아야 한다.
        MockMultipartFile file = new MockMultipartFile(
                "files", "악성.exe", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> fileStore.store(file))
                .isInstanceOf(InvalidImageFileException.class);
    }

    @Test
    void store_whenEmptyFile_rejects() {
        MockMultipartFile file = new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileStore.store(file))
                .isInstanceOf(InvalidImageFileException.class);
    }

    @Test
    void delete_removesBothOriginalAndThumbnail() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "files", "지울사진.png", "image/png", pngBytes(300, 300));
        StoredFile stored = fileStore.store(file);

        fileStore.delete(stored.storeFileName());

        assertThat(Files.exists(tempDir.resolve(stored.storeFileName()))).isFalse();
        assertThat(Files.exists(fileStore.resolveThumbnail(stored.storeFileName()))).isFalse();
    }

    @Test
    void delete_whenFileAlreadyGone_doesNotThrow() {
        // 파일이 이미 없어도 DB 정리는 계속되어야 한다(고아 레코드 방지).
        fileStore.delete("존재하지-않는-파일.png");
    }
}
