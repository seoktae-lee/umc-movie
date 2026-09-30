package com.umc.movie.controller;

import com.umc.movie.dto.ErrorResponse;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * (선택 미션) @PathVariable·@RequestParam 검증 실패만 다룬다.
 *
 * 그대로 두면 400 응답의 message에 "Validation failed for method='public ...MemberController...'"처럼
 * 서버 내부 메서드 이름이 노출된다. 우리가 @Size·@Email에 적은 문구로 바꿔서 내려준다.
 * 예외를 전부 한곳에서 다루는 방법은 6주차에 배운다.
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handle(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    }
}
