package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.exception.GeneralException;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
                .isInstanceOf(GeneralException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(ratingRepository, never()).findByMember_MemberIdOrderByRatingIdDesc(any(), any());
    }

    @Test
    @DisplayName("있는 유저인데 평점이 없으면 404가 아니라 빈 배열")
    void emptyIsNotError() {
        given(memberRepository.existsById(2L)).willReturn(true);
        given(ratingRepository.findByMember_MemberIdOrderByRatingIdDesc(2L, PageRequest.of(0, 20))).willReturn(List.of());

        assertThat(ratingService.getRatingsByMember(2L, 0, 20)).isEmpty();
    }

    /** id가 채워진 회원. (DB 없이 테스트하므로 직접 넣는다) */
    private static Member member(long id) {
        Member member = Member.builder().email(id + "@umc.com").password("hash").nickname("회원" + id).build();
        ReflectionTestUtils.setField(member, "memberId", id);
        return member;
    }

    private static Rating rating(long ratingId, Member owner) {
        Rating rating = Rating.builder().member(owner).movieId(550L).score(3).comment("보통").build();
        ReflectionTestUtils.setField(rating, "ratingId", ratingId);
        return rating;
    }

    @Test
    @DisplayName("평점 등록: 없는 회원이면 404")
    void createMemberNotFound() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.create(999L, new CreateRatingRequest(550L, 5, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("평점 등록: 같은 영화에 이미 남겼으면 저장하지 않고 409")
    void createDuplicated() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member(1)));
        given(ratingRepository.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(true);

        assertThatThrownBy(() -> ratingService.create(1L, new CreateRatingRequest(550L, 5, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.RATING_DUPLICATED);
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("평점 수정: 본인 평점이면 값이 바뀐다")
    void updateOwn() {
        Rating rating = rating(10L, member(1));
        given(ratingRepository.findById(10L)).willReturn(Optional.of(rating));

        ratingService.update(10L, 1L, new UpdateRatingRequest(5, "다시 보니 좋다"));

        assertThat(rating.getScore()).isEqualTo(5);
        assertThat(rating.getComment()).isEqualTo("다시 보니 좋다");
    }

    @Test
    @DisplayName("평점 수정·삭제: 다른 회원의 평점이면 403, 아무것도 바꾸지 않는다")
    void forbidden() {
        Rating rating = rating(10L, member(1));
        given(ratingRepository.findById(10L)).willReturn(Optional.of(rating));

        assertThatThrownBy(() -> ratingService.update(10L, 2L, new UpdateRatingRequest(1, "남의 것")))
                .extracting("errorCode").isEqualTo(ErrorCode.RATING_FORBIDDEN);
        assertThatThrownBy(() -> ratingService.delete(10L, 2L))
                .extracting("errorCode").isEqualTo(ErrorCode.RATING_FORBIDDEN);
        assertThat(rating.getScore()).isEqualTo(3);
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("평점 수정·삭제: 없는 평점이면 404 (주인 검사보다 먼저)")
    void ratingNotFound() {
        given(ratingRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.delete(999L, 1L))
                .extracting("errorCode").isEqualTo(ErrorCode.RATING_NOT_FOUND);
    }
}
