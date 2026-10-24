package com.umc.movie.repository;

import com.umc.movie.entity.Rating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Member_MemberId: Rating.member(연관관계)의 memberId로 조회한다. 최신(= id 큰) 순.
    // Pageable을 받으면 LIMIT/OFFSET이 붙는다. (선택 미션: 페이지네이션)
    List<Rating> findByMember_MemberIdOrderByRatingIdDesc(Long memberId, Pageable pageable);

    // 언더스코어(_)는 연관 엔티티를 타고 들어가라는 표시. rating.member.memberId + movieId로 존재 여부를 본다.
    boolean existsByMember_MemberIdAndMovieId(Long memberId, Long movieId);
}
