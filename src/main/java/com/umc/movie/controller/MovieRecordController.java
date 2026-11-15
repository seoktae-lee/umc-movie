package com.umc.movie.controller;

import com.umc.movie.annotation.LoginMember;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.MovieRecordRequest;
import com.umc.movie.dto.MovieRecordResponse;
import com.umc.movie.service.MovieRecordService;
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
@RequestMapping("/movie-records")
@RequiredArgsConstructor
public class MovieRecordController {

    private final MovieRecordService movieRecordService;

    @PostMapping
    public ResponseEntity<ApiResponse<MovieRecordResponse>> save(
            @LoginMember Long memberId,
            @Valid @RequestBody MovieRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("영화 기록을 저장했습니다.", movieRecordService.save(memberId, request)));
    }
}
