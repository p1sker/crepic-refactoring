package com.crepic.member.presentation;

import com.crepic.global.error.ErrorResponse; // ⭐️ 에러 응답 가방 임포트!
import com.crepic.member.application.MemberService;
import com.crepic.member.dto.MemberLoginRequest;
import com.crepic.member.dto.MemberLoginResponse;
import com.crepic.member.dto.MemberSignUpRequest;
import com.crepic.member.dto.MemberSignUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content; // ⭐️ 추가
import io.swagger.v3.oas.annotations.media.Schema;   // ⭐️ 추가
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 관련 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // ==========================================
    // 📝 1. 회원가입 API
    // ==========================================
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다. 성공 시 생성된 회원 ID를 포함한 JSON 데이터를 반환합니다.")
    @ApiResponse(responseCode = "201", description = "가입 성공 (JSON Body 반환)")
    // ⭐️ S+++급 디테일: 400 에러 시 ErrorResponse 가방 모양을 보여주라고 강제 지정!
    @ApiResponse(responseCode = "400", description = "중복된 이메일/닉네임 또는 유효하지 않은 입력값",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(produces = "application/json")
    public ResponseEntity<MemberSignUpResponse> signUp(@Valid @RequestBody MemberSignUpRequest request) {

        Long memberId = memberService.signUp(request);

        MemberSignUpResponse responseBody = new MemberSignUpResponse(
                memberId,
                "회원가입이 완료되었습니다."
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    // ==========================================
    // 🔑 2. 로그인 API
    // ==========================================
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT Access Token을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 (JSON Body에 토큰 포함)")
    // ⭐️ S+++급 디테일: 401, 403 에러 시에도 ErrorResponse 가방 모양을 보여주라고 강제 지정!
    @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치 (M005)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "정지되거나 탈퇴한 계정 (M006)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<MemberLoginResponse> login(@Valid @RequestBody MemberLoginRequest request) {

        String token = memberService.login(request);

        MemberLoginResponse responseBody = new MemberLoginResponse(
                token,
                "로그인에 성공했습니다."
        );

        return ResponseEntity.ok(responseBody);
    }
}