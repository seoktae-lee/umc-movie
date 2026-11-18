# 9주차 Backend 노션 제출본 (원고)

> 노션 "태이의 워크북 / 9주차 - 함께 테스트하고 다듬기"에 넣는 원고. 페이지의 섹션 순서 그대로다.
> `🖼️` 자리에 `docs/images/week9/` 이미지를 넣는다. **토글 안에 넣지 말 것.**

## 🎯 핵심 키워드

**Swagger / OpenAPI** — OpenAPI는 API를 설명하는 규격(JSON 문서)이고, Swagger UI는 그 JSON을 사람이 보기 좋게 그려 주는 화면이다. 우리 서버는 `/v3/api-docs`로 OpenAPI JSON을, `/swagger-ui.html`로 화면을 내려 준다. springdoc이 컨트롤러와 DTO를 읽어 문서를 만들기 때문에 코드를 고치면 문서도 같이 바뀐다. 손으로 쓴 명세 표는 코드가 바뀌는 순간 조용히 거짓말을 시작하지만, 생성된 문서는 어긋날 수 없다. 단, 설명·예시·실패 응답·토큰 필요 여부는 사람이 `@Operation`, `@Schema`, `@ApiResponses`, `@SecurityRequirement`로 적어 줘야 한다.

**단위 테스트** — 클래스 하나의 판단(주로 Service)만 확인한다. Repository 자리에 가짜를 끼워 DB 없이 돌고 매우 빠르다. "이미 즐겨찾기한 경우 409를 던지는가" 같은 로직을 확인하고, 틀리면 "로직이 틀렸다"고 알려 준다. 반대로 URL 오타, 인증 설정 누락, 요청 DTO 검증 규칙은 절대 못 잡는다.

**통합 테스트** — 요청부터 응답까지 전체 흐름을 확인한다. 컨트롤러, 인터셉터, 검증, JPA, DB까지 진짜로 돈다(`@SpringBootTest` + `MockMvc`). 느리지만 "연결이 틀렸다"를 알려 준다. 개발용 MySQL을 쓰면 데이터가 쌓이고 서로 영향을 주므로, 테스트가 돌 때만 메모리에 만들어지는 H2를 `@ActiveProfiles("test")`로 붙였다.

**Mock, Stub** — 둘 다 진짜 객체 대신 끼우는 가짜다. Stub은 "이 호출에는 이 값을 돌려줘"라고 상황을 만드는 쪽이고(`given(repository.exists...).willReturn(true)`), Mock은 "이 메서드가 호출됐는가/안 됐는가"를 검증하는 쪽이다(`verify(repository, never()).save(...)`). Mockito의 `@Mock` 객체가 두 역할을 다 한다. 중복 테스트는 Stub으로 "이미 있다고 치고", Mock 검증으로 "저장은 시도조차 안 했다"까지 본다.

**given-when-then** — 테스트를 상황(given) → 실행(when) → 확인(then) 세 덩어리로 쓰는 모양이다. Postman으로 머릿속에서 하던 "이 상태에서 이걸 누르면 이게 나와야 해"를 그대로 옮긴 것이다. 테스트 이름도 이 문장으로 적으면 실패했을 때 무엇이 깨졌는지 바로 읽힌다.

**회귀 테스트** — "예전에 되던 게 지금도 되는지" 확인하는 테스트다. 이번 주에 중복 검사를 지워 보니 사람이 아니라 테스트가 먼저 알려 줬다. 고쳐도 뭐가 깨지는지 바로 알 수 있으면 리팩토링이 무섭지 않다. 이번 주에 테스트가 72개에서 94개로 늘었다.

## 📢 학습 후기

(초안) 8주차까지는 기능을 만들 때마다 서버를 켜고, 토큰을 받아 헤더에 붙이고, 상태 코드와 `code`를 눈으로 확인했다. 테스트 코드는 이 다섯 단계를 글로 적어 둔 것이라는 설명이 가장 와닿았다. 한 번 적으면 `./gradlew test` 한 줄로 94개가 모두 다시 확인된다.

