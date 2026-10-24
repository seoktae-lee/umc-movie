package com.umc.movie.exception;

import lombok.Getter;

/**
 * 업무 규칙 위반(중복·없음·권한 없음)을 나타내는 예외.
 * Service는 throw만 하고, 응답 모양은 GlobalExceptionHandler가 정한다.
 */
@Getter
public class GeneralException extends RuntimeException {

    private final ErrorCode errorCode;

    public GeneralException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
