package com.umc.movie.config;

/**
 * Swagger 문서에 보여 줄 응답 예시(JSON). 여러 API가 같은 실패 응답을 쓰므로 한곳에 모은다. (9주차)
 *
 * 값은 실제 서버가 내려 주는 code·message와 같아야 한다. 문서와 동작이 어긋나면 이 예시가 거짓말이 된다.
 * (그래서 통합 테스트가 같은 code를 검증한다)
 */
public final class ApiExamples {

    private ApiExamples() {}

    public static final String UNAUTHORIZED = """
            {
              "isSuccess": false,
              "code": "AUTH401_2",
              "message": "로그인이 필요한 요청입니다.",
              "result": null
            }
            """;

    public static final String LOGIN_FAILED = """
            {
              "isSuccess": false,
              "code": "AUTH401_1",
              "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
              "result": null
            }
            """;

    public static final String INVALID_INPUT = """
            {
              "isSuccess": false,
              "code": "COMMON4001",
              "message": "별점은 1점 이상이어야 합니다.",
              "result": null
            }
            """;

    public static final String MEMBER_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "MEMBER404_1",
              "message": "해당 회원을 찾을 수 없습니다.",
              "result": null
            }
            """;

    public static final String EMAIL_DUPLICATED = """
            {
              "isSuccess": false,
              "code": "MEMBER4091",
              "message": "이미 사용 중인 이메일입니다.",
              "result": null
            }
            """;

    public static final String NICKNAME_DUPLICATED = """
            {
              "isSuccess": false,
              "code": "MEMBER409_1",
              "message": "이미 사용 중인 닉네임입니다.",
              "result": null
            }
            """;

    public static final String RATING_DUPLICATED = """
            {
              "isSuccess": false,
              "code": "RATING409_1",
              "message": "이미 이 영화에 평점을 남기셨습니다.",
              "result": null
            }
            """;

    public static final String RATING_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "RATING404_1",
              "message": "해당 평점을 찾을 수 없습니다.",
              "result": null
            }
            """;

    public static final String RATING_FORBIDDEN = """
            {
              "isSuccess": false,
              "code": "RATING403_1",
              "message": "본인이 작성한 평점만 수정하거나 삭제할 수 있습니다.",
              "result": null
            }
            """;

    public static final String FAVORITE_DUPLICATED = """
            {
              "isSuccess": false,
              "code": "FAVORITE409_1",
              "message": "이미 즐겨찾기한 영화입니다.",
              "result": null
            }
            """;

    public static final String FAVORITE_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "FAVORITE404_1",
              "message": "해제할 즐겨찾기를 찾을 수 없습니다.",
              "result": null
            }
            """;
}
