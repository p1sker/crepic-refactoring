package com.crepic.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j // ⭐️ 고성능 로깅 필수!
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================================
    // 🛡️ 1. 주방장의 호통 처리 (BusinessException)
    // ==========================================================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode(); // 프라이팬에서 이름표(Enum) 꺼내기

        log.warn("BusinessException 발생: {}", errorCode.getMessage());

        // ErrorCode의 정보를 바탕으로 예쁜 봉투(ErrorResponse)를 만들어 프론트로 전송!
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    // ==========================================================
    // 🛡️ 2. 입구컷 처리 (DTO 유효성 검사 실패 시)
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

    // ==========================================================
    // 🚨 3. 최후의 보루 (예상치 못한 서버 에러)
    // ==========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception e) {
        log.error("Unhandled Exception 발생: ", e); // ⭐️ 상세 로그는 서버 파일에만 기록!

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // 🛡️ JSON 파싱 에러 (타입 불일치 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON 파싱 에러: {}", e.getMessage());
        return ResponseEntity.status(400).body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "요청 데이터 형식이 올바르지 않습니다."));
    }
}