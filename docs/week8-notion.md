# 8주차 Backend 노션 제출본 (원고)

> 노션 "태이의 워크북 / 8주차 - 서비스의 핵심 기능 만들기"에 넣는 원고. 페이지의 섹션 순서 그대로다.
> `🖼️` 자리에 `docs/images/week8/` 이미지를 넣는다. **토글 안에 넣지 말 것.**

## 🎯 핵심 키워드

**트랜잭션(Transaction)** — 여러 작업을 한 덩어리로 묶는 봉투다. 안의 작업이 모두 성공하면 반영하고, 하나라도 실패하면 모두 없던 일로 한다. 이번 주 `POST /movie-records`는 평점 저장과 즐겨찾기 저장이라는 두 작업을 하나의 `@Transactional` 안에서 처리한다.

**commit / rollback** — commit은 트랜잭션 안의 변경을 DB에 확정하는 것이고, rollback은 모두 취소하는 것이다. Spring은 `@Transactional` 메서드가 정상 종료하면 commit, `RuntimeException`이 밖으로 나가면 rollback한다. 내가 던진 `ApiException`이 `RuntimeException`이라 따로 설정하지 않아도 롤백된다. 직접 확인했다: 이미 즐겨찾기한 영화로 영화 기록을 보내면 평점 INSERT가 먼저 나가지만 `FAVORITE409_1`이 나면서 평점 행이 남지 않았다.

**원자성(Atomicity)** — 쪼갤 수 없다는 뜻으로, 트랜잭션이 "전부 아니면 전무"여야 한다는 성질이다. 카드 결제와 주문 생성이 함께 성공하거나 함께 실패해야 하는 것과 같다. 반쪽만 저장된 상태가 DB에 남지 않는 것이 원자성이 지켜 주는 약속이다.

**ACID** — 트랜잭션이 지키는 네 성질이다. 원자성(Atomicity), 일관성(Consistency: 제약을 어기는 상태로 끝나지 않음), 격리성(Isolation: 동시에 실행돼도 서로의 중간 상태를 보지 않음), 지속성(Durability: commit된 결과는 장애가 나도 남음). 이번 실습에서 직접 본 것은 원자성과 일관성(UNIQUE 제약)이고, 격리성과 지속성은 개념으로만 정리했다.

**UNIQUE 제약 조건** — 컬럼 조합의 값이 중복되지 못하게 DB가 막는 규칙이다. favorite에는 `(member_id, movie_id)` 복합 UNIQUE가 있다. `movie_id`만 UNIQUE였다면 한 영화가 서비스 전체에서 한 번만 즐겨찾기될 수 있어서 두 값을 한 쌍으로 묶는다. 서버의 사전 검사는 친절한 오류 메시지(`FAVORITE409_1`)를 위한 것이고, DB 제약은 동시 요청까지 포함해 데이터를 지키는 마지막 안전망이다. 안전망이 터져도 같은 응답이 나가도록 `uk_favorite_member_movie` 위반을 `FAVORITE409_1`로 바꿔 주는 매핑을 `GlobalExceptionHandler`에 추가했다.

**낙관적 잠금(Optimistic Locking)** — "충돌은 드물 것"이라 보고 잠그지 않고 진행하되, 저장 시점에 버전(`@Version`)이 바뀌었는지 확인해 충돌을 발견하는 방식이다. 충돌이 나면 예외를 내고 다시 시도하게 한다. 반대인 비관적 잠금은 읽을 때부터 행을 잠근다. 이번 주 코드에는 적용하지 않았고 개념만 정리했다. 즐겨찾기는 (member_id, movie_id)가 UNIQUE라 버전 없이도 중복이 막힌다.

## 📢 학습 후기

(초안) 트랜잭션이 없을 때 평점만 저장되고 즐겨찾기가 저장되지 않으면, 사용자는 "5점 주고 즐겨찾기도 눌렀다"고 생각하는데 DB에는 평점만 있게 된다. 사용자가 의도한 기록과 서버의 상태가 어긋나고, 그 어긋남은 오류 없이 조용히 남아서 더 찾기 어렵다. 실제로 `@Transactional`을 잠시 빼고 같은 요청을 보내 보니 409 응답이 나왔는데도 평점 행(`ratingId 6`)이 DB에 남았다. 다시 붙이니 남지 않았다.

