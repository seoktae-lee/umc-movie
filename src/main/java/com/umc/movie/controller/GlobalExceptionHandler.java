package com.umc.movie.controller;

import com.umc.movie.dto.ApiResponse;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 어느 Controller에서 예외가 나든 같은 실패 형식(ApiResponse)으로 바꿔 돌려준다.
 * Controller마다 try-catch를 쓰지 않아도 된다.
 *
 * 5주차의 ValidationExceptionHandler(경로 값 검증만 처리)를 이 클래스로 합쳤다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 업무 규칙 위반: 중복(409), 없음(404), 남의 평점(403). */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(GeneralException exception) {
        return toResponse(exception.getErrorCode(), exception.getMessage());
    }

    /** @Valid @RequestBody DTO 검증 실패. 첫 번째 필드 오류 문구를 그대로 보여 준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT, message);
    }

    /** @PathVariable·@RequestParam 제약(@Size·@Email·@Min) 검증 실패. (5주차 GET API) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException exception) {
        String message = exception.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT, message);
    }

    /** JSON 문법 오류, score에 "다섯" 같은 문자열 등 본문을 읽을 수 없을 때. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException exception) {
        return toResponse(ErrorCode.INVALID_BODY, ErrorCode.INVALID_BODY.getMessage());
    }

    /** /members/abc/ratings처럼 숫자 자리에 숫자가 아닌 값이 올 때. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return toResponse(ErrorCode.INVALID_PATH, exception.getName() + " 값의 형식이 올바르지 않습니다.");
    }

    /**
     * DB 제약 위반. 사전 중복 검사를 통과한 요청끼리 거의 동시에 저장하면 여기로 온다. (Challenge)
     * 어떤 UNIQUE 키가 막았는지 보고, 사전 검사와 같은 code·message로 바꿔 준다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        String cause = String.valueOf(exception.getMostSpecificCause().getMessage());
        ErrorCode errorCode = mapConstraint(cause);
        log.warn("[DB 제약 위반 → {}] {}", errorCode.getCode(), cause);
        return toResponse(errorCode, errorCode.getMessage());
    }

    /** 워크북 방식(ResponseStatusException)으로 던진 예외도 같은 형식으로 바꾼다. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        return ResponseEntity.status(status)
                .body(ApiResponse.failure("COMMON" + status, exception.getReason()));
    }

    /** 예상하지 못한 오류. 내부 메시지·스택은 로그에만 남기고 응답에는 노출하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("처리하지 못한 예외", exception);
        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    /** MySQL 메시지 예: Duplicate entry 'a@b.com' for key 'member.email' */
    static ErrorCode mapConstraint(String cause) {
        if (cause.contains("member.email")) return ErrorCode.MEMBER_EMAIL_DUPLICATED;
        if (cause.contains("member.nickname")) return ErrorCode.MEMBER_NICKNAME_DUPLICATED;
        if (cause.contains("uk_rating_member_movie")) return ErrorCode.RATING_DUPLICATED;
        return ErrorCode.DATA_CONFLICT;
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.failure(errorCode.getCode(), message));
    }
}
