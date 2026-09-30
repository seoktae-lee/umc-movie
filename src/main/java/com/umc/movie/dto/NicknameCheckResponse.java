package com.umc.movie.dto;

/** 닉네임 중복 체크 응답. 사용할 수 있으면 true. */
public record NicknameCheckResponse(boolean available) {}
