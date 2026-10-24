"""
6주차 Challenge — 같은 이메일로 회원가입 요청을 "동시에" 여러 개 보내 DB UNIQUE 위반을 재현한다.

사용법 (서버가 8080에서 실행 중이어야 한다):
    python3 scripts/concurrent_signup.py same1@umc.com 30 동시1-

인자: 이메일, 요청 수(기본 30), 닉네임 접두사(실행마다 바꿔야 닉네임 중복 409가 섞이지 않는다)
결과: (상태 코드, code)별 개수. 서버 로그에 "[DB 제약 위반 → MEMBER4091]"이 찍힌 요청이
      사전 중복 검사를 통과했지만 DB가 막은 요청이다. 타이밍에 따라 0건일 수도 있어 몇 번 반복한다.
"""
import json, sys, threading, urllib.request, collections
N=int(sys.argv[2]) if len(sys.argv)>2 else 30
email=sys.argv[1]
# 모든 스레드가 여기서 기다렸다가 한꺼번에 출발한다. (xargs로는 요청이 조금씩 어긋나 재현이 안 됐다)
barrier=threading.Barrier(N); out=[]
def go(i):
    body=json.dumps({"email":email,"password":"Umc!1234","nickname":f"{sys.argv[3]}{i}"}).encode()
    req=urllib.request.Request("http://localhost:8080/members",data=body,headers={"Content-Type":"application/json"},method="POST")
    barrier.wait()
    try:
        r=urllib.request.urlopen(req); out.append((r.status,json.load(r)["code"]))
    except urllib.error.HTTPError as e:
        out.append((e.code,json.load(e)["code"]))
ts=[threading.Thread(target=go,args=(i,)) for i in range(N)]
[t.start() for t in ts]; [t.join() for t in ts]
for k,v in collections.Counter(out).items(): print(v,"x",k)
