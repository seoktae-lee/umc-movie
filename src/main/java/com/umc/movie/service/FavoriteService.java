package com.umc.movie.service;

import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.FavoriteStatusResponse;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;

    /**
     * 즐겨찾기 등록. 같은 영화를 이미 담았으면 409.
     * 서버의 사전 검사는 친절한 오류(FAVORITE409_1)를 위한 것이고,
     * DB의 UNIQUE(member_id, movie_id)는 동시 요청까지 막는 마지막 안전망이다.
     */
    @Transactional
    public FavoriteResponse create(Long memberId, CreateFavoriteRequest request) {
        return create(memberId, request.movieId());
    }

    /** 영화 기록 저장(MovieRecordService)도 같은 규칙을 쓰므로 영화 ID만 받는 형태를 열어 둔다. */
    @Transactional
    public FavoriteResponse create(Long memberId, Long movieId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBER404_1", "해당 회원을 찾을 수 없습니다."));

        if (favoriteRepository.existsByMember_MemberIdAndMovieId(memberId, movieId)) {
            throw new ApiException(HttpStatus.CONFLICT, "FAVORITE409_1", "이미 즐겨찾기한 영화입니다.");
        }

        Favorite favorite = Favorite.builder().member(member).movieId(movieId).build();
        return FavoriteResponse.from(favoriteRepository.save(favorite));
    }

    /** 내 즐겨찾기만, 최신 등록 순. 하나도 없으면 오류가 아니라 빈 배열이다. */
    @Transactional(readOnly = true)
    public List<FavoriteResponse> getMyFavorites(Long memberId) {
        return favoriteRepository.findByMember_MemberIdOrderByFavoriteIdDesc(memberId).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse getStatus(Long memberId, Long movieId) {
        return new FavoriteStatusResponse(favoriteRepository.existsByMember_MemberIdAndMovieId(memberId, movieId));
    }

    /** 없던 관계는 해제할 수 없다. 요청자는 확인됐지만 데이터가 없으므로 401이 아니라 404. */
    @Transactional
    public void delete(Long memberId, Long movieId) {
        Favorite favorite = favoriteRepository.findByMember_MemberIdAndMovieId(memberId, movieId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FAVORITE404_1",
                        "해제할 즐겨찾기를 찾을 수 없습니다."));
        favoriteRepository.delete(favorite);
    }
}
