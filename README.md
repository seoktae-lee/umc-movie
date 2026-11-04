# umc-movie

UMC 11기 Backend 워크북 5주차부터 만드는 **영화 평점 서비스** 백엔드.
영화 정보는 프론트가 TMDB에서 직접 받고, 이 서버는 회원·평점·즐겨찾기처럼 **우리 서비스에만 있는 데이터**를 맡는다.

- Spring Boot 3.5.0 · Java 21 · Gradle · Spring Data JPA · MySQL
- 프론트: [MovieLog (Flutter)](https://github.com/seoktae-lee/movielog)

## 실행

```bash
# 1. DB (로컬 MySQL)
mysql -uroot < docs/sql/01_schema.sql
mysql -uroot < docs/sql/02_seed.sql

# 2. JWT 설정 (.env는 깃에 올리지 않는다. JWT_SECRET은 32자 이상)
cp .env.example .env

# 3. 서버 (root 비밀번호가 있으면 DB_PASSWORD 환경 변수로 넘긴다)
DB_PASSWORD= ./gradlew bootRun     # http://localhost:8080

# 4. CORS 확인용 간이 프론트
cd mock-frontend && npx serve -l 5173   # http://localhost:5173
```

## API (5주차)

| API | Method | URL | Request | Success Response (200) |
| --- | --- | --- | --- | --- |
| 닉네임 중복 체크 | GET | `/members/nickname/{nickname}` | Path: nickname (string, 2~12자) | `{"available": true}` |
| 이메일 중복 체크 | GET | `/members/email/{email}` | Path: email (string, 이메일 형식) | `{"available": true}` |
| 유저별 평점 조회 | GET | `/members/{memberId}/ratings` | Path: memberId (number) / Query: page(기본 0), size(기본 20, 1~50) | `[{"ratingId": 1, "movieId": 550, "score": 5, "comment": "최고의 영화!", "createdAt": "…", "updatedAt": "…"}]` |

오류: 형식 오류 `400 {"status": 400, "message": "닉네임은 2~12자여야 합니다."}` / 없는 회원 `404 {"message": "존재하지 않는 유저입니다.", …}`

## API (7주차: 토큰 기반)

`필요`로 표시한 API는 헤더에 `Authorization: Bearer {토큰}`을 함께 보낸다.

| API | Method | URL | 토큰 | 성공 |
| --- | --- | --- | --- | --- |
| 로그인 | POST | `/auth/login` | 불필요 | 200 `{nickname, accessToken}` |
| 내 정보 조회 | GET | `/members/me` | 필요 | 200 `{email, nickname, createdAt}` |
| 내 정보 수정 | PATCH | `/members/me` | 필요 | 200 `{email, nickname, createdAt}` |
| 평점 등록 | POST | `/ratings` | 필요 | 201 평점 |
| 평점 수정 | PATCH | `/ratings/{ratingId}` | 필요 | 200 평점 |
| 평점 삭제 | DELETE | `/ratings/{ratingId}` | 필요 | 200 `result: null` |

6주차의 `/members/{memberId}/ratings` 등록·수정·삭제는 없어졌다. 같은 경로의 조회(GET)는 공개 API로 남아 있다.

## 구조

```
src/main/java/com/umc/movie
├── config       WebConfig (CORS·관문 등록), PasswordConfig
├── controller   AuthController, MemberController, RatingController, GlobalExceptionHandler
├── interceptor  AuthInterceptor (토큰 검사)
├── util         JwtUtil (토큰 발급·검증)
├── exception    ApiException, ErrorCode, GeneralException
├── service      AuthService, MemberService, RatingService
├── repository   MemberRepository, RatingRepository
├── entity       BaseTimeEntity, Member, Rating
└── dto          요청·응답 record, ApiResponse
```

문서: [docs/week5-report.md](docs/week5-report.md) · [docs/week5-troubleshooting.md](docs/week5-troubleshooting.md) · Postman: [postman/umc-week5.postman_collection.json](postman/umc-week5.postman_collection.json)

7주차 문서: [docs/week7-notion.md](docs/week7-notion.md) · [docs/week7-troubleshooting.md](docs/week7-troubleshooting.md) · Postman: [postman/umc-week7.postman_collection.json](postman/umc-week7.postman_collection.json)
