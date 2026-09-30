package com.umc.movie.controller;

import com.umc.movie.dto.RatingResponse;
import com.umc.movie.service.RatingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 파일은 자원(Rating) 기준으로 나누고, URL은 프론트가 이해하기 쉬운 /members/... 모양으로 맞춘다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class RatingController {

    private final RatingService ratingService;

    /**
     * 유저별 평점 조회. 최신순.
     * (선택 미션) ?page=0&size=10으로 잘라 받을 수 있다. 생략하면 첫 페이지 20개.
     */
    @GetMapping("/{memberId}/ratings")
    public List<RatingResponse> getMemberRatings(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.") int size) {
        return ratingService.getRatingsByMember(memberId, page, size);
    }
}
