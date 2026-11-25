package com.umc.movie.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서(OpenAPI)의 표지와 인증 방식을 등록한다. (9주차)
 *
 * 손으로 쓴 명세 표는 코드가 바뀌는 순간 조용히 거짓말을 시작한다.
 * 컨트롤러와 DTO를 읽어 문서를 만들게 하면 둘이 어긋날 수 없다.
 */
@Configuration
public class SwaggerConfig {

    /** 각 API에 {@code @SecurityRequirement(name = "bearerAuth")}로 연결할 때 쓰는 이름. */
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI movieOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("영화 평점 서비스 API")
                        .description("UMC 11기 백엔드 워크북에서 만든 회원/평점/즐겨찾기 API 문서입니다.")
                        .version("v1"))
                // Authorize(자물쇠) 버튼에 쓸 인증 방식을 등록한다.
                // 여기서는 "이런 인증 방식이 있다"고 선언만 하고, 실제로 어떤 API에 필요한지는
                // 각 컨트롤러의 @SecurityRequirement가 정한다.
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP) // Authorization 헤더를 쓰는 방식
                                .scheme("bearer")               // "Bearer {토큰}" 형태
                                .bearerFormat("JWT")));         // 문서에 표시할 토큰 종류
    }
}
