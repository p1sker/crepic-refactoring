package com.crepic.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 로그인 요청 객체")
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberLoginRequest(

        @Schema(description = "가입된 이메일 주소", example = "rhwnstj00@naver.com")
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "비밀번호 (로그인 시에는 정규식 검증을 생략하고 DB 대조만 수행)", example = "peter@7840")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
    // 1. Fail-Safe: 프론트엔드에서 묻은 공백 제거
    public MemberLoginRequest {
        if (email != null) email = email.trim();
    }

    // 2. 보안: 서버 로그에 비밀번호 평문 노출 절대 방어
    @Override
    public String toString() {
        return "MemberLoginRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}