package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.EmailCheckResponse;
import com.umc.movie.dto.MemberInfoResponse;
import com.umc.movie.dto.MemberResponse;
import com.umc.movie.dto.MemberUpdateRequest;
import com.umc.movie.dto.NicknameCheckResponse;
import com.umc.movie.dto.SignUpRequest;
import com.umc.movie.service.MemberService;
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
public class MemberController {

    private final MemberService memberService;

    /**
     * 닉네임 중복 체크. (선택 미션) 2~12자가 아니면 DB를 보기 전에 400으로 응답한다.
     * Spring 6.1부터는 @PathVariable에 붙인 제약을 컨트롤러가 직접 검사한다.
     */
    @GetMapping("/nickname/{nickname}")
    public NicknameCheckResponse checkNickname(
            @PathVariable @Size(min = 2, max = 12, message = "닉네임은 2~12자여야 합니다.") String nickname) {
        return memberService.checkNickname(nickname);
    }

    /** 이메일 중복 체크. (선택 미션) 이메일 형식이 아니면 400. */
    @GetMapping("/email/{email}")
    public EmailCheckResponse checkEmail(
            @PathVariable @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "이메일 형식이 아닙니다.") String email) {
        return memberService.checkEmail(email);
    }

    /**
     * 회원가입. @Valid가 SignUpRequest의 검증 애노테이션을 실행한다.
     * 실패하면 이 메서드에 들어오지도 않고 GlobalExceptionHandler가 400으로 응답한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        MemberResponse response = memberService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입에 성공했습니다.", response));
    }

    /** 내 정보 조회. URL에 누구인지 없다. @LoginMember가 토큰 주인의 memberId를 넣어 준다. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(
            @LoginMember Long memberId) {
        MemberInfoResponse response = memberService.getMyInfo(memberId);
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 조회했습니다.", response));
    }

    /** 내 정보(닉네임) 수정. 다른 회원이 쓰는 닉네임이면 409, 내 닉네임 그대로면 통과. */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMyInfo(
            @LoginMember Long memberId,
            @RequestBody @Valid MemberUpdateRequest request) {
        MemberInfoResponse response = memberService.updateMyInfo(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 수정했습니다.", response));
    }
}
