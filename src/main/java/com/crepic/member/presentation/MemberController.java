package com.crepic.member.presentation;

import com.crepic.global.error.ErrorResponse;
import com.crepic.member.application.MemberService;
import com.crepic.member.dto.MemberLoginRequest;
import com.crepic.member.dto.MemberLoginResponse;
import com.crepic.member.dto.MemberSignUpRequest;
import com.crepic.member.dto.MemberSignUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
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
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 Access Token 및 Refresh Token을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공 (JSON Body에 토큰 2개 포함)")
    // ⭐️ S+++급 디테일: 401, 403 에러 시에도 ErrorResponse 가방 모양을 보여주라고 강제 지정!
    @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치 (M005)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "정지되거나 잠긴 계정 (M006)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<MemberLoginResponse> login(@Valid @RequestBody MemberLoginRequest request) {

        // ⭐️ 완벽하게 깔끔해진 포인트: 서비스에서 2개의 토큰이 담긴 DTO를 바로 받아옵니다.
        MemberLoginResponse response = memberService.login(request);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 🚪 3. 로그아웃 API (⭐️ 신규 추가됨)
    // ==========================================
    @Operation(summary = "로그아웃", description = "현재 Access Token을 블랙리스트에 등록하고 Refresh Token을 폐기합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping(value = "/logout")
    public ResponseEntity<String> logout(
            // 1. 프론트엔드가 헤더에 실어 보낸 Access Token 추출
            @RequestHeader("Authorization") String authHeader,
            // 2. Spring Security 컨텍스트에서 현재 로그인한 유저 정보 추출 (이메일 획득용)
            @AuthenticationPrincipal UserDetails userDetails) {

        // "Bearer " 접두사 제거
        String accessToken = extractToken(authHeader);

        // Security UserDetails의 username에 이메일이 들어있다고 가정합니다.
        // 🚨 (만약 현재 시큐리티 세팅상 username에 이메일이 없다면, @RequestBody로 email을 직접 받도록 수정해도 좋습니다!)
        memberService.logout(accessToken, userDetails.getUsername());

        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }

    // ==========================================
    // 🛠️ 유틸 메서드
    // ==========================================
    private String extractToken(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("유효하지 않은 인증 헤더입니다.");
    }
}