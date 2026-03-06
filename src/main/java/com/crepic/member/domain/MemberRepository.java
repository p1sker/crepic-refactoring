package com.crepic.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<엔티티, PK타입>을 상속받으면 기본적인 CRUD(저장, 조회, 수정, 삭제) 기능이 공짜로 생깁니다!
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 1. 이메일로 회원 찾기 (이미 완벽!)
    Optional<Member> findByEmail(String email);

    // 2. 이메일 중복 확인 (이미 완벽!)
    boolean existsByEmail(String email);

    // ⭐️ 3. 닉네임 중복 확인 (추가 필수!)
    // 엔티티에 nickname도 unique = true였으므로, 가입 전 반드시 체크해야 함
    boolean existsByNickname(String nickname);

    // ⭐️ 4. (심화) 이메일로 찾을 때 '정지된 회원'이나 '휴면 회원'까지 고려해야 할까?
    // 기본 findByEmail은 @SQLRestriction("deleted_at IS NULL") 덕분에 탈퇴 안 한 사람만 찾아줍니다.
    // 만약 "탈퇴한 사람을 포함해서" 찾아야 하는 특수한 기획이 있다면 따로 쿼리를 짜야 하지만,
    // 지금은 이 정도로 충분히 강력합니다.
}