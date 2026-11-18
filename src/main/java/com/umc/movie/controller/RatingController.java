package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.config.ApiExamples;
import com.umc.movie.config.SwaggerConfig;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 등록·수정·삭제는 "누구의 요청인지"를 요청에서 받지 않고 토큰에서 알아낸다. (7주차)
 * 값을 받지 않는 것이 곧 그 값을 조작당하지 않는 방법이다.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "평점", description = "영화 평점 API. 유저별 조회는 공개, 등록·수정·삭제는 토큰이 필요합니다.")
public class RatingController {

    private final RatingService ratingService;

    /**
     * 유저별 평점 조회. 최신순. 누구나 볼 수 있는 공개 목록이라 토큰 없이 호출한다.
     * (선택 미션) ?page=0&size=10으로 잘라 받을 수 있다. 생략하면 첫 페이지 20개.
     */
    @Operation(
            summary = "유저별 평점 조회",
            description = """
                    한 회원이 남긴 평점을 최신순으로 돌려줍니다. 누구나 볼 수 있는 공개 목록이라 토큰이 필요 없습니다.
                    성공 응답은 공통 틀로 감싸지 않고 **배열을 그대로** 돌려줍니다. page는 0부터, size는 1~50(기본 20)입니다.
                    """
    )
    @GetMapping("/members/{memberId}/ratings")
    public List<RatingResponse> getMemberRatings(
            @Parameter(description = "평점을 조회할 회원 ID", example = "1")
            @PathVariable Long memberId,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @Parameter(description = "한 페이지 크기 (1~50)", example = "20")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.") int size) {
        return ratingService.getRatingsByMember(memberId, page, size);
    }

    /** 평점 등록. 201 + 등록된 평점. */
    @Operation(
            summary = "평점 등록",
            description = "토큰의 주인이 TMDB 영화 하나에 1~5점 평점을 남깁니다. 🔒 토큰이 필요합니다. 한 영화에 한 번만 남길 수 있습니다."
    )
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "등록 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "평점을 등록했습니다.",
                              "result": {
                                "ratingId": 1, "movieId": 550, "score": 5, "comment": "최고의 영화!",
                                "createdAt": "2026-11-17T21:00:00", "updatedAt": "2026-11-17T21:00:00"
                              }
                            }
                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "별점이 1~5가 아니거나 후기가 500자를 넘음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.INVALID_INPUT))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "같은 영화에 이미 평점을 남김",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.RATING_DUPLICATED)))
    })
    @PostMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> create(
            @LoginMember Long memberId,
            @Valid @RequestBody CreateRatingRequest request) {
        RatingResponse response = ratingService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("평점을 등록했습니다.", response));
    }

    /** 평점 수정. 200 + 수정된 평점. 본인 평점이 아니면 403, 없으면 404. */
    @Operation(
            summary = "평점 수정",
            description = "내가 남긴 평점의 별점과 후기를 **통째로 교체**합니다. 🔒 토큰이 필요합니다. 후기를 빼면 기존 후기가 null이 됩니다."
    )
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공 (result는 수정된 평점)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "다른 회원의 평점",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.RATING_FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "없는 평점 (주인 검사보다 먼저 확인)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.RATING_NOT_FOUND)))
    })
    @PatchMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<RatingResponse>> update(
            @LoginMember Long memberId,
            @Parameter(description = "수정할 평점 ID", example = "1")
            @PathVariable Long ratingId,
            @Valid @RequestBody UpdateRatingRequest request) {
        RatingResponse response = ratingService.update(ratingId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("평점을 수정했습니다.", response));
    }

    /** 평점 삭제. 204는 본문을 보낼 수 없어 공통 형식을 지키려고 200 + result null로 응답한다. */
    @Operation(
            summary = "평점 삭제",
            description = "내가 남긴 평점을 삭제합니다. 🔒 토큰이 필요합니다. 성공은 204가 아니라 **200 + result null**입니다."
    )
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공 (result는 null)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "다른 회원의 평점",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.RATING_FORBIDDEN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "이미 삭제됐거나 없는 평점",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.RATING_NOT_FOUND)))
    })
    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginMember Long memberId,
            @Parameter(description = "삭제할 평점 ID", example = "1")
            @PathVariable Long ratingId) {
        ratingService.delete(ratingId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("평점을 삭제했습니다.", null));
    }
}
