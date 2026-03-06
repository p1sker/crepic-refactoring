package com.crepic.member.application;

import com.crepic.global.error.BusinessException;
import com.crepic.global.error.ErrorCode;
import com.crepic.global.security.jwt.JwtTokenProvider;
import com.crepic.member.domain.Member;
import com.crepic.member.domain.MemberRepository;
import com.crepic.member.domain.MemberStatus;
import com.crepic.member.dto.MemberLoginRequest;
import com.crepic.member.dto.MemberLoginResponse; // ⭐️ 추가됨! (DTO 반환용)
import com.crepic.member.dto.MemberSignUpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
// ⭐️ S+++급 디테일 1: 클래스 레벨은 '읽기 전용'으로 덮어버립니다. (조회 성능 극대화)
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

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

        // 3. 엔티티 조립 (정적 팩토리 메서드 create 활용하여 안전하게 생성)
        Member newMember = Member.create(
                request.email(),
                encodedPassword,
                request.nickname()
        );

        // 4. 창고(DB)에 저장 + ⭐️ 2차 방어막 (동시성 제어 추가!)
        try {
            Member savedMember = memberRepository.save(newMember);
            return savedMember.getId();

        } catch (DataIntegrityViolationException e) {
            log.warn("회원가입 동시성 충돌 발생 (DB 유니크 제약조건 방어) - email: {}, nickname: {}", request.email(), request.nickname());
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }
    }

    // ==========================================================
    // 🛡️ 내부 검증 로직 (프라이빗 메서드로 분리하여 가독성 극대화)
    // ==========================================================

    private void checkDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }
    }

    private void checkDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATION);
        }
    }

    // ==========================================================
    // ⭐️ 로그인 로직 (브루트 포스 방어 + Refresh Token 적용)
    // ==========================================================
    public MemberLoginResponse login(MemberLoginRequest request) {
        String email = request.email();
        String lockKey = "login:lock:" + email;
        String attemptKey = "login:attempts:" + email;

        // 1. 계정 잠금 여부 확인 (DB 조회도, 암호화 연산도 하지 않고 입구에서 바로 컷!)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("잠긴 계정 로그인 시도 차단 - email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 회원 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);

            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
            }

            if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_TIME_MINUTES, TimeUnit.MINUTES);
                redisTemplate.delete(attemptKey);
                log.warn("비밀번호 5회 이상 실패, 계정 잠금 처리 - email: {}", email);
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

            log.info("비밀번호 실패 ({}회) - email: {}", attempts, email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 보안 점검
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_STATUS);
        }

        // 5. 로그인 성공 시 처리
        redisTemplate.delete(attemptKey);

        // 6. 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // 7. Refresh Token을 Redis에 저장
        redisTemplate.opsForValue().set(
                "jwt:refresh:" + member.getEmail(),
                refreshToken,
                14,
                TimeUnit.DAYS
        );

        return new MemberLoginResponse(accessToken, refreshToken);
    }

    // ==========================================================
    // 🔄 토큰 재발급 로직 (Refresh Token 교환소)
    // ==========================================================
    public MemberLoginResponse reissue(String refreshToken) {

        // 1. Refresh Token 자체의 유효성(위변조, 만료 여부) 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token으로 재발급 시도");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 토큰에서 유저 ID 추출 및 회원 조회
        Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 3. Redis에 저장된 Refresh Token과 일치하는지 검증 (로그아웃된 토큰인지 확인)
        String redisRefreshToken = redisTemplate.opsForValue().get("jwt:refresh:" + member.getEmail());
        if (redisRefreshToken == null || !redisRefreshToken.equals(refreshToken)) {
            log.warn("탈취되거나 이미 로그아웃된 Refresh Token 사용 시도 - email: {}", member.getEmail());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. ⭐️ RTR(Refresh Token Rotation) 적용: Access와 Refresh 둘 다 새로 발급!
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // 5. Redis의 Refresh Token 갱신
        redisTemplate.opsForValue().set(
                "jwt:refresh:" + member.getEmail(),
                newRefreshToken,
                14,
                TimeUnit.DAYS
        );

        log.info("JWT 토큰 재발급 완료 - email: {}", member.getEmail());

        return new MemberLoginResponse(newAccessToken, newRefreshToken);
    }

    // ==========================================================
    // ⭐️ 로그아웃 로직 (블랙리스트 처리)
    // ==========================================================
    @Transactional
    public void logout(String accessToken, String email) {

        if (Boolean.TRUE.equals(redisTemplate.hasKey("jwt:refresh:" + email))) {
            redisTemplate.delete("jwt:refresh:" + email);
        }

        Long expiration = jwtTokenProvider.getExpiration(accessToken);

        redisTemplate.opsForValue().set(
                "jwt:blacklist:" + accessToken,
                "logout",
                expiration,
                TimeUnit.MILLISECONDS
        );

        log.info("로그아웃 성공 및 블랙리스트 등록 완료 - email: {}", email);
    }
}