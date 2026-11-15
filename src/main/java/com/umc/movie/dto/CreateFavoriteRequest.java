package com.umc.movie.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 즐겨찾기 등록 요청. 회원은 토큰에서 알아내므로 memberId는 받지 않는다. */
public record CreateFavoriteRequest(
        @NotNull(message = "영화 ID를 입력해주세요.")
        @Positive(message = "영화 ID는 양수여야 합니다.")
        Long movieId
) {}
