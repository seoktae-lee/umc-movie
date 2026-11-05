package com.umc.movie.service;

import com.umc.movie.dto.LoginRequest;
import com.umc.movie.dto.LoginResponse;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AuthServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtUtil jwtUtil = new JwtUtil("test-only-secret-key-for-unit-tests-32-characters", 3600L);
    private final AuthService authService = new AuthService(memberRepository, passwordEncoder, jwtUtil);

    private Member member() {
        Member member = Member.builder().email("a@umc.com").password(passwordEncoder.encode("Umc!1234")).nickname("영화광").build();
        ReflectionTestUtils.setField(member, "memberId", 5L);
        return member;
    }

    @Test
    @DisplayName("로그인 성공: 닉네임과 해당 회원의 토큰을 돌려준다")
    void login() {
        given(memberRepository.findByEmail("a@umc.com")).willReturn(Optional.of(member()));

        LoginResponse response = authService.login(new LoginRequest("a@umc.com", "Umc!1234"));

        assertThat(response.nickname()).isEqualTo("영화광");
        assertThat(jwtUtil.getMemberId(response.accessToken())).isEqualTo(5L);
    }

    @Test
    @DisplayName("없는 이메일과 틀린 비밀번호는 코드와 문구까지 똑같다")
    void sameFailure() {
        given(memberRepository.findByEmail("none@umc.com")).willReturn(Optional.empty());
        given(memberRepository.findByEmail("a@umc.com")).willReturn(Optional.of(member()));

        ApiException noEmail = catchThrowableOfType(ApiException.class,
                () -> authService.login(new LoginRequest("none@umc.com", "Umc!1234")));
        ApiException wrongPassword = catchThrowableOfType(ApiException.class,
                () -> authService.login(new LoginRequest("a@umc.com", "틀린비밀번호")));

        assertThat(noEmail.getCode()).isEqualTo("AUTH401_1").isEqualTo(wrongPassword.getCode());
        assertThat(noEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
        assertThat(noEmail.getStatus()).isEqualTo(wrongPassword.getStatus());
    }
}
