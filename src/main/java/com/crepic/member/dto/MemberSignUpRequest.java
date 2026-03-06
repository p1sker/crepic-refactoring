package com.crepic.member.dto;

import com.crepic.global.annotation.Nickname;
import com.crepic.global.annotation.Password;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 가입 요청 객체")
// ⭐️ 마지막 0.1% 디테일: 모르는 데이터가 들어와도 에러 내지 말고 무시해라!
@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberSignUpRequest(

        @NotBlank(message = "{email.notblank}")
        @Email(message = "{email.format}")
        String email,

        @Password
        String password,

        @Nickname
        String nickname
) {
        // 공백 제거 정제 로직
        public MemberSignUpRequest {
                if (email != null) email = email.trim();
                if (nickname != null) nickname = nickname.trim();
        }

        // 비밀번호 로그 노출 철통 방어
        @Override
        public String toString() {
                return "MemberSignUpRequest{" +
                        "email='" + email + '\'' +
                        ", password='[PROTECTED]'" +
                        ", nickname='" + nickname + '\'' +
                        '}';
        }
}