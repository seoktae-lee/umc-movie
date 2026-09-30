package com.umc.movie.repository;

import com.umc.movie.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 메서드 이름만으로 "select count(*) > 0 ... where nickname = ?" 쿼리가 만들어진다.
    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
