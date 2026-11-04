package com.umc.movie.config;

import com.umc.movie.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 서비스 전체에 같은 CORS 정책과 토큰 관문을 한곳에서 적용한다.
 * (컨트롤러마다 붙이는 것보다 빠뜨릴 일이 없다)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 허용할 출처 목록. application.yml의 app.cors.allowed-origins에서 읽는다.
    private final String[] allowedOrigins;
    private final AuthInterceptor authInterceptor;

    public WebConfig(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins,
            AuthInterceptor authInterceptor
    ) {
        this.allowedOrigins = allowedOrigins;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins(allowedOrigins) // 이 출처에서 오는 요청만 허용
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*"); // Authorization 헤더도 여기에 포함된다
    }

    /**
     * 잠글 경로를 나열하지 않고 "전부 잠그고 열 경로만 나열"한다.
     * 새 API를 만들고 목록에 추가하는 걸 깜빡해도 잠긴 채로 남는다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",              // 로그인
                        "/members",              // 회원가입 (POST /members)
                        "/members/nickname/**",  // 닉네임 중복 체크
                        "/members/email/**",     // 이메일 중복 체크
                        "/members/*/ratings",    // 유저별 평점 조회 (누구나 볼 수 있는 목록)
                        "/error"                 // 스프링이 내부적으로 쓰는 경로
                );
    }
}
