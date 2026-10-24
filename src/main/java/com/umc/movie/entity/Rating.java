package com.umc.movie.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rating", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "movie_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 영화는 우리 DB에 없다. TMDB가 준 영화 id를 FK가 아닌 일반 컬럼으로 담는다.
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder
    public Rating(Member member, Long movieId, Integer score, String comment) {
        this.member = member;
        this.movieId = movieId;
        this.score = score;
        this.comment = comment;
    }

    /**
     * 별점·후기 수정. setter를 열지 않고 "수정"이라는 의미 있는 메서드 하나만 둔다.
     * 트랜잭션 안에서 값을 바꾸면 JPA가 변경을 감지해 UPDATE를 보낸다. (dirty checking)
     */
    public void update(Integer score, String comment) {
        this.score = score;
        this.comment = comment;
    }
}
