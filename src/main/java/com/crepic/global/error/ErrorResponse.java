package com.crepic.global.error;

public record ErrorResponse(
        int status,
        String errorCode,
        String message
) {
    // 💡 S+++급 디테일: ErrorCode만 던져주면 알아서 ErrorResponse를 만들어주는 정적 팩토리 메서드
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }

    // 💡 유효성 검사처럼 메시지를 그때그때 바꿔야 할 때 쓰는 메서드
    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return new ErrorResponse(
                errorCode.getStatus().value(),
                errorCode.getCode(),
                customMessage
        );
    }
}