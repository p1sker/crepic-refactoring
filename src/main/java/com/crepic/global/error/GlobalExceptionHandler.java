package com.crepic.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================================
    // 🛡️ 1. 주방장의 호통 처리 (BusinessException)
    // ==========================================================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException 발생: {}", errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    // ==========================================================
    // 📢 2. [수정] 런타임 예외 필터링 (로그 오염 방지용)
    // ==========================================================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        // 💣 쿠폰 소진 & 중복 요청은 스택 트레이스 없이 한 줄만 찍기! ⭐️
        if (e.getMessage() != null && (e.getMessage().contains("소진") || e.getMessage().contains("이미"))) {
            log.warn("📢 발급 제한: {}", e.getMessage());
            return ResponseEntity
                    .status(400) // Bad Request
                    .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage()));
        }

        // 그 외 진짜 예상치 못한 런타임 에러는 상세히 기록
        log.error("예상치 못한 RuntimeException 발생: ", e);
        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // ==========================================================
    // 🛡️ 3. 입구컷 처리 (DTO 유효성 검사 실패 시)
    // ==========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("ValidationException 발생: {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage));
    }

    // 🛡️ JSON 파싱 에러 (타입 불일치 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON 파싱 에러: {}", e.getMessage());
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "요청 데이터 형식이 올바르지 않습니다."));
    }

    // ==========================================================
    // 🚨 4. 최후의 보루 (진짜 예상치 못한 서버 에러)
    // ==========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e) {
        log.error("Unhandled Exception 발생: ", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}