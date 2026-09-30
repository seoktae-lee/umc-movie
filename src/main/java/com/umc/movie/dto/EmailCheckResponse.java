package com.umc.movie.dto;

/** 이메일 중복 체크 응답. 사용할 수 있으면 true. */
public record EmailCheckResponse(boolean available) {}
