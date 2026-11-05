package com.umc.movie.resolver;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.exception.ApiException;
import com.umc.movie.interceptor.AuthInterceptor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** @LoginMember Long 파라미터에 인터셉터가 요청에 담아 둔 memberId를 넣어 준다. */
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Object memberId = webRequest.getAttribute(AuthInterceptor.MEMBER_ID, RequestAttributes.SCOPE_REQUEST);

        // 관문을 열어 둔 경로에 @LoginMember를 잘못 붙이면 값이 없다. null로 넘기지 않고 막는다.
        if (memberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_2", "로그인이 필요한 요청입니다.");
        }
        return memberId;
    }
}
