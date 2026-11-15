package com.umc.movie.dto;

/** 영화 기록 저장 결과. addToFavorites가 false였다면 favorite은 null이다. */
public record MovieRecordResponse(RatingResponse rating, FavoriteResponse favorite) {}