서버의 중복 검사와 DB의 UNIQUE 제약을 둘 다 두는 이유는 역할이 다르기 때문이다. 서버 검사는 사용자에게 `FAVORITE409_1`이라는 명확한 오류를 주기 위한 것이고, DB 제약은 두 요청이 거의 동시에 검사를 통과하는 경우까지 막는 마지막 안전망이다. 둘 중 하나만 있으면 오류 문구가 불친절하거나(DB만), 동시 요청에 뚫린다(서버만).

이번 주에 이해가 가장 잘 된 흐름은 "한 사용자 행동 = 한 트랜잭션"이다. `RatingService.create()`와 `FavoriteService.create()`를 컨트롤러가 연달아 부르지 않고 `MovieRecordService` 하나가 바깥 트랜잭션을 열면, 안쪽 두 서비스의 `@Transactional`(기본 REQUIRED)이 그 트랜잭션에 참여한다. 아직 낯선 것은 격리 수준과 낙관적 잠금이다. 동시 요청에서 두 번째가 실제로 DB UNIQUE에 막히는 상황은 이번 주에 직접 재현하지 못했다.

## ✅ 실습 체크리스트

- [x] Favorite 엔티티가 BaseTimeEntity를 상속하고, member와 movieId를 저장하도록 만들었다.
- [x] POST /favorites에서 토큰의 회원 ID를 사용하고 요청으로 memberId를 받지 않았다.
- [x] 같은 영화의 즐겨찾기 중복 요청에 409 FAVORITE409_1이 오는 것을 확인했다.
- [x] GET /favorites에서 내 즐겨찾기만 최신 등록 순서로 조회되는 것을 확인했다.
- [x] GET /favorites/{movieId}에서 등록 전후의 isFavorite 값이 각각 false, true로 바뀌는 것을 확인했다.
- [x] DELETE /favorites/{movieId}가 200과 공통 응답 형식으로 동작하는 것을 확인했다.
- [x] 평점과 즐겨찾기를 함께 저장하는 코드가 하나의 트랜잭션 안에서 동작하도록 만들었다.

## ☑️ 실습 인증

**POST /favorites 성공 응답과 같은 요청의 중복 오류 응답**: 첫 카드가 201, 두 번째가 409 `FAVORITE409_1`이다. 토큰이 없으면 `AUTH401_2`, movieId가 0이면 400이다.

🖼️ `01-favorite-create-duplicate.png`

**GET /favorites 목록, GET /favorites/{movieId} 여부, DELETE /favorites/{movieId} 해제**: 목록은 최신 등록 순(551이 550보다 위), 여부는 등록 후 true / 없는 영화 false, 해제는 200과 `result: null`이고 다시 해제하면 404 `FAVORITE404_1`이다.

🖼️ `02-favorite-list-status-delete.png`

**POST /movie-records 성공 응답(rating과 favorite이 함께 있음)**: 같은 요청을 다시 보내면 `RATING409_1`, `addToFavorites: false`이면 평점만 저장된다.

🖼️ `03-movie-record-created.png`

**트랜잭션 롤백 확인**: 이미 즐겨찾기한 영화(603)로 영화 기록을 보내 일부러 즐겨찾기 단계에서 실패시켰다. 응답은 409 `FAVORITE409_1`이고, DB에 평점 행이 남지 않았다.

🖼️ `04-transaction-rollback.png`

**트랜잭션 코드**: `MovieRecordService.save()`의 `@Transactional`이다.

🖼️ `05-transactional-code.png`

두 저장을 묶은 이유: 평점만 저장되고 즐겨찾기가 실패하는 반쪽 상태를 남기지 않고, 하나라도 실패하면 먼저 저장한 것도 취소하기 위해서다.

## 💪 미션 기록

**미션 완료 정보**

