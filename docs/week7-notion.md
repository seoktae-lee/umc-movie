# 7주차 Backend 노션 제출본 (원고)

> 노션 7주차 Backend 페이지에 위에서부터 붙여넣는 원고. 워크북 순서 그대로다.
> `🖼️` 표시 자리에 `docs/images/week7/` 이미지를 넣는다. **토글 안에 넣지 말 것.**
> 캡처는 Postman 화면이 아니라, 실제 서버에 보낸 요청·응답 기록(newman JSON)을 6주차와 같은 카드로 렌더링한 것이다. 토큰은 표시만 줄였다.

## 📌 미션 완료 정보

| 항목 | 내용 |
| --- | --- |
| 이름 / 닉네임 | 이석태 / 태이 |
| GitHub 저장소 | https://github.com/seoktae-lee/umc-movie |
| Pull Request | (push 후 추가) |
| 브랜치 | `feature/week-7` |
| 스택 | Spring Boot 3.5 · Java 21 · MySQL |
| 검증 | 단위·슬라이스 테스트 50개 통과 / Postman 컬렉션 31요청, 검증 82개 통과 |

## 🎯 핵심 키워드

- **인증과 인가** — 인증은 "너 누구야?"(로그인 때 이메일과 비밀번호로 확인), 인가는 "너 이거 해도 돼?"(요청마다 확인)다. 우리 서비스의 인가는 등급이 아니라 "이 평점이 네 것이냐" 하나다.
- **401과 403** — 401은 누군지 모르겠다는 뜻(토큰 없음·위조·만료)이고 403은 누군지는 알지만 네 것이 아니라는 뜻이다. 이름과 달리 401이 인증 실패, 403이 인가 실패다.
- **세션과 토큰** — 세션은 서버가 로그인 상태를 보관하고 번호표만 주는 방식이고, 토큰은 필요한 정보를 토큰 자체에 담아 손님이 들고 다니는 방식이다. 토큰은 서버가 기억할 것이 없어 서버를 늘리기 쉽지만, 한번 발급하면 취소하기 어렵고 내용이 누구에게나 보인다.
- **Authorization 헤더와 Bearer** — 토큰은 "무엇을"이 아니라 "누가"에 대한 정보라 봉투 겉면인 헤더에 싣는다. GET과 DELETE는 바디를 쓰지 않는 게 관례라 바디로는 보낼 수 없다. 형식은 `Authorization: Bearer {토큰}`이고 Bearer 뒤에 띄어쓰기가 한 칸 있다.
- **JWT 구조** — header.payload.signature 세 덩어리다. header와 payload는 Base64URL 인코딩일 뿐 암호화가 아니므로 비밀번호 같은 값을 넣으면 안 된다.
- **서명 검증과 HMAC** — 서버는 받은 header와 payload를 자기 비밀키로 다시 서명해 토큰의 signature와 비교한다. 내용을 바꾸면 값이 달라지고, 비밀키를 모르면 맞는 서명을 만들 수 없어 위조가 막힌다. 서명이 지키는 것은 위조 방지이지 기밀성이 아니다. 내 키는 48바이트가 넘어 jjwt가 HS384를 골랐다.
- **bcrypt와 salt 비교** — 해시는 되돌릴 수 없어서 입력값을 같은 방식으로 해시해 비교한다. salt가 해시 문자열 안에 들어 있어 `matches()`가 꺼내 쓴다. 그래서 같은 비밀번호도 저장값이 달라지는데 비교는 가능하다.
- **HandlerInterceptor와 Guard** — 컨트롤러 앞에서 요청을 들여보낼지 정하는 공통 관문이다. 로비 게이트 하나로 거르면 API마다 검사 코드를 복사하지 않아도 된다. Spring은 전부 잠그고 열 곳을 나열할 수 있어 깜빡해도 안전한 쪽으로 기운다. NestJS의 Guard는 필요한 곳에 하나씩 붙인다. (나는 Spring으로 구현)
- **소유권 검증** — 토큰에서 꺼낸 memberId와 평점의 주인을 비교한다. 요청에서 memberId를 받지 않는 것이 곧 그 값을 조작당하지 않는 방법이다.

## 💪 실습 기록

### API 명세

