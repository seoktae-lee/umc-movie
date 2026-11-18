package com.umc.movie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import com.umc.movie.util.JwtUtil;
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
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 평점 API의 통합 테스트. (9주차 필수 미션)
 *
 * 7주차에 Postman으로 하나씩 눌러 보던 "토큰 없이 → 401, 토큰 담아 → 201, 중복 → 409, 남의 것 → 403"을
 * 코드로 고정한다. 명령어 하나로 전부 다시 확인되고, 어딘가 망가지면 어떤 것이 왜 깨졌는지 알려 준다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RatingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Member member;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .email("challenger@umc.com").password("x").nickname("영화조아").build());
        authorizationHeader = bearer(member);
    }

    private String bearer(Member who) {
        return "Bearer " + jwtUtil.createAccessToken(who.getMemberId(), who.getEmail());
    }

    @Test
    @DisplayName("토큰 없이 POST /ratings를 부르면 401과 AUTH401_2를 응답하고 저장하지 않는다")
    void createRatingWithoutToken() throws Exception {
        mockMvc.perform(post("/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRatingRequest(550L, 5, "최고"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_2"));

        org.assertj.core.api.Assertions.assertThat(ratingRepository.count()).isZero();
    }

    @Test
    @DisplayName("토큰을 담아 POST /ratings를 부르면 201과 저장된 평점을 응답한다")
    void createRating() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRatingRequest(550L, 5, "최고의 영화!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.movieId").value(550))
                .andExpect(jsonPath("$.result.score").value(5))
                .andExpect(jsonPath("$.result.comment").value("최고의 영화!"));
    }

    @Test
    @DisplayName("같은 영화에 평점을 다시 등록하면 409와 RATING409_1을 응답한다")
    void createRatingWhenDuplicated() throws Exception {
        ratingRepository.save(Rating.builder().member(member).movieId(550L).score(4).comment("처음").build());

        mockMvc.perform(post("/ratings")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRatingRequest(550L, 5, "다시"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RATING409_1"));
    }

    @Test
    @DisplayName("별점이 1~5를 벗어나면 400이고 저장하지 않는다")
    void createRatingWithInvalidScore() throws Exception {
        mockMvc.perform(post("/ratings")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRatingRequest(550L, 6, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));

        org.assertj.core.api.Assertions.assertThat(ratingRepository.count()).isZero();
    }

    @Test
    @DisplayName("남의 평점을 수정하려 하면 403과 RATING403_1을 응답하고 값은 그대로다")
    void updateOthersRating() throws Exception {
        Member other = memberRepository.save(Member.builder()
                .email("other@umc.com").password("x").nickname("다른사람").build());
        Rating othersRating = ratingRepository.save(
                Rating.builder().member(other).movieId(550L).score(3).comment("남의 후기").build());

        mockMvc.perform(patch("/ratings/" + othersRating.getRatingId())
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRatingRequest(1, "고쳐 쓰기"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RATING403_1"));

        Rating unchanged = ratingRepository.findById(othersRating.getRatingId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(unchanged.getScore()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(unchanged.getComment()).isEqualTo("남의 후기");
    }

    @Test
    @DisplayName("유저별 평점 조회는 토큰 없이 열려 있고, 성공 응답은 공통 틀 없이 배열이다")
    void getMemberRatingsIsPublic() throws Exception {
        ratingRepository.save(Rating.builder().member(member).movieId(550L).score(5).comment("a").build());
        ratingRepository.save(Rating.builder().member(member).movieId(680L).score(4).comment("b").build());

        mockMvc.perform(get("/members/" + member.getMemberId() + "/ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].movieId").value(680));
    }
}
