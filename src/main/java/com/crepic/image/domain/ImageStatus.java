package com.crepic.image.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageStatus {
    ON_SALE("판매 중"),
    RESERVED("예약 중"),
    SOLD_OUT("판매 완료"),
    HIDDEN("숨김 처리"),
    BANNED("정지됨");

    private final String description;
}