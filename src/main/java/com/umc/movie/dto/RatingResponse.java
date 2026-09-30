package com.umc.movie.dto;

import com.umc.movie.entity.Rating;

import java.time.LocalDateTime;

/**
 * 유저별 평점 조회 응답의 한 항목.
 * Entity를 그대로 내보내지 않고, API 명세에서 합의한 필드만 담는다. (member·password 등은 나가지 않는다)
 */
public record RatingResponse(
        Long ratingId,
        Long movieId,
        Integer score,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getRatingId(),
                rating.getMovieId(),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }
}
