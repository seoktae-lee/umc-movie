package com.umc.movie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 로컬 MySQL(umc_movie)에 연결해 앱 전체가 뜨는지 확인한다.
 * ddl-auto: validate라서 엔티티와 테이블이 다르면 여기서 실패한다.
 */
@SpringBootTest
class MovieApplicationTests {

    @Test
    @DisplayName("엔티티와 umc_movie 테이블 구조가 맞아 앱이 뜬다")
    void contextLoads() {
    }
}
