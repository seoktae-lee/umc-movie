package com.umc.movie.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    // RFC 5321 기준 이메일 최대 길이
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // 평문이 아니라 bcrypt 해시(항상 60자)를 저장한다. (시드 데이터 2명은 더미 문자열)
    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    // Member.builder().email(..).password(..).nickname(..).build() 로 만든다.
    @Builder
    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    // @Setter를 열지 않고 의도가 드러나는 메서드만 둔다. 이 엔티티에서 바뀔 수 있는 값은 닉네임뿐이다.
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
