# 6주차 Backend 노션 제출본 (원고)

> 노션 6주차 Backend 페이지("6주차 - 입력한 데이터를 저장하기")에 붙여넣는 원고. 워크북 순서 그대로다.
> `🖼️` 자리에 `docs/images/week6/` 이미지를 넣고, **사진 바로 아래 한 줄 설명을 일반 텍스트로** 둔다.
> **토글 안에 넣지 말 것.** (워크북의 「미션 기록」 토글도 펼친 채로 두거나, 토글 밖에 쓴다)

## ☑️ 실습 인증

🖼️ `01-signup-success.png`
회원가입 성공 — `POST /members` → 201 Created, `COMMON201`. result에는 memberId·email·nickname만 있고 password는 없다.

🖼️ `02-signup-validation.png`
검증 실패 — 이메일 형식 / 비밀번호 규칙 / 닉네임 길이를 어기면 400 `COMMON4001`과 DTO에 적은 문구가 그대로 온다. 서비스는 호출되지 않는다.

🖼️ `04-rating-create-update.png`
평점 등록 201(result = ratingId) → 수정 200 `COMMON200` → 5주차 조회 API로 별점 5·후기·updatedAt이 바뀐 것을 확인했다.

## 🎯 핵심 키워드

1. **Bean Validation과 @Valid** — DTO 필드에 `@NotBlank`·`@Email`·`@Pattern`·`@Size`·`@Min`·`@Max`로 규칙을 적고, Controller 파라미터에 `@Valid`를 붙이면 메서드에 들어오기 전에 검사한다. 실패하면 `MethodArgumentNotValidException`이 나고 Service는 호출되지 않는다. 형식·범위처럼 "값만 보고 판단할 수 있는 것"은 여기서 끝낸다.
2. **class-validator와 ValidationPipe (NestJS)** — 같은 역할을 데코레이터(`@IsEmail`, `@Matches`)와 전역 `ValidationPipe`가 한다. `whitelist`는 DTO에 없는 값을 지우고, `forbidNonWhitelisted`는 그런 값이 오면 400으로 막는다. (나는 Spring으로 구현)
3. **bcrypt와 salt round** — 비밀번호는 평문이 아니라 해시만 저장한다. bcrypt는 해시할 때마다 무작위 salt를 섞어서 같은 비밀번호도 매번 다른 60자 문자열이 된다. `$2a$10$...`의 10이 salt round(cost)로, 2^10번 반복해 무차별 대입을 느리게 만든다. 확인은 `matches(평문, 해시)`로 한다. 해시는 되돌릴 수 없다.
4. **201 Created / 204 No Content / 409 Conflict** — 새로 만들었으면 201(+ 만든 것의 id), 지웠고 돌려줄 게 없으면 204(본문 없음), 이미 있는 것과 부딪히면 409. 여기에 형식 오류 400, 남의 것 403, 없는 것 404를 구분했다.
5. **UNIQUE 제약 조건과 동시성** — 서비스의 `existsByEmail` 검사와 INSERT 사이에는 틈이 있다. 두 요청이 같은 순간에 "없음"을 보면 둘 다 INSERT하러 간다. 이때 DB의 UNIQUE가 마지막으로 하나만 통과시킨다. 실제로 30개를 동시에 보내 9개가 DB에서 막히는 것을 재현했다.
6. **@RestControllerAdvice / Exception Filter** — 예외를 Controller마다 try-catch하지 않고 한 클래스(`GlobalExceptionHandler`)에서 받아 같은 실패 형식으로 바꾼다. NestJS는 `ExceptionFilter`가 같은 역할이다.

## 💪 미션 기록 — Standard Mission

### 1) API 명세

| 기능 | Method | 경로 | 요청 Body | 성공 | 실패 |
| --- | --- | --- | --- | --- | --- |
| 회원가입 | POST | /members | email, password, nickname | 201 `COMMON201` + {memberId, email, nickname} | 400 `COMMON4001` 형식 / 409 `MEMBER4091` 이메일 중복 / 409 `MEMBER4092` 닉네임 중복 |
| 평점 등록 | POST | /members/{memberId}/ratings | movieId, score, comment? | 201 `COMMON201` + ratingId | 400 별점·후기 / 404 `MEMBER4041` 없는 회원 / 409 `RATING4091` 같은 영화 중복 |
| 평점 수정 | PATCH | /members/{memberId}/ratings/{ratingId} | score, comment? | 200 `COMMON200` | 400 / 403 `RATING4031` 남의 평점 / 404 `RATING4041` 없는 평점 |
| 평점 삭제 | DELETE | /members/{memberId}/ratings/{ratingId} | 없음 | 204 (본문 없음) | 403 `RATING4031` / 404 `RATING4041` |

