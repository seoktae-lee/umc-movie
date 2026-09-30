# 5주차 트러블슈팅 (Backend)

워크북 형식(이슈 / 문제 / 해결 / 참고레퍼런스) 그대로 적었다.

## No.1 — CORS 설정 전, 간이 프론트에서 응답을 못 받음 (의도한 재현)

**이슈**
👉 `http://localhost:5173`에서 버튼을 누르면 화면에는 `Failed to fetch`만 뜨고, Console에 아래 오류가 찍혔다.

```
Access to fetch at 'http://localhost:8080/members/nickname/%EC%88%98%ED%98%84' from origin 'http://localhost:5173'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
TypeError: Failed to fetch
```

**문제**
👉 5173과 8080은 포트가 달라 다른 출처다. 서버 응답에 `Access-Control-Allow-Origin`이 없으니 브라우저가 응답을 JavaScript에 넘기기 직전에 막았다. (요청은 서버에 도착했다)

**해결**
👉 `WebConfig implements WebMvcConfigurer`에서 `allowedOrigins("http://localhost:5173")`로 허용했다. 같은 페이지에서 `200 {"available": false}` 확인.

**참고레퍼런스**
- https://developer.mozilla.org/ko/docs/Web/HTTP/CORS
- https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html

## No.2 — 400 응답의 message에 서버 내부 메서드 이름이 노출됨

**이슈**
👉 `/members/nickname/a`에 400은 왔지만 message가 이랬다.

```
"message": "Validation failed for method='public com.umc.movie.dto.NicknameCheckResponse com.umc.movie.controller.MemberController.checkNickname(java.lang.String)'. Error count: 1"
```

**문제**
👉 `@PathVariable @Size(...)` 검증이 실패하면 Spring 6.1+가 `HandlerMethodValidationException`을 던지고, 기본 오류 응답은 그 예외 메시지를 그대로 쓴다. 패키지·클래스·메서드 이름은 사용자에게 보여 줄 정보도 아니고, 내부 구조를 알려 주는 셈이다.

**해결**
👉 `@RestControllerAdvice`로 이 예외만 받아 `@Size`·`@Email`에 적은 문구로 바꿨다. → `{"status": 400, "message": "닉네임은 2~12자여야 합니다."}`
예외 전체를 한곳에서 다루는 방법은 6주차에 배우므로, 이번에는 검증 실패 한 가지만 처리했다.

**참고레퍼런스**
- https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html#validation-beanvalidation-spring-method

## No.3 — 404·400 응답에 이유(message)가 비어 있음

**이슈**
👉 `ResponseStatusException(NOT_FOUND, "존재하지 않는 유저입니다.")`을 던졌는데 응답 JSON에 `message`가 없었다.

**문제**
👉 Spring Boot는 보안상 기본값으로 `server.error.include-message: never`라서 오류 메시지를 응답에서 뺀다.

**해결**
👉 `application.yml`에 `server.error.include-message: always`. 여기 나가는 문구는 우리가 직접 쓴 것뿐이라 노출돼도 괜찮다.

**참고레퍼런스**
- https://docs.spring.io/spring-boot/appendix/application-properties/index.html#application-properties.server.server.error.include-message

## No.4 — 함께 켠 include-binding-errors 때문에 400이 500으로 바뀜

**이슈**
👉 `include-binding-errors: always`도 켜자 형식 오류 요청이 전부 500이 됐다.

```
HttpMessageNotWritableException: Could not write JSON: HV000116: getParameterIndex() may only be invoked for nodes of type ElementKind.PARAMETER.
```

**문제**
👉 검증 오류 목록(`errors`)을 응답에 넣으려다 Hibernate Validator의 오류 객체를 JSON으로 바꾸지 못했다. 오류를 알려 주려던 설정이 오류를 만들었다.

**해결**
👉 `include-binding-errors`는 끄고 message만 남겼다. 필요한 정보(어느 값이 왜 틀렸는지)는 No.2의 핸들러가 message로 준다.

**참고레퍼런스**
- https://github.com/spring-projects/spring-boot/issues (HV000116 검색)

## No.5 — 웹 앱에서는 CORS에 막혀도 "서버에 연결할 수 없어요"로 보임

**이슈**
👉 MovieLog Flutter 앱을 웹(5173)으로 띄우고 서버의 허용 출처를 다른 주소로 바꾸자, 마이페이지에 "MovieLog 서버에 연결할 수 없어요"가 떴다. 서버는 켜져 있었다.

**문제**
👉 브라우저는 CORS로 막은 이유를 JavaScript에 알려 주지 않는다. Dio가 받는 건 서버가 꺼졌을 때와 같은 `connectionError`뿐이고, 진짜 이유는 Console에만 있다.

```
Access to XMLHttpRequest at 'http://localhost:8080/members/1/ratings' from origin 'http://localhost:5173'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

**해결**
👉 코드로 구분할 수 없으므로, 웹에서 "연결할 수 없어요"가 뜨면 **먼저 브라우저 Console과 서버 CORS 설정을 본다**는 순서를 정했다. (워크북: "CORS 에러가 나면 원인은 항상 백엔드") iOS 시뮬레이터(네이티브)에서는 같은 설정에서도 정상 동작했다 — CORS는 브라우저만의 규칙이다.

**참고레퍼런스**
- https://fetch.spec.whatwg.org/#cors-protocol

## No.6 — Initializr가 준 Boot 4.1.1을 워크북 기준 3.5.0으로 내림

**이슈**
👉 start.spring.io에서 받은 `build.gradle`이 Spring Boot 4.1.1, Gradle Wrapper 9.7.1이었다. 워크북은 3.5.0 고정.

**문제**
👉 3.5.0 플러그인이 새 Gradle 9에서 동작할지 확실하지 않았다.

**해결**
👉 워크북의 `build.gradle`로 바꾸고 `./gradlew compileJava`·`test`·`bootRun`으로 확인 — Gradle 9.7.1에서도 문제없이 동작해 Wrapper는 그대로 두었다.

**참고레퍼런스**
- https://docs.spring.io/spring-boot/3.5/system-requirements.html
