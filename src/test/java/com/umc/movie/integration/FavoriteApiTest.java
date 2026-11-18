package com.umc.movie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 즐겨찾기 API의 요청 → 응답 전체 흐름을 검증하는 통합 테스트. (9주차 실습 5)
 *
 * 단위 테스트(FavoriteServiceTest)는 "중복이면 409를 던지는가" 같은 Service의 판단을 보고,
 * 이 테스트는 URL·인증 관문·요청 검증·DB 저장·응답 모양이 **연결**됐는지를 본다.
 * 컨트롤러, 인터셉터, 검증, JPA, DB(H2)까지 전부 진짜로 돈다.
 */
@SpringBootTest        // 진짜 서버를 띄우듯 모든 Bean을 올린다
@AutoConfigureMockMvc  // HTTP 요청을 흉내 내는 MockMvc를 준비한다
@ActiveProfiles("test") // application-test.yml: 개발용 MySQL 대신 메모리 DB(H2)
@Transactional         // 테스트가 끝나면 저장한 데이터를 되돌린다
class FavoriteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // 객체를 JSON 문자열로 바꿔 준다

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Member member;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        // 로그인한 회원이 있어야 하므로 회원을 한 명 저장하고 토큰을 직접 발급한다.
        member = memberRepository.save(Member.builder()
                .email("challenger@umc.com")
                .password("$2a$10$abcdefghijklmnopqrstuv") // 로그인 API를 거치지 않으므로 아무 값이나 괜찮다
                .nickname("영화조아")
                .build());

        authorizationHeader = "Bearer " + jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
    }

    @Test
    @DisplayName("토큰 없이 즐겨찾기를 등록하면 401과 AUTH401_2를 응답한다")
    void createFavoriteWithoutToken() throws Exception {
        mockMvc.perform(post("/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFavoriteRequest(550L))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401_2"));
    }

    @Test
    @DisplayName("토큰을 담아 즐겨찾기를 등록하면 201과 저장된 영화 ID를 응답한다")
    void createFavorite() throws Exception {
        mockMvc.perform(post("/favorites")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFavoriteRequest(550L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.result.movieId").value(550));
    }

    @Test
    @DisplayName("이미 즐겨찾기한 영화를 다시 등록하면 409와 FAVORITE409_1을 응답한다")
    void createFavoriteWhenDuplicated() throws Exception {
        favoriteRepository.save(Favorite.builder().member(member).movieId(550L).build());

        mockMvc.perform(post("/favorites")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFavoriteRequest(550L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FAVORITE409_1"));
    }

    @Test
    @DisplayName("즐겨찾기하지 않은 영화의 여부를 조회하면 200과 isFavorite false를 응답한다")
    void getFavoriteStatus() throws Exception {
        // "데이터가 없는 상태"는 오류가 아니라 정상이다. 404로 바꾸면 이 테스트가 먼저 알려 준다.
        mockMvc.perform(get("/favorites/550")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isFavorite").value(false));
    }

    @Test
    @DisplayName("Swagger 문서 경로는 토큰 없이도 열려 있다")
    void apiDocsIsPublic() throws Exception {
        // 인터셉터의 "일단 다 잠그고 열 경로만 연다" 규칙 때문에 새로 생긴 Swagger 주소도 잠겨 있었다.
        // 예외 목록을 누가 건드리면 이 테스트가 먼저 알려 준다.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 즐겨찾기 목록은 내 것만 최신순으로, 없으면 오류가 아니라 빈 배열이다")
    void listOnlyMyFavorites() throws Exception {
        mockMvc.perform(get("/favorites").header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));

        Member other = memberRepository.save(Member.builder()
                .email("other@umc.com").password("x").nickname("다른사람").build());
        favoriteRepository.save(Favorite.builder().member(member).movieId(550L).build());
        favoriteRepository.save(Favorite.builder().member(member).movieId(680L).build());
        favoriteRepository.save(Favorite.builder().member(other).movieId(13L).build());

        mockMvc.perform(get("/favorites").header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].movieId").value(680))
                .andExpect(jsonPath("$.result[1].movieId").value(550));
    }

    @Test
    @DisplayName("즐겨찾기를 해제하면 200과 result null, 한 번 더 해제하면 404 FAVORITE404_1")
    void deleteFavorite() throws Exception {
        favoriteRepository.save(Favorite.builder().member(member).movieId(550L).build());

        mockMvc.perform(delete("/favorites/550").header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(nullValue()));

        mockMvc.perform(delete("/favorites/550").header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE404_1"));
    }
}
