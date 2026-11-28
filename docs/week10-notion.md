# 10주차 Backend 노션 제출본 (원고)

> 노션 "태이의 워크북 / 10주차 - 배포하고 결과물 완성하기"에 넣는 원고. 페이지의 섹션 순서 그대로다.
> `🖼️` 자리에 `docs/images/week10/` 이미지를 넣는다.

## 🎯 핵심 키워드

**개발 환경/운영 환경** — 같은 코드라도 어디서 도느냐에 따라 DB 주소, 비밀 값, 허용할 Origin, 스키마 정책이 달라야 한다. Spring은 이것을 프로필로 나눈다. `application.yml`에는 모든 환경에 같은 값(ddl-auto=validate, JWT 키 이름)만 두고, `application-local.yml`은 내 컴퓨터 MySQL, `application-prod.yml`은 RDS를 본다. `SPRING_PROFILES_ACTIVE=prod`로 실행하면 운영 파일이 공통 파일을 덮어쓴다. 기본 프로필을 `local`로 정해 두어 아무것도 지정하지 않으면 운영 설정을 실수로 쓰지 않는다. 테스트는 9주차처럼 `@ActiveProfiles("test")`로 H2를 쓴다.

**빌드 산출물(JAR, dist)** — 소스 코드를 서버가 바로 실행할 수 있는 한 덩어리로 만든 것. Spring은 `./gradlew bootJar`로 의존 라이브러리와 내장 Tomcat까지 들어간 실행 가능한 JAR(약 58MB)을 만든다. 서버에는 소스나 Gradle이 필요 없고 JDK 21만 있으면 `java -jar`로 돈다. NestJS는 TypeScript를 JS로 컴파일한 `dist`가 산출물이고, 서버에 Node.js가 필요하다.

**EC2와 RDS** — EC2는 내 서버(가상 컴퓨터)를 빌리는 것, RDS는 AWS가 관리해 주는 데이터베이스다. 앱은 EC2에서 돌고 데이터는 RDS에 있으므로, EC2를 재부팅하거나 새로 만들어도 데이터는 남는다. 실제로 EC2를 재부팅한 뒤에도 평점 두 건이 그대로 조회됐다.

**VPC와 보안 그룹** — VPC는 AWS 안의 내 사설 네트워크, 보안 그룹은 리소스 앞의 방화벽이다. 보안 그룹의 소스를 IP 대신 "다른 보안 그룹"으로 줄 수 있다. RDS 3306의 소스를 EC2 보안 그룹으로 지정하면 EC2만 DB에 붙을 수 있고, 퍼블릭 액세스를 꺼서 인터넷에서는 주소조차 풀리지 않는다. 0.0.0.0/0은 앱 포트 8080에만 썼다.

**환경변수와 비밀 값** — DB 비밀번호와 JWT 서명 키는 코드·설정 파일·GitHub 어디에도 적지 않았다. AWS SSM Parameter Store에 SecureString(암호화)으로 넣고, EC2가 부팅할 때 자기 IAM 역할로 읽어서 root만 읽을 수 있는 환경 파일(`/etc/movie.env`, 권한 600)에 쓴다. `application-prod.yml`은 `${DB_PASSWORD}`처럼 이름만 가진다. 기본값을 주지 않았기 때문에 값이 빠지면 조용히 잘못된 값으로 뜨지 않고 시작 단계에서 실패한다.

**CORS와 Origin** — Origin은 스킴+호스트+포트(`http://localhost:5173`)다. 브라우저는 다른 Origin의 API 응답을 서버가 허락했을 때만 읽게 해 준다. 허용 목록은 운영에서 `FRONTEND_ORIGINS` 환경변수로 받는다. 배포 서버에 프리플라이트를 보내 보니 `localhost:5173`은 200과 `Access-Control-Allow-Origin`을 받았고, 목록에 없는 Origin은 403을 받았다. Postman이나 Flutter 모바일 앱은 브라우저가 아니라서 CORS와 상관없다.

## 📢 학습 후기

