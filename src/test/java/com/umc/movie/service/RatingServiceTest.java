package com.umc.movie.service;

import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    RatingRepository ratingRepository;

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    RatingService ratingService;

    @Test
    @DisplayName("없는 유저면 평점을 조회하지 않고 404")
    void notFound() {
        given(memberRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> ratingService.getRatingsByMember(999L, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(ratingRepository, never()).findByMember_MemberIdOrderByRatingIdDesc(any(), any());
    }

    @Test
    @DisplayName("있는 유저인데 평점이 없으면 404가 아니라 빈 배열")
    void emptyIsNotError() {
        given(memberRepository.existsById(2L)).willReturn(true);
        given(ratingRepository.findByMember_MemberIdOrderByRatingIdDesc(2L, PageRequest.of(0, 20))).willReturn(List.of());

        assertThat(ratingService.getRatingsByMember(2L, 0, 20)).isEmpty();
    }
}
