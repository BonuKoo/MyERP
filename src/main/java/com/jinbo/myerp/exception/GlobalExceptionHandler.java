package com.jinbo.myerp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(CertificationAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCertificationAlreadyExists(CertificationAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(DepartmentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentAlreadyExists(DepartmentAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(PositionAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePositionAlreadyExists(PositionAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(EmployeeAlreadyLinkedException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeAlreadyLinked(EmployeeAlreadyLinkedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, e.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockConflict(OptimisticLockConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(InvalidLeaveRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLeaveRequest(InvalidLeaveRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler({
            PartnerNotFoundException.class,
            CompanyUserNotFoundException.class,
            CategoryMainNotFoundException.class,
            CategorySubNotFoundException.class,
            ItemNotFoundException.class,
            ItemImageNotFoundException.class,
            ItemSpecNotFoundException.class,
            CompanyInfoNotFoundException.class,
            PurchaseNotFoundException.class,
            SaleNotFoundException.class,
            PaymentNotFoundException.class,
            DepartmentNotFoundException.class,
            PositionNotFoundException.class,
            EmployeeNotFoundException.class,
            LeaveRequestNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    /**
     * 업로드 파일이 비었거나 허용되지 않는 형식/확장자인 경우. 사용자 입력 문제이므로
     * 400이다 — 이 핸들러가 없으면 아래 Exception.class catch-all에 잡혀 500이 되고,
     * 클라이언트가 "내 파일이 문제"인지 "서버가 고장"인지 구분할 수 없게 된다.
     */
    @ExceptionHandler(InvalidImageFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImageFile(InvalidImageFileException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    /**
     * application.yml의 multipart 크기 제한을 넘긴 업로드. 스프링이 서블릿 레벨에서
     * 던지므로 컨트롤러에 도달하지 못한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 파일 크기를 초과했습니다."));
    }

    /**
     * 요청 본문 자체를 읽을 수 없는 경우(깨진 JSON, 잘못된 인코딩, 필드 타입 불일치).
     * 클라이언트가 보낸 값의 문제이므로 400이다. 이 핸들러가 없으면 Exception.class
     * catch-all에 걸려 500이 나가고, 5xx를 서버 결함 지표로 쓰는 전제가 깨진다.
     *
     * <p>파싱 실패 원인은 내부 구현(Jackson) 메시지라 그대로 노출하지 않는다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("읽을 수 없는 요청 본문: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message));
    }

    /**
     * 권한 부족(403). SecurityConfig의 URL 패턴 단계에서 거부된 경우는 필터에서
     * accessDeniedHandler가 처리하므로 여기 오지 않는다. 이 핸들러가 필요한 건
     * 컨트롤러/서비스가 직접 던진 AccessDeniedException(예: 회원가입 권한 검사)이다 —
     * 그건 DispatcherServlet 안에서 발생하므로 아래 Exception.class catch-all이
     * 먼저 삼켜서 403이 500으로 둔갑한다. 더 구체적인 타입이라 catch-all보다 먼저 매칭된다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, e.getMessage()));
    }

    /**
     * 매핑되지 않은 URL은 이 예외로 도달한다. 아래 Exception.class catch-all이
     * 이것까지 잡으면 클라이언트의 URL 오타/오호출이 서버 결함(500)으로 둔갑해
     * 원인 파악을 방해하고, k6/모니터링에서 5xx를 서버 결함 지표로 쓰는 전제도
     * 깨진다. 더 구체적인 타입이라 Exception.class보다 먼저 매칭된다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."));
    }
}
