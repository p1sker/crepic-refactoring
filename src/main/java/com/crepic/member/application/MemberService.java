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
import lombok.extern.slf4j.Slf4j; // ⭐️ 동시성 이슈 로깅을 위해 추가
import org.springframework.dao.DataIntegrityViolationException; // ⭐️ DB 유니크 예외 캐치용 추가
import org.springframework.data.redis.core.StringRedisTemplate; // ⭐️ 브루트포스 방어용 Redis 추가!
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit; // ⭐️ Redis TTL 시간 단위 설정을 위해 추가!

@Slf4j // ⭐️ 에러 상황을 서버 콘솔에 남기기 위해 추가
@Service
@RequiredArgsConstructor
// ⭐️ S+++급 디테일 1: 클래스 레벨은 '읽기 전용'으로 덮어버립니다. (조회 성능 극대화)
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // 방금 SecurityConfig에서 만든 암호화 기계!
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate; // ⭐️ Redis 의존성 주입 완료

    // ⭐️ S+++급 디테일 2: 계정 잠금 정책을 상수로 관리하여 유지보수성 극대화
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 15;

    /**
     * 회원가입 로직
     */
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

        // 4. 창고(DB)에 저장 + ⭐️ 2차 방어막 (동시성 제어 추가!)
        try {
            Member savedMember = memberRepository.save(newMember);

            // 5. 가입된 유저의 ID 반환 (컨트롤러가 이 번호를 받아서 201 Created 응답에 씀)
            return savedMember.getId();

        } catch (DataIntegrityViolationException e) {
            // 🚨 0.001초 차이로 1차 방어막(existsBy)을 뚫고 들어온 요청이 DB 유니크 제약조건에 막혔을 때!
            log.warn("회원가입 동시성 충돌 발생 (DB 유니크 제약조건 방어) - email: {}, nickname: {}", request.email(), request.nickname());

            // 프론트엔드에게 500 Internal Server Error 대신, 사용자가 이해할 수 있는 비즈니스 에러로 '변환'해서 던져줍니다.
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }
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
    // ⭐️ 로그인 로직 (브루트 포스 공격 완벽 방어 추가)
    // ==========================================================

    // 조회가 주 목적이므로 readOnly = true (이미 클래스 레벨에 있다면 안 적어도 됩니다)
    public String login(MemberLoginRequest request) {
        String email = request.email();
        String lockKey = "login:lock:" + email;
        String attemptKey = "login:attempts:" + email;

        // 1. 계정 잠금 여부 확인 (DB 조회도, 암호화 연산도 하지 않고 입구에서 바로 컷!)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("잠긴 계정 로그인 시도 차단 - email: {}", email);
            // (추후 ErrorCode에 ACCOUNT_LOCKED 등 403 에러 코드를 하나 파서 던지면 프론트에서 타이머를 띄워주기 좋습니다)
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 회원 조회: 이메일이 우리 DB에 존재하는가?
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 3. 비밀번호 검증: 입력한 평문 비번 vs DB에 저장된 암호화 비번 대조
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {

            // ⭐️ 해커의 공격 방어: 틀렸을 경우 Redis에 실패 횟수 1 증가
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);

            if (attempts != null && attempts == 1) {
                // 처음 틀린 순간부터 카운터의 수명을 15분으로 설정합니다.
                redisTemplate.expire(attemptKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            }

            if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
                // 5회 이상 실패 시 Lock 키(빨간딱지)를 붙이고 횟수 카운터는 날려버립니다.
                redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
                redisTemplate.delete(attemptKey);
                log.warn("비밀번호 5회 이상 실패, 계정 잠금 처리 - email: {}", email);
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

            log.info("비밀번호 실패 ({}회) - email: {}", attempts, email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 보안 점검: 정지당했거나 탈퇴한 회원은 아닌가?
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        // ⭐️ 5. 로그인 성공 시 과거의 실패 횟수 기록을 깔끔하게 지워줍니다.
        redisTemplate.delete(attemptKey);

        // 6. 모든 검문을 통과했다면 AccessToken 발급!
        return jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
    }
}