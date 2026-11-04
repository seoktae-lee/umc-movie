package com.umc.movie.controller;

import com.umc.movie.config.WebConfig;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.exception.ApiException;
import com.umc.movie.interceptor.AuthInterceptor;
import com.umc.movie.service.RatingService;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
@Import({WebConfig.class, AuthInterceptor.class, JwtUtil.class})
class RatingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtUtil jwtUtil;

    @MockitoBean
    RatingService ratingService;

    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 30, 10, 0);
    private static final RatingResponse RATING = new RatingResponse(7L, 13L, 4, "따뜻한 영화", AT, AT);

    private String bearer(Long memberId) {
        return "Bearer " + jwtUtil.createAccessToken(memberId, memberId + "@umc.com");
    }

    @Test
    @DisplayName("명세대로 ratingId·movieId·score·comment·createdAt·updatedAt 배열을 응답한다")
    void ratings() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 9, 30, 10, 0);
        given(ratingService.getRatingsByMember(1L, 0, 20)).willReturn(List.of(
                new RatingResponse(2L, 155L, 4, "다시 봐도 명작", at, at),
                new RatingResponse(1L, 550L, 5, "최고의 영화!", at, at)));

        mockMvc.perform(get("/members/{memberId}/ratings", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ratingId").value(2))
                .andExpect(jsonPath("$[0].movieId").value(155))
                .andExpect(jsonPath("$[0].createdAt").value("2026-09-30T10:00:00"))
                // 엔티티의 member·password 같은 필드는 응답에 없다.
                .andExpect(jsonPath("$[0].member").doesNotExist());
    }

    @Test
    @DisplayName("page·size Query Parameter를 서비스로 넘긴다")
    void paging() throws Exception {
        given(ratingService.getRatingsByMember(1L, 1, 1)).willReturn(List.of());

        mockMvc.perform(get("/members/1/ratings").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("size가 범위를 벗어나면 400")
    void pagingRange() throws Exception {
        mockMvc.perform(get("/members/1/ratings").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/members/1/ratings").param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 유저는 404")
    void notFound() throws Exception {
        given(ratingService.getRatingsByMember(999L, 0, 20))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));

        mockMvc.perform(get("/members/{memberId}/ratings", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("평점 등록: 토큰의 memberId로 등록하고 201 + result에 평점")
    void create() throws Exception {
        given(ratingService.create(eq(1L), any(CreateRatingRequest.class))).willReturn(RATING);

        mockMvc.perform(post("/ratings").header(AUTHORIZATION, bearer(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 4, \"comment\": \"따뜻한 영화\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.ratingId").value(7));
    }

    @Test
    @DisplayName("토큰 없이 평점을 등록·수정·삭제하면 401 AUTH401_2, 서비스는 호출되지 않는다")
    void noToken() throws Exception {
        String body = "{\"movieId\": 13, \"score\": 4}";
        mockMvc.perform(post("/ratings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_2"));
        mockMvc.perform(patch("/ratings/7").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.delete("/ratings/7"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ratingService);
    }

    @Test
    @DisplayName("별점이 1~5가 아니면 400, 후기가 500자를 넘어도 400")
    void invalidScore() throws Exception {
        mockMvc.perform(post("/ratings").header(AUTHORIZATION, bearer(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("별점은 5점 이하여야 합니다."));
        mockMvc.perform(post("/ratings").header(AUTHORIZATION, bearer(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 3, \"comment\": \"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("후기는 500자 이하로 입력해주세요."));

        verify(ratingService, never()).create(any(), any());
    }

    @Test
    @DisplayName("중복 평점은 409 RATING409_1")
    void duplicated() throws Exception {
        given(ratingService.create(eq(1L), any(CreateRatingRequest.class)))
                .willThrow(new ApiException(HttpStatus.CONFLICT, "RATING409_1", "이미 이 영화에 평점을 남기셨습니다."));

        mockMvc.perform(post("/ratings").header(AUTHORIZATION, bearer(1L)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 550, \"score\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING409_1"));
    }

    @Test
    @DisplayName("평점 수정: 200 + result에 평점, 남의 평점이면 403 RATING403_1")
    void update() throws Exception {
        String body = "{\"score\": 5, \"comment\": \"다시 보니 좋다\"}";
        given(ratingService.update(eq(7L), eq(1L), any(UpdateRatingRequest.class))).willReturn(RATING);
        mockMvc.perform(patch("/ratings/7").header(AUTHORIZATION, bearer(1L)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.ratingId").value(7));

        given(ratingService.update(eq(7L), eq(2L), any(UpdateRatingRequest.class)))
                .willThrow(new ApiException(HttpStatus.FORBIDDEN, "RATING403_1", "본인이 작성한 평점만 수정하거나 삭제할 수 있습니다."));
        mockMvc.perform(patch("/ratings/7").header(AUTHORIZATION, bearer(2L)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RATING403_1"));
    }

    @Test
    @DisplayName("평점 삭제: 200 + result null, 없는 평점이면 404 RATING404_1")
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/ratings/7").header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result").value(nullValue()));

        willThrow(new ApiException(HttpStatus.NOT_FOUND, "RATING404_1", "해당 평점을 찾을 수 없습니다."))
                .given(ratingService).delete(999L, 1L);
        mockMvc.perform(MockMvcRequestBuilders.delete("/ratings/999").header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATING404_1"));
    }

    @Test
    @DisplayName("memberId 자리에 숫자가 아니면 400 COMMON4003")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/members/abc/ratings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4003"));
    }

    @Test
    @DisplayName("없는 경로는 404, 허용하지 않는 메서드는 405이고 500으로 덮이지 않는다")
    void frameworkStatusIsKept() throws Exception {
        mockMvc.perform(get("/nope").header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON404"));
        // 6주차 URL. /members/*/ratings는 조회 때문에 관문이 열려 있어 토큰 없이도 여기까지 온다.
        mockMvc.perform(post("/members/1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 4}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON405"));
    }
}
