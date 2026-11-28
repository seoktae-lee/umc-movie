package com.umc.movie.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10주차: 개발/운영 설정 분리 검증.
 * 운영 프로필이 스키마를 자동으로 바꾸지 않고, 비밀 값을 파일에 적지 않았는지 확인한다.
 */
class ProfileConfigTest {

    private PropertySource<?> load(String file) throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
        return sources.get(0);
    }

    @Test
    void 운영_프로필은_스키마를_검증만_한다() throws IOException {
        PropertySource<?> prod = load("application-prod.yml");
        assertThat(prod.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }

    @Test
    void 운영_프로필의_DB_주소와_비밀값은_환경변수로만_받는다() throws IOException {
        PropertySource<?> prod = load("application-prod.yml");
        assertThat(prod.getProperty("spring.datasource.url").toString()).contains("${DB_HOST}");
        assertThat(prod.getProperty("spring.datasource.username")).isEqualTo("${DB_USER}");
        assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(prod.getProperty("app.cors.allowed-origins")).isEqualTo("${FRONTEND_ORIGINS}");
        // 운영에서는 로컬 .env를 읽지 않는다.
        assertThat(prod.getProperty("spring.config.import")).isNull();
    }

    @Test
    void 공통_설정의_기본_프로필은_local이다() throws IOException {
        PropertySource<?> common = load("application.yml");
        assertThat(common.getProperty("spring.profiles.default")).isEqualTo("local");
        assertThat(common.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }
}
