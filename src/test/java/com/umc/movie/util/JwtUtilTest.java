package com.umc.movie.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-only-secret-key-for-unit-tests-32-characters";

    @Test
    @DisplayName("발급한 토큰에서 memberId를 그대로 꺼낸다")
    void roundTrip() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600L);

        assertThat(jwtUtil.getMemberId(jwtUtil.createAccessToken(5L, "a@umc.com"))).isEqualTo(5L);
    }

    @Test
    @DisplayName("다른 비밀키로 서명한 토큰은 AUTH401_3")
    void otherKey() {
        String token = new JwtUtil("another-secret-key-with-at-least-32-characters", 3600L).createAccessToken(5L, "a@umc.com");

        assertThatThrownBy(() -> new JwtUtil(SECRET, 3600L).getMemberId(token))
                .extracting("code").isEqualTo("AUTH401_3");
    }

    @Test
    @DisplayName("시계 오차 여유 60초까지 지난 토큰은 AUTH401_4")
    void expired() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -61L);

        assertThatThrownBy(() -> jwtUtil.getMemberId(jwtUtil.createAccessToken(5L, "a@umc.com")))
                .extracting("code").isEqualTo("AUTH401_4");
    }

    @Test
    @DisplayName("빈 문자열과 토큰이 아닌 문자열은 AUTH401_3")
    void malformed() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 3600L);

        assertThatThrownBy(() -> jwtUtil.getMemberId("")).extracting("code").isEqualTo("AUTH401_3");
        assertThatThrownBy(() -> jwtUtil.getMemberId("abc.def.ghi")).extracting("code").isEqualTo("AUTH401_3");
    }
}
