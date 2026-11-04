package com.umc.movie.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 상태 코드·code·message를 함께 담아 던지는 예외. (7주차)
 * 인증·소유권 실패는 컨트롤러가 아니라 관문이나 서비스 안쪽에서 발견되므로,
 * 어디서 던지든 GlobalExceptionHandler가 같은 실패 형식으로 바꿔 준다.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status; // 응답으로 내보낼 HTTP 상태 코드
    private final String code;       // 응답 바디의 code 필드에 실릴 문자열

    public ApiException(HttpStatus status, String code, String message) {
        super(message); // message는 부모인 RuntimeException이 들고 있다
        this.status = status;
        this.code = code;
    }
}