| 항목 | 내용 |
| --- | --- |
| 이름 / 닉네임 | 이석태 / 태이 |
| GitHub 저장소 | https://github.com/seoktae-lee/umc-movie |
| Pull Request | https://github.com/seoktae-lee/umc-movie/pull/2 |
| 브랜치 | feature/week-8 |
| 스택 | Spring Boot 3.5 · Java 21 · MySQL |
| 검증 | 테스트 72개 통과 / 실제 서버에 16개 요청을 보내 모두 기대한 상태 코드와 code 확인 |

**API 명세**

| API | Method | URL | 토큰 | 성공 | 대표 실패 |
| --- | --- | --- | --- | --- | --- |
| 즐겨찾기 등록 | POST | /favorites | 필요 | 201 favoriteId, movieId, createdAt | 409 FAVORITE409_1 / 400 COMMON4001 |
| 내 즐겨찾기 조회 | GET | /favorites | 필요 | 200 배열(최신 등록순, 없으면 []) | 401 AUTH401_2 |
| 즐겨찾기 여부 | GET | /favorites/{movieId} | 필요 | 200 isFavorite | 401 AUTH401_2 |
| 즐겨찾기 해제 | DELETE | /favorites/{movieId} | 필요 | 200 result null | 404 FAVORITE404_1 |
| 영화 기록 저장 | POST | /movie-records | 필요 | 201 rating, favorite | 409 RATING409_1·FAVORITE409_1 |

**실습 1. Favorite 엔티티와 DTO**

왜 이렇게 했나: 즐겨찾기는 영화의 속성이 아니라 회원과 영화의 관계다. `movieId`는 TMDB ID라 FK가 아닌 일반 컬럼이다. 시간 컬럼은 `BaseTimeEntity`를 상속하고, 5주차 SQL의 `uk_favorite_member_movie` 이름을 `@UniqueConstraint`에 그대로 적었다.

```java
@Entity
@Table(name = "favorite",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorite_member_movie", columnNames = {"member_id", "movie_id"}))
public class Favorite extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;
}
```

**실습 2. 즐겨찾기 등록과 조회**

왜 이렇게 했나: 등록 전에 존재 여부를 먼저 검사해 `FAVORITE409_1`을 주고, 그래도 뚫리는 동시 요청은 DB UNIQUE가 막는다. 안전망이 터져도 같은 code가 나가도록 `GlobalExceptionHandler.mapConstraint`에 `uk_favorite_member_movie`를 추가했다. 해제할 데이터가 없으면 401이 아니라 404다. 요청자는 확인됐고 데이터만 없기 때문이다. 여부 조회는 없어도 오류가 아니라 `isFavorite:false`로 응답한다.

```java
if (favoriteRepository.existsByMember_MemberIdAndMovieId(memberId, movieId)) {
    throw new ApiException(HttpStatus.CONFLICT, "FAVORITE409_1", "이미 즐겨찾기한 영화입니다.");
}
Favorite favorite = Favorite.builder().member(member).movieId(movieId).build();
return FavoriteResponse.from(favoriteRepository.save(favorite));
```

`AuthInterceptor`는 수정하지 않았다. 기본이 "전부 잠그고 열 경로만 나열"이라 `/favorites`, `/movie-records`가 자동으로 보호되고, 제외 목록에도 추가하지 않았다. 토큰 없이 호출해 401을 확인했다.

**실습 3. 평점과 즐겨찾기를 함께 저장 (POST /movie-records)**

왜 이렇게 했나: 컨트롤러가 두 서비스를 연달아 부르면 각각 별도 트랜잭션이라 앞의 저장만 남을 수 있다. 전용 `MovieRecordService`가 바깥 트랜잭션을 열고, 안쪽 `create()`들은 그 트랜잭션에 참여한다.

```java
@Transactional
public MovieRecordResponse save(Long memberId, MovieRecordRequest request) {
    RatingResponse rating = ratingService.create(memberId,
            new CreateRatingRequest(request.movieId(), request.score(), request.comment()));

    FavoriteResponse favorite = null;
    if (request.addToFavorites()) {
        favorite = favoriteService.create(memberId, request.movieId());
    }
    return new MovieRecordResponse(rating, favorite);
}
```

