package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.service.RatingService;
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
public class RatingController {

    private final RatingService ratingService;

    /**
     * 유저별 평점 조회. 최신순. 누구나 볼 수 있는 공개 목록이라 토큰 없이 호출한다.
     * (선택 미션) ?page=0&size=10으로 잘라 받을 수 있다. 생략하면 첫 페이지 20개.
     */
    @GetMapping("/members/{memberId}/ratings")
    public List<RatingResponse> getMemberRatings(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.") int size) {
        return ratingService.getRatingsByMember(memberId, page, size);
    }

    /** 평점 등록. 201 + 등록된 평점. */
    @PostMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> create(
            @LoginMember Long memberId,
            @Valid @RequestBody CreateRatingRequest request) {
        RatingResponse response = ratingService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("평점을 등록했습니다.", response));
    }

    /** 평점 수정. 200 + 수정된 평점. 본인 평점이 아니면 403, 없으면 404. */
    @PatchMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<RatingResponse>> update(
            @LoginMember Long memberId,
            @PathVariable Long ratingId,
            @Valid @RequestBody UpdateRatingRequest request) {
        RatingResponse response = ratingService.update(ratingId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("평점을 수정했습니다.", response));
    }

    /** 평점 삭제. 204는 본문을 보낼 수 없어 공통 형식을 지키려고 200 + result null로 응답한다. */
    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginMember Long memberId,
            @PathVariable Long ratingId) {
        ratingService.delete(ratingId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("평점을 삭제했습니다.", null));
    }
}
