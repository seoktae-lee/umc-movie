# Infra 2주차 — 관측 가능한 영화 백엔드

개인 워크북: https://www.notion.so/e49b57f4596b83b0915a81cfc41f22b0

## 준비와 검증 완료

- Actuator와 Prometheus registry, HTTP 히스토그램을 추가했다.
- 관리 포트는 컨테이너 9091이다. Docker는 `127.0.0.1:9091:9091`로 게시하며 ALB는 8080만 사용한다. 로컬 Compose는 `127.0.0.1:18091:9091`이다.
- Spring 테스트와 bootJar, Docker 빌드가 통과했다.
- 로컬 테스트 전용 회원으로 `POST /movie-records` HTTP 201을 확인했다. HTTP 경로별 histogram bucket, JVM, HikariCP 지표가 `/actuator/prometheus`에 노출된다.
- `dashboard.json`은 워크북 Node.js 대시보드를 Spring 메트릭으로 수정한 준비본이다. 아직 Grafana에 등록하지 않았다.
- `pipelines`는 워크북 자산을 바탕으로 준비했다. 앱 수집 주소는 `127.0.0.1:9091`, 경로는 `/actuator/prometheus`다.
- `bootstrap-ec2.sh`는 기존 1주차 부트스트랩에 Alloy를 추가한 준비본이다. `APP_IMAGE`에 고정한 ECR 이미지 URI를 넣어 시작 템플릿 user-data로 사용해야 한다. 아직 AWS에 적용하지 않았다.

Spring 히스토그램 설정은 `management.metrics.distribution` 아래에 둔다. 참고: https://docs.spring.io/spring-boot/3.5/reference/actuator/metrics.html

## 다음 실행 순서

1. AWS 현재 ASG·ALB·RDS·무료 크레딧 상태를 확인한다. CPU Standard를 유지한다.
2. Grafana Fleet/Prometheus URL과 사용자 ID, 쓰기 토큰을 확보한다. 토큰과 MySQL DSN은 SecureString으로 `/umc-movie/week2/`에 저장한다.
3. 기존 EC2 역할에 이 경로의 GetParameter만 추가한다. MySQL 관측 전용 계정을 준비한다.
4. Fleet에 호스트·MySQL·앱 메트릭 파이프라인을 등록한다. 환경 변수는 부트스트랩에 명시돼 있다. 토큰을 코드나 워크북에 쓰지 않는다.
5. 새 이미지를 ECR에 태그로 올리고 시작 템플릿을 새 버전으로 만든 뒤 순차 교체한다. 두 Alloy와 `up` 지표를 확인한다.
6. 대시보드를 등록한 뒤 `run-load.py`로 단계마다 새 회원을 만들어 실험한다. 원본에서 Authorization을 지우고 결과 파일에 저장한다.
7. CPU·MySQL 게이지와 처리량을 함께 기록하고 한계를 해석한다. SLO 초안은 `SLO.md`에 있다.
8. 지정된 이메일 Contact point와 p95 1초 초과 1분 알림을 생성하고 실제 알림 수신을 확인한다. 아직 알림이나 실측 결과는 없다.

```bash
BASE_URL=http://<실제-ALB-DNS> python3 infra/week2/run-load.py --rps 200
```

같은 방식으로 400, 600, 800을 실행하되 단계 사이 회복 상태를 확인한다. 각 단계는 40초, 타임아웃 5초, 기준 구간 8초다. 사전 요청은 영화 ID 999999999를 사용해 본 측정의 1부터 시작하는 counter와 겹치지 않는다.

## 연결 상태와 승인

Grafana 스택 `crimsoncrocodile2803.grafana.net` 생성은 확인했다. AWS 로그인 완료는 사용자에게 전달받았으나, 컴퓨터 제어가 AWS의 비활성 메뉴만 읽고 있어 서버·크레딧의 최신 상태는 아직 확인하지 못했다. 이 상태에서 AWS나 Grafana 설정 변경을 진행하지 않았다.

사용자는 관측용 Grafana 쓰기 토큰, EC2 관측 파라미터 읽기 권한, MySQL 관측 계정 생성과 위 Grafana 스택으로 CPU·메모리·DB 연결 수·HTTP 경로별 통계 전송을 승인했다. 지정한 본인 이메일로 테스트·실제 임계값 알림 전송도 승인했다. 해당 승인은 이어서 사용하며 같은 내용을 다시 확인받지 않는다.

2주차 필수 미션은 아직 완료되지 않았다. 개인 워크북에는 검증된 준비 과정만 기록했다.
