package com.umc.movie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.movie.dto.MovieRecordRequest;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /movie-records가 "정말 되돌리는지"를 테스트로 증명한다. (9주차 선택 미션)
 *
 * 8주차에는 "한 트랜잭션이라 하나라도 실패하면 모두 저장되지 않는다"고 배웠을 뿐, 눈으로 확인하기는 어려웠다.
 *
 * ⚠️ 이 클래스에는 일부러 {@code @Transactional}을 붙이지 않았다.
 * 테스트 메서드가 바깥 트랜잭션을 열면 서비스의 트랜잭션이 그 안에 합류해서, 롤백이 일어나도
 * 같은 트랜잭션 안에서 조회하는 테스트에는 저장한 평점이 그대로 보인다. (롤백을 증명할 수 없다)
 * 그래서 실제로 커밋/롤백이 일어나게 두고, 대신 {@code @AfterEach}에서 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieRecordTransactionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Member member;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        cleanUp();
        member = memberRepository.save(Member.builder()
                .email("tx@umc.com").password("x").nickname("트랜잭션").build());
        authorizationHeader = "Bearer " + jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
    }

    @AfterEach
    void cleanUp() {
        ratingRepository.deleteAll();
        favoriteRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("이미 즐겨찾기한 영화에 addToFavorites=true로 저장하면 409이고 평점도 남지 않는다")
    void rollsBackRatingWhenFavoriteFails() throws Exception {
        favoriteRepository.save(Favorite.builder().member(member).movieId(550L).build());
        long ratingsBefore = ratingRepository.count();

        mockMvc.perform(post("/movie-records")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieRecordRequest(550L, 4, "재미있다", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE409_1"));

        // "예외가 났다"에서 멈추지 않고, "데이터가 남지 않았다"까지 확인해야 의미가 있다.
        assertThat(ratingRepository.count())
                .as("즐겨찾기 저장이 실패했는데 평점이 남아 있다 = 롤백되지 않았다")
                .isEqualTo(ratingsBefore);
        assertThat(favoriteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("addToFavorites=true로 성공하면 평점과 즐겨찾기가 모두 저장된다")
    void savesBothWhenSucceeds() throws Exception {
        mockMvc.perform(post("/movie-records")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieRecordRequest(680L, 5, null, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.rating.movieId").value(680))
                .andExpect(jsonPath("$.result.favorite.movieId").value(680));

        assertThat(ratingRepository.count()).isEqualTo(1);
        assertThat(favoriteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("addToFavorites=false면 평점만 저장하고 favorite은 null이다")
    void savesOnlyRatingWhenNotFavorite() throws Exception {
        mockMvc.perform(post("/movie-records")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MovieRecordRequest(13L, 3, "보통", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.favorite").value(nullValue()));

        assertThat(ratingRepository.count()).isEqualTo(1);
        assertThat(favoriteRepository.count()).isZero();
    }
}
