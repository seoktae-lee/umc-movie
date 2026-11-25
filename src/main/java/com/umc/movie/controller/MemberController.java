package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.config.ApiExamples;
import com.umc.movie.config.SwaggerConfig;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.EmailCheckResponse;
import com.umc.movie.dto.MemberInfoResponse;
import com.umc.movie.dto.MemberResponse;
import com.umc.movie.dto.MemberUpdateRequest;
import com.umc.movie.dto.NicknameCheckResponse;
import com.umc.movie.dto.SignUpRequest;
import com.umc.movie.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
@Tag(name = "회원", description = "회원가입, 중복 확인, 내 정보 API. 가입·중복 확인은 공개, 내 정보(/members/me)는 토큰이 필요합니다.")
public class MemberController {

    private final MemberService memberService;

    /**
     * 닉네임 중복 체크. (선택 미션) 2~12자가 아니면 DB를 보기 전에 400으로 응답한다.
     * Spring 6.1부터는 @PathVariable에 붙인 제약을 컨트롤러가 직접 검사한다.
     */
    @Operation(
            summary = "닉네임 중복 확인",
            description = "사용할 수 있는 닉네임이면 available이 true입니다. 토큰 없이 호출하는 공개 API입니다. 2~12자가 아니면 400."
    )
    @GetMapping("/nickname/{nickname}")
    public NicknameCheckResponse checkNickname(
            @PathVariable @Size(min = 2, max = 12, message = "닉네임은 2~12자여야 합니다.") String nickname) {
        return memberService.checkNickname(nickname);
    }

    /** 이메일 중복 체크. (선택 미션) 이메일 형식이 아니면 400. */
    @Operation(
            summary = "이메일 중복 확인",
            description = "사용할 수 있는 이메일이면 available이 true입니다. 토큰 없이 호출하는 공개 API입니다. 이메일 형식이 아니면 400."
    )
    @GetMapping("/email/{email}")
    public EmailCheckResponse checkEmail(
            @PathVariable @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "이메일 형식이 아닙니다.") String email) {
        return memberService.checkEmail(email);
    }

    /**
     * 회원가입. @Valid가 SignUpRequest의 검증 애노테이션을 실행한다.
     * 실패하면 이 메서드에 들어오지도 않고 GlobalExceptionHandler가 400으로 응답한다.
     */
    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 닉네임으로 회원을 등록합니다. 토큰이 없어도 호출할 수 있는 공개 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "회원가입 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "회원가입에 성공했습니다.",
                              "result": { "memberId": 1, "email": "challenger@umc.com", "nickname": "영화조아" }
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "입력 형식 오류 (이메일 형식, 비밀번호 규칙, 닉네임 길이)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON4001",
                              "message": "비밀번호는 대문자/소문자/숫자/특수문자를 포함한 8자 이상이어야 합니다.",
                              "result": null
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 사용 중인 이메일(MEMBER4091) 또는 닉네임(MEMBER4092)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.EMAIL_DUPLICATED)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        MemberResponse response = memberService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입에 성공했습니다.", response));
    }

    /** 내 정보 조회. URL에 누구인지 없다. @LoginMember가 토큰 주인의 memberId를 넣어 준다. */
    @Operation(
            summary = "내 정보 조회",
            description = "토큰의 주인의 이메일·닉네임·가입일을 돌려줍니다. 🔒 토큰이 필요합니다. (URL에 memberId가 없습니다)"
    )
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "회원 정보를 조회했습니다.",
                              "result": { "email": "challenger@umc.com", "nickname": "영화조아", "createdAt": "2026-11-17T21:00:00" }
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나(AUTH401_2) 무효(AUTH401_3)·만료(AUTH401_4)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "토큰은 유효하지만 회원이 없음(탈퇴 등)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.MEMBER_NOT_FOUND)))
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(
            @LoginMember Long memberId) {
        MemberInfoResponse response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 조회했습니다.", response));
    }

    /** 내 정보(닉네임) 수정. 다른 회원이 쓰는 닉네임이면 409, 내 닉네임 그대로면 통과. */
    @Operation(
            summary = "내 정보(닉네임) 수정",
            description = "닉네임만 바꿀 수 있습니다. 🔒 토큰이 필요합니다. 내 닉네임 그대로면 통과하고, 다른 회원이 쓰는 닉네임이면 409입니다."
    )
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공 (result는 최신 내 정보)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "다른 회원이 이미 쓰는 닉네임",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NICKNAME_DUPLICATED)))
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMyInfo(
            @LoginMember Long memberId,
            @RequestBody @Valid MemberUpdateRequest request) {
        MemberInfoResponse response = memberService.updateMyInfo(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 수정했습니다.", response));
    }
}
