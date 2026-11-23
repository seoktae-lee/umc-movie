package com.umc.movie.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 즐겨찾기 = "어떤 회원이 어떤 TMDB 영화를 보관한다"는 관계 한 줄.
 * 영화 정보는 TMDB가 갖고 있으므로 우리는 movieId만 저장한다. (movie 테이블이 없으니 FK도 아니다)
 * 생성·수정 시각은 BaseTimeEntity를 상속해 다시 선언하지 않는다.
 */
@Entity
@Table(name = "favorite",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorite_member_movie", columnNames = {"member_id", "movie_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Builder
    public Favorite(Member member, Long movieId) {
        this.member = member;
        this.movieId = movieId;
    }
}
