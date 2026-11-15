package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.MovieRecordRequest;
import com.umc.movie.dto.MovieRecordResponse;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MovieRecordServiceTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 6, 21, 10);

    @Mock
    RatingService ratingService;

    @Mock
    FavoriteService favoriteService;

    @InjectMocks
    MovieRecordService movieRecordService;

    private final RatingResponse rating = new RatingResponse(3L, 238L, 5, "다시 보고 싶은 영화입니다.", AT, AT);

    @Test
    @DisplayName("addToFavorites가 true면 평점과 즐겨찾기를 모두 저장한다")
    void both() {
        given(ratingService.create(anyLong(), any(CreateRatingRequest.class))).willReturn(rating);
        given(favoriteService.create(1L, 238L)).willReturn(new FavoriteResponse(2L, 238L, AT));

        MovieRecordResponse response = movieRecordService.save(1L, new MovieRecordRequest(238L, 5, "좋다", true));

        assertThat(response.rating().ratingId()).isEqualTo(3L);
        assertThat(response.favorite().favoriteId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("addToFavorites가 false면 평점만 저장하고 favorite은 null")
    void ratingOnly() {
        given(ratingService.create(anyLong(), any(CreateRatingRequest.class))).willReturn(rating);

        MovieRecordResponse response = movieRecordService.save(1L, new MovieRecordRequest(238L, 5, "좋다", false));

        assertThat(response.favorite()).isNull();
        verify(favoriteService, never()).create(anyLong(), anyLong());
    }

    @Test
    @DisplayName("즐겨찾기 저장이 실패하면 예외가 그대로 올라가 바깥 트랜잭션이 롤백된다")
    void favoriteFailurePropagates() {
        given(ratingService.create(anyLong(), any(CreateRatingRequest.class))).willReturn(rating);
        given(favoriteService.create(1L, 238L))
                .willThrow(new ApiException(HttpStatus.CONFLICT, "FAVORITE409_1", "이미 즐겨찾기한 영화입니다."));

        assertThatThrownBy(() -> movieRecordService.save(1L, new MovieRecordRequest(238L, 5, "좋다", true)))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("FAVORITE409_1");
    }

    @Test
    @DisplayName("save()는 하나의 @Transactional 안에서 실행된다")
    void isTransactional() throws NoSuchMethodException {
        Method save = MovieRecordService.class.getMethod("save", Long.class, MovieRecordRequest.class);

        assertThat(save.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
