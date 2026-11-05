package com.umc.movie.repository;

import com.umc.movie.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 메서드 이름만으로 "select count(*) > 0 ... where nickname = ?" 쿼리가 만들어진다.
    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    // "이 닉네임을 쓰는 회원이 있는가? 단, 나 자신은 빼고"를 한 번에 확인한다.
    boolean existsByNicknameAndMemberIdNot(String nickname, Long memberId);
}
