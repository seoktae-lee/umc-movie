package com.umc.movie.controller;

import com.umc.movie.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("MySQL UNIQUE 위반 메시지를 보고 사전 검사와 같은 code로 바꾼다 (Challenge)")
    void mapConstraint() {
        assertThat(GlobalExceptionHandler.mapConstraint("Duplicate entry 'a@b.com' for key 'member.email'"))
                .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        assertThat(GlobalExceptionHandler.mapConstraint("Duplicate entry '태이' for key 'member.nickname'"))
                .isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        assertThat(GlobalExceptionHandler.mapConstraint("Duplicate entry '1-550' for key 'rating.uk_rating_member_movie'"))
                .isEqualTo(ErrorCode.RATING_DUPLICATED);
        assertThat(GlobalExceptionHandler.mapConstraint("Cannot add or update a child row"))
                .isEqualTo(ErrorCode.DATA_CONFLICT);
    }
}