(초안) **내 컴퓨터에서 성공한 테스트와 외부 주소에서 성공한 요청은 어떤 점이 달랐나요?** 테스트 97개는 H2 메모리 DB, 내 JDK, 내 `.env` 위에서 통과한 것이다. 배포 서버의 요청은 다른 컴퓨터(EC2), 다른 DB(RDS MySQL 8.4), 다른 설정(prod 프로필), 다른 비밀 값 위에서 성공했다. 코드가 같아도 "주소·계정·네트워크 규칙·런타임"이 모두 바뀌기 때문에 테스트 통과가 배포 성공을 보장하지 않는다. 실제로 막힌 곳도 코드가 아니라 빌드 환경(JRE와 JDK 차이)이었다. 반대로 같은 스키마 파일을 쓰고 운영도 ddl-auto=validate로 두었기 때문에 "로컬은 되는데 운영 테이블이 다르다"는 문제는 생기지 않았다.

**배포 중 가장 오래 막혔던 문제는 무엇이었고, 어떤 확인으로 원인을 찾았나요?** CloudShell에서 JAR을 만들 때 Gradle이 "Java 21 설치를 찾을 수 없다"고 실패했다. `java -version`은 21이 나와서 처음엔 이해가 안 됐는데, 그건 컴파일러가 없는 JRE였고 Gradle toolchain은 `javac`가 있는 JDK를 찾는다. JDK를 깔아도 이미 떠 있던 Gradle 데몬이 예전 탐색 결과를 들고 있어서 같은 오류가 한 번 더 났다. 데몬을 끄고 JDK 경로를 직접 알려 준 뒤에 빌드됐다. 그다음 EC2가 떴는데 API가 응답이 없어서 SSM으로 부팅 로그를 봤더니, 패키지 설치 → JAR 다운로드 → 스키마 적용 → Spring 시작이 순서대로 진행 중이었을 뿐이었다. t3.micro에서는 Spring이 뜨는 데만 1분 넘게 걸렸다. "안 된다"와 "아직 안 됐다"를 로그로 구분하는 게 중요했다.

## ✅ 실습 체크리스트

- [x] 9주차 단위/통합 테스트가 통과하는지 확인했어요. (`./gradlew test` 97개, 실패 0)
- [x] 개발/운영 설정을 분리하고 운영 DB의 자동 스키마 변경을 껐어요. (`application-prod.yml` ddl-auto=validate, 검증 테스트 추가)
- [x] RDS를 준비하고 EC2에서만 DB 포트에 접속할 수 있도록 설정했어요. (퍼블릭 액세스 False, 3306 소스 = EC2 보안 그룹)
- [x] 빌드 산출물을 EC2에서 실행하고 SSH를 닫은 뒤에도 API가 열리는지 확인했어요. (systemd 서비스, SSH 없이 운영, 재부팅 후 자동 복구)
- [x] 프론트 Origin과 API 주소를 설정하고 회원가입/로그인/조회/등록을 점검했어요.
- [x] 키 페어, DB 비밀번호, JWT 키의 Git 추적 여부와 AWS 예상 비용을 확인했어요. 실습 종료 시 EC2/RDS 중지/삭제 계획도 적었어요.

## ☑️ 실습 인증

**배포된 API 주소**: `http://52.64.68.67:8080` (Swagger: `http://52.64.68.67:8080/swagger-ui.html`)

🖼️ w10-01-swagger-deployed.png — 배포 서버에서 열린 Swagger. Servers가 `http://52.64.68.67:8080`이다.

🖼️ w10-02-test-build.png — 배포 전 `./gradlew test` 97개 통과와 `bootJar` 결과(58MB JAR).

🖼️ w10-03-api-standard.png — 배포 주소에서 회원가입 201 → 로그인 200 → 토큰 없이 `GET /members/me` 401 → 토큰으로 200 → 평점 201 → 공개 조회 → 즐겨찾기 빈 목록·등록 201·여부 조회·목록·중복 409. 성공/실패 모두 `isSuccess, code, message, result` 형식이다. (토큰·비밀번호는 `***`)

