"""8주차 시나리오를 실행 중인 서버(localhost:8080)에 실제로 보내고,
요청·응답을 카드 HTML로 렌더링한다. (Postman 화면이 아니라 서버 응답 기록이다. 토큰은 줄여 표시)
사용: python3 scripts/week8_capture.py   # 결과: docs/images/week8/*.html, postman/umc-week8.postman_collection.json
"""
import json, subprocess, time, html, urllib.request, urllib.error, os

H = "http://localhost:8080"
OUT = "docs/images/week8"
os.makedirs(OUT, exist_ok=True)
stamp = str(int(time.time()))[-6:]
EMAIL, PW = f"w8{stamp}@umc.com", "Week8pass!"
REASON = {200: "OK", 201: "Created", 400: "Bad Request", 401: "Unauthorized", 404: "Not Found", 409: "Conflict"}

def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(H + path, data=data, method=method)
    if body is not None: req.add_header("Content-Type", "application/json")
    if token: req.add_header("Authorization", "Bearer " + token)
    t = time.time()
    try:
        r = urllib.request.urlopen(req); code, txt = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        code, txt = e.code, e.read().decode()
    return code, json.loads(txt), int((time.time() - t) * 1000)

def sql(q):
    return subprocess.run(["mysql", "-uroot", "umc_movie", "-e", q], capture_output=True, text=True).stdout.strip()

log = []  # postman 컬렉션용
def step(title, method, path, body, token, expect, note=None):
    code, res, ms = call(method, path, body, token)
    ok = code == expect[0] and (res.get("code") == expect[1] if len(expect) > 1 else True)
    log.append(dict(title=title, method=method, path=path, body=body, auth=bool(token), status=code, res=res, ms=ms, ok=ok, exp=expect))
    print(("OK  " if ok else "FAIL"), code, title)
    return log[-1]

call("POST", "/members", dict(email=EMAIL, password=PW, nickname="w8" + stamp))
tok = call("POST", "/auth/login", dict(email=EMAIL, password=PW))[1]["result"]["accessToken"]

C = {}
C["01"] = [
 step("POST /favorites 등록(201)", "POST", "/favorites", {"movieId": 550}, tok, (201, "COMMON201")),
 step("POST /favorites 같은 영화 중복(409)", "POST", "/favorites", {"movieId": 550}, tok, (409, "FAVORITE409_1")),
 step("POST /favorites 토큰 없음(401)", "POST", "/favorites", {"movieId": 551}, None, (401, "AUTH401_2")),
 step("POST /favorites movieId 0(400)", "POST", "/favorites", {"movieId": 0}, tok, (400, "COMMON4001")),
]
step("POST /favorites 두 번째 영화 551", "POST", "/favorites", {"movieId": 551}, tok, (201,))
C["02"] = [
 step("GET /favorites 목록(최신 등록순)", "GET", "/favorites", None, tok, (200, "COMMON200")),
 step("GET /favorites/550 여부(true)", "GET", "/favorites/550", None, tok, (200, "COMMON200")),
 step("GET /favorites/999 여부(false)", "GET", "/favorites/999", None, tok, (200, "COMMON200")),
 step("DELETE /favorites/550 해제(200)", "DELETE", "/favorites/550", None, tok, (200, "COMMON200")),
 step("DELETE /favorites/550 다시(404)", "DELETE", "/favorites/550", None, tok, (404, "FAVORITE404_1")),
 step("GET /favorites/550 해제 후(false)", "GET", "/favorites/550", None, tok, (200, "COMMON200")),
]
C["03"] = [
 step("POST /movie-records 평점+즐겨찾기(201)", "POST", "/movie-records", {"movieId": 238, "score": 5, "comment": "다시 보고 싶은 영화입니다.", "addToFavorites": True}, tok, (201, "COMMON201")),
 step("POST /movie-records 같은 요청(409)", "POST", "/movie-records", {"movieId": 238, "score": 5, "comment": "다시 보고 싶은 영화입니다.", "addToFavorites": True}, tok, (409, "RATING409_1")),
 step("POST /movie-records 평점만(addToFavorites false)", "POST", "/movie-records", {"movieId": 278, "score": 4, "comment": "평점만", "addToFavorites": False}, tok, (201, "COMMON201")),
]
mid = sql(f"select member_id from member where email='{EMAIL}'").split("\n")[1]
def counts(movie):
    r = sql(f"select count(*) from rating where member_id={mid} and movie_id={movie}").split("\n")[1]
    f = sql(f"select count(*) from favorite where member_id={mid} and movie_id={movie}").split("\n")[1]
    return r, f
step("POST /favorites 603 먼저 즐겨찾기", "POST", "/favorites", {"movieId": 603}, tok, (201,))
before = counts(603)
rb = step("POST /movie-records 603 (즐겨찾기가 이미 있어 실패)", "POST", "/movie-records", {"movieId": 603, "score": 4, "comment": "롤백 확인", "addToFavorites": True}, tok, (409, "FAVORITE409_1"))
after = counts(603)
C["04"] = [log[-1]]
db_note = f"요청 전  rating(603)={before[0]}행, favorite(603)={before[1]}행\n요청 후  rating(603)={after[0]}행, favorite(603)={after[1]}행   ← 평점이 먼저 저장됐지만 롤백되어 0행"
print(db_note)

