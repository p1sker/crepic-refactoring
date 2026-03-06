package com.crepic.member.application;

import com.crepic.global.error.BusinessException; // ⭐️ 추가됨!
import com.crepic.global.error.ErrorCode;         // ⭐️ 추가됨!
import com.crepic.global.security.jwt.JwtTokenProvider;
import com.crepic.member.domain.Member;
import com.crepic.member.domain.MemberRepository;
import com.crepic.member.domain.MemberStatus;
import com.crepic.member.dto.MemberLoginRequest;
import com.crepic.member.dto.MemberSignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// ⭐️ S+++급 디테일 1: 클래스 레벨은 '읽기 전용'으로 덮어버립니다. (조회 성능 극대화)
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // 방금 SecurityConfig에서 만든 암호화 기계!
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입 로직
     */
    // ⭐️ S+++급 디테일 2: 데이터를 쓰거나 수정하는 메서드에만 진짜 트랜잭션을 걸어줍니다.
    @Transactional
    public Long signUp(MemberSignUpRequest request) {

        // 1. 방어 로직 (Fail-Fast): 헛수고하기 전에 입구에서 중복부터 컷!
        checkDuplicateEmail(request.email());
        checkDuplicateNickname(request.nickname());

        // 2. 무기 장착: 비밀번호 암호화 (절대 평문으로 DB에 넣지 않음)
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. 엔티티 조립 (우리가 만든 Builder 패턴 활용)
        Member newMember = Member.builder()
                .email(request.email())
                .password(encodedPassword) // 암호화된 비밀번호 삽입!
                .nickname(request.nickname())
                .build();

        // 4. 창고(DB)에 저장
        Member savedMember = memberRepository.save(newMember);

        // 5. 가입된 유저의 ID 반환 (컨트롤러가 이 번호를 받아서 201 Created 응답에 씀)
        return savedMember.getId();
    }

    // ==========================================================
    // 🛡️ 내부 검증 로직 (프라이빗 메서드로 분리하여 가독성 극대화)
    // ==========================================================

    private void checkDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            // ✅ 완벽해진 방어 로직: 쌩뚱맞은 에러 대신, 우리가 약속한 규격의 에러를 던집니다!
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }
    }

    private void checkDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            // ✅ 손으로 타이핑하다 오타 낼 일이 0%가 되었습니다!
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATION);
        }
    }

    // ==========================================================
    // ⭐️ 로그인 로직 (비밀번호 대조 및 토큰 발급)
    // ==========================================================

    // 조회가 주 목적이므로 readOnly = true (이미 클래스 레벨에 있다면 안 적어도 됩니다)
    @Transactional(readOnly = true)
    public String login(MemberLoginRequest request) {

        // 1. 회원 조회: 이메일이 우리 DB에 존재하는가?
        Member member = memberRepository.findByEmail(request.email())
                // ⭐️ 보안 완벽: 이메일이 없어도 "정보가 일치하지 않는다"고 뭉뚱그림
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 검증: 입력한 평문 비번 vs DB에 저장된 암호화 비번 대조
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            // ⭐️ 보안 완벽: 비밀번호가 틀려도 이메일이 틀렸을 때와 똑같은 에러를 던짐! (해커 혼란)
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 보안 점검: 정지당했거나 탈퇴한 회원은 아닌가?
        if (member.getStatus() != MemberStatus.ACTIVE) {
            // (상태 이상 에러 코드도 따로 만들어두시면 완벽합니다!)
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        // 4. 모든 검문을 통과했다면 AccessToken 발급!
        return jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
    }
}