🖼️ w10-04-cors.png — 프리플라이트: `http://localhost:5173`은 200 + `Access-Control-Allow-Origin`, 목록에 없는 Origin은 403.

🖼️ w10-05-reboot-alive.png — EC2를 재부팅해도 약 2분 30초 뒤 API가 다시 200을 주고, 재부팅 전 데이터가 RDS에 그대로 있다.

🖼️ w10-07-aws-resources.png — EC2(t3.micro, running), RDS(db.t4g.micro, PubliclyAccessible False), 보안 그룹(EC2는 8080만, DB 3306은 EC2 보안 그룹에서만).

## 💪 미션 기록

**미션 완료 정보**

| 항목 | 내용 |
| --- | --- |
| 스택 | 🍃 Spring Boot 3.5 · Java 21 · MySQL 8.4 |
| 저장소 / 브랜치 | https://github.com/seoktae-lee/umc-movie · `feature/week-10` |
| Pull Request | https://github.com/seoktae-lee/umc-movie/pull/4 |
| 배포 주소 | `http://52.64.68.67:8080` |
| 리전 / 리소스 | ap-southeast-2 · EC2 t3.micro(Amazon Linux 2023) · RDS MySQL 8.4 db.t4g.micro 20GB(gp3, 암호화) · S3(비공개, JAR 전달용) · SSM Parameter Store |

**실습 1. 개발/운영 설정 분리**

| 파일 | 내용 |
| --- | --- |
| `application.yml` | 공통: `spring.profiles.default=local`, ddl-auto=validate, `jwt.secret=${JWT_SECRET}` |
| `application-local.yml` | 로컬 MySQL(`umc_movie`), `.env` 읽기, CORS `http://localhost:5173` |
| `application-prod.yml` | `server.address=0.0.0.0`, `jdbc:mysql://${DB_HOST}:3306/movie_db`, `${DB_USER}`/`${DB_PASSWORD}`, CORS `${FRONTEND_ORIGINS}`, ddl-auto=validate |
| `src/test/resources/application-test.yml` | 9주차 H2 그대로 + CORS 목록 추가(공통 파일에서 옮겼기 때문) |

- `.env.production.example`에는 키 이름만 두고 값은 비웠다. `.gitignore`에 `.env.*`(예시 파일만 예외)와 `*.pem`을 추가했다.
- `ProfileConfigTest`: 운영 프로필이 validate인지, DB 주소·계정·비밀번호·Origin이 전부 `${...}`인지, 기본 프로필이 local인지 YAML을 직접 읽어 확인한다.

**실습 2. RDS와 EC2 준비** — `deploy/week10/create.sh`(CloudShell에서 실행)

1. 보안 그룹 두 개: `umc-movie-week10-ec2`(8080 ← 0.0.0.0/0), `umc-movie-week10-db`(3306 ← EC2 보안 그룹).
2. DB 비밀번호·JWT 키를 `openssl rand`로 만들어 SSM SecureString에만 저장. 화면이나 파일에 출력하지 않는다.
3. RDS: MySQL 8.4, `--db-name movie_db`, `--no-publicly-accessible`, 스토리지 암호화, 백업 보존 0(실습 데이터만).
4. 스키마: 2주차 `01_schema.sql`에서 `CREATE DATABASE`/`USE`만 뺀 `10_prod_schema.sql`. EC2가 처음 부팅할 때 `member` 테이블이 없을 때만 적용한다. 운영 앱은 테이블을 만들지 않는다(validate).

**실습 3. 빌드한 서버를 EC2에서 실행**

- CloudShell에서 `./gradlew clean bootJar` → 비공개 S3 버킷에 업로드.
- EC2 user-data(`deploy/week10/user-data.sh`): Corretto 21 설치 → S3에서 JAR·스키마 받기 → SSM에서 비밀 값 읽어 `/etc/movie.env`(600) 작성 → systemd `movie.service` 등록·시작(`Restart=on-failure`). 로그는 `~/movie-app.log`.
- 시작 로그에서 `The following 1 profile is active: "prod"`, `Tomcat initialized with port 8080` 확인.

