package com.umc.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Member·Rating·Favorite이 똑같이 가지는 생성·수정 시각.
 * 테이블이 되지는 않고(@MappedSuperclass), 상속한 엔티티에 컬럼 정의만 물려준다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    // 7주차부터 등록·수정 응답에 시각을 싣는다. JVM 시각은 소수 초까지 있어 DATETIME에 저장된 값과 달라지므로,
    // source = DB로 DB가 정한 시각을 쓰고 저장 직후 다시 읽어 응답과 조회가 같은 값을 보여 주게 한다.
    // 처음 저장될 때 채운다. 이후에는 바뀌지 않는다.
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 저장(INSERT/UPDATE)될 때마다 현재 시각으로 다시 채운다.
    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
