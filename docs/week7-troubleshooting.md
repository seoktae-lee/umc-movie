# 7주차 트러블슈팅 (Backend)

워크북 형식(이슈 / 문제 / 해결 / 참고레퍼런스) 그대로 적었다. 전부 실제 서버를 띄워 확인한 내용이다.

## No.1 없는 경로와 허용하지 않는 메서드가 전부 500으로 나옴

**`이슈`**

👉 6주차 URL인 `POST /members/3/ratings`를 보내자 405가 아니라 500 `COMMON500`이 돌아왔다. 토큰을 넣고 보낸 `GET /nope`(없는 경로)와 `PUT /members/me`(없는 메서드)도 모두 500이었다.

**`문제`**

👉 6주차에 만든 `@ExceptionHandler(Exception.class)`가 스프링의 `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`까지 잡았다. 스프링이 404와 405로 응답할 예외를 만능 핸들러가 500으로 덮은 것이다. 워크북이 "만능 핸들러를 달면 정확한 코드가 500으로 덮인다"고 경고한 상황 그대로다. 6주차에는 이런 경로를 시험하지 않아 몰랐고, 이번 주 URL이 바뀌면서 드러났다.

**`해결`**

👉 예상 못 한 오류의 내부 메시지를 숨기는 기능은 필요해서 핸들러를 지우지 않았다. 대신 스프링이 상태 코드를 이미 아는 예외(`ErrorResponse`를 구현한 예외)는 그 코드를 그대로 쓰게 했다.

```java
if (exception instanceof ErrorResponse errorResponse) {
    int status = errorResponse.getStatusCode().value();
    return ResponseEntity.status(status)
            .body(ApiResponse.failure("COMMON" + status, errorResponse.getBody().getDetail()));
}
```

| 요청 | 고치기 전 | 고친 후 |
| --- | --- | --- |
| POST /members/3/ratings (토큰 없음) | 500 | 405 `COMMON405` |
| PATCH /members/3/ratings/1 (토큰 있음) | 500 | 404 `COMMON404` |
| GET /nope (토큰 있음) | 500 | 404 `COMMON404` |
| PUT /members/me (토큰 있음) | 500 | 405 `COMMON405` |

배운 점: 만능 핸들러는 실패 형식을 통일해 주지만 원인을 가릴 수 있다. 성공 경로뿐 아니라 "없는 경로"도 직접 호출해 봐야 한다.

**`참고레퍼런스`**

- Spring ErrorResponse: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/ErrorResponse.html

## No.2 평점 수정 응답의 updatedAt이 수정 전 값으로 나옴

**`이슈`**

👉 수정 응답을 `RatingResponse`로 바꾼 뒤 비교하니, 응답의 `updatedAt`은 `19:46:28`인데 바로 조회한 값과 DB 값은 `19:46:30`이었다.

**`문제`**

👉 수정은 dirty checking이라 트랜잭션이 끝날 때 UPDATE가 나간다. `updatedAt`도 그때 정해지는데, 응답 DTO는 그 전에 만들어져서 옛 값이 실렸다. 6주차에는 수정 응답의 result가 null이라 드러나지 않았다.

**`해결`**

👉 응답을 만들기 전에 `ratingRepository.flush()`로 UPDATE를 먼저 내보냈다. Postman 컬렉션에 "수정 응답의 updatedAt과 조회 값이 같다" 검증을 넣었다.

## No.3 평점 등록 응답의 시각이 DB에 저장된 값과 다름

**`이슈`**

👉 등록 응답의 `createdAt`은 `19:46:27.95278`인데 DB에는 `19:46:28`이 저장돼 있었다. 자릿수도 초 단위도 달랐다.

**`문제`**

👉 `@CreationTimestamp`가 기본으로 JVM 시각을 넣어 소수 초까지 가지고 있었고, 등록 응답은 그 값을 그대로 내보냈다. 반면 `DATETIME` 컬럼은 소수 초를 반올림해 저장한다. 같은 평점의 시각이 응답과 조회에서 달라지는 것이다.

**`해결`**

👉 `BaseTimeEntity`의 두 시각을 `source = SourceType.DB`로 바꿨다. DB가 정한 시각을 쓰고 저장 직후 다시 읽으므로 응답과 조회가 같은 값을 보여 준다. 대신 저장할 때 SELECT가 한 번 더 나간다.

**`참고레퍼런스`**

- Hibernate SourceType: https://docs.jboss.org/hibernate/orm/6.6/javadocs/org/hibernate/annotations/SourceType.html

## No.4 같은 닉네임으로 동시에 수정하면 다른 code가 나옴

**`이슈`**

👉 두 회원이 같은 닉네임으로 동시에 수정하는 요청을 10라운드 보내자, 결과가 200 10개와 409 `MEMBER4092` 10개였다. 약속한 코드는 `MEMBER409_1`이다.

**`문제`**

👉 두 요청이 사전 검사를 함께 통과하면 DB UNIQUE가 하나를 막는다. UPDATE가 트랜잭션 커밋 때 나가므로 서비스 밖에서 예외가 나고, 전역 핸들러가 회원가입용 변환 규칙에 따라 `MEMBER4092`로 바꿨다.

**`해결`**

👉 서비스 안에서 `memberRepository.flush()`로 UPDATE를 먼저 내보내고, `DataIntegrityViolationException`을 `MEMBER409_1`로 바꿨다. 다시 재현한 결과는 200 10개, 409 `MEMBER409_1` 10개다. 재현 스크립트: `scripts/concurrent_nickname.py`

## No.5 유효시간을 1초로 줄였는데 토큰이 바로 만료되지 않음

**`이슈`**

👉 `AUTH401_4`를 보려고 `JWT_ACCESS_EXPIRATION=1`로 서버를 띄웠다. 토큰 payload의 `exp`는 `iat`보다 1초 뒤인데, 발급 3초 후와 30초 후 요청 모두 200이었다.

**`문제`**

👉 `JwtUtil`의 `clockSkewSeconds(60)` 때문이다. 서버 간 시계 오차를 고려해 만료 후 60초까지 유효로 본다. 버그가 아니라 워크북 코드에 적힌 의도된 동작이다.

**`해결`**

👉 발급 65초 후에 같은 토큰을 보내 401 `AUTH401_4`를 확인했다. 실제 토큰의 최대 사용 가능 시간은 유효시간 + 60초라는 점도 함께 알게 됐다.

**`참고레퍼런스`**

- JWT(RFC 7519) exp 항목의 시계 오차 허용: https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4
