package com.umc.movie.controller;

import com.umc.movie.config.WebConfig;
import com.umc.movie.dto.EmailCheckResponse;
import com.umc.movie.dto.MemberResponse;
import com.umc.movie.dto.SignUpRequest;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.exception.GeneralException;
import com.umc.movie.dto.NicknameCheckResponse;
import com.umc.movie.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@Import(WebConfig.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    @Test
    @DisplayName("이미 있는 닉네임이면 available=false")
    void nicknameTaken() throws Exception {
        given(memberService.checkNickname("수현")).willReturn(new NicknameCheckResponse(false));

        mockMvc.perform(get("/members/nickname/{nickname}", "수현"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("점(.)이 들어간 이메일도 경로에서 잘리지 않고 그대로 전달된다")
    void emailWithDot() throws Exception {
        given(memberService.checkEmail("new@umc.com")).willReturn(new EmailCheckResponse(true));

        mockMvc.perform(get("/members/email/{email}", "new@umc.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("닉네임이 2~12자가 아니면 DB를 보기 전에 400")
    void nicknameLength() throws Exception {
        mockMvc.perform(get("/members/nickname/{nickname}", "가"))
                .andExpect(status().isBadRequest())
                // 서버 내부 메서드 이름이 아니라 우리가 적은 문구가 나간다.
                .andExpect(jsonPath("$.message").value("닉네임은 2~12자여야 합니다."));
        mockMvc.perform(get("/members/nickname/{nickname}", "열세글자닉네임입니다하하하"))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).checkNickname(anyString());
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400")
    void emailFormat() throws Exception {
        mockMvc.perform(get("/members/email/{email}", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 형식이 아닙니다."));

        verify(memberService, never()).checkEmail(anyString());
    }

    @Test
    @DisplayName("허용한 출처(5173)에서 오면 Access-Control-Allow-Origin 헤더를 붙인다")
    void corsAllowed() throws Exception {
        given(memberService.checkNickname("태이")).willReturn(new NicknameCheckResponse(true));

        mockMvc.perform(get("/members/nickname/{nickname}", "태이").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("허용하지 않은 출처는 403, 헤더 없음")
    void corsRejected() throws Exception {
        mockMvc.perform(get("/members/nickname/{nickname}", "태이").header("Origin", "http://localhost:5174"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("preflight(OPTIONS)에 허용 메서드를 알려 준다")
    void preflight() throws Exception {
        mockMvc.perform(options("/members/nickname/{nickname}", "태이")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE"));
    }

    private static final String VALID_BODY = """
            {"email": "umc@example.com", "password": "Umc!1234", "nickname": "moviefan"}
            """;

    @Test
    @DisplayName("회원가입 성공: 201 + 공통 응답, password는 응답에 없다")
    void signUpCreated() throws Exception {
        given(memberService.signUp(any(SignUpRequest.class)))
                .willReturn(new MemberResponse(3L, "umc@example.com", "moviefan"));

        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."))
                .andExpect(jsonPath("$.result.memberId").value(3))
                .andExpect(jsonPath("$.result.password").doesNotExist());
    }

    @Test
    @DisplayName("회원가입 검증 실패: 400 COMMON4001 + DTO에 적은 문구, 서비스는 호출되지 않는다")
    void signUpInvalid() throws Exception {
        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "umc@example.com", "password": "password", "nickname": "moviefan"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON4001"))
                .andExpect(jsonPath("$.message").value("비밀번호는 대문자/소문자/숫자/특수문자를 포함한 8자 이상이어야 합니다."))
                .andExpect(jsonPath("$.result").isEmpty());

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 중복: 이메일은 MEMBER4091, 닉네임은 MEMBER4092")
    void signUpDuplicated() throws Exception {
        given(memberService.signUp(any(SignUpRequest.class)))
                .willThrow(new GeneralException(ErrorCode.MEMBER_EMAIL_DUPLICATED))
                .willThrow(new GeneralException(ErrorCode.MEMBER_NICKNAME_DUPLICATED));

        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER4091"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER4092"));
    }

    @Test
    @DisplayName("JSON이 깨져 있으면 400 COMMON4002")
    void brokenJson() throws Exception {
        mockMvc.perform(post("/members").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON4002"));
    }
}