| API | Method | URL | 토큰 | 성공 | 대표 실패 |
| --- | --- | --- | --- | --- | --- |
| 로그인 | POST | /auth/login | 불필요 | 200 nickname, accessToken | 401 `AUTH401_1` / 400 `COMMON4001` |
| 내 정보 조회 | GET | /members/me | 필요 | 200 email, nickname, createdAt | 401 `AUTH401_2·3·4` / 404 `MEMBER404_1` |
| 내 정보 수정 | PATCH | /members/me | 필요 | 200 조회와 동일 | 409 `MEMBER409_1` / 400 `COMMON4001` |
| 평점 등록 | POST | /ratings | 필요 | 201 평점 | 409 `RATING409_1` / 400 `COMMON4001` |
| 평점 수정 | PATCH | /ratings/{ratingId} | 필요 | 200 평점 | 403 `RATING403_1` / 404 `RATING404_1` |
| 평점 삭제 | DELETE | /ratings/{ratingId} | 필요 | 200 result null | 403 `RATING403_1` / 404 `RATING404_1` |
| 유저별 평점 조회 | GET | /members/{memberId}/ratings | 불필요 | 200 평점 배열 | 404 `MEMBER404_1` |

### 실습 1. 로그인 API

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

🖼️ `01-login-success.png`
로그인 성공: 200과 함께 nickname, accessToken을 돌려준다.

🖼️ `02-login-fail.png`
비밀번호 틀림과 없는 이메일의 응답 본문이 완전히 같다. 컬렉션에서 두 본문을 직접 비교해 확인했다.

🖼️ `03-jwt-decoded.png`
비밀키 없이 Base64URL 디코딩만으로 payload가 보인다. 알고리즘은 HS384, 비밀번호는 들어 있지 않다.

### 실습 2. 토큰을 검사하는 관문

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

```java
registry.addInterceptor(authInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/auth/**", "/members", "/members/nickname/**",
                "/members/email/**", "/members/*/ratings", "/error");
```

🖼️ `12-old-url-locked.png`
없는 경로도 토큰이 없으면 401이다. 전부 잠금이 동작한다는 뜻이다. 6주차 등록 URL은 405로 막힌다.

🖼️ `13-preflight.png`
토큰이 필요한 경로의 preflight가 토큰 없이 200으로 통과하고 authorization 헤더를 허용한다.

### 실습 3. 내 정보 조회

왜 이렇게 했나: URL에 누구인지 적을 칸이 없다. 관문이 담아 둔 값만 꺼내므로 컨트롤러는 토큰을 몰라도 된다. 엔티티를 그대로 내보내면 password까지 나가므로 DTO로 옮겼다.

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(
        @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId) {
    MemberInfoResponse response = memberService.getMyInfo(memberId);
    return ResponseEntity.ok(ApiResponse.ok("회원 정보를 조회했습니다.", response));
}
```

🖼️ `04-me-token.png`
헤더 없음 401 `AUTH401_2`, 정상 토큰 200, payload 한 글자를 바꾼 토큰 401 `AUTH401_3`, 서명을 뺀 alg none 토큰 401 `AUTH401_3`.

**정상 조작으로 안 나오는 오류 재현 방법**

- **토큰 만료 `AUTH401_4`**: 정상 조작으로는 1시간을 기다려야 한다. 그래서 `JWT_ACCESS_EXPIRATION=1 ./gradlew bootRun`으로 유효시간을 1초로 줄여 서버를 띄우고 로그인한 뒤, 같은 토큰을 발급 3초, 30초, 65초 후에 보냈다. 시계 오차 여유 60초 때문에 앞의 둘은 200이고 65초 후에 401이 났다.

🖼️ `05-me-expired.png`
같은 토큰이 3초 후와 30초 후에는 200, 65초 후에는 401 `AUTH401_4`.

- **토큰은 유효한데 회원이 없음 `MEMBER404_1`**: 이번 주차에는 탈퇴 API가 없어 정상 조작으로는 나오지 않는다. 회원가입과 로그인 후 `DELETE FROM member WHERE member_id = {그 회원}`으로 DB에서 직접 지우고 같은 토큰으로 호출했다.

🖼️ `06-me-member-deleted.png`
삭제 전 200, 삭제 후 조회와 평점 등록 모두 404 `MEMBER404_1`. 토큰은 서버가 기억하지 않아 회원이 사라져도 검증은 통과한다.

### 실습 4. 내 정보 수정

왜 이렇게 했나: 내 닉네임을 그대로 보냈을 때 중복으로 막으면 곤란하다. 그래서 나를 뺀 회원 중에서만 찾는다. 동시에 들어와 DB UNIQUE에 막혀도 같은 코드로 응답하도록 UPDATE를 서비스 안에서 내보낸다.

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
수정 성공 200, 내 닉네임 그대로 200, 다른 회원의 닉네임 409 `MEMBER409_1`, 한 글자 닉네임 400.

**재현 방법**: 정상 조작으로는 두 요청이 같은 순간에 들어가기 어렵다. 회원 두 명을 만든 뒤 `python3 scripts/concurrent_nickname.py 이메일A 비밀번호A 이메일B 비밀번호B`로 스레드 두 개를 Barrier에서 동시에 출발시켰다. 10라운드를 반복했다.

🖼️ `14-nickname-race.png`
동시 수정 10라운드: 200이 10개, 409 `MEMBER409_1`이 10개. 라운드마다 한 명만 성공한다.

### 실습 5. 평점 CRUD 토큰 기반 리팩토링

왜 이렇게 했나: URL에 memberId가 남아 있으면 클라이언트가 바꿔 보낼 수 있다. 값을 받지 않고 토큰에서 알아낸다. 삭제는 204면 본문을 보낼 수 없어 공통 형식을 지키려고 200으로 바꿨다. 조회는 공개 기능이라 그대로 뒀다.

```java
@PostMapping("/ratings")
public ResponseEntity<ApiResponse<RatingResponse>> create(
        @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
        @Valid @RequestBody CreateRatingRequest request) {
    RatingResponse response = ratingService.create(memberId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("평점 등록에 성공했습니다.", response));
}
```

```java
// 먼저 찾고(없으면 404), 찾았으면 주인을 본다(내 것이 아니면 403).
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

