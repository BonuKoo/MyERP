package com.jinbo.myerp.file;

import com.jinbo.myerp.exception.InvalidImageFileException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드된 이미지를 디스크에 저장하고 목록용 썸네일을 함께 만든다.
 *
 * <p>목록 화면은 카드 20개를 한 번에 그리므로 원본(수 MB)을 그대로 내려주면 화면 하나에
 * 수십 MB가 오간다. 저장 시점에 한 번만 축소해두면 조회 때마다 리사이즈할 필요가 없다.
 * 축소는 표준 라이브러리(ImageIO)만으로 처리해 의존성을 늘리지 않았다 — 대신 JDK가
 * 기본 지원하는 포맷만 허용한다(webp 제외).
 */
@Component
public class FileStore {

    private static final Logger log = LoggerFactory.getLogger(FileStore.class);

    /** 썸네일의 긴 변 길이(px). 카드 폭이 220px 그리드라 400이면 고해상도 화면에서도 충분하다. */
    public static final int THUMBNAIL_MAX_EDGE = 400;

    private static final String THUMBNAIL_SUFFIX = "_thumb";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

    @Value("${file.dir}")
    private String fileDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(fileDir));
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 디렉터리를 만들 수 없습니다: " + fileDir, e);
        }
    }

    public Path resolve(String storeFileName) {
        return Paths.get(fileDir).resolve(storeFileName);
    }

    public Path resolveThumbnail(String storeFileName) {
        int pos = storeFileName.lastIndexOf('.');
        String name = storeFileName.substring(0, pos);
        String ext = storeFileName.substring(pos);
        return Paths.get(fileDir).resolve(name + THUMBNAIL_SUFFIX + ext);
    }

    public StoredFile store(MultipartFile multipartFile) throws IOException {
        validate(multipartFile);

        String originalFilename = multipartFile.getOriginalFilename();
        String ext = extractExt(originalFilename);
        String storeFileName = UUID.randomUUID() + "." + ext;
        Path target = resolve(storeFileName);

        multipartFile.transferTo(target);
        writeThumbnail(target, storeFileName, ext);

        return new StoredFile(
                originalFilename,
                storeFileName,
                target.toString(),
                multipartFile.getContentType(),
                multipartFile.getSize());
    }

    /**
     * 원본과 썸네일을 함께 지운다. 파일이 이미 없어도 예외를 던지지 않는다 —
     * 호출자는 대개 DB 레코드도 함께 정리하는 중인데, 디스크 쪽이 먼저 사라졌다고
     * 해서 고아 레코드를 남기는 게 더 나쁘다.
     */
    public void delete(String storeFileName) {
        deleteQuietly(resolve(storeFileName));
        deleteQuietly(resolveThumbnail(storeFileName));
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("파일 삭제 실패(무시하고 계속): {}", path, e);
        }
    }

    private void validate(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new InvalidImageFileException("빈 파일은 업로드할 수 없습니다.");
        }

        String contentType = multipartFile.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidImageFileException("지원하지 않는 이미지 형식입니다: " + contentType);
        }

        // Content-Type은 클라이언트가 보내는 값이라 위조될 수 있다. 확장자도 함께 검사해
        // 실행 파일이 이미지인 척 들어오는 걸 막는다.
        String ext = extractExt(multipartFile.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new InvalidImageFileException("지원하지 않는 확장자입니다: " + ext);
        }
    }

    /**
     * 확장자만 추출한다. 점이 없으면 거부한다 — 참고한 원본 코드는 lastIndexOf(".")가
     * -1일 때 substring(0)이 되어 파일명 전체가 확장자로 둔갑했다.
     */
    private String extractExt(String originalFilename) {
        if (originalFilename == null) {
            throw new InvalidImageFileException("파일명이 없습니다.");
        }
        int pos = originalFilename.lastIndexOf('.');
        if (pos < 0 || pos == originalFilename.length() - 1) {
            throw new InvalidImageFileException("확장자가 없는 파일은 업로드할 수 없습니다: " + originalFilename);
        }
        return originalFilename.substring(pos + 1).toLowerCase(Locale.ROOT);
    }

    private void writeThumbnail(Path source, String storeFileName, String ext) throws IOException {
        BufferedImage original;
        try (InputStream in = Files.newInputStream(source)) {
            original = ImageIO.read(in);
        }
        // 확장자·Content-Type을 통과했더라도 실제 내용이 이미지가 아닐 수 있다.
        if (original == null) {
            Files.deleteIfExists(source);
            throw new InvalidImageFileException("이미지로 읽을 수 없는 파일입니다.");
        }

        int width = original.getWidth();
        int height = original.getHeight();
        int longEdge = Math.max(width, height);

        int targetWidth;
        int targetHeight;
        if (longEdge <= THUMBNAIL_MAX_EDGE) {
            // 원본이 이미 작으면 확대하지 않는다(화질만 나빠지고 용량은 안 준다).
            targetWidth = width;
            targetHeight = height;
        } else {
            double ratio = (double) THUMBNAIL_MAX_EDGE / longEdge;
            targetWidth = Math.max(1, (int) Math.round(width * ratio));
            targetHeight = Math.max(1, (int) Math.round(height * ratio));
        }

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        var graphics = thumbnail.createGraphics();
        try {
            graphics.drawImage(original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH),
                    0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        ImageIO.write(thumbnail, ext.equals("jpg") ? "jpeg" : ext, resolveThumbnail(storeFileName).toFile());
    }
}
