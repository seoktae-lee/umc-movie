package com.umc.movie.annotation;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 값은 로그인한 사용자(토큰의 주인)에게서 온다는 표시. (선택 미션)
 *
 * 9주차: 요청으로 받는 값이 아니므로 Swagger 문서의 입력칸에서는 숨긴다.
 * (숨기지 않으면 문서에 memberId를 직접 넣는 칸이 생겨 "토큰으로 알아낸다"는 설계와 어긋난다)
 */
@Parameter(hidden = true)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
}
