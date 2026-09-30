# 5주차 Backend 노션 제출본 (원고)

> 노션 5주차 Backend 페이지에 붙이는 원고. 워크북 순서 그대로.
> 붙일 때는 Claude가 섹션별로 클립보드에 넣어 주는 것을 노션 **일반 줄**에서 `Cmd + V`.
> 🖼️ 자리에는 `docs/images/` 파일을 끌어다 놓는다. 토글 안에 넣지 않는다.

## 🎯 핵심 키워드

### 1. SOP와 CORS
- **SOP(Same-Origin Policy, 동일 출처 정책)** — 브라우저의 기본 보안 규칙. 지금 열린 페이지와 **출처(프로토콜 + 호스트 + 포트)**가 다른 곳의 응답을 JavaScript가 읽지 못하게 막는다. 내가 로그인해 둔 은행 사이트의 응답을, 다른 사이트의 스크립트가 몰래 읽어 가는 것을 막기 위해서다. `http://localhost:5173`과 `http://localhost:8080`은 포트 하나만 달라도 다른 출처다.
- **CORS(Cross-Origin Resource Sharing)** — SOP에 예외를 여는 약속. **서버가** 응답 헤더 `Access-Control-Allow-Origin: http://localhost:5173`으로 "이 출처는 읽어도 된다"고 알려 주면, 브라우저가 응답을 넘겨준다.
- **핵심 포인트**
  - 요청은 서버에 **도착하고 처리도 된다.** 브라우저가 응답을 JavaScript에 넘기기 직전에 막을 뿐이다. 그래서 서버 로그는 정상인데 화면은 비어 있다.
  - 허용 여부는 **서버 응답 헤더로만** 정해진다 → CORS 에러는 프론트 코드를 고쳐도 사라지지 않고, 원인은 항상 백엔드 설정이다.
  - PATCH·DELETE나 커스텀 헤더(`Authorization` 등)가 있으면 브라우저가 본 요청 전에 **preflight(OPTIONS)**로 "이 메서드·헤더 써도 되나"를 먼저 묻는다. 서버는 `Access-Control-Allow-Methods`·`-Headers`로 답한다.
  - 브라우저만의 규칙이라 Postman·Flutter 네이티브 앱(dio)은 적용받지 않는다. 실제로 같은 서버 설정에서 iOS 앱은 정상, 웹 앱은 차단됐다.
- **Spring 설정** — `WebMvcConfigurer.addCorsMappings()`로 전역 설정(이번 실습) 또는 `@CrossOrigin`으로 컨트롤러별 설정. 서비스 전체에 같은 정책이면 전역이 빠뜨릴 일이 없다.

### 2. Same-Origin과 Same-Site의 차이
| | Same-Origin | Same-Site |
| --- | --- | --- |
| 같다고 보는 기준 | 프로토콜 + 호스트 + **포트**까지 모두 같음 | **등록 가능 도메인(eTLD+1)**만 같음 (+ 스킴) |
| 예: `https://a.example.com` vs `https://b.example.com` | 다름 | 같음 |
| 예: `http://localhost:5173` vs `http://localhost:8080` | 다름 | 같음 |
| 브라우저가 판단하는 것 | 응답을 **JavaScript가 읽어도 되는가** (SOP·CORS) | 요청에 **쿠키를 자동으로 실어 보낼 것인가** (SameSite 쿠키) |

Same-Site가 훨씬 느슨하다. 이름은 비슷하지만 **판단하는 대상 자체가 다르다** — 하나는 "읽기", 하나는 "쿠키 전송".

### 3. 쿠키의 SameSite 속성
쿠키를 **다른 사이트에서 시작된 요청**(cross-site)에 실어 보낼지 정하는 속성. CSRF(사용자가 로그인된 상태를 이용해 다른 사이트가 요청을 위조하는 공격)를 막는 장치다.
- `Strict` — 같은 사이트에서 시작된 요청에만 보낸다. 외부 링크를 눌러 들어와도 첫 요청엔 안 실려 로그인이 풀린 것처럼 보일 수 있다.
- `Lax` — 기본값(대부분의 브라우저). 다른 사이트에서 **링크로 이동(GET 최상위 이동)**할 때는 보내고, 다른 사이트의 폼 POST·이미지·fetch에는 안 보낸다. 편의와 보안의 절충.
- `None` — 모든 요청에 보낸다. 반드시 `Secure`(HTTPS)와 함께 써야 한다. 다른 도메인의 프론트가 쿠키 기반 인증을 써야 할 때만.
- 이 과정의 로그인은 **헤더(`Authorization: Bearer`) 방식**이라 SameSite를 직접 건드리지 않는다. 헤더는 브라우저가 자동으로 실어 보내지 않으므로 CSRF 위험 자체가 적다.

