package com.umc.movie.dto;

/** 오류 응답. 화면은 message를 그대로 사용자에게 보여 줄 수 있다. */
public record ErrorResponse(int status, String message) {}
