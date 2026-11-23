package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.FavoriteStatusResponse;
import com.umc.movie.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 즐겨찾기는 로그인한 회원의 데이터다. 모든 API가 토큰으로 회원을 알아내고 memberId는 받지 않는다. */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> create(
            @LoginMember Long memberId,
            @Valid @RequestBody CreateFavoriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("즐겨찾기를 등록했습니다.", favoriteService.create(memberId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> list(@LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok("즐겨찾기 목록을 조회했습니다.", favoriteService.getMyFavorites(memberId)));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<FavoriteStatusResponse>> status(
            @LoginMember Long memberId,
            @PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.ok("즐겨찾기 여부를 조회했습니다.", favoriteService.getStatus(memberId, movieId)));
    }

    /** 삭제도 공통 응답 형식을 지키려고 204가 아니라 200 + result null로 응답한다. (7주차 평점 삭제와 같다) */
    @DeleteMapping("/{movieId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginMember Long memberId,
            @PathVariable Long movieId) {
        favoriteService.delete(memberId, movieId);
        return ResponseEntity.ok(ApiResponse.ok("즐겨찾기를 해제했습니다.", null));
    }
}
