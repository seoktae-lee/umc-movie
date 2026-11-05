#!/usr/bin/env python3
"""회원 두 명이 같은 닉네임으로 동시에 PATCH /members/me를 보낸다. (7주차 재현용)

사전 검사(existsByNicknameAndMemberIdNot)를 함께 통과한 요청은 DB UNIQUE가 막는다.
어느 쪽에서 막혀도 409 MEMBER409_1이어야 한다.

사용: python3 scripts/concurrent_nickname.py 이메일A 비밀번호A 이메일B 비밀번호B [라운드 수]
"""
import json
import sys
import threading
import urllib.error
import urllib.request
from collections import Counter

BASE = "http://localhost:8080"


def call(method, path, body, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(BASE + path, json.dumps(body).encode(), headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def login(email, password):
    return call("POST", "/auth/login", {"email": email, "password": password})[1]["result"]["accessToken"]


def main():
    email_a, password_a, email_b, password_b = sys.argv[1:5]
    rounds = int(sys.argv[5]) if len(sys.argv) > 5 else 10
    tokens = [login(email_a, password_a), login(email_b, password_b)]
    counter = Counter()

    for number in range(rounds):
        barrier = threading.Barrier(len(tokens))  # 두 스레드를 같은 순간에 출발시킨다

        def send(token):
            barrier.wait()
            status, body = call("PATCH", "/members/me", {"nickname": f"경쟁{number}"}, token)
            counter[f"{status} {body['code']}"] += 1

        threads = [threading.Thread(target=send, args=(token,)) for token in tokens]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()

    print(dict(counter))


if __name__ == "__main__":
    main()
