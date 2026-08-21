package com.sodosiro.domain.digging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Table(
        name = "digging_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_digging_like_user",
                columnNames = {"digging_id", "user_id"}
        ),
        indexes = @Index(name = "idx_digging_like_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("디깅 좋아요")
public class DiggingLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "digging_id", nullable = false)
    @Comment("대상 디깅 PK")
    private Long diggingId;

    @Column(name = "user_id", nullable = false)
    @Comment("좋아요 누른 사용자")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static DiggingLike of(Long diggingId, Long userId) {
        DiggingLike like = new DiggingLike();
        like.diggingId = diggingId;
        like.userId = userId;
        return like;
    }
}
