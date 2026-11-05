# 7주차 Backend 노션 제출본 (원고)

> 노션 "태이의 워크북 / 7주차 - 로그인한 사용자를 구분하기"에 넣는 원고. 페이지의 섹션 순서 그대로다.
> `🖼️` 자리에 `docs/images/week7/` 이미지를 넣는다. **토글 안에 넣지 말 것.** 키워드도 토글 아래 본문에 적는다.

## 🎯 핵심 키워드

**인증(Authentication)과 인가(Authorization)** — 인증은 "너 누구야?"를 확인하는 일로, 로그인할 때 이메일과 비밀번호로 한 번 한다. 인가는 "너 이거 해도 돼?"를 확인하는 일로, 요청할 때마다 한다. 콘서트장에 비유하면 신분증 확인이 인증이고 팔찌 색 확인이 인가다. 내 구현에서는 인증을 인터셉터가, 인가(이 평점이 네 것인가)를 서비스가 맡는다. 평점의 주인은 DB를 봐야 알 수 있는 기능별 규칙이기 때문이다.

**해시와 salt, bcrypt** — 해시는 되돌릴 수 없어서, 입력값을 같은 방식으로 해시한 결과끼리 비교한다. salt는 해시할 때 섞는 무작위 값으로, 같은 비밀번호도 저장값이 달라지게 해 미리 계산한 사전 공격을 막는다. `$2a$10$` 뒤 22자가 salt이고 해시 문자열 안에 함께 저장되므로 `matches()`가 꺼내 비교할 수 있다. 내 서버에서도 `a@umc.com`의 비밀번호가 `$2a$10$`로 시작하는 60자로 저장된 것을 확인했다.

**JWT (Header / Payload / Signature, Claim)** — 점으로 나뉜 세 덩어리 문자열이다. Header에는 알고리즘, Payload에는 Claim(토큰에 담긴 정보. sub, email, iat, exp), Signature에는 앞의 두 덩어리를 비밀키로 서명한 값이 들어 있다. Header와 Payload는 Base64URL 인코딩일 뿐 암호화가 아니므로 비밀번호를 넣으면 안 된다. 내용을 바꾸면 서명이 맞지 않아 위조가 막힌다. 서명이 지키는 것은 위조 방지이지 기밀성이 아니다. 내 키는 48바이트가 넘어 jjwt가 HS384를 골랐다.

**Bearer 인증과 Authorization 헤더** — 토큰은 "무엇을"이 아니라 "누가"에 대한 정보라 바디가 아닌 헤더에 싣는다. GET과 DELETE는 바디를 쓰지 않는 게 관례이지만 헤더는 모든 메서드에 붙일 수 있다. 형식은 `Authorization: Bearer {토큰}`이고, Bearer는 "이 토큰을 지참한 사람"이라는 뜻이며 뒤에 띄어쓰기가 한 칸 있다. 이 형식이 아니면 토큰이 없는 것으로 보고 `AUTH401_2`로 응답한다.

**HandlerInterceptor / Guard** — 컨트롤러 앞에서 요청을 들여보낼지 정하는 공통 관문이다. Spring의 HandlerInterceptor는 `preHandle`에서 토큰을 검증하고 memberId를 요청에 담는다. 경로로 등록하므로 전부 잠근 뒤 열 곳만 나열할 수 있어, 새 API에서 깜빡해도 잠긴 채로 남는다. NestJS의 Guard는 `@UseGuards`를 붙인 곳에만 적용된다. (나는 Spring으로 구현)

**401 Unauthorized와 403 Forbidden의 차이** — 401은 누군지 모르겠다는 뜻(토큰 없음, 위조, 만료)이고 403은 누군지는 알지만 네 것이 아니라는 뜻이다. 이름과 달리 401이 인증 실패, 403이 인가 실패다. 같은 수정 요청을 토큰 B로 보내면 403, 토큰 없이 보내면 401이 나오는 것을 직접 확인했다. 프론트도 401이면 로그인 화면으로, 403이면 권한 안내로 다르게 처리한다.

## 📢 학습 후기

(초안) 이번 주에는 서버가 요청을 보낸 사람을 스스로 판단하게 됐다. 요청에서 memberId를 받지 않는 것만으로 조작할 칸이 사라지는 점이 가장 인상적이었다.

정상 흐름보다 경계 상황에서 배운 것이 많았다. 6주차부터 있던 만능 핸들러가 404와 405를 500으로 덮고 있었고, 유효시간을 1분으로 줄인 토큰이 1분 뒤에도 통과하는 것을 보고 시계 오차 여유를 이해했다.

더 공부할 부분은 토큰을 발급한 뒤 취소하기 어렵다는 문제와 Refresh Token이다. 없는 이메일과 틀린 비밀번호의 응답 본문은 같아도 응답 시간은 약 1ms와 약 65ms로 달라서, 이 차이를 어떻게 다룰지도 찾아보고 싶다.

