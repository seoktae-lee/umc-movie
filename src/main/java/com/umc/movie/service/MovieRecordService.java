package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.MovieRecordRequest;
import com.umc.movie.dto.MovieRecordResponse;
import com.umc.movie.dto.RatingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "평점을 남기고 즐겨찾기에도 넣기"라는 한 가지 사용자 행동을 처리하는 전용 Service.
 *
 * RatingService.create()와 FavoriteService.create()를 컨트롤러가 연달아 부르면 각각 별도 트랜잭션이라
 * 앞의 저장만 남을 수 있다. 여기서 바깥 트랜잭션 하나를 열면 두 호출이 그 트랜잭션에 참여(REQUIRED)해서
 * 하나라도 예외를 던지면 먼저 저장한 것까지 롤백된다.
 */
@Service
@RequiredArgsConstructor
public class MovieRecordService {

    private final RatingService ratingService;
    private final FavoriteService favoriteService;

    @Transactional
    public MovieRecordResponse save(Long memberId, MovieRecordRequest request) {
        RatingResponse rating = ratingService.create(memberId,
                new CreateRatingRequest(request.movieId(), request.score(), request.comment()));

        FavoriteResponse favorite = null;
        if (request.addToFavorites()) {
            favorite = favoriteService.create(memberId, request.movieId());
        }
        return new MovieRecordResponse(rating, favorite);
    }
}
