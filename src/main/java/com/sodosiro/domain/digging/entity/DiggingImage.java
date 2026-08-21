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
        name = "digging_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_digging_image_order",
                columnNames = {"digging_id", "display_order"}
        ),
        indexes = @Index(name = "idx_digging_image_digging", columnList = "digging_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("디깅 첨부 이미지 (최대 5장)")
public class DiggingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "digging_id", nullable = false)
    @Comment("소속 디깅 PK")
    private Long diggingId;

    @Column(name = "image_url", columnDefinition = "text", nullable = false)
    @Comment("S3 이미지 URL")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    @Comment("노출 순서 (0~4)")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static DiggingImage of(Long diggingId, String imageUrl, int displayOrder) {
        DiggingImage image = new DiggingImage();
        image.diggingId = diggingId;
        image.imageUrl = imageUrl;
        image.displayOrder = displayOrder;
        return image;
    }
}
