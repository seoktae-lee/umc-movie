# UMC Infra 1주차 — 영화 백엔드

개인 워크북 실습용 구성. 리전은 `ap-southeast-2`, 앱 포트는 `8080`, 헬스체크는 `/health`다.

## 실행 순서

AWS CloudShell에서 준비 브랜치를 체크아웃하고 실행한다.

```bash
bash infra/week1/create-baseline.sh
bash infra/week1/push-image.sh
bash infra/week1/launch-baseline.sh
```

단일 EC2가 정상 응답하면 zero-downtime-lab으로 5 RPS 부하를 주며 SSM Run Command로 컨테이너를 재시작한다. 배포 시각과 요청 결과를 JSON으로 보관한다.

```bash
bash infra/week1/create-asg.sh
```

ALB 대상 두 대가 healthy인 것을 확인하고 같은 부하로 인스턴스 새로 고침 전후를 측정한다. 새로 고침은 최소 정상 용량 100%, 최대 정상 용량 150%로 진행한다. 두 측정의 요청 수·실패 수·실패율·outage·downtime·p95를 개인 워크북에 기록한다.

## 구성

- 단일 EC2: Amazon Linux 2023, t3.micro, 암호화 EBS 12 GiB, IMDSv2 필수, SSH 미개방.
- RDS: MySQL 8.4, db.t4g.micro, 20 GiB, 암호화, 외부 접속 차단. EC2 보안 그룹만 3306에 접근한다.
- SecureString: `/umc-movie/week1/db-password`, `/umc-movie/week1/jwt-secret`.
- EC2 역할: SSM 관리, 실습 파라미터 조회, 실습 ECR 이미지 읽기, RDS 엔드포인트 조회.
- 단일 서버 측정용 8080 포트는 외부 공개. 측정 후 ALB 전환 시 공개 규칙을 제거한다.
- ALB: HTTP 80, 앱으로는 8080 전달. ASG는 기본 2대, 최대 3대.

실습 리소스는 AWS 크레딧을 소비한다. 측정 완료 후 유지할 리소스와 삭제할 리소스를 확인한다. RDS는 실습 데이터용이며 자동 백업을 사용하지 않는다. 스크립트에는 비밀값을 기록하지 않는다.

## 검증 기록

- Spring 테스트 통과.
- 로컬 Docker 이미지 빌드 및 MySQL 연동 성공.
- `/health` HTTP 200.
- 측정 도구 테스트 179개 통과.
- 로컬 연결 점검: 15초, 75건 성공, 실패 0, p95 8ms. 배포 전후 비교값으로 사용하지 않는다.

AWS 단일 서버 측정: GET `/health`, 5 RPS, 120초, 기준 구간 5초, 타임아웃 2초. 기준 구간 이후 SSM으로 `docker restart umc-movie`를 실행했다.

| 항목 | 단일 EC2 | ALB + ASG |
| --- | ---: | ---: |
| 요청 | 600 | 10,250 (9,001 + 1,249) |
| 실패 | 166 | 0 |
| 실패율 | 27.67% | 0% |
| outage | 1회 | 0회 |
| downtime | 33.4초 | 관측 0초 |
| 성공 요청 p95 | 291ms | 본 측정 284ms / 연장 283ms |

원본은 `results/before.json`에 보관한다. 기준 구간 실패율은 0%, 도구 경고는 없다. 같은 이미지의 재시작으로 단일 서버의 중단을 측정한 결과다.

## 실제 AWS 진행 상태

- 단일 측정용 EC2 `i-059df67754ba93240`는 측정 후 중지했다.
- EC2 8080의 전체 인터넷 허용 규칙을 제거했다. ALB 보안 그룹에서 오는 접근만 허용한다.
- ALB: `umc-movie-week1-1589656575.ap-southeast-2.elb.amazonaws.com`.
- ASG: `umc-movie-week1`, 정상 용량 2대, 최대 3대.
- 시작 템플릿 버전 2는 DB 초기화를 반복하지 않는다.
- 롤백 태그: ECR `umc-movie-week1:baseline-751a967`. 같은 이미지의 digest를 보관하며 태그를 덮어쓰지 않는다.
- 현재 검증은 애플리케이션 코드 변경 없이 시작 템플릿 버전 1 → 2로 서버를 교체하는 실험이다.
- ASG 교체 ID: `88f8a279-2e1e-47c4-a655-4b295fcf6ed0`.

교체 중 첫 추가 서버가 부팅 초기화 상태에서 진행되지 않아 재부팅을 시도했다. ASG가 ELB 헬스체크 실패로 해당 서버를 종료하고 새 서버를 생성했다. 기존 정상 서버 2대는 유지됐다. CPU Unlimited 설정은 적용하지 않았으며, 사용자 요청에 따라 Standard를 유지한다. 교체 완료 전 측정값은 최종 결과로 사용하지 않는다.

## 최종 결과

2026-11-16 13:32:51 UTC에 ASG 순차 교체가 `Successful`, 100%로 완료됐다. 서버 `i-02c18d843c031a7ce`, `i-0c5ee38bc11002735`가 모두 시작 템플릿 버전 2와 Healthy 상태임을 확인했다.

본 측정은 30분 동안 9,001건, 연장 측정은 약 250초 동안 1,249건이었다. 두 구간 모두 5 RPS, 타임아웃 2초, 기준 구간 5초다. 교체 완료를 확인한 뒤 연장 측정을 종료했다. 두 측정 사이에는 2.761초의 미측정 구간이 있다. 따라서 이 결과는 관측 구간의 중단이 0초라는 뜻이며, 모든 순간의 무중단을 보증하지 않는다. p95는 구간별 값이며 합산 백분위수로 계산하지 않았다. 단일 서버와 ASG의 측정 시간도 다르므로 실패율은 각 실험 구간 기준이다.

최종 원본은 `results/before.json`, `results/after-main.json`, `results/after-extension.json`이다. `results/after-in-progress.json`은 이전 중간 체크포인트다.

단일 서버는 컨테이너가 정지한 동안 대체 프로세스가 없어 요청을 받지 못했다. ALB와 ASG에서는 기존 정상 서버들이 요청을 처리하는 동안 새 서버를 먼저 준비했고, 새 서버가 정상 판정을 받은 뒤 기존 서버의 연결을 정리해 교체했다. 이 실험에서 애플리케이션 코드는 동일하고 시작 템플릿을 버전 1에서 2로 변경했다.

단일 실험 서버와 로컬 Compose는 중지했다. ASG 2대, ALB, RDS 등 AWS 실습 리소스는 유지 중이므로 크레딧이 소비된다.