## ☑️ 실습 인증

**필수 미션: 전체 시나리오 9단계** — 워크북 순서대로 실행했다. 컬렉션은 34개 요청, 검증 92개 모두 통과했다.

🖼️ `16-scenario-9steps.png`

**로그인 성공 응답**: accessToken이 보이도록 전체를 표시했다.

🖼️ `01-login-success.png`

**토큰 없이 GET /members/me 호출 시 401**: 첫 번째 카드가 `AUTH401_2`다. 정상 토큰은 200, 위조 토큰은 `AUTH401_3`이다.

🖼️ `04-me-token.png`

**다른 회원의 토큰으로 평점 수정 시 403**: 토큰 B는 `RATING403_1`, 토큰 없음은 `AUTH401_2`로 code가 서로 다르다.

🖼️ `10-rating-forbidden.png`

**본인 토큰으로 평점 등록 201**: 요청 바디에 memberId가 없다.

🖼️ `08-rating-create.png`

## 💪 미션 기록

**미션 완료 정보**

| 항목 | 내용 |
| --- | --- |
| 이름 / 닉네임 | 이석태 / 태이 |
| GitHub 저장소 | https://github.com/seoktae-lee/umc-movie |
| Pull Request | https://github.com/seoktae-lee/umc-movie/pull/1 |
| 브랜치 | feature/week-7 |
| 스택 | Spring Boot 3.5 · Java 21 · MySQL |
| 검증 | 테스트 52개 통과 / Postman 컬렉션 34요청, 검증 92개 통과 |

**API 명세**

| API | Method | URL | 토큰 | 성공 | 대표 실패 |
| --- | --- | --- | --- | --- | --- |
| 로그인 | POST | /auth/login | 불필요 | 200 nickname, accessToken | 401 AUTH401_1 / 400 COMMON4001 |
| 내 정보 조회 | GET | /members/me | 필요 | 200 email, nickname, createdAt | 401 AUTH401_2·3·4 / 404 MEMBER404_1 |
| 내 정보 수정 | PATCH | /members/me | 필요 | 200 조회와 동일 | 409 MEMBER409_1 / 400 COMMON4001 |
| 평점 등록 | POST | /ratings | 필요 | 201 평점 | 409 RATING409_1 / 400 COMMON4001 |
| 평점 수정 | PATCH | /ratings/{ratingId} | 필요 | 200 평점 | 403 RATING403_1 / 404 RATING404_1 |
| 평점 삭제 | DELETE | /ratings/{ratingId} | 필요 | 200 result null | 403 RATING403_1 / 404 RATING404_1 |
| 유저별 평점 조회 | GET | /members/{memberId}/ratings | 불필요 | 200 평점 배열 | 404 MEMBER404_1 |

**실습 1. 로그인 API**

왜 이렇게 했나: 없는 이메일과 틀린 비밀번호를 나눠 알려주면 어떤 이메일이 가입됐는지 알아낼 수 있다. 그래서 두 경우 모두 같은 예외를 던진다.

```java
public LoginResponse login(LoginRequest request) {
    Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_1",
                    "이메일 또는 비밀번호가 올바르지 않습니다."));

    if (!passwordEncoder.matches(request.password(), member.getPassword())) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_1",
                "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    String accessToken = jwtUtil.createAccessToken(member.getMemberId(), member.getEmail());
    return new LoginResponse(member.getNickname(), accessToken);
}
```

비밀번호 틀림과 없는 이메일의 응답 본문이 완전히 같다. 컬렉션에서 두 본문을 직접 비교해 확인했다.

🖼️ `02-login-fail.png`

토큰을 로컬에서 디코딩한 결과다. 비밀키 없이 payload가 보이고 비밀번호는 들어 있지 않다. (jwt.io에 직접 넣어 본 캡처는 아니다)

🖼️ `03-jwt-decoded.png`

**실습 2. 토큰을 검사하는 관문**

왜 이렇게 했나: 컨트롤러마다 검사하면 하나만 빠뜨려도 그 API가 열린다. 관문 하나에 모으고, 전부 잠근 뒤 열 경로만 나열했다. preflight에는 토큰이 없으므로 OPTIONS는 통과시킨다.

```java
if (HttpMethod.OPTIONS.matches(request.getMethod())) {
    return true;
}

String header = request.getHeader(HttpHeaders.AUTHORIZATION);
if (header == null || !header.startsWith(BEARER_PREFIX)) {
    throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_2", "로그인이 필요한 요청입니다.");
}

Long memberId = jwtUtil.getMemberId(header.substring(BEARER_PREFIX.length()));
request.setAttribute(MEMBER_ID, memberId);
return true;
```

