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
        name = "digging_bookmark",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_digging_bookmark_user",
                columnNames = {"digging_id", "user_id"}
        ),
        indexes = @Index(name = "idx_digging_bookmark_user", columnList = "user_id, id DESC")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("디깅 즐겨찾기")
public class DiggingBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "digging_id", nullable = false)
    @Comment("대상 디깅 PK")
    private Long diggingId;

    @Column(name = "user_id", nullable = false)
    @Comment("즐겨찾기한 사용자")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static DiggingBookmark of(Long diggingId, Long userId) {
        DiggingBookmark bookmark = new DiggingBookmark();
        bookmark.diggingId = diggingId;
        bookmark.userId = userId;
        return bookmark;
    }
}
