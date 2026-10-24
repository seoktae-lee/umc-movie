package com.umc.movie.dto;

/**
 * 6주차부터 쓰기 API가 쓰는 공통 응답 틀. 성공과 실패가 같은 모양이다.
 *
 * <pre>
 * { "isSuccess": true,  "code": "COMMON201",  "message": "...", "result": {...} }
 * { "isSuccess": false, "code": "MEMBER4091", "message": "...", "result": null }
 * </pre>
 */
public record ApiResponse<T>(boolean isSuccess, String code, String message, T result) {

    public static <T> ApiResponse<T> created(String message, T result) {
        return new ApiResponse<>(true, "COMMON201", message, result);
    }

    public static <T> ApiResponse<T> ok(String message, T result) {
        return new ApiResponse<>(true, "COMMON200", message, result);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