🖼️ `08-rating-create.png`
토큰 없음 401, 등록 성공 201, 같은 영화 409 `RATING409_1`, 별점 6 400.

🖼️ `09-rating-update.png`
수정 200 후 토큰 없이 공개 조회. 수정 응답의 updatedAt과 조회 값이 같다.

🖼️ `10-rating-forbidden.png`
다른 회원의 토큰으로 수정·삭제 403 `RATING403_1`, 없는 평점 404, 토큰 없음 401.

🖼️ `11-rating-delete.png`
삭제 200 + result null, 다시 삭제 404, 목록은 빈 배열.

🖼️ `15-newman-summary.png`
컬렉션 31개 요청과 검증 82개 전체 결과.

### 워크북과 다르게 한 점

- 6주차 `ErrorCode`와 `GeneralException`은 회원가입과 DB 제약 변환에 남기고, 이번 주 실패만 워크북의 `ApiException`과 코드표를 썼다. 그래서 닉네임 중복은 회원가입에서 `MEMBER4092`, 내 정보 수정에서 `MEMBER409_1`이다.
- 워크북은 만능 핸들러를 달지 말라고 하지만 6주차 코드에 이미 있었다. 내부 메시지를 숨기는 역할은 필요해서 남기고, 스프링이 상태 코드를 아는 예외는 그 코드를 쓰게 고쳤다. (트러블슈팅 No.1)
- 응답과 DB의 시각을 맞추려고 `SourceType.DB`와 `flush()`를 추가했다. (트러블슈팅 No.2·3)
- 내 정보 수정은 `save()` 대신 `@Transactional`과 `flush()`를 썼다. (트러블슈팅 No.4)
- 유저별 평점 조회의 페이지네이션(선택 미션)과 `application.yml` 파일 이름은 기존 그대로다. `.env`는 `spring.config.import`로 읽는다.

## 💪 미션 / ✅ 체크리스트

(워크북 뒷부분을 받은 뒤 채움)

## ⚡ 트러블 슈팅

- **No.1 없는 경로와 메서드가 전부 500** — 만능 핸들러가 스프링의 404·405를 덮었다. `ErrorResponse` 예외는 원래 상태 코드를 쓰게 고쳤다.
- **No.2 수정 응답의 updatedAt이 옛 값** — UPDATE가 응답을 만든 뒤에 나갔다. `flush()`를 먼저 호출했다.
- **No.3 등록 응답의 시각이 DB와 다름** — JVM 시각의 소수 초와 DATETIME 반올림 차이다. `SourceType.DB`로 통일했다.
- **No.4 동시 닉네임 수정의 code가 다름** — 커밋 때 난 DB 제약 위반이 회원가입 규칙으로 변환됐다. 서비스 안에서 flush하고 `MEMBER409_1`로 바꿨다.
- **No.5 유효시간 1초인데 바로 만료되지 않음** — 시계 오차 여유 60초 때문이다. 65초 후 `AUTH401_4`를 확인했다.

자세한 내용: 저장소 `docs/week7-troubleshooting.md`

## 📢 학습 후기 (초안)

이번 주에는 서버가 요청을 보낸 사람을 스스로 판단하게 됐다. 요청에서 memberId를 받지 않는 것만으로 조작할 칸 자체가 사라지는 점이 가장 인상적이었다.

정상 흐름보다 경계 상황에서 배운 것이 많았다. 6주차부터 있던 만능 핸들러가 404와 405를 500으로 덮고 있었고, 응답의 시각이 DB와 다른 것도 직접 비교해 보고서야 알았다.

아직 더 공부할 부분은 토큰을 발급한 뒤 취소하기 어렵다는 문제와 Refresh Token이다. 없는 이메일과 틀린 비밀번호의 응답 본문은 같아도 응답 시간은 약 1ms와 약 64ms로 차이가 나서, 이 차이를 어떻게 다룰지도 찾아보고 싶다.
