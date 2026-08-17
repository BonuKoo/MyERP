package com.jinbo.myerp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

    @ExceptionHandler({
            PartnerNotFoundException.class,
            CompanyUserNotFoundException.class,
            CategoryMainNotFoundException.class,
            CategorySubNotFoundException.class,
            ItemNotFoundException.class,
            ItemSpecNotFoundException.class,
            CompanyInfoNotFoundException.class,
            PurchaseNotFoundException.class,
            SaleNotFoundException.class,
            PaymentNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(HttpStatus.NOT_FOUND, e.getMessage()));
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
