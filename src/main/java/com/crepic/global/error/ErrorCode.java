package com.crepic.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==========================================
    // 👤 회원(Member) 관련 에러
    // ==========================================
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "M001", "이미 사용 중인 이메일입니다."),
    NICKNAME_DUPLICATION(HttpStatus.CONFLICT, "M002", "이미 사용 중인 닉네임입니다."),
    MEMBER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "M003", "이미 탈퇴한 회원입니다."),
    MEMBER_BANNED(HttpStatus.FORBIDDEN, "M004", "정지된 회원입니다. 고객센터에 문의하세요."),

    // ⭐️ [추가됨] 계정 정보 불일치 (해커의 계정 유추를 막기 위해 이메일/비밀번호 에러를 하나로 통합)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M005", "이메일 또는 비밀번호가 일치하지 않습니다."),

    // ⭐️ [추가됨] 활성 상태가 아닌 계정의 로그인 시도
    INVALID_MEMBER_STATUS(HttpStatus.FORBIDDEN, "M006", "로그인할 수 없는 상태의 계정입니다."),

    // ==========================================
    // 🌐 공통(Global) 에러
    // ==========================================
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G001", "올바르지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G999", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status; // HTTP 상태 코드 (400, 404, 409 등)
    private final String code;       // 프론트엔드와 맞춘 에러 코드 (M001 등)
    private final String message;    // 사용자에게 보여줄 친절한 메시지
}