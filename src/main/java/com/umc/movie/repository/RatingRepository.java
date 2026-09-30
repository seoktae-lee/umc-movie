package com.umc.movie.repository;

import com.umc.movie.entity.Rating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Member_MemberId: Rating.member(연관관계)의 memberId로 조회한다. 최신(= id 큰) 순.
    // Pageable을 받으면 LIMIT/OFFSET이 붙는다. (선택 미션: 페이지네이션)
    List<Rating> findByMember_MemberIdOrderByRatingIdDesc(Long memberId, Pageable pageable);
}
