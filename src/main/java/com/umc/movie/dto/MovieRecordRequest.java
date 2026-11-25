package com.umc.movie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 영화 기록 저장 요청. 평점 필드에 addToFavorites가 더해진 모양이다. */
public record MovieRecordRequest(
        @Schema(description = "TMDB 영화 ID", example = "550")
        @NotNull(message = "영화 ID를 입력해주세요.")
        @Positive(message = "영화 ID는 양수여야 합니다.")
        Long movieId,

        @Schema(description = "별점 (1~5 정수)", example = "4")
        @NotNull(message = "별점을 입력해주세요.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        Integer score,

        @Schema(description = "후기 (선택, 500자 이하)", example = "재미있다")
        @Size(max = 500, message = "후기는 500자 이하로 입력해주세요.")
        String comment,

        // 생략하면 false. 평점만 저장한다.
        @Schema(description = "true면 즐겨찾기도 함께 저장. 생략하면 false", example = "true")
        boolean addToFavorites
) {}
