# 5주차 미션 완료 정보 (Backend) — 화면과 API를 처음 연결하기

| 항목 | 내용 |
| --- | --- |
| 이름 / 닉네임 | 이석태 / 태이 |
| 선택 스택 | Spring Boot 3.5.0 · Java 21 · Gradle · MySQL 26.7 |
| 저장소 | (GitHub 저장소 생성 후 기입) — 로컬 `~/development/UMC-11th/umc-movie` |
| 연결한 화면 | MovieLog Flutter 앱 — https://github.com/seoktae-lee/movielog/pull/7 |
| 테스트 | `./gradlew test` 15개 통과 (Controller 11 · Service 3 · contextLoads 1) |
| Postman | `postman/umc-week5.postman_collection.json` 13요청 13검증 통과 (newman) |
| 트러블슈팅 | `docs/week5-troubleshooting.md` (6건) |

## 실습 체크리스트

- [x] 로컬 MySQL에 member·rating·favorite 테이블 + member/rating 시드 (`docs/sql/01_schema.sql`, `02_seed.sql`)
- [x] Postman: `GET /members/nickname/{nickname}`, `GET /members/email/{email}` available 응답
- [x] Postman: `GET /members/{memberId}/ratings` 최신순 목록, 없는 회원 404
- [x] CORS 설정 전: 간이 프론트 Console에 CORS 에러 (`images/01-cors-before-page.png`, `cors-console-before.txt`)
- [x] CORS 설정 후: 같은 간이 프론트에서 정상 응답 (`images/02-cors-after-5173.png`)

## 미션

| 구분 | 내용 | 증거 |
| --- | --- | --- |
| 필수 | 실제 앱 화면에서 API 3개 호출 | iOS 마이페이지 내 평점(`05`), 회원가입 중복 확인 3장면(`08`~`10`), 웹 CORS 허용/차단(`06`·`07`) |
| 선택 | 닉네임 2~12자·이메일 형식 → 400 | `postman-02`, `ValidationExceptionHandler` |
| 선택 | 페이지네이션 `?page=0&size=10` | `postman-02` (page=0·1, size=1) |
| 선택 | CORS 허용 범위 비교 | 5173만 허용(`03`: 5174 차단) vs `*`(`04`: 5174 허용, 모르는 출처에도 `*`) |

## 설계 메모

- 영화는 TMDB 것이라 테이블을 만들지 않고 `movie_id`를 **FK가 아닌 일반 컬럼**으로 둔다.
- `rating`·`favorite`에 `(member_id, movie_id)` **복합 UNIQUE** — 같은 회원이 같은 영화에 두 번 남기지 못하게 DB가 막는다.
- 응답은 Entity가 아니라 record DTO. `RatingResponse`에는 `member`·`password`가 나가지 않는다.
- `available`은 `exists`를 뒤집은 값 — 화면이 "쓸 수 있나?"를 바로 읽도록.
- 평점 0개 회원은 **빈 배열(200)**, 없는 회원은 **404** — 둘을 구분한다.
- CORS 허용 출처는 `application.yml`의 `app.cors.allowed-origins`에서 읽는다(코드 수정 없이 바꿔 비교).

## 앱 연결 (MovieLog Flutter, `feature/week-5`)

```
[회원가입] 중복 확인 버튼 ─ SignUpViewModel ─ MovieLogMemberService ─ GET /members/nickname/{n}, /members/email/{e}
[마이페이지] 내 평점 ───── MyRatingsViewModel ─┬ MovieLogMemberService ─ GET /members/1/ratings   (우리 서버)
                                                └ TmdbMovieService ─────── GET /movie/{movieId}      (TMDB, 제목·포스터)
```

- 자체 API Client는 TMDB Client와 분리 (TMDB Token이 우리 서버로 가지 않게).
- 로그인이 7주차라 "나"는 시드 1번 회원으로 고정.
- 서버가 준 400·404 message를 그대로 화면에 보여 준다.
