package com.umc.movie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해시 도구를 Bean으로 등록한다.
 * BCrypt는 해시할 때마다 salt를 새로 만들어 넣으므로, 같은 비밀번호도 매번 다른 60자 문자열이 된다.
 * 기본 strength(salt round)는 10 → 2^10번 반복해 무차별 대입을 느리게 만든다.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