def card(l):
    cls = "ok" if 200 <= l["status"] < 300 else "err"
    hdr = "Authorization: Bearer " + (os.environ.get("X","") or "eyJhbGciOiJIUzM4NC…") if l["auth"] else None
    body = json.dumps(l["body"], ensure_ascii=False, indent=2) if l["body"] is not None else None
    return f'''<section><div class="top"><b class="m {l['method']}">{l['method']}</b><code>localhost:8080{html.escape(l['path'])}</code><span>{html.escape(l['title'])}</span></div>
<div class="row"><div class="l"><h6>REQUEST HEADER</h6><pre>{html.escape(hdr) if hdr else '<i>Authorization 없음</i>'}</pre>{('<h6>REQUEST BODY</h6><pre>'+html.escape(body)+'</pre>') if body else ''}</div>
<div class="r"><h4 class="{cls}">{l['status']} {REASON.get(l['status'],'')} · {l['ms']}ms</h4><h6>RESPONSE BODY</h6><pre>{html.escape(json.dumps(l['res'], ensure_ascii=False, indent=2))}</pre>
<p class="chk">✓ status {l['exp'][0]}{(' · code '+html.escape(l['exp'][1])) if len(l['exp'])>1 else ''}</p></div></div></section>'''

CSS = '''body{font-family:-apple-system,"Apple SD Gothic Neo",sans-serif;background:#f4f5f7;margin:0;padding:28px;width:1360px;color:#222}h1{font-size:26px;margin:0 0 6px}p.d{color:#555;margin:0 0 20px;font-size:14px}
section{background:#fff;border:1px solid #d9dce1;border-radius:14px;margin-bottom:16px;overflow:hidden}.top{display:flex;gap:14px;align-items:center;padding:12px 18px;border-bottom:1px solid #e5e7eb}.top span{margin-left:auto;color:#666;font-size:13px}
.m{color:#fff;padding:4px 12px;border-radius:6px;font-size:14px}.POST{background:#b8730a}.GET{background:#1c7c3d}.DELETE{background:#c0392b}.row{display:flex}.l{width:50%;padding:12px 18px;border-right:1px solid #e5e7eb}.r{width:50%;padding:12px 18px}
h6{margin:10px 0 4px;font-size:11px;color:#666;font-weight:500}pre{background:#f6f6f6;border-radius:8px;padding:10px 12px;margin:0;font:13px/1.5 Menlo,monospace;white-space:pre-wrap;word-break:break-all}
h4{margin:0 0 6px;font-size:16px}.ok{color:#1c7c3d}.err{color:#d9480f}.chk{color:#1c7c3d;font-size:12px;margin:8px 0 0}code{font:15px Menlo,monospace}'''
TITLES = {"01": ("즐겨찾기 등록과 중복 오류", "토큰의 회원으로 등록하고 memberId는 보내지 않는다. 같은 영화를 한 번 더 보내면 409 FAVORITE409_1."),
          "02": ("즐겨찾기 조회·여부·해제", "내 것만 최신 등록 순. 즐겨찾기하지 않은 영화의 여부 조회는 오류가 아니라 isFavorite:false."),
          "03": ("영화 기록 저장(평점+즐겨찾기)", "result에 rating과 favorite이 함께 담긴다. 같은 요청을 다시 보내면 RATING409_1."),
          "04": ("트랜잭션 롤백 확인", "603은 이미 즐겨찾기한 영화다. 평점이 먼저 저장된 뒤 즐겨찾기에서 실패하지만, 같은 트랜잭션이라 평점도 취소된다.")}
for k, (t, d) in TITLES.items():
    extra = f'<section><div class="top"><b>DB 확인</b></div><pre style="margin:12px 18px">{html.escape(db_note)}</pre></section>' if k == "04" else ""
    open(f"{OUT}/{k}.html", "w").write(f'<!doctype html><meta charset="utf-8"><style>{CSS}</style><h1>{t}</h1><p class="d">{d}</p>' + "".join(card(l) for l in C[k]) + extra)

# Postman 컬렉션 (환경 변수 없이 실행 가능한 순서형)
items = []
for l in log:
    if l["auth"] is False and l["status"] == 401 and l["path"] == "/favorites" and l["method"] == "GET": continue
    ev = [f'pm.test("status {l["exp"][0]}", () => pm.response.to.have.status({l["exp"][0]}));']
    if len(l["exp"]) > 1: ev.append(f'pm.test("code {l["exp"][1]}", () => pm.expect(pm.response.json().code).to.eql("{l["exp"][1]}"));')
    hdr = [{"key": "Authorization", "value": "Bearer {{accessToken}}"}] if l["auth"] else []
    if l["body"] is not None: hdr.append({"key": "Content-Type", "value": "application/json"})
    req = {"method": l["method"], "header": hdr, "url": "{{baseUrl}}" + l["path"]}
    if l["body"] is not None: req["body"] = {"mode": "raw", "raw": json.dumps(l["body"], ensure_ascii=False, indent=2)}
    items.append({"name": l["title"], "request": req, "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": ev}}]})
open("postman/umc-week8.postman_collection.json", "w").write(json.dumps({
  "info": {"name": "UMC 8주차 - 즐겨찾기와 영화 기록", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
           "description": "먼저 7주차 POST /auth/login으로 받은 accessToken을 컬렉션 변수 accessToken에 넣는다. 영화 ID는 매번 새 회원이라는 전제다."},
  "variable": [{"key": "baseUrl", "value": "http://localhost:8080"}, {"key": "accessToken", "value": ""}], "item": items}, ensure_ascii=False, indent=2))
print("FAILED:", [l["title"] for l in log if not l["ok"]])