가장 놀라웠던 건 일부러 깨뜨려 본 결과다. 워크북은 중복 검사를 지우면 단위 테스트와 통합 테스트가 둘 다 빨간불이 된다고 했는데, 내 프로젝트에서는 단위 테스트 2개만 실패하고 통합 테스트(`FavoriteApiTest`)는 초록불로 남았다. 이유는 7주차에 만든 안전망 때문이다. 서비스의 사전 검사를 지워도 DB의 UNIQUE 제약이 막고, `GlobalExceptionHandler`가 그 위반을 같은 `FAVORITE409_1`로 바꿔 주기 때문에 바깥에서 보면 응답이 똑같다. 통합 테스트는 "바깥에서 본 결과"를 보고 단위 테스트는 "서비스가 스스로 판단했는가"를 본다는 차이를 직접 봤다. 한 겹이 사라져도 응답이 같으면 통합 테스트는 모른다는 뜻이기도 해서, 두 종류가 서로 다른 것을 지켜 준다는 걸 알았다.

트랜잭션 테스트에서는 테스트 클래스에 `@Transactional`을 붙이면 안 된다는 점이 인상 깊었다. 테스트가 바깥 트랜잭션을 열면 서비스의 트랜잭션이 거기에 합류해서, 롤백이 일어나도 같은 트랜잭션 안에서 조회하는 테스트에는 저장한 평점이 그대로 보인다. 롤백을 증명하려면 실제로 커밋과 롤백이 일어나게 두고 `@AfterEach`에서 직접 지워야 했다. 실제로 `MovieRecordService`의 `@Transactional`을 지우자 "expected 0 but was 1"로 실패했다.

아직 헷갈리는 것은 Mock과 Stub의 경계다. Mockito 한 객체가 둘 다 하니까 코드에서는 구분이 흐리고, 용어로만 구분하고 있다.

## ✅ 실습 체크리스트

- [x] Swagger를 설치하고 브라우저에서 API 문서 화면을 열었다.
- [x] 토큰이 필요한 API에 자물쇠 표시가 뜨고, 공개 API에는 뜨지 않는 것을 확인했다.
- [x] Swagger의 Authorize 버튼에 토큰을 넣고 즐겨찾기 등록을 성공시켰다.
- [x] 같은 요청을 반복해 문서에 적어둔 409 응답이 실제로 나오는 것을 확인했다.
- [x] 가짜 Repository를 사용한 단위 테스트 2개를 작성하고 통과시켰다.
- [x] 테스트 전용 메모리 DB를 붙여 통합 테스트 4개를 작성하고 통과시켰다.
- [x] 중복 검사 코드를 일부러 지워 테스트가 실패하는 것을 확인하고 되돌렸다.

## ☑️ 실습 인증

**Swagger 문서 화면 전체 목록**: 회원·인증·평점·즐겨찾기·영화 기록 5개 묶음에 API 15개가 모두 나타난다. 자물쇠는 토큰이 필요한 API에만 있고 회원가입, 로그인, 중복 확인, 유저별 평점 조회에는 없다.

🖼️ `02-swagger-all-apis.png`

**즐겨찾기 등록 API를 펼친 화면**: 설명, 입력 예시(`movieId: 550`), 성공(201)과 실패(401, 409) 응답 예시가 적혀 있다.

🖼️ `03-swagger-favorite-post-doc.png`

**Authorize로 토큰을 넣고 `POST /favorites` 201, 한 번 더 호출해 409**: 문서에 적어 둔 실패 응답 예시와 실제 응답(`FAVORITE409_1`)이 같다. Logout 후 호출하면 401 `AUTH401_2`가 나온다. (토큰은 가렸다)

🖼️ `04-swagger-authorize.png`
🖼️ `05-swagger-favorite-201.png`
🖼️ `06-swagger-favorite-409.png`
🖼️ `07-swagger-favorite-401.png`

**단위 테스트와 통합 테스트 통과**: 단위 6개(가짜 Repository), 통합은 `FavoriteApiTest` 7개를 포함해 H2로 돈다. 전체는 94개다.

🖼️ `08-test-unit-pass.png`
🖼️ `09-test-integration-pass.png`
🖼️ `10-test-all-pass.png`

