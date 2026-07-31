package com.sodosiro.domain.like.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 관광지 좋아요.
 * content_id(tourist_spot) 또는 kakao_spot_id 중 정확히 하나만 값을 가진다.
 * kakao_spot 삭제 시 ON DELETE CASCADE 로 함께 제거된다.
 */
@Entity
@Getter
@Table(
        name = "spot_like",
        indexes = @Index(name = "idx_like_user_created", columnList = "user_id, id DESC")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("관광지 좋아요")
public class SpotLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("좋아요 누른 사용자")
    private Long userId;

    @Column(name = "content_id")
    @Comment("tourist_spot content_id (일반 관광지)")
    private Long contentId;

    @Column(name = "kakao_spot_id")
    @Comment("kakao_spot id (인기 관광지, ON DELETE CASCADE)")
    private Long kakaoSpotId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("좋아요 일시")
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static SpotLike ofTourist(Long userId, Long contentId) {
        SpotLike like = new SpotLike();
        like.userId = userId;
        like.contentId = contentId;
        return like;
    }

    public static SpotLike ofKakao(Long userId, Long kakaoSpotId) {
        SpotLike like = new SpotLike();
        like.userId = userId;
        like.kakaoSpotId = kakaoSpotId;
        return like;
    }
}