없는 경로도 토큰이 없으면 401이다. 전부 잠금이 동작한다는 뜻이다. 6주차 등록 URL은 405로 막힌다.

🖼️ `12-old-url-locked.png`

토큰이 필요한 경로의 preflight가 토큰 없이 200으로 통과하고 authorization 헤더를 허용한다.

🖼️ `13-preflight.png`

**실습 3. 내 정보 조회**

왜 이렇게 했나: URL에 누구인지 적을 칸이 없고 토큰이 알려 준다. 엔티티를 그대로 내보내면 password까지 나가므로 DTO로 옮겼다. 조회 결과는 실습 인증의 캡처에 있다.

정상 조작으로 안 나오는 오류 재현: 이번 주차에는 탈퇴 API가 없어 "토큰은 유효한데 회원이 없음"이 나오지 않는다. 그래서 회원가입과 로그인 후 `DELETE FROM member WHERE member_id = {그 회원}`으로 DB에서 직접 지우고 같은 토큰으로 호출했다. 조회와 평점 등록 모두 404 `MEMBER404_1`이다. 토큰은 서버가 기억하지 않아 회원이 사라져도 검증은 통과한다.

🖼️ `06-me-member-deleted.png`

**실습 4. 내 정보 수정**

왜 이렇게 했나: 내 닉네임을 그대로 보냈을 때 중복으로 막으면 곤란해서 나를 뺀 회원 중에서만 찾는다. 동시에 들어와 DB UNIQUE에 막혀도 같은 코드로 응답하도록 UPDATE를 서비스 안에서 내보낸다.

```java
if (memberRepository.existsByNicknameAndMemberIdNot(request.nickname(), memberId)) {
    throw new ApiException(HttpStatus.CONFLICT, "MEMBER409_1", "이미 사용 중인 닉네임입니다.");
}

member.updateNickname(request.nickname());
try {
    memberRepository.flush();
} catch (DataIntegrityViolationException e) {
    throw new ApiException(HttpStatus.CONFLICT, "MEMBER409_1", "이미 사용 중인 닉네임입니다.");
}
return MemberInfoResponse.from(member);
```

🖼️ `07-me-update.png`

정상 조작으로 안 나오는 오류 재현: 두 요청이 같은 순간에 들어가기 어려워서, 회원 두 명을 만든 뒤 `python3 scripts/concurrent_nickname.py 이메일A 비밀번호A 이메일B 비밀번호B`로 스레드 두 개를 Barrier에서 동시에 출발시켰다. 10라운드 결과는 200이 10개, 409 `MEMBER409_1`이 10개다.

🖼️ `14-nickname-race.png`

**실습 5. 평점 CRUD 토큰 기반 리팩토링**

왜 이렇게 했나: URL에 memberId가 남아 있으면 클라이언트가 바꿔 보낼 수 있다. 값을 받지 않고 토큰에서 알아낸다. 삭제는 204면 본문을 보낼 수 없어 공통 형식을 지키려고 200으로 바꿨다. 조회는 공개 기능이라 그대로 뒀다.

수정 200 후 토큰 없이 공개 조회. 수정 응답의 updatedAt과 조회 값이 같다.

🖼️ `09-rating-update.png`

삭제 200 + result null, 다시 삭제 404, 목록은 빈 배열.

🖼️ `11-rating-delete.png`

**실습 6. 401과 403을 직접 만들어 보기**

왜 이렇게 했나: 관문은 "누구인지"까지만 확인하고 "이 평점이 네 것인지"는 서비스가 DB를 보고 확인한다. 먼저 찾고(없으면 404) 찾았으면 주인을 본다(403). 순서가 반대면 없는 평점에 "권한이 없습니다"라고 답하게 된다. 결과는 실습 인증의 403 캡처에 있다.

```java
private Rating findOwnedRating(Long ratingId, Long memberId) {
    Rating rating = ratingRepository.findById(ratingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RATING404_1",
                    "해당 평점을 찾을 수 없습니다."));

    if (!rating.getMember().getMemberId().equals(memberId)) {
        throw new ApiException(HttpStatus.FORBIDDEN, "RATING403_1",
                "본인이 작성한 평점만 수정하거나 삭제할 수 있습니다.");
    }
    return rating;
}
```

컬렉션 전체 요청과 검증 결과.

🖼️ `15-newman-summary.png`

**선택 미션 1. 토큰 만료를 직접 겪어 보기**

재현 방법: `JWT_ACCESS_EXPIRATION=60 ./gradlew bootRun`으로 재시작하고 로그인한 뒤 같은 토큰을 시간차로 보냈다. 워크북은 1분을 기다리라고 하지만 시계 오차 여유 60초 때문에 발급 65초 후에도 200이었고, 발급 125초 후에 401 `AUTH401_4`가 나왔다. 토큰이 없을 때의 `AUTH401_2`와 code가 구분된다.

