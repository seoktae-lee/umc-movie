package com.umc.movie.dto;

/** 즐겨찾기 여부. 하지 않았어도 오류가 아니라 정상 상태이므로 false로 응답한다. */
public record FavoriteStatusResponse(boolean isFavorite) {}
