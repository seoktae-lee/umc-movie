package com.umc.movie.resolver;

import com.umc.movie.interceptor.AuthInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginMemberArgumentResolverTest {

    private final LoginMemberArgumentResolver resolver = new LoginMemberArgumentResolver();

    @Test
    @DisplayName("인터셉터가 담아 둔 memberId를 꺼낸다")
    void resolve() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.MEMBER_ID, 5L);

        assertThat(resolver.resolveArgument(null, null, new ServletWebRequest(request), null)).isEqualTo(5L);
    }

    @Test
    @DisplayName("관문이 열린 경로라 값이 없으면 null을 넘기지 않고 AUTH401_2")
    void noMemberId() {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, request, null))
                .extracting("code").isEqualTo("AUTH401_2");
    }
}
