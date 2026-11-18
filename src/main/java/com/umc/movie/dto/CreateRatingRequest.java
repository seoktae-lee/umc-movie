package com.umc.movie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 평점 등록 요청. movieId는 TMDB 영화 id라 우리 DB의 movie 테이블을 참조하지 않는다. */
public record CreateRatingRequest(
        @Schema(description = "TMDB 영화 ID (우리 DB의 영화 테이블이 아니라 TMDB의 id)", example = "550")
        @NotNull(message = "영화 ID를 입력해주세요.")
        @Positive(message = "영화 ID는 양수여야 합니다.")
        Long movieId,

        @Schema(description = "별점 (1~5 정수)", example = "5")
        @NotNull(message = "별점을 입력해주세요.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        Integer score,

        // 선택 입력. null이면 검사하지 않는다.
        @Schema(description = "후기 (선택, 500자 이하)", example = "최고의 영화!")
        @Size(max = 500, message = "후기는 500자 이하로 입력해주세요.")
        String comment
) {}
