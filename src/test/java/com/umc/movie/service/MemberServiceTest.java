package com.umc.movie.service;

import com.umc.movie.dto.MemberResponse;
import com.umc.movie.dto.SignUpRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    // 진짜 BCrypt를 써서 해시 결과까지 확인한다.
    @Spy
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    private final SignUpRequest request = new SignUpRequest("new@umc.com", "Umc!1234", "태이");

    @Test
    @DisplayName("회원가입: 비밀번호는 평문이 아니라 60자 bcrypt 해시로 저장하고, 응답에는 넣지 않는다")
    void signUpHashesPassword() {
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        MemberResponse response = memberService.signUp(request);

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(saved.capture());
        String hash = saved.getValue().getPassword();
        assertThat(hash).isNotEqualTo("Umc!1234").hasSize(60).startsWith("$2a$10$");
        assertThat(passwordEncoder.matches("Umc!1234", hash)).isTrue();
        assertThat(response.email()).isEqualTo("new@umc.com");
        assertThat(response.nickname()).isEqualTo("태이");
    }

    @Test
    @DisplayName("회원가입: 이메일 중복과 닉네임 중복은 서로 다른 code (Challenge)")
    void duplicatedCodes() {
        given(memberRepository.existsByEmail("new@umc.com")).willReturn(true);
        assertThatThrownBy(() -> memberService.signUp(request))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);

        given(memberRepository.existsByEmail("new@umc.com")).willReturn(false);
        given(memberRepository.existsByNickname("태이")).willReturn(true);
        assertThatThrownBy(() -> memberService.signUp(request))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATED);

        verify(memberRepository, never()).save(any());
    }
}
