#!/usr/bin/env python3
"""한 단계만 실행한다. 토큰·비밀번호는 결과 파일에 기록하지 않는다."""
import argparse
import json
import os
from pathlib import Path
import secrets
import time
import urllib.request


def request(url, data=None, headers=None):
    encoded = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=encoded, headers=headers or {})
    with urllib.request.urlopen(req, timeout=30) as response:
        return response.status, json.load(response)


parser = argparse.ArgumentParser()
parser.add_argument('--rps', type=int, choices=[200, 400, 600, 800], required=True)
parser.add_argument('--duration', type=int, default=40)
args = parser.parse_args()
base = os.environ['BASE_URL'].rstrip('/')
lab = os.environ.get('LAB_URL', 'http://localhost:9376').rstrip('/')
json_headers = {'Content-Type': 'application/json'}
suffix = secrets.token_hex(4)
email = f'obs{suffix}@example.com'
password = 'Aa1!' + secrets.token_hex(16)
status, _ = request(base + '/members', {'email': email, 'password': password, 'nickname': 'obs' + suffix}, json_headers)
assert status == 201, '테스트 회원 생성 실패'
_, login = request(base + '/auth/login', {'email': email, 'password': password}, json_headers)
token = login['result']['accessToken']
headers = {**json_headers, 'Authorization': 'Bearer ' + token}
status, _ = request(base + '/movie-records', {'movieId': 999999999, 'score': 5, 'comment': 'week2 preflight', 'addToFavorites': True}, headers)
assert status == 201, '쓰기 사전 요청이 201이 아님'
scenario = {
    'name': f'UMC 영화 쓰기 한계 측정 {args.rps} RPS',
    'target': {'url': base + '/movie-records', 'method': 'POST', 'headers': headers,
               'body': '{"movieId":{{counter}},"score":5,"comment":"week2 lab","addToFavorites":true}', 'timeoutMs': 5000},
    'success': {'statusCodes': [201]},
    'load': {'rps': args.rps, 'durationSec': args.duration},
    'analysis': {'slowThresholdMs': 1000, 'baselineSec': 8, 'minConsecutiveFailures': 3},
}
_, started = request(lab + '/api/runs', scenario, json_headers)
run_id = started['runId']
print(json.dumps({'runId': run_id, 'rps': args.rps, 'preflightStatus': status}), flush=True)
while True:
    _, summary = request(lab + '/api/runs/' + run_id)
    if summary['status'] != 'running':
        break
    time.sleep(1)
_, exported = request(lab + '/api/runs/' + run_id + '/export.json')
exported['scenario']['target']['headers']['Authorization'] = 'Bearer [REDACTED]'
out = Path(__file__).parent / 'results'
out.mkdir(exist_ok=True)
(out / f'load-{args.rps}-{run_id[:8]}.json').write_text(json.dumps(exported, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(summary, ensure_ascii=False), flush=True)
