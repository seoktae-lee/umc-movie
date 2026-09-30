package com.umc.movie.service;

import com.umc.movie.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    MemberService memberService;

    @Test
    @DisplayName("exists=true를 available=false로 뒤집는다")
    void invert() {
        given(memberRepository.existsByNickname("수현")).willReturn(true);
        given(memberRepository.existsByEmail("new@umc.com")).willReturn(false);

        assertThat(memberService.checkNickname("수현").available()).isFalse();
        assertThat(memberService.checkEmail("new@umc.com").available()).isTrue();
    }
}
