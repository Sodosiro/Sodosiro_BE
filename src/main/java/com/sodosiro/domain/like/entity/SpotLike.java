package com.sodosiro.domain.like.entity;

import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 관광지 좋아요 (tourist_spot 전용).
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

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(name = "content_id", nullable = false)
    @Comment("tourist_spot content_id")
    private Long contentId;

    /** 기존 ID 기반 조회와 호환되는 읽기 전용 연관관계. */
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private TouristSpot touristSpot;

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
}
