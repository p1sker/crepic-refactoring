package com.crepic.member.domain;

import com.crepic.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 🚨 [수정 1] @SQLDelete 제거: 엔티티의 withdraw() 로직이 무시되는 치명적 버그 방지
@SQLRestriction("deleted_at IS NULL") // 조회 시 논리적 삭제된 데이터는 안 보이게 필터링
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, unique = true, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 🚨 [수정 2] Builder를 private으로 숨겨 무분별한 객체 생성을 막음
    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = Role.ROLE_USER;
        this.status = MemberStatus.ACTIVE;
    }

    // ⭐️ [수정 3] 정적 팩토리 메서드 도입: Image, Category 엔티티와 생성 방식 통일
    public static Member create(String email, String password, String nickname) {
        Assert.hasText(email, "이메일은 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");

        return Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();
    }

    // ==========================================
    // ⭐️ S+++급 핵심 (Rich Domain Model + Guard Clause)
    // ==========================================

    /**
     * 회원 탈퇴 (소프트 딜리트)
     */
    public void withdraw() {
        if (this.status == MemberStatus.DELETED) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }
        if (this.status == MemberStatus.BANNED) {
            throw new IllegalStateException("정지된 회원은 탈퇴할 수 없습니다. 고객센터에 문의하세요.");
        }

        // 🚨 [수정 4] 1분 취약점 해결: 시간 대신 절대 겹치지 않는 UUID를 사용하여 이메일 더미화
        String emailSuffix = "_del_" + UUID.randomUUID().toString().substring(0, 8);
        if (this.email.length() + emailSuffix.length() > 100) {
            this.email = this.email.substring(0, 100 - emailSuffix.length());
        }
        this.email += emailSuffix;

        // 닉네임 처리: UUID 앞 10자리로 완전히 덮어씌움
        this.nickname = UUID.randomUUID().toString().substring(0, 10);

        this.status = MemberStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void changeNickname(String newNickname) {
        Assert.hasText(newNickname, "변경할 닉네임은 비어있을 수 없습니다.");
        // ⭐️ DB 에러가 나기 전에 도메인에서 먼저 컷! (DB 컬럼 길이 10)
        Assert.isTrue(newNickname.length() <= 10, "닉네임은 10자를 초과할 수 없습니다.");

        if (this.nickname.equals(newNickname)) return;
        this.nickname = newNickname;
    }

    public void changePassword(String newEncodedPassword) {
        Assert.hasText(newEncodedPassword, "변경할 비밀번호는 비어있을 수 없습니다.");
        this.password = newEncodedPassword;
    }

    // ==========================================
    // 💡 완벽한 '상태 머신(State Machine)' 로직
    // ==========================================

    public void changeRole(Role newRole) {
        Assert.notNull(newRole, "변경할 권한은 필수입니다.");
        this.role = newRole;
    }

    public void changeEmail(String newEmail) {
        Assert.hasText(newEmail, "변경할 이메일은 비어있을 수 없습니다.");
        this.email = newEmail;
    }

    public void ban() {
        if (this.status == MemberStatus.DELETED) {
            throw new IllegalStateException("이미 탈퇴한 회원은 정지할 수 없습니다.");
        }
        if (this.status == MemberStatus.BANNED) {
            throw new IllegalStateException("이미 정지된 회원입니다.");
        }
        this.status = MemberStatus.BANNED;
    }

    public void unban() {
        if (this.status != MemberStatus.BANNED) {
            throw new IllegalStateException("정지된 회원만 해제할 수 있습니다.");
        }
        this.status = MemberStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status != MemberStatus.ACTIVE) {
            throw new IllegalStateException("활동 중인 회원만 휴면 상태로 전환할 수 있습니다.");
        }
        this.status = MemberStatus.INACTIVE;
    }

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
        return id != null && id.equals(member.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}