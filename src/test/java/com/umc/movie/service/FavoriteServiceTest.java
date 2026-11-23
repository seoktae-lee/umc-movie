package com.umc.movie.service;

import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    FavoriteRepository favoriteRepository;

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    FavoriteService favoriteService;

    private Member member() {
        Member member = Member.builder().email("a@umc.com").password("x").nickname("태이").build();
        ReflectionTestUtils.setField(member, "memberId", 1L);
        return member;
    }

    @Test
    @DisplayName("이미 즐겨찾기한 영화면 저장하지 않고 409 FAVORITE409_1")
    void duplicated() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(favoriteRepository.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(true);

        assertThatThrownBy(() -> favoriteService.create(1L, 550L))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("FAVORITE409_1");
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록하면 토큰의 회원과 영화 ID로 한 행을 저장한다")
    void create() {
        Member member = member();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(favoriteRepository.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(false);
        given(favoriteRepository.save(any(Favorite.class))).willAnswer(call -> {
            Favorite favorite = call.getArgument(0);
            ReflectionTestUtils.setField(favorite, "favoriteId", 9L);
            return favorite;
        });

        FavoriteResponse response = favoriteService.create(1L, 550L);

        assertThat(response.favoriteId()).isEqualTo(9L);
        assertThat(response.movieId()).isEqualTo(550L);
    }

    @Test
    @DisplayName("즐겨찾기가 하나도 없으면 오류가 아니라 빈 목록")
    void emptyList() {
        given(favoriteRepository.findByMember_MemberIdOrderByFavoriteIdDesc(1L)).willReturn(List.of());

        assertThat(favoriteService.getMyFavorites(1L)).isEmpty();
    }

    @Test
    @DisplayName("즐겨찾기하지 않은 영화의 여부 조회는 404가 아니라 isFavorite false")
    void statusFalse() {
        given(favoriteRepository.existsByMember_MemberIdAndMovieId(1L, 7L)).willReturn(false);

        assertThat(favoriteService.getStatus(1L, 7L).isFavorite()).isFalse();
    }

    @Test
    @DisplayName("없는 즐겨찾기를 해제하면 404 FAVORITE404_1")
    void deleteNotFound() {
        given(favoriteRepository.findByMember_MemberIdAndMovieId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.delete(1L, 7L))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("FAVORITE404_1");
    }

    @Test
    @DisplayName("해제하면 그 행을 삭제한다")
    void delete() {
        Favorite favorite = Favorite.builder().member(member()).movieId(550L).build();
        given(favoriteRepository.findByMember_MemberIdAndMovieId(1L, 550L)).willReturn(Optional.of(favorite));

        favoriteService.delete(1L, 550L);

        verify(favoriteRepository).delete(favorite);
    }
}
