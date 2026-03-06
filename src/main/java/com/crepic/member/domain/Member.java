package com.crepic.member.domain;

import com.crepic.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert; // ⭐️ Spring의 검증 도구 추가

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// DB 레벨에서의 논리적 삭제 처리
@SQLDelete(sql = "UPDATE members SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // ⭐️ S++급 디테일: BCrypt 해싱 후의 길이를 고려한 최적화 (보통 60자)
    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, unique = true, length = 10) // DTO의 맥스 사이즈와 일치시킴
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 💡 Builder의 역할을 명확히: 생성 시점에 들어갈 필수 데이터만 받음
    @Builder
    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = Role.ROLE_USER; // 초기 가입 시 무조건 USER
        this.status = MemberStatus.ACTIVE; // 초기 가입 시 무조건 ACTIVE
    }

    // ==========================================
    // ⭐️ S+++급 핵심 (Rich Domain Model + Guard Clause)
    // ==========================================

    /**
     * 회원 탈퇴 (소프트 딜리트)
     */
    public void withdraw() {
        // 1번 방어막: 이미 탈퇴한 사람인가?
        if (this.status == MemberStatus.DELETED) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        // 2번 방어막: 현재 정지 상태인가?
        // 사고 친 유저가 조사가 끝나기 전에 증거 인멸이나 계정 세탁을 위해 도망가는 것을 방지
        if (this.status == MemberStatus.BANNED) {
            throw new IllegalStateException("정지된 회원은 탈퇴할 수 없습니다. 고객센터에 문의하세요.");
        }

        // ⭐️ 3단계: 유니크 제약 조건 해제 및 DB 컬럼 길이(Overflow) 완벽 방어

        // [이메일 처리] 컬럼 제한 100자. 타임스탬프(약 17자)를 붙여서 재가입 허용
        String emailSuffix = "_del_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        if (this.email.length() + emailSuffix.length() > 100) {
            this.email = this.email.substring(0, 100 - emailSuffix.length());
        }
        this.email += emailSuffix;

        // [닉네임 처리] 컬럼 제한 10자. UUID 앞 10자리로 완전히 덮어씌움 (실무 표준 방식)
        // 기존 닉네임을 해방시켜서 다른 유저가 바로 사용할 수 있게 만듦 + 길이 에러 원천 차단
        this.nickname = java.util.UUID.randomUUID().toString().substring(0, 10);

        // 4단계: 상태 변경 및 탈퇴 시간 기록
        this.status = MemberStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 닉네임 변경 로직
     */
    public void changeNickname(String newNickname) {
        Assert.hasText(newNickname, "변경할 닉네임은 비어있을 수 없습니다.");

        // 💡 굳이 똑같은 걸로 바꿀 필요는 없으니 효율성을 위해 체크!
        if (this.nickname.equals(newNickname)) {
            return;
        }
        this.nickname = newNickname;
    }

    /**
     * 비밀번호 변경 로직
     */
    public void changePassword(String newEncodedPassword) {
        Assert.hasText(newEncodedPassword, "변경할 비밀번호는 비어있을 수 없습니다.");
        this.password = newEncodedPassword;
    }

    // ==========================================
    // 💡 완벽한 '상태 머신(State Machine)' 로직
    // ==========================================

    /**
     * 권한 변경 (예: 일반 유저 -> 관리자 승격)
     */
    public void changeRole(Role newRole) {
        Assert.notNull(newRole, "변경할 권한은 필수입니다.");
        this.role = newRole;
    }

    /**
     * 이메일 변경
     */
    public void changeEmail(String newEmail) {
        Assert.hasText(newEmail, "변경할 이메일은 비어있을 수 없습니다.");
        this.email = newEmail;
    }

    /**
     * 계정 정지 (BANNED) - 메서드명도 상태에 맞춰 ban으로 직관적으로 변경
     */
    public void ban() {
        if (this.status == MemberStatus.DELETED) {
            throw new IllegalStateException("이미 탈퇴한 회원은 정지할 수 없습니다.");
        }
        if (this.status == MemberStatus.BANNED) {
            throw new IllegalStateException("이미 정지된 회원입니다.");
        }
        this.status = MemberStatus.BANNED;
    }

    /**
     * 계정 정지 해제 (BANNED -> ACTIVE)
     */
    public void unban() {
        if (this.status != MemberStatus.BANNED) {
            throw new IllegalStateException("정지된 회원만 해제할 수 있습니다.");
        }
        this.status = MemberStatus.ACTIVE;
    }

    /**
     * 휴면 계정 전환 (ACTIVE -> INACTIVE)
     * (배치(Batch) 서버가 1년 이상 미접속자를 찾아서 이 메서드를 호출함)
     */
    public void deactivate() {
        if (this.status != MemberStatus.ACTIVE) {
            throw new IllegalStateException("활동 중인 회원만 휴면 상태로 전환할 수 있습니다.");
        }
        this.status = MemberStatus.INACTIVE;
    }

    /**
     * 휴면 계정 복구 (INACTIVE -> ACTIVE)
     * (유저가 휴면 상태에서 로그인하여 본인인증을 마쳤을 때 호출함)
     */
    public void activate() {
        if (this.status != MemberStatus.INACTIVE) {
            throw new IllegalStateException("휴면 상태인 회원만 활성화할 수 있습니다.");
        }
        this.status = MemberStatus.ACTIVE;
    }

    // ==========================================
    // ⭐️ S+++급의 핵심: JPA 엔티티 전용 Equals & HashCode
    // ==========================================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member member)) return false;
        // id가 null이 아닐 때만 비교 (영속화된 객체 기준)
        return id != null && id.equals(member.id);
    }

    @Override
    public int hashCode() {
        // 객체의 생명주기(비영속 -> 영속)와 무관하게 항상 일관된 해시코드 반환
        return getClass().hashCode();
    }

}