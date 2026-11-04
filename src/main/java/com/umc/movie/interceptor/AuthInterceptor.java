package com.umc.movie.interceptor;

import com.umc.movie.exception.ApiException;
import com.umc.movie.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 컨트롤러 앞에서 토큰을 검사하는 공통 관문. 컨트롤러는 토큰을 몰라도 memberId만 받아 쓴다. */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String MEMBER_ID = "memberId"; // 컨트롤러에서 꺼낼 때 쓸 이름
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    /**
     * 컨트롤러가 실행되기 "전"에 호출된다.
     * true를 반환하면 요청이 컨트롤러까지 이어지고, 예외를 던지면 여기서 끝난다.
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        // 브라우저가 본 요청 전에 보내는 CORS 사전 요청(preflight)에는 토큰이 붙지 않는다.
        // 여기서 막으면 브라우저에서 오는 요청이 모두 실패하므로 그냥 통과시킨다.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // "Bearer {토큰}" 형식이 아니면 아예 토큰을 보내지 않은 것으로 본다
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_2",
                    "로그인이 필요한 요청입니다.");
        }

        // 검증에 실패하면 JwtUtil이 예외를 던지므로 여기까지 왔다면 유효한 토큰이다
        Long memberId = jwtUtil.getMemberId(header.substring(BEARER_PREFIX.length()));

        // 뒤이어 실행될 컨트롤러가 꺼내 쓸 수 있도록 요청에 담아 둔다
        request.setAttribute(MEMBER_ID, memberId);

        return true;
    }
}
