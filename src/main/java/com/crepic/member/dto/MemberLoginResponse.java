package com.crepic.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 성공 응답 객체")
public record MemberLoginResponse(
        @Schema(description = "API 접근용 Access Token (수명: 30분)", example = "eyJhbGciOi...")
        String accessToken,

        @Schema(description = "토큰 재발급용 Refresh Token (수명: 14일)", example = "eyJhbGciOi...")
        String refreshToken
) {}