🖼️ `05-me-expired.png`

**선택 미션 2. @LoginMember로 memberId 꺼내기**

컨트롤러에서 `AuthInterceptor`와 문자열 memberId가 사라졌다. 이름만 봐도 로그인한 사용자에게서 온 값임이 드러난다.

```java
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object memberId = webRequest.getAttribute(AuthInterceptor.MEMBER_ID, RequestAttributes.SCOPE_REQUEST);
        if (memberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_2", "로그인이 필요한 요청입니다.");
        }
        return memberId;
    }
}
```

🖼️ `17-login-member.png`

**워크북과 다르게 한 점**

- 6주차 `ErrorCode`와 `GeneralException`은 회원가입과 DB 제약 변환에 남기고, 이번 주 실패만 워크북의 `ApiException`과 코드표를 썼다. 그래서 닉네임 중복은 회원가입에서 `MEMBER4092`, 내 정보 수정에서 `MEMBER409_1`이다.

- 워크북은 만능 핸들러를 달지 말라고 하지만 6주차 코드에 이미 있었다. 내부 메시지를 숨기는 역할은 필요해서 남기고, 스프링이 상태 코드를 아는 예외는 그 코드를 쓰게 고쳤다.

- 응답과 DB의 시각을 맞추려고 `SourceType.DB`와 `flush()`를 추가했다.

- 유저별 평점 조회의 페이지네이션(선택 미션)과 `application.yml` 파일 이름은 기존 그대로다. `.env`는 `spring.config.import`로 읽는다.

**캡처 안내**: Postman 화면이 아니라, 실제 서버에 보낸 요청과 응답 기록(newman JSON)을 6주차와 같은 카드로 렌더링했다. 로그인 성공 캡처 외에는 토큰을 줄여 표시했다.

## ⚡ 트러블 슈팅

**No.1 없는 경로와 허용하지 않는 메서드가 전부 500으로 나옴**

이슈: 6주차 URL인 `POST /members/3/ratings`를 보내자 405가 아니라 500 `COMMON500`이 돌아왔다. 토큰을 넣은 `GET /nope`와 `PUT /members/me`도 500이었다.

문제: 6주차에 만든 `@ExceptionHandler(Exception.class)`가 스프링의 `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`까지 잡아 500으로 덮었다.

해결: 핸들러는 남기되 `ErrorResponse`를 구현한 예외는 그 상태 코드를 그대로 쓰게 했다. 이제 405 `COMMON405`, 404 `COMMON404`로 응답한다.

**No.2 평점 수정 응답의 updatedAt이 수정 전 값으로 나옴**

이슈: 수정 응답의 `updatedAt`은 19:46:28인데 조회와 DB 값은 19:46:30이었다.

문제: UPDATE는 트랜잭션이 끝날 때 나가는데 응답 DTO는 그 전에 만들어졌다.

해결: 응답을 만들기 전에 `ratingRepository.flush()`를 호출했다. 컬렉션에 두 값이 같은지 검증을 넣었다.

**No.3 평점 등록 응답의 시각이 DB에 저장된 값과 다름**

이슈: 응답의 `createdAt`은 19:46:27.95278인데 DB에는 19:46:28이 저장돼 있었다.

문제: JVM 시각을 그대로 내보냈고, DATETIME 컬럼은 소수 초를 반올림해 저장한다.

해결: `BaseTimeEntity`의 시각을 `source = SourceType.DB`로 바꿔 DB가 정한 값을 다시 읽게 했다.

**No.4 같은 닉네임으로 동시에 수정하면 다른 code가 나옴**

이슈: 동시 수정 10라운드에서 409 `MEMBER409_1`이 아니라 회원가입용 `MEMBER4092`가 10개 나왔다.

문제: UPDATE가 커밋 때 나가 서비스 밖에서 DB 제약 위반이 났고, 전역 핸들러가 회원가입 규칙으로 변환했다.

해결: 서비스 안에서 `flush()`하고 `DataIntegrityViolationException`을 `MEMBER409_1`로 바꿨다.

**No.5 유효시간을 60초로 줄였는데 1분 뒤에도 토큰이 통과함**

이슈: 워크북대로 `JWT_ACCESS_EXPIRATION=60`으로 줄였는데 발급 65초 후에도 200이었다.

문제: `clockSkewSeconds(60)` 때문에 만료 후 60초까지 유효로 본다. 버그가 아니라 의도된 동작이다.

해결: 발급 125초 후에 같은 토큰을 보내 401 `AUTH401_4`를 확인했다. 실제 사용 가능 시간은 유효시간 + 60초다.

참고레퍼런스: https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4 / https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/ErrorResponse.html / https://docs.jboss.org/hibernate/orm/6.6/javadocs/org/hibernate/annotations/SourceType.html
