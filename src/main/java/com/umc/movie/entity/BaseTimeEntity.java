package com.umc.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Member·Rating·Favorite이 똑같이 가지는 생성·수정 시각.
 * 테이블이 되지는 않고(@MappedSuperclass), 상속한 엔티티에 컬럼 정의만 물려준다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    // 처음 저장될 때 Hibernate가 자동으로 채운다. 이후에는 바뀌지 않는다.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 저장(INSERT/UPDATE)될 때마다 현재 시각으로 다시 채운다.
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
