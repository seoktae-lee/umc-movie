package com.umc.movie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 평점 수정 요청. 영화는 바꿀 수 없으므로 movieId가 없다. */
public record UpdateRatingRequest(
        @Schema(description = "별점 (1~5 정수)", example = "4")
        @NotNull(message = "별점을 입력해주세요.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        Integer score,

        @Schema(description = "후기. 통째로 교체하므로 빼면 기존 후기가 지워집니다. (500자 이하)", example = "다시 봐도 좋다")
        @Size(max = 500, message = "후기는 500자 이하로 입력해주세요.")
        String comment
) {}
