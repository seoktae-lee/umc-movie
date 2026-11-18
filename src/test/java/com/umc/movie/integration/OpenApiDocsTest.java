package com.umc.movie.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 문서(OpenAPI)가 실제 동작과 같은 말을 하는지 확인한다. (9주차)
 *
 * 문서에 "🔒 토큰 필요"라고 적어 놓고 실제로는 열려 있거나 그 반대면, 문서가 거짓말을 하는 것이다.
 * 5번(토큰이 필요한가요)은 실수하기 가장 쉬운 항목이라 테스트로 고정한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger UI 화면과 OpenAPI JSON은 토큰 없이 열린다")
    void swaggerIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authorize 버튼에 쓸 Bearer(JWT) 인증 방식이 등록돼 있다")
    void bearerSchemeRegistered() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.info.title").value("영화 평점 서비스 API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    @DisplayName("토큰이 필요한 API에는 자물쇠(security)가 있고, 공개 API에는 없다")
    void lockOnlyOnProtectedApis() throws Exception {
        String[][] protectedApis = {
                {"/favorites", "post"}, {"/favorites", "get"},
                {"/favorites/{movieId}", "get"}, {"/favorites/{movieId}", "delete"},
                {"/ratings", "post"}, {"/ratings/{ratingId}", "patch"}, {"/ratings/{ratingId}", "delete"},
                {"/members/me", "get"}, {"/members/me", "patch"},
                {"/movie-records", "post"},
        };
        for (String[] api : protectedApis) {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['" + api[0] + "']." + api[1] + ".security[0]", hasKey("bearerAuth")));
        }

        String[][] publicApis = {
                {"/auth/login", "post"}, {"/members", "post"},
                {"/members/nickname/{nickname}", "get"}, {"/members/email/{email}", "get"},
                {"/members/{memberId}/ratings", "get"},
        };
        for (String[] api : publicApis) {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['" + api[0] + "']." + api[1] + ".security").doesNotExist());
        }
    }

    @Test
    @DisplayName("토큰으로 알아내는 memberId는 문서의 입력칸에 나타나지 않는다")
    void loginMemberIsHidden() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/favorites'].post.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/ratings/{ratingId}'].patch.parameters[*].name",
                        not(org.hamcrest.Matchers.hasItem("memberId"))));
    }

    @Test
    @DisplayName("즐겨찾기 등록은 성공(201)과 실패(401, 409) 응답이 모두 문서에 적혀 있다")
    void favoriteResponsesDocumented() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/favorites'].post.responses['409']").exists());
    }

    @Test
    @DisplayName("요청 DTO의 예시 값이 문서에 채워져 입력칸에 미리 들어간다")
    void examplesAreFilled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.SignUpRequest.properties.email.example").value("challenger@umc.com"))
                .andExpect(jsonPath("$.components.schemas.CreateFavoriteRequest.properties.movieId.example").value(550));
    }
}
