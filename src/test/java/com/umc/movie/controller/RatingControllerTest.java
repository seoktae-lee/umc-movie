package com.umc.movie.controller;

import com.umc.movie.config.WebConfig;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.exception.GeneralException;
import com.umc.movie.service.RatingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
@Import(WebConfig.class)
class RatingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RatingService ratingService;

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
    @DisplayName("평점 등록: 201 + result에 ratingId")
    void create() throws Exception {
        given(ratingService.create(eq(1L), any(CreateRatingRequest.class))).willReturn(7L);

        mockMvc.perform(post("/members/1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 4, \"comment\": \"따뜻한 영화\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result").value(7));
    }

    @Test
    @DisplayName("별점이 1~5가 아니면 400, 후기가 500자를 넘어도 400")
    void invalidScore() throws Exception {
        mockMvc.perform(post("/members/1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("별점은 5점 이하여야 합니다."));
        mockMvc.perform(post("/members/1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 13, \"score\": 3, \"comment\": \"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("후기는 500자 이하로 입력해주세요."));

        verify(ratingService, never()).create(any(), any());
    }

    @Test
    @DisplayName("중복 평점은 409 RATING4091")
    void duplicated() throws Exception {
        given(ratingService.create(eq(1L), any(CreateRatingRequest.class)))
                .willThrow(new GeneralException(ErrorCode.RATING_DUPLICATED));

        mockMvc.perform(post("/members/1/ratings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 550, \"score\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING4091"));
    }

    @Test
    @DisplayName("평점 수정: 200 COMMON200, 남의 평점이면 403 RATING4031")
    void update() throws Exception {
        String body = "{\"score\": 5, \"comment\": \"다시 보니 좋다\"}";
        mockMvc.perform(patch("/members/1/ratings/7").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"));

        willThrow(new GeneralException(ErrorCode.RATING_FORBIDDEN))
                .given(ratingService).update(eq(7L), eq(2L), any(UpdateRatingRequest.class));
        mockMvc.perform(patch("/members/2/ratings/7").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RATING4031"));
    }

    @Test
    @DisplayName("평점 삭제: 204 본문 없음, 없는 평점이면 404 RATING4041")
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/members/1/ratings/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        willThrow(new GeneralException(ErrorCode.RATING_NOT_FOUND)).given(ratingService).delete(999L, 1L);
        mockMvc.perform(MockMvcRequestBuilders.delete("/members/1/ratings/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RATING4041"));
    }

    @Test
    @DisplayName("memberId 자리에 숫자가 아니면 400 COMMON4003")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/members/abc/ratings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4003"));
    }
}