### 4. 복합 UNIQUE 제약을 사용하는 이유
`UNIQUE KEY uk_favorite_member_movie (member_id, movie_id)` — 두 컬럼의 **조합**이 겹치지 않게 한다.
- 막으려는 것: "한 회원이 같은 영화를 두 번 즐겨찾기", "같은 영화에 평점 두 개". 각 컬럼 하나만 UNIQUE면 한 회원이 즐겨찾기를 하나밖에 못 하거나(member_id), 한 영화를 한 명만 즐겨찾기(movie_id)하게 돼 말이 안 된다. **조합**이어야 규칙이 정확하다.
- 왜 애플리케이션 코드 검사(`exists` 후 `save`)만으론 부족한가: 버튼 연타·동시 요청이면 두 요청이 동시에 "없음"을 보고 둘 다 저장할 수 있다(경쟁 상태). **DB 제약은 마지막 방어선**이라 이런 경우도 막는다. (4주차 제목 UNIQUE와 같은 이유)
- 덤: 이 조합으로 **인덱스**가 생겨 "이 회원이 이 영화에 남긴 평점" 조회가 빨라진다. 인덱스 순서가 (member_id, movie_id)라 member_id로 시작하는 조회(유저별 평점 조회)에도 쓰인다.
- 주의: 위반하면 DB가 예외(`DataIntegrityViolationException`)를 던지므로, 6주차 평점 생성 API에서 이를 409 Conflict로 바꿔 줘야 한다.

### 5. 외부 Open API Key는 프론트/백엔드 중 어디에?
**원칙: 비밀이어야 하는 키는 백엔드에 둔다.**
- 프론트(웹 JS·앱 번들)에 넣은 값은 사용자 기기에 그대로 내려간다. 웹은 개발자 도구 Network 탭에서, 앱은 번들을 풀어서 볼 수 있다. `.env`와 `.gitignore`는 **GitHub 커밋을 막을 뿐** 배포된 앱 안에서는 숨겨지지 않는다. (프론트 5주차에서 TMDB Token을 `.env` → asset으로 넣으면서 확인한 부분)
- 키가 새면: 남이 내 키로 호출해 **요금·호출 한도가 소진**되고, 쓰기 권한이 있는 키라면 데이터까지 위험하다.
- 백엔드에 두는 방법: 프론트 → **우리 서버(프록시)** → 외부 API. 키는 서버 환경 변수에만 있고, 서버에서 호출 횟수 제한·캐싱·응답 가공도 할 수 있다.
- 예외: **공개용으로 설계된 키**(예: 지도 SDK의 브라우저 키)는 프론트에 두되, 콘솔에서 **허용 도메인·앱 번들 ID 제한**을 걸어 다른 곳에서 못 쓰게 한다.
- 이번 과정: TMDB는 읽기 전용이고 학습용이라 프론트가 직접 호출(워크북 결정). 실제 서비스라면 TMDB 호출도 우리 서버를 거치게 하는 편이 안전하다.

## ✅ 실습 체크리스트
- [x] 로컬 MySQL에 member, rating, favorite 테이블이 생성되어 있고, member/rating에는 시드 데이터도 들어 있다.
- [x] Postman으로 GET /members/nickname/{nickname}, GET /members/email/{email}이 available 값을 올바르게 응답한다.
- [x] Postman으로 GET /members/{memberId}/ratings가 최신순으로 평점 목록을 응답한다.
- [x] CORS 설정 전, 간이 프론트에서 콘솔에 CORS 에러가 뜨는 것을 직접 확인했다.
- [x] CORS 설정 후, 같은 간이 프론트에서 정상 응답을 확인했다.

## ☑️ 실습 인증
- 🖼️ `postman-01-postman-required.png` — API 3개 요청 URL·상태·응답 JSON
- 🖼️ `01-cors-before-page.png` + Console 로그(아래) — CORS 설정 전
- 🖼️ `02-cors-after-5173.png` — CORS 설정 후
- 🖼️ `05-app-my-ratings-ios.png` · `08`~`10` 회원가입 3장 — 실제 앱에서 호출

```
Access to fetch at 'http://localhost:8080/members/nickname/%EC%88%98%ED%98%84' from origin 'http://localhost:5173' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
TypeError: Failed to fetch
```

## 💪 미션 기록

(Claude가 섹션별로 클립보드에 넣어 줌 — 1) 새 프로젝트·DB 2) 엔티티 3) Repository·DTO 4) Service·Controller 5) CORS 6) 실제 앱 연결 7) 선택 미션)

## ⚡ 트러블슈팅
`docs/week5-troubleshooting.md` 6건 (이슈 / 문제 / 해결 / 참고레퍼런스)

## 📢 학습 후기
(학습 세션 후 작성)
