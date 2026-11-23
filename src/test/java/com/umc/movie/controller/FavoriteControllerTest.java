package com.umc.movie.controller;

import com.umc.movie.config.WebConfig;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.FavoriteStatusResponse;
import com.umc.movie.dto.MovieRecordResponse;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.exception.ApiException;
import com.umc.movie.interceptor.AuthInterceptor;
import com.umc.movie.service.FavoriteService;
import com.umc.movie.service.MovieRecordService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FavoriteController.class, MovieRecordController.class})
@Import({WebConfig.class, AuthInterceptor.class, JwtUtil.class})
class FavoriteControllerTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 6, 21, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtUtil jwtUtil;

    @MockitoBean
    FavoriteService favoriteService;

    @MockitoBean
    MovieRecordService movieRecordService;

    private String bearer(Long memberId) {
        return "Bearer " + jwtUtil.createAccessToken(memberId, memberId + "@umc.com");
    }

    @Test
    @DisplayName("토큰 없이 POST /favorites 하면 401 AUTH401_2이고 서비스를 부르지 않는다")
    void noToken() throws Exception {
        mockMvc.perform(post("/favorites").contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":550}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_2"));
        verifyNoInteractions(favoriteService);
    }

    @Test
    @DisplayName("POST /favorites는 토큰의 회원으로 등록하고 201 + 공통 형식으로 응답한다")
    void create() throws Exception {
        given(favoriteService.create(any(Long.class), any(com.umc.movie.dto.CreateFavoriteRequest.class)))
                .willReturn(new FavoriteResponse(1L, 550L, AT));

        mockMvc.perform(post("/favorites").header(AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":550}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.favoriteId").value(1))
                .andExpect(jsonPath("$.result.movieId").value(550));
    }

    @Test
    @DisplayName("movieId가 0이면 400")
    void zeroMovieId() throws Exception {
        mockMvc.perform(post("/favorites").header(AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(favoriteService);
    }

    @Test
    @DisplayName("중복 등록은 409 FAVORITE409_1")
    void duplicated() throws Exception {
        given(favoriteService.create(any(Long.class), any(com.umc.movie.dto.CreateFavoriteRequest.class)))
                .willThrow(new ApiException(HttpStatus.CONFLICT, "FAVORITE409_1", "이미 즐겨찾기한 영화입니다."));

        mockMvc.perform(post("/favorites").header(AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":550}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE409_1"));
    }

    @Test
    @DisplayName("GET /favorites 빈 목록은 200 + []")
    void emptyList() throws Exception {
        given(favoriteService.getMyFavorites(1L)).willReturn(List.of());

        mockMvc.perform(get("/favorites").header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(0));
    }

    @Test
    @DisplayName("GET /favorites/{movieId}는 isFavorite을 담아 200")
    void favoriteStatus() throws Exception {
        given(favoriteService.getStatus(1L, 550L)).willReturn(new FavoriteStatusResponse(true));

        mockMvc.perform(get("/favorites/{movieId}", 550).header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isFavorite").value(true));
    }

    @Test
    @DisplayName("DELETE /favorites/{movieId}는 200 + result null")
    void deleteFavorite() throws Exception {
        mockMvc.perform(delete("/favorites/{movieId}", 550).header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(nullValue()));
        verify(favoriteService).delete(1L, 550L);
    }

    @Test
    @DisplayName("없는 즐겨찾기 해제는 404 FAVORITE404_1")
    void deleteNotFound() throws Exception {
        org.mockito.BDDMockito.willThrow(new ApiException(HttpStatus.NOT_FOUND, "FAVORITE404_1", "해제할 즐겨찾기를 찾을 수 없습니다."))
                .given(favoriteService).delete(1L, 7L);

        mockMvc.perform(delete("/favorites/{movieId}", 7).header(AUTHORIZATION, bearer(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE404_1"));
    }

    @Test
    @DisplayName("POST /movie-records는 rating과 favorite을 함께 담아 201")
    void movieRecord() throws Exception {
        given(movieRecordService.save(any(Long.class), any(com.umc.movie.dto.MovieRecordRequest.class)))
                .willReturn(new MovieRecordResponse(new RatingResponse(3L, 238L, 5, "좋다", AT, AT),
                        new FavoriteResponse(2L, 238L, AT)));

        mockMvc.perform(post("/movie-records").header(AUTHORIZATION, bearer(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":238,\"score\":5,\"comment\":\"좋다\",\"addToFavorites\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.rating.ratingId").value(3))
                .andExpect(jsonPath("$.result.favorite.favoriteId").value(2));
    }

    @Test
    @DisplayName("토큰 없이 POST /movie-records 하면 401")
    void movieRecordNoToken() throws Exception {
        mockMvc.perform(post("/movie-records").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":238,\"score\":5}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(movieRecordService);
    }
}
