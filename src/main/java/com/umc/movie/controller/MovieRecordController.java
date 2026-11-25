package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.config.ApiExamples;
import com.umc.movie.config.SwaggerConfig;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.MovieRecordRequest;
import com.umc.movie.dto.MovieRecordResponse;
import com.umc.movie.service.MovieRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 평점과 즐겨찾기를 한 요청으로 저장한다. 트랜잭션 학습용 엔드포인트. */
@RestController
@Tag(name = "영화 기록", description = "평점과 즐겨찾기를 한 번에 저장하는 API")
@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
@RequestMapping("/movie-records")
@RequiredArgsConstructor
public class MovieRecordController {

    private final MovieRecordService movieRecordService;

    @Operation(
            summary = "영화 기록 저장 (평점 + 즐겨찾기)",
            description = """
                    평점을 저장하고, addToFavorites가 true면 즐겨찾기도 함께 저장합니다. 🔒 토큰이 필요합니다.
                    **한 트랜잭션**이라 둘 중 하나라도 실패하면 모두 저장되지 않습니다.
                    addToFavorites가 false면 result.favorite은 null이 정상입니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "저장 성공 (result.rating, result.favorite)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "토큰이 없거나 유효하지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 평점을 남겼거나(RATING409_1) 이미 즐겨찾기함(FAVORITE409_1). 이때 평점도 저장되지 않습니다.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FAVORITE_DUPLICATED)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MovieRecordResponse>> save(
            @LoginMember Long memberId,
            @Valid @RequestBody MovieRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("영화 기록을 저장했습니다.", movieRecordService.save(memberId, request)));
    }
}
