package com.crepic.global.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // RuntimeException의 기본 메시지도 세팅
        this.errorCode = errorCode;
    }
}