package com.crepic.member.dto;

public record MemberLoginResponse(
        String accessToken,
        String message
) {
}