공통 응답 형식:

```json
{ "isSuccess": true,  "code": "COMMON201",  "message": "회원가입에 성공했습니다.", "result": { "memberId": 3, "email": "umc@example.com", "nickname": "moviefan" } }
{ "isSuccess": false, "code": "MEMBER4091", "message": "이미 사용 중인 이메일입니다.", "result": null }
```

입력 규칙: email 형식·필수 / nickname 2~12자 / password 대·소문자·숫자·특수문자 포함 8자 이상 / movieId 양수 / score 1~5 / comment 500자 이하(선택)

### 2) 요청·응답 캡처 (Postman 컬렉션 18개 요청을 newman으로 실행, 검증 37개 전부 통과)

🖼️ `03-signup-duplicated.png`
이미 있는 이메일 → 409 `MEMBER4091`, 이미 있는 닉네임 → 409 `MEMBER4092`. 같은 409라도 code와 message가 다르다.

🖼️ `05-rating-create-fail.png`
같은 회원이 같은 영화(13)에 다시 등록 → 409 `RATING4091` / 별점 6 → 400 "별점은 5점 이하여야 합니다." / 없는 회원 999 → 404 `MEMBER4041`.

🖼️ `06-rating-forbidden-notfound.png`
회원 3의 평점을 회원 1이 수정·삭제 → 403 `RATING4031`. 없는 ratingId 99999 → 404 `RATING4041`. 404를 먼저 보고(평점이 있는지), 있으면 주인을 본다(403).

🖼️ `07-rating-delete.png`
DELETE → 204 No Content(본문 없음) → 조회하면 빈 배열 → 같은 평점을 다시 삭제하면 404.

🖼️ `08-db-bcrypt.png`
DB의 password 컬럼 — 요청은 `Umc!1234`였지만 `$2a$10$…` 60자 bcrypt 해시만 저장됐다.

🖼️ `10-newman-summary.png`
컬렉션 18개 요청의 상태 코드와 검증 결과 요약.

### 3) 핵심 코드

```java
// MemberController — @Valid가 DTO 검증을 실행한다. 실패하면 이 메서드에 들어오지 않는다.
@PostMapping
public ResponseEntity<ApiResponse<MemberResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
    MemberResponse response = memberService.signUp(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("회원가입에 성공했습니다.", response));
}
```

```java
// MemberService — 중복 검사 → bcrypt 해시 → 저장 → password 뺀 DTO 반환
@Transactional
public MemberResponse signUp(SignUpRequest request) {
    if (memberRepository.existsByEmail(request.email())) {
        throw new GeneralException(ErrorCode.MEMBER_EMAIL_DUPLICATED);      // 409 MEMBER4091
    }
    if (memberRepository.existsByNickname(request.nickname())) {
        throw new GeneralException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);   // 409 MEMBER4092
    }
    Member member = Member.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .nickname(request.nickname())
            .build();
    return MemberResponse.from(memberRepository.save(member));
}
```

```java
// RatingService — 수정·삭제가 함께 쓰는 소유자 확인. 없으면 404, 내 것이 아니면 403.
private Rating findOwnedRating(Long ratingId, Long memberId) {
    Rating rating = ratingRepository.findById(ratingId)
            .orElseThrow(() -> new GeneralException(ErrorCode.RATING_NOT_FOUND));
    if (!rating.getMember().getMemberId().equals(memberId)) {
        throw new GeneralException(ErrorCode.RATING_FORBIDDEN);
    }
    return rating;
}
```

코드 링크: (GitHub 저장소 생성 후 추가)
- `src/main/java/com/umc/movie/controller/GlobalExceptionHandler.java`
- `src/main/java/com/umc/movie/service/MemberService.java` · `RatingService.java`
- `src/main/java/com/umc/movie/exception/ErrorCode.java`
- `postman/umc-week6.postman_collection.json`

### 4) 확인 포인트

- **password가 응답에 없는가?** — 없다. 엔티티 대신 `MemberResponse(memberId, email, nickname)`만 돌려준다. Postman 검증 `password가 응답에 없다` 통과.
- **중복 평점이 DB에 저장되지 않는가?** — 사전 검사(`existsByMember_MemberIdAndMovieId`)에서 409로 막고, 그래도 들어오면 `(member_id, movie_id)` 복합 UNIQUE가 막는다. 등록 후 같은 요청을 보내도 rating 행은 1개다.
- **DELETE가 204로 응답하는가?** — 204, 본문 없음. Postman 검증 `본문 없음` 통과.

### 5) 워크북과 다르게 한 점