**@Transactional과 DataSource.transaction() 비교**: 나는 Spring이라 `@Transactional`(선언형)을 썼다. NestJS의 `dataSource.transaction(async manager => {...})`는 콜백 안의 manager로 한 작업만 같은 트랜잭션이 된다. 선언형은 메서드 경계가 트랜잭션 경계이고, 콜백형은 코드 블록이 경계라서 manager를 빠뜨리면 트랜잭션 밖에서 저장될 수 있다. 목적(전부 성공 또는 전부 취소)은 같다.

**롤백 비교 실험**: 먼저 603을 즐겨찾기한 뒤 `POST /movie-records`(movieId 603, addToFavorites true)를 보냈다. 같은 요청을 `@Transactional` 유무로 비교했다.

| 조건 | 응답 | DB의 평점 |
| --- | --- | --- |
| `@Transactional` 있음 | 409 FAVORITE409_1 | 없음 (롤백) |
| `@Transactional` 잠시 제거 | 409 FAVORITE409_1 | **남음** (ratingId 6) |

제거 실험 뒤 코드는 바로 원복했고, 남은 실험 행도 삭제했다.

**실습 4. 실패 상황을 서비스 규칙으로 만들기**

| 상황 | 기대 | 실제 |
| --- | --- | --- |
| 토큰 없이 POST /favorites | 401 AUTH401_2 | 401 AUTH401_2 |
| 같은 movieId 두 번 | 409 FAVORITE409_1 | 409 FAVORITE409_1 |
| 즐겨찾기하지 않은 영화 여부 | 200 isFavorite:false | 200 false |
| 없는 즐겨찾기 해제 | 404 FAVORITE404_1 | 404 FAVORITE404_1 |
| movieId 0 | 400 | 400 COMMON4001 |
| 빈 목록 조회 | 200 [] | 200 [] |

**워크북과 다르게 한 점 / 한계**

- 워크북 Spring 참고 코드는 열어 보지 않고 명세와 요구사항만 보고 직접 작성했다. 클래스 이름과 응답 형식은 명세를 따랐지만 세부 문구(오류 메시지 등)는 워크북과 다를 수 있다.
- 즐겨찾기 중복 등록을 두 요청이 동시에 보내 DB UNIQUE가 실제로 막는 상황은 재현하지 못했다. 단위 테스트와 코드로만 확인했다.
- `POST /movie-records`는 `RatingService`·`FavoriteService`를 재사용한다. 재사용한 만큼 안쪽 서비스의 존재 검사가 한 번 더 일어난다.
- 선택 미션(다음 주차용 API 문서화)은 하지 않았다.
- 캡처는 Postman 화면이 아니라 실제 서버에 보낸 요청과 응답 기록을 카드로 렌더링한 것이다. 토큰은 줄여 표시했다. 같은 요청을 Postman에서 돌릴 수 있는 `postman/umc-week8.postman_collection.json`을 저장소에 넣었다. (accessToken 변수만 채우면 된다) 생성 스크립트는 `scripts/week8_capture.py`다.

## ⚡ 트러블 슈팅

**No.1 컨트롤러 테스트가 컴파일되지 않음 (void cannot be dereferenced)**

이슈: `FavoriteControllerTest`가 `void cannot be dereferenced` 오류 12개로 컴파일에 실패했다.

문제: 테스트 메서드 이름을 `status()`와 `delete()`로 지었는데, 같은 클래스에 정적 import한 `MockMvcResultMatchers.status()`와 `MockMvcRequestBuilders.delete()`와 이름이 겹쳐 클래스 안의 void 메서드가 먼저 선택됐다.

해결: `favoriteStatus()`, `deleteFavorite()`로 이름을 바꿨다.

**No.2 롤백이 정말 트랜잭션 덕분인지 확신이 없었음**

이슈: 409가 나면 평점도 안 남는 것을 확인했지만, 원래부터 안 남는 것일 수도 있다고 생각했다.

문제: 성공한 결과만 비교하면 `@Transactional`의 효과인지 알 수 없다.

해결: 같은 요청을 `@Transactional`을 뺀 코드에서 다시 보내, 평점 행이 남는 것을 확인한 뒤 원복했다.

참고레퍼런스: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
