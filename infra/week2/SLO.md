# Infra 2주차 SLO 정의서

## 대상과 목표

- 서비스: UMC 영화 백엔드, `POST /movie-records`로 평점과 즐겨찾기를 한 트랜잭션에서 저장한다.
- 가용성 SLI: 유효한 쓰기 요청 중 정상 응답한 비율. 부하 실험에서는 HTTP 201을 성공으로 본다.
- 가용성 SLO: 최근 30일 유효 요청의 99% 이상 성공. 오류 예산은 1%다.
- 지연 SLI: 서버에서 측정한 HTTP 요청 처리 시간의 히스토그램. 인스턴스의 버킷을 합산한 뒤 p95를 계산한다.
- 지연 SLO: 최근 30일 유효 요청의 95% 이상이 1초 이내 완료.
- 알림: `/movie-records` 서버 p95가 1초를 1분 이상 초과하면 석태님이 지정한 이메일로 전송한다. 데이터 없음과 수집 실패도 별도로 구분한다.

이는 학습용으로 정한 목표다. 수십 초의 부하 실험으로 30일 SLO 달성을 증명하지 않는다. 부하 도구의 지연에는 클라이언트와 시드니 리전 사이 네트워크 왕복 시간이 포함되므로 서버 지연과 구분한다.

## 검증 계획

1. 단계마다 새 테스트 회원을 만들고, 별도 영화 ID로 쓰기 1건을 보내 HTTP 201을 확인한다.
2. 200 → 400 → 600 → 800 RPS를 단계별 40초 측정한다. 타임아웃 5초, 기준 구간 8초. 각 단계 사이 회복 상태를 확인한다.
3. 처리량·p95·실패 유형·호스트별 CPU·MySQL threads_connected/threads_running을 기록한다. 중복 평점에 따른 409와 서버 포화를 구분한다.
4. 처리량이 목표를 따르지 못하고 p95·타임아웃이 함께 증가하는 구간을 한계로 판단한다. 관측 범위에서 꺾이지 않으면 한계를 단정하지 않는다.
5. 측정된 포화 구간의 부하를 제한된 시간 동안 재현해 실제 p95 알림 수신을 확인한다.

## 쿼리

```promql
sum(rate(http_server_requests_seconds_count{job="app",uri="/movie-records",status="201"}[1m]))

histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="app",uri="/movie-records"}[1m])))

1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[1m]))

max(mysql_global_status_threads_connected)
max(mysql_global_status_threads_running)
```

RDS를 두 Alloy가 수집하므로 DB 게이지는 합계 대신 max로 본다. HTTP 경로에는 회원 ID나 원문 URL을 레이블로 넣지 않는다. JWT·비밀번호·Grafana 토큰은 측정 결과와 Git에 남기지 않는다.