- 워크북은 중복을 `IllegalArgumentException` → 409(`COMMON4092`)로 처리했다. `IllegalArgumentException`은 코드 버그에서도 나올 수 있어서, 업무 규칙 위반만 `GeneralException(ErrorCode)`로 던지고 code를 `MEMBER4091`처럼 도메인별로 나눴다.
- 5주차의 `ValidationExceptionHandler`(경로 값 검증)와 `ErrorResponse`는 `GlobalExceptionHandler` 하나로 합쳤다. 5주차 GET API의 400·404도 이제 같은 실패 형식으로 나간다.
- 깨진 JSON·`"score": "다섯"`(400 `COMMON4002`), `/members/abc/ratings`(400 `COMMON4003`), 예상 못 한 오류(500, 내부 메시지 숨김)도 같은 형식으로 처리했다.

## 💪 미션 기록 — Challenge Mission

1. **이메일·닉네임 중복을 다른 code로** — `MEMBER4091`(이메일), `MEMBER4092`(닉네임). 🖼️ `03-signup-duplicated.png`
2. **DB UNIQUE 위반도 같은 형식으로** — `DataIntegrityViolationException`을 받으면 MySQL 메시지의 키 이름을 보고 사전 검사와 같은 code로 바꾼다.

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
    String cause = String.valueOf(exception.getMostSpecificCause().getMessage());
    ErrorCode errorCode = mapConstraint(cause);
    log.warn("[DB 제약 위반 → {}] {}", errorCode.getCode(), cause);
    return toResponse(errorCode, errorCode.getMessage());
}

// MySQL 메시지 예: Duplicate entry 'a@b.com' for key 'member.email'
static ErrorCode mapConstraint(String cause) {
    if (cause.contains("member.email")) return ErrorCode.MEMBER_EMAIL_DUPLICATED;
    if (cause.contains("member.nickname")) return ErrorCode.MEMBER_NICKNAME_DUPLICATED;
    if (cause.contains("uk_rating_member_movie")) return ErrorCode.RATING_DUPLICATED;
    return ErrorCode.DATA_CONFLICT;
}
```

3. **재현** — 🖼️ `09-challenge-db-unique.png`
같은 이메일로 회원가입 30개를 `threading.Barrier`로 동시에 보냈다(`scripts/concurrent_signup.py`). 결과: 201 1개, 409 `MEMBER4091` 29개. 서버 로그에 `[DB 제약 위반 → MEMBER4091]`이 **9건** — 이 9개는 `existsByEmail`에서 "없음"을 보고 통과했지만 INSERT에서 DB UNIQUE에 막혔다. DB에 저장된 행은 1개.

**왜 사전 검사를 통과해도 DB 제약 위반이 날 수 있는가?** — `existsByEmail`(SELECT)과 `save`(INSERT)는 서로 다른 순간에 실행된다. 요청 A와 B가 거의 동시에 SELECT하면 둘 다 "없음"을 보고, 둘 다 INSERT하러 간다. 이 틈은 서비스 코드로는 막을 수 없고, 같은 값을 두 번 넣지 못하게 하는 DB UNIQUE만 막을 수 있다. 그래서 사전 검사는 "친절한 문구"용, DB 제약은 "최종 보증"용으로 둘 다 둔다. 사용자 입장에서는 어느 쪽에서 막혀도 같은 409 `MEMBER4091`을 받는다.

## ✅ 실습 체크리스트

- [x] 회원가입 요청에서 이메일/닉네임/비밀번호를 검증했다.
- [x] 비밀번호를 bcrypt 해시로 저장하고 응답 DTO에서 제외했다.
- [x] 평점 등록/수정/삭제 API를 구현하고, 수정과 삭제 전 요청 memberId와 평점 소유자를 비교했다.
- [x] 평점의 score 범위와 comment 길이를 검증했다.
- [x] 성공과 실패 응답이 같은 구조를 사용한다.
- [x] 중복 회원 또는 중복 평점 요청에서 409, 잘못된 입력에서 400, 다른 회원의 평점 요청에서 403, 없는 평점 요청에서 404 오류를 확인했다.

## ⚡ 트러블 슈팅

(`docs/week6-troubleshooting.md` 3건 — 이슈 / 문제 / 해결 / 참고레퍼런스)

1. 동시 회원가입을 보냈는데 DB 제약 위반이 0건 — xargs는 요청이 조금씩 어긋남 → Barrier로 동시 출발
2. 재현 결과에 MEMBER4092가 섞임 — 앞 라운드 닉네임 재사용 → 실행마다 닉네임 접두사 변경
3. 초기화 직후인데 member_id가 8 — 실패한 INSERT도 AUTO_INCREMENT 번호를 소비

## 📢 학습 후기

(함께 학습한 뒤 작성 — 핵심 키워드 중 완벽히 이해한 것 / 아직 헷갈리는 것)
