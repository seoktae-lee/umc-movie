package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.exception.GeneralException;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsByMember(Long memberId, int page, int size) {
        // 평점이 0개인 유저(빈 배열)와 없는 유저(404)를 구분한다.
        if (!memberRepository.existsById(memberId)) {
            throw new GeneralException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return ratingRepository.findByMember_MemberIdOrderByRatingIdDesc(memberId, PageRequest.of(page, size))
                .stream()
                .map(RatingResponse::from)
                .toList();
    }

    /** 평점 등록. 없는 회원이면 404, 같은 영화에 이미 남겼으면 409. */
    @Transactional
    public Long create(Long memberId, CreateRatingRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorCode.MEMBER_NOT_FOUND));

        // rating 테이블에는 (member_id, movie_id) 복합 UNIQUE가 걸려 있어 DB도 막아 준다.
        // 우리가 정한 문구로 응답하려고 먼저 확인한다.
        if (ratingRepository.existsByMember_MemberIdAndMovieId(memberId, request.movieId())) {
            throw new GeneralException(ErrorCode.RATING_DUPLICATED);
        }

        Rating rating = Rating.builder()
                .member(member)
                .movieId(request.movieId())
                .score(request.score())
                .comment(request.comment())
                .build();

        return ratingRepository.save(rating).getRatingId();
    }

    /** 평점 수정. 값만 바꾸면 트랜잭션이 끝날 때 JPA가 UPDATE를 보낸다. (dirty checking) */
    @Transactional
    public void update(Long ratingId, Long memberId, UpdateRatingRequest request) {
        Rating rating = findOwnedRating(ratingId, memberId);
        rating.update(request.score(), request.comment());
    }

    @Transactional
    public void delete(Long ratingId, Long memberId) {
        Rating rating = findOwnedRating(ratingId, memberId);
        ratingRepository.delete(rating);
    }

    /**
     * 평점을 찾고, 그 평점이 요청자의 것인지까지 확인한다. 수정·삭제가 같은 검사를 하므로 묶었다.
     * 먼저 찾아보고(없으면 404), 찾았으면 주인을 본다(내 것이 아니면 403).
     */
    private Rating findOwnedRating(Long ratingId, Long memberId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new GeneralException(ErrorCode.RATING_NOT_FOUND));

        // member는 LAZY지만 getMemberId()는 프록시가 이미 가진 id라 추가 SELECT가 나가지 않는다.
        if (!rating.getMember().getMemberId().equals(memberId)) {
            throw new GeneralException(ErrorCode.RATING_FORBIDDEN);
        }
        return rating;
    }
}