**워크북과 다르게 한 점 (이유)**

- **SSH 키 페어(.pem) 대신 SSM**: 22번 포트를 아예 열지 않았다. EC2 역할에 `AmazonSSMManagedInstanceCore`를 붙이고 로그 확인은 SSM Run Command로 했다. 잃어버리거나 유출될 `.pem` 파일이 없고, "SSH를 닫아도 살아 있나"는 처음부터 SSH 없이 systemd로 돌아가고 재부팅 후에도 자동으로 다시 뜨는 것으로 확인했다.
- **`nohup java -jar &` 대신 systemd**: 프로세스가 죽거나 서버가 재부팅돼도 다시 시작된다.
- **JAR 전달은 S3**: scp 대신 비공개 버킷 + EC2 역할의 `s3:GetObject` 권한만 썼다.

**실습 4. 프론트 Origin과 배포 결과 점검**

- 프론트 Origin: `FRONTEND_ORIGINS=http://localhost:5173`(SSM 파라미터 → 환경 파일). 프론트를 아직 배포하지 않아서 로컬 개발 주소만 넣었다. 프론트를 배포하면 쉼표로 실제 Origin을 추가하고 서비스를 재시작하면 된다.
- 점검 결과(로컬에서 배포 주소로 요청, 원본 로그 `docs/logs/week10-api-check.txt`):

| 요청 | 결과 |
| --- | --- |
| `POST /members` | 201 COMMON201 |
| `POST /auth/login` | 200, accessToken |
| `GET /members/me` 토큰 없음 / 있음 | 401 AUTH401_2 / 200 |
| `POST /ratings` → `GET /members/1/ratings` | 201 → 목록에 보임 |
| `GET /favorites`(빈 목록) → `POST /favorites` → `GET /favorites/27205` → `GET /favorites` | 200 `[]` → 201 → `isFavorite: true` → 1건 |
| `POST /favorites` 중복 | 409 FAVORITE409_1 |
| CORS 프리플라이트 `localhost:5173` / 다른 Origin | 200 + 허용 헤더 / 403 |

- `GET /members/{memberId}/ratings`는 5주차 공개 API라 공통 응답으로 감싸지 않은 배열을 그대로 내려 준다(9주차까지와 같음).

**선택 미션: 배포 환경에서 `POST /movie-records` 롤백 확인**

- 정상: movieId 157336, `addToFavorites: true` → 201, rating과 favorite이 함께 생기고 `GET /favorites/157336`이 true.
- 실패: movieId 155를 먼저 즐겨찾기한 뒤 같은 영화로 평점+즐겨찾기 통합 저장 → 409 FAVORITE409_1. 평점은 즐겨찾기보다 **먼저** 저장되는 단계이므로, 트랜잭션이 없다면 평점만 남아야 한다.
- 후속 조회 `GET /members/1/ratings` → `[157336, 27205]`, **155 평점 없음**. 운영 RDS에서도 평점 저장이 롤백됐다. 9주차 H2 통합 테스트(`MovieRecordTransactionTest`)와 같은 결과다.
- 운영 DB에 실습 데이터가 남는 점을 고려해 이번 점검 전용 테스트 계정(`@example.com`)과 영화 ID를 따로 썼다.

🖼️ w10-06-challenge-rollback.png

**비용 확인과 리소스 정리 계획**

| 리소스 | 사양 | 대략적 비용(시드니) |
| --- | --- | --- |
| EC2 | t3.micro + 퍼블릭 IPv4 + gp3 10GB | 시간당 약 $0.02 |
| RDS | db.t4g.micro + gp3 20GB | 시간당 약 $0.03 |
| S3·SSM | JAR 58MB, 파라미터 3개 | 무시할 수준 |

