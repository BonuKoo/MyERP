package com.jinbo.myerp.file;

/**
 * 디스크에 저장이 끝난 파일의 메타데이터.
 *
 * <p>참고한 원본 코드(myboard)의 FileStore는 도메인 엔티티(UploadFileOfBoard)를 직접
 * 반환했지만, 여기서는 인프라 계층이 특정 도메인에 묶이지 않도록 이 중립적인 record를
 * 반환한다. 도메인 객체(ItemImage)로의 변환은 Service가 담당한다 — 나중에 품목 외의
 * 다른 대상에도 파일을 붙이게 되면 FileStore를 그대로 재사용할 수 있다.
 */
public record StoredFile(
        String uploadFileName,
        String storeFileName,
        String filePath,
        String fileType,
        long fileSize
) {
}
