package com.sodosiro.domain.review.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "review_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_image_order",
                columnNames = {"review_id", "display_order"}
        ),
        indexes = @Index(name = "idx_review_image_review", columnList = "review_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("리뷰 첨부 이미지")
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "review_id", nullable = false)
    @Comment("소속 리뷰 PK")
    private Long reviewId;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "review_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private Review review;

    @Column(name = "image_url", columnDefinition = "text", nullable = false)
    @Comment("S3 이미지 URL")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    @Comment("노출 순서")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static ReviewImage of(Long reviewId, String imageUrl, int displayOrder) {
        ReviewImage image = new ReviewImage();
        image.reviewId     = reviewId;
        image.imageUrl     = imageUrl;
        image.displayOrder = displayOrder;
        return image;
    }
}