- 합계 하루 약 $1~1.3. AWS 크레딧에서 차감되는지 Billing > Credits에서 확인한다.
- 정리: 리뷰가 끝나면 `bash deploy/week10/cleanup.sh` 한 번으로 EC2 종료 → RDS 삭제(스냅샷 없음, 실습 데이터만) → 서브넷 그룹·보안 그룹·S3 버킷·SSM 파라미터·IAM 역할까지 지운다. 남는 스토리지나 스냅샷이 없는지 콘솔에서 한 번 더 본다.

**한계**

- HTTP만 열었다. 프론트가 HTTPS로 배포되면 브라우저가 혼합 콘텐츠로 막으므로 ALB+ACM 인증서나 리버스 프록시가 필요하다.
- EC2 퍼블릭 IP는 인스턴스를 중지했다 켜면 바뀐다(재부팅은 유지). 고정하려면 탄력적 IP가 필요하다.
- Swagger를 학습용으로 열어 두었다. 실제 서비스라면 운영에서는 끈다.

## ⚡ 트러블 슈팅

**⚡이슈 No.1 CloudShell에서 bootJar가 "Cannot find a Java installation ... languageVersion=21"로 실패**

- 증상: `java -version`은 21인데 Gradle이 Java 21을 못 찾음.
- 확인: `ls /usr/lib/jvm` → CloudShell 기본 java는 `java-21-openjdk`의 **JRE**(javac 없음). `build.gradle`의 toolchain(`JavaLanguageVersion.of(21)`)은 컴파일러가 있는 JDK를 찾는다. 처음 스크립트는 `java -version` 출력만 보고 "21이 있다"고 판단해 JDK 설치를 건너뛰었다.
- 수정: `java-21-amazon-corretto-devel` 설치. 설치 직후에도 같은 오류가 났는데, 이미 떠 있던 Gradle 데몬이 이전 탐색 결과를 들고 있었기 때문이다. `./gradlew --stop` 후 `~/.gradle/gradle.properties`에 `org.gradle.java.installations.paths=/usr/lib/jvm/java-21-amazon-corretto`를 적었다. 스크립트도 `java -version` 대신 `javac` 존재 여부로 판단하도록 고쳤다.
- 재검증: bootJar 성공 → S3 업로드 → 이후 단계 진행.

**⚡이슈 No.2 EC2는 running인데 API가 응답 없음(curl 000)**

- 증상: 인스턴스 생성 후 몇 분째 `http://52.64.68.67:8080` 연결 실패.
- 확인: SSH가 없으므로 `aws ssm send-command`로 `/var/log/umc-movie-bootstrap.log`, `systemctl is-active movie`, `~/movie-app.log`를 읽었다. 순서대로 패키지 설치 → JAR 다운로드 → 서비스 등록 → `profile is active: "prod"` → `Tomcat initialized with port 8080`가 진행 중이었다.
- 원인: 오류가 아니라 t3.micro(1GB)에서 Spring 시작이 1분 넘게 걸린 것. 보안 그룹·DB 연결 문제였다면 로그에 `Communications link failure`가 남았을 것이다.
- 재검증: 약 1분 뒤 200. 재부팅 후에도 약 2분 30초 만에 자동 복구.

**⚡이슈 No.3 프로필을 나눈 뒤 통합 테스트 22개가 ApplicationContext 로딩 실패**

- 증상: `./gradlew test`에서 `@ActiveProfiles("test")`를 쓰는 4개 클래스(OpenApiDocs·Rating·MovieRecordTransaction·Favorite)의 22개가 `ApplicationContext failure threshold (1) exceeded`로 실패. 단위 테스트는 통과.
- 확인: 공통점은 test 프로필뿐이었다. 이번에 옮긴 값 중 `WebConfig`가 생성자에서 `@Value("${app.cors.allowed-origins}")`로 꼭 받아야 하는 값이 있다.
- 원인: CORS 목록을 공통 `application.yml`에서 `application-local.yml`로 옮겼는데, test 프로필은 local 파일을 읽지 않아 이 값이 비었다.
- 수정: `application-test.yml`에 테스트용 CORS 목록을 추가. 97개 통과.
