package com.umc.movie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 서비스 전체에 같은 CORS 정책을 한곳에서 적용한다.
 * (컨트롤러마다 @CrossOrigin을 붙이는 것보다 빠뜨릴 일이 없다)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 허용할 출처 목록. application.yml의 app.cors.allowed-origins에서 읽는다.
    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins(allowedOrigins) // 이 출처에서 오는 요청만 허용
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .allowedHeaders("*");
    }
}
