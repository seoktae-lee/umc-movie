# 6주차 트러블슈팅 (Backend)

워크북 형식(이슈 / 문제 / 해결 / 참고레퍼런스) 그대로 적었다.

## No.1 동시 회원가입을 보냈는데 DB 제약 위반이 한 번도 안 일어남

**`이슈`**

👉 Challenge를 재현하려고 같은 이메일로 회원가입 20개를 `xargs -P 20 curl ...`로 동시에 보냈다. 결과는 201 한 개, 409 `MEMBER4091` 19개였고, 서버 로그의 `[DB 제약 위반]`은 **0건**이었다. 19개 모두 사전 중복 검사(`existsByEmail`)에서 막혔다는 뜻이다.

**`문제`**

👉 `xargs`가 curl 프로세스를 하나씩 띄우는 사이에 몇 ms씩 차이가 났다. 첫 요청이 INSERT를 끝낸 뒤에 나머지가 `existsByEmail`을 실행해서 모두 "이미 있음"을 봤다. DB 제약까지 가려면 여러 요청이 **같은 순간에** "없음"을 봐야 한다.

**`해결`**

👉 파이썬 스레드 30개를 만들고 `threading.Barrier(30)`에서 모두 기다리게 한 뒤 한꺼번에 출발시켰다(`scripts/concurrent_signup.py`). 30개 중 9개가 사전 검사를 통과했다가 DB UNIQUE(`member.email`)에 막혔고, `GlobalExceptionHandler`가 이를 사전 검사와 같은 409 `MEMBER4091`로 바꿔 응답했다. DB에는 1행만 저장됐다. (`docs/images/week6/09-challenge-db-unique.png`)

배운 점: 사전 중복 검사는 "대부분"을 막아 줄 뿐이다. 마지막 안전망은 DB 제약이다.

**`참고레퍼런스`**

- Python threading.Barrier: https://docs.python.org/3/library/threading.html#barrier-objects

## No.2 동시 가입 재현 결과에 닉네임 중복(MEMBER4092)이 섞임

**`이슈`**

👉 재현을 여러 번 돌리자 이메일을 매번 바꿨는데도 결과에 `409 MEMBER4092`(닉네임 중복)가 1개씩 섞였다.

**`문제`**

👉 닉네임을 `race{번호}`로 만들어서, 앞 라운드에서 가입에 성공한 `race9` 같은 닉네임을 다음 라운드의 같은 번호 요청이 다시 썼다. Service는 이메일 → 닉네임 순서로 검사하므로, 새 이메일이라 이메일 검사는 통과하고 닉네임 검사에서 걸렸다.

**`해결`**

👉 실행할 때마다 닉네임 접두사를 바꾸도록 스크립트에 세 번째 인자를 추가했다. (`동시1-0` … `동시1-29`) 닉네임은 12자 제한이 있어 `concurrent_29`(13자)처럼 길게 만들면 400이 나므로 짧게 잡았다.

배운 점: 이메일과 닉네임 중복을 다른 code로 나눠 둔 덕분에(Challenge), 어떤 값이 겹쳤는지 응답만 보고 바로 알 수 있었다.

## No.3 초기화 직후인데 동시 가입으로 생긴 회원의 member_id가 3이 아니라 8

**`이슈`**

👉 `03_reset_week6.sql`로 `AUTO_INCREMENT = 3`을 맞춘 뒤 동시 가입을 재현했더니, 저장된 1행의 `member_id`가 8이었다.

**`문제`**

👉 MySQL(InnoDB)은 INSERT를 시도할 때 AUTO_INCREMENT 번호를 먼저 가져간다. UNIQUE 위반으로 INSERT가 실패해도 가져간 번호는 돌려주지 않는다. 그래서 DB에서 막힌 요청들이 3~7번을 써 버렸다.

**`해결`**

👉 오류가 아니라 정상 동작이다. id는 "순서대로 빈틈없는 번호"가 아니라 "겹치지 않는 식별자"로만 써야 한다. 캡처에 이유를 함께 적었다.

**`참고레퍼런스`**

- MySQL AUTO_INCREMENT Handling in InnoDB: https://dev.mysql.com/doc/refman/8.4/en/innodb-auto-increment-handling.html
