# 9주차 트러블슈팅

## No.1 Swagger 주소가 401(`AUTH401_2`)로 막힘

- **이슈**: `/v3/api-docs`, `/swagger-ui/index.html`이 문서 대신 `AUTH401_2`를 응답한다.
- **문제**: 7주차의 "전부 잠그고 열 경로만 나열" 인터셉터가 Swagger 주소까지 잠갔다. 예외 목록을 빼고 테스트하니 `FavoriteApiTest.apiDocsIsPublic`이 `Status expected:<200> but was:<401>`로 실패했다.
- **해결**: `WebConfig`에 `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`를 예외로 추가했다. `/swagger-ui.html`은 `/swagger-ui/index.html`로 다시 보내므로 두 패턴이 모두 필요하다.

## No.2 문서에 memberId 입력칸이 생김

- **이슈**: `@LoginMember Long memberId`가 문서에 필수 쿼리 파라미터로 나타난다.
- **문제**: springdoc은 컨트롤러 파라미터를 모두 요청 값으로 읽는다. `@Parameter(hidden = true)`를 빼니 `found: [{"name":"memberId","in":"query","required":true}]`.
- **해결**: `@LoginMember`에 `@Parameter(hidden = true)`를 붙였다. `OpenApiDocsTest.loginMemberIsHidden`이 확인한다.

## No.3 중복 검사를 지웠는데 통합 테스트가 초록불

- **이슈**: 단위 테스트 2개만 실패하고 `FavoriteApiTest` 7개는 통과했다.
- **문제**: DB UNIQUE(`uk_favorite_member_movie`)가 막고 `GlobalExceptionHandler`가 같은 `FAVORITE409_1`로 바꿔 주기 때문에 바깥 응답이 같다.
- **해결**: 고칠 것은 없다. 단위 테스트는 서비스의 판단을, 통합 테스트는 연결된 결과를 지킨다는 차이로 이해했다.

## No.4 트랜잭션 테스트에서 롤백이 증명되지 않음

- **이슈**: 테스트 클래스에 `@Transactional`을 붙이면 서비스의 `@Transactional`을 지워도 롤백 테스트가 통과할 수 있다.
- **문제**: 테스트의 바깥 트랜잭션에 서비스 트랜잭션이 합류해, 같은 트랜잭션 안에서 조회하면 저장한 평점이 보인다.
- **해결**: `MovieRecordTransactionTest`는 `@Transactional` 없이 실제 커밋·롤백이 일어나게 하고 `@AfterEach`에서 직접 지웠다. 서비스의 `@Transactional`을 지우면 `expected: 0L but was: 1L`로 실패한다.

## No.5 Swagger 애노테이션과 공통 응답 DTO의 이름 충돌

- **이슈**: `ApiResponse`가 Swagger의 `@ApiResponse`와 우리 DTO에서 겹친다.
- **문제**: 같은 이름의 클래스를 한 파일에서 둘 다 import할 수 없다.
- **해결**: Swagger 쪽을 풀네임(`io.swagger.v3.oas.annotations.responses.ApiResponse`)으로 적었다.
