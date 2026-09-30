package com.umc.movie.controller;

import com.umc.movie.config.WebConfig;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.service.RatingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
