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
| 요청 | 600 | 측정 전 |
| 실패 | 166 | 측정 전 |
| 실패율 | 27.67% | 측정 전 |
| outage | 1회 | 측정 전 |
| downtime | 33.4초 | 측정 전 |
| 성공 요청 p95 | 291ms | 측정 전 |

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

## 이어서 확인할 상태

새 서버 `i-02c18d843c031a7ce`가 생성됐으며 SSM Online을 확인했다. 마지막 앱 준비 로그는 Docker 설치 중이었다. 이후 컴퓨터 제어 입력이 전달되지 않아 AWS 최신 교체 상태는 확인하지 못했다.

로컬 측정 ID는 `034f3b78-b42a-4f69-9d1e-778c814ead53`이고 최대 30분으로 시작했다. `results/after-in-progress.json`은 중간 결과이며 완료된 무중단 배포 결과로 사용하면 안 된다. CloudShell에서 refresh ID 상태가 Successful인지, 대상 2대가 healthy인지 확인한 뒤 측정을 종료하고 최종 JSON을 다시 저장해야 한다.