**중복 검사를 지웠을 때 실패한 테스트**: `FavoriteServiceTest`의 "이미 즐겨찾기한 영화면 저장하지 않고 409" 테스트가 실패한다. 중복 검사가 없으니 `ApiException` 대신 저장 단계로 넘어가 `NullPointerException`이 났기 때문이다. (통합 테스트는 DB UNIQUE 안전망 덕분에 초록불이었다. 후기 참고)

🖼️ `11-break-duplicate-fail.png`

## 💪 미션 기록

**미션 완료 정보**

| 항목 | 내용 |
| --- | --- |
| 이름 / 닉네임 | 이석태 / 태이 |
| GitHub 저장소 | https://github.com/seoktae-lee/umc-movie |
| Pull Request | (PR 생성 후 기입) |
| 브랜치 | feature/week-9 |
| 스택 | Spring Boot 3.5 · Java 21 · MySQL · springdoc 2.8.9 · H2(테스트 전용) |
| 검증 | 테스트 94개 통과(8주차 72개 + 9주차 22개) / Swagger UI에서 실제 호출 |

**실습 1. Swagger 붙이고 문서 화면 열기**

왜 이렇게 했나: 라이브러리 한 줄과 설정 클래스 하나로 이미 API가 문서에 나타난다. 다만 7주차에 "전부 잠그고 열 경로만 연다"로 인터셉터를 만들어서 Swagger 주소도 잠긴다. 그래서 `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`를 예외 목록에 넣었다. `/swagger-ui.html`은 `/swagger-ui/index.html`로 다시 보내므로 두 주소가 서로 다른 경로라 패턴 하나로는 둘 다 열리지 않는다.

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9'
```

```java
@Bean
public OpenAPI movieOpenAPI() {
    return new OpenAPI()
            .info(new Info().title("영화 평점 서비스 API").version("v1"))
            .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
}
```

**실습 2·3. 공개 API와 인증 API 문서화**

왜 이렇게 했나: 문서에서 프론트엔드가 궁금한 건 언제 부르는지, 무엇을 보내는지, 성공하면 어떤 모양인지, 실패하면 어떤 상태 코드와 `code`인지, 토큰이 필요한지다. 앞의 네 가지는 `@Operation`·`@Schema`·`@ApiResponses`로, 마지막은 `@SecurityRequirement(name = "bearerAuth")`로 적었다. 우리 공통 응답 DTO 이름도 `ApiResponse`라서 Swagger 쪽은 풀네임(`io.swagger.v3.oas.annotations.responses.ApiResponse`)으로 적었다. 여러 API가 같은 실패 응답을 쓰므로 예시 JSON은 `ApiExamples` 상수에 모았다.

```java
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH) // 컨트롤러의 모든 API가 토큰을 요구
public class FavoriteController {
    @Operation(summary = "즐겨찾기 등록", description = "... 🔒 토큰이 필요합니다.")
    @ApiResponses({ /* 201, 401 AUTH401_2, 409 FAVORITE409_1 */ })
    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> create(@LoginMember Long memberId, ...)
```

**실습 4. 단위 테스트**

7주차에 만든 `FavoriteServiceTest`가 이미 워크북의 실습 4(저장 / 중복이면 409이고 저장하지 않음)를 포함한다. 같은 판단을 확인하는 테스트라 새로 중복해서 만들지 않고 그대로 사용했다. (`./gradlew test --tests '*FavoriteServiceTest'`, 6개 통과)

**실습 5. 통합 테스트**

왜 이렇게 했나: `@SpringBootTest` + `@AutoConfigureMockMvc`로 진짜 요청을 흘려보내고, H2를 `application-test.yml`(`@ActiveProfiles("test")`)로 붙였다. 워크북은 `src/test/resources/application.yaml`을 쓰지만, 그렇게 하면 기존 `MovieApplicationTests`(실제 MySQL의 테이블 구조와 엔티티가 맞는지 `validate`로 확인)까지 H2로 바뀌어 그 검증 의미가 사라진다. 그래서 프로필 파일로 분리했다.

| 상황 | 기대 결과 | 테스트 |
| --- | --- | --- |
| 토큰 없이 등록 | 401, AUTH401_2 | `createFavoriteWithoutToken` |
| 정상 등록 | 201, movieId 반환 | `createFavorite` |
| 중복 등록 | 409, FAVORITE409_1 | `createFavoriteWhenDuplicated` |
| 즐겨찾기 안 한 영화 조회 | 200, isFavorite false | `getFavoriteStatus` |
| Swagger 경로 | 토큰 없이 200 | `apiDocsIsPublic` |

여기에 내 즐겨찾기 목록(내 것만, 없으면 빈 배열)과 해제(200 result null, 다시 해제하면 404)를 더해 7개로 만들었다.

**실습 6. 일부러 깨뜨려 보기**

| 깨뜨린 것 | 실패한 테스트 | 초록불로 남은 테스트 |
| --- | --- | --- |
| `FavoriteService`의 중복 검사 주석 처리 | `FavoriteServiceTest` 2개 | `FavoriteApiTest` 7개 전부 (DB UNIQUE 안전망) |
| `MovieRecordService`의 `@Transactional` 제거 | `MovieRecordTransactionTest`, `MovieRecordServiceTest` 각 1개 | 나머지 |

두 번 모두 실패를 확인한 뒤 코드를 원래대로 되돌렸고, 전체 94개가 다시 초록불이 되는 것까지 확인했다.

**필수 미션: 문서 채우고 평점 API 테스트하기**

- 문서화: 평점 등록/수정/삭제/유저별 조회, 내 정보 조회/수정, 즐겨찾기 목록/여부/해제, 영화 기록 저장까지 모든 API에 한 줄 설명, 토큰 필요 여부(자물쇠), 실패 응답의 상태 코드와 `code`를 적었다. 회원·인증·평점·즐겨찾기·영화 기록 5개 묶음이다.
- 단위 테스트 2개: `RatingServiceTest`의 "같은 영화에 이미 남겼으면 저장하지 않고 409"(`RATING409_1`), "다른 회원의 평점이면 403"(`RATING403_1`). 7주차에 만든 테스트이고, 이번 미션의 요구사항을 그대로 만족해서 새로 만들지 않았다.
- 통합 테스트 2개: `RatingApiTest`의 `createRatingWithoutToken`(토큰 없이 POST /ratings → 401, 저장 안 됨), `createRating`(토큰 담아 → 201). 여기에 409 중복, 400 별점 범위, 남의 평점 수정 403(값이 그대로인지까지), 유저별 조회는 토큰 없이 열려 있는지 4개를 더해 6개다.
- 확인 포인트: 실패 응답의 상태 코드와 `code`를 함께 검사했고, 테스트 이름만 읽어도 무엇을 확인하는지 알 수 있게 적었다.

**선택 미션: 트랜잭션이 진짜 되돌리는지 테스트로 증명하기**

`MovieRecordTransactionTest.rollsBackRatingWhenFavoriteFails`: 이미 즐겨찾기한 영화(550)에 `addToFavorites: true`로 영화 기록을 보내면 `FAVORITE409_1`이다. "예외가 났다"에서 멈추지 않고 평점 개수가 요청 전과 같은지(`ratingRepository.count()`)까지 확인한다. 이 클래스에는 일부러 `@Transactional`을 붙이지 않았다(후기 참고). `MovieRecordService.save()`의 `@Transactional`을 지우면 `expected: 0L but was: 1L`로 실패하고, 되돌리면 통과한다.

🖼️ `12-break-transaction-fail.png`

**문서가 실제 동작과 같은 말을 하는지 확인하는 테스트 (추가)**

`OpenApiDocsTest` 6개: 보호 API 10개에는 `security: bearerAuth`가 있고 공개 API 5개에는 없는지, 토큰으로 알아내는 `memberId`가 문서 입력칸에 없는지, 응답 예시가 문서에 있는지를 확인한다. 문서에 "🔒 토큰 필요"라고 적고 실제로는 열려 있으면 문서가 거짓말을 하는 것이라, 5번(토큰이 필요한가요)은 실수하기 가장 쉬운 항목을 테스트로 고정했다.

**한계**

- 위 테스트는 서버끼리 주고받는 요청이라 CORS는 잡지 못한다. 프론트와 연결하다 CORS 에러가 나면 5주차의 서버 설정 문제이고, 브라우저에서 한 번은 직접 확인해야 한다.
- Swagger의 Authorize에 넣은 토큰은 캡처에서 모두 가렸다.
- 로컬 서버와 워크북 서버(`workbook.devnoco.com`)는 별개다. 이번 주 Swagger는 내 로컬 서버(umc-movie)의 문서다.

## ⚡ 트러블 슈팅

**No.1 Swagger 주소가 401(AUTH401_2)로 막힘**

이슈: Swagger를 붙였는데 `/v3/api-docs`와 `/swagger-ui/index.html`이 문서 대신 `AUTH401_2` 응답을 낸다.

문제: 7주차에 "전부 잠그고 열 경로만 나열"로 만든 인터셉터가 새로 생긴 Swagger 주소까지 잠갔다. 예외 목록을 빼고 테스트하니 `FavoriteApiTest.apiDocsIsPublic`이 `Status expected:<200> but was:<401>`로 실패했다.

해결: `WebConfig`의 예외 목록에 `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`를 추가했다. `/swagger-ui.html`과 `/swagger-ui/**`는 서로 다른 경로라 둘 다 적었다. 나중에 누가 예외 목록을 건드리면 이 테스트가 먼저 알려 준다.

**No.2 문서에 memberId 입력칸이 생김**

이슈: 토큰으로 알아내는 `@LoginMember Long memberId`가 Swagger 문서에서 필수 쿼리 파라미터 `memberId`로 나타난다.

문제: springdoc은 컨트롤러 파라미터를 모두 요청 값으로 읽는다. 그러면 문서에 memberId를 직접 넣는 칸이 생겨 "토큰으로 알아낸다"는 7주차 설계와 어긋난다. `@Parameter(hidden = true)`를 빼고 테스트하니 `found: [{"name":"memberId","in":"query","required":true,...}]`가 나왔다.

해결: `@LoginMember` 애노테이션 자체에 `@Parameter(hidden = true)`를 붙였다. 이 애노테이션을 쓰는 모든 곳에 한 번에 적용되고, `OpenApiDocsTest`가 확인한다.

**No.3 중복 검사를 지웠는데 통합 테스트가 초록불**

이슈: 워크북은 단위·통합 테스트가 둘 다 빨간불이 된다고 했는데, `FavoriteApiTest`는 7개 모두 통과했다.

문제: 테스트가 틀린 게 아니라 DB의 UNIQUE 제약(`uk_favorite_member_movie`)이 두 번째 저장을 막고, `GlobalExceptionHandler`가 그 위반을 `FAVORITE409_1`로 바꿔 주기 때문이다. 바깥에서 본 응답이 똑같아서 통합 테스트는 사전 검사가 사라진 것을 모른다.

해결: 고칠 것은 없고 해석을 바꿨다. 단위 테스트는 서비스의 판단을, 통합 테스트는 연결된 결과를 지킨다. 안전망이 있는 항목은 단위 테스트가 먼저 알려 준다.

**No.4 트랜잭션 테스트에서 롤백이 증명되지 않음**

이슈: 처음에 다른 통합 테스트처럼 클래스에 `@Transactional`을 붙이면 `@Transactional`을 지워도 테스트가 통과할 것 같았다.

문제: 테스트 메서드가 바깥 트랜잭션을 열면 서비스의 트랜잭션이 그 안에 합류한다. 예외가 나도 같은 트랜잭션 안에서 조회하는 테스트에는 저장한 평점이 그대로 보여 롤백을 증명할 수 없다.

해결: `MovieRecordTransactionTest`에는 `@Transactional`을 붙이지 않고, 실제로 커밋·롤백이 일어나게 둔 뒤 `@BeforeEach`·`@AfterEach`에서 회원·평점·즐겨찾기를 직접 지웠다.

**No.5 Swagger 애노테이션과 공통 응답 DTO의 이름 충돌**

이슈: `ApiResponse`를 import하면 Swagger의 `@ApiResponse`와 우리 `ApiResponse<T>` DTO가 겹친다.

문제: 같은 이름의 클래스 두 개를 한 파일에서 import할 수 없다.

해결: 우리 DTO는 본문 곳곳에서 쓰이므로 Swagger 쪽을 풀네임(`io.swagger.v3.oas.annotations.responses.ApiResponse`)으로 적었다.

참고레퍼런스: https://springdoc.org/
