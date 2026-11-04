package com.umc.movie.service;

import com.umc.movie.dto.EmailCheckResponse;
import com.umc.movie.dto.MemberInfoResponse;
import com.umc.movie.dto.MemberResponse;
import com.umc.movie.dto.MemberUpdateRequest;
import com.umc.movie.dto.NicknameCheckResponse;
import com.umc.movie.dto.SignUpRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ApiException;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.exception.GeneralException;
import com.umc.movie.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        boolean exists = memberRepository.existsByNickname(nickname);
        // "이미 있다(exists)"를 "쓸 수 있다(available)"로 뒤집어 화면이 바로 쓰기 좋게 만든다.
        return new NicknameCheckResponse(!exists);
    }

    @Transactional(readOnly = true)
    public EmailCheckResponse checkEmail(String email) {
        boolean exists = memberRepository.existsByEmail(email);
        return new EmailCheckResponse(!exists);
    }

    /**
     * 회원가입. (저장 API의 두 번째 문: 서비스 검증)
     * 1) 이메일·닉네임 중복 확인 → 각각 다른 code로 409
     * 2) 비밀번호를 bcrypt로 해시
     * 3) 저장
     * 4) password를 뺀 응답 DTO 반환
     *
     * 1)을 통과해도 동시에 같은 값으로 가입하면 DB UNIQUE가 마지막으로 막는다. (세 번째 문)
     * 그 경우는 GlobalExceptionHandler가 DataIntegrityViolationException을 같은 409로 바꾼다.
     */
    @Transactional
    public MemberResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new GeneralException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new GeneralException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        // IDENTITY 전략이라 save() 순간 INSERT가 나간다. UNIQUE 위반도 여기서 바로 터진다.
        Member savedMember = memberRepository.save(member);
        return MemberResponse.from(savedMember);
    }

    @Transactional(readOnly = true)
    public MemberInfoResponse getMyInfo(Long memberId) {
        return MemberInfoResponse.from(findMember(memberId));
    }

    @Transactional
    public MemberInfoResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = findMember(memberId);

        // 나를 제외한 다른 회원이 그 닉네임을 쓰고 있다면 막는다. (내 닉네임 그대로 보내는 것은 통과)
        // DB에도 UNIQUE 제약이 걸려 있지만, 제약 위반 예외 대신 우리가 정한 응답을 주려고 먼저 확인한다.
        if (memberRepository.existsByNicknameAndMemberIdNot(request.nickname(), memberId)) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMBER409_1", "이미 사용 중인 닉네임입니다.");
        }

        member.updateNickname(request.nickname());
        try {
            // 위 검사를 동시에 통과한 요청은 DB UNIQUE가 막는다. 커밋까지 미루면 회원가입용 code로 나가므로
            // 여기서 UPDATE를 내보내 사전 검사와 같은 code로 바꾼다.
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMBER409_1", "이미 사용 중인 닉네임입니다.");
        }
        return MemberInfoResponse.from(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER404_1",
                        "해당 회원을 찾을 수 없습니다."));
    }
}
