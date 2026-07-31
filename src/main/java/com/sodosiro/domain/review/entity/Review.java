package com.sodosiro.domain.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_user_spot",
                columnNames = {"content_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_review_content", columnList = "content_id"),
                @Index(name = "idx_review_user",    columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("사용자 리뷰")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("리뷰 PK")
    private Long id;

    @Column(name = "content_id", nullable = false)
    @Comment("관광지 content_id (tourist_spot 참조)")
    private Long contentId;

    @Column(name = "user_id", nullable = false)
    @Comment("작성자 user_id (users 참조)")
    private Long userId;

    @Column(name = "rating", nullable = false)
    @Comment("별점 (1~5)")
    private Short rating;

    @Column(name = "body", columnDefinition = "text")
    @Comment("리뷰 본문")
    private String body;

    @Column(name = "is_deleted", nullable = false)
    @Comment("소프트 딜리트 여부")
    private Boolean isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("작성 일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    /**
     * Initializes the creation timestamp and deletion status before persistence.
     */
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    /**
     * Creates a review with the specified content, user, rating, and body.
     *
     * @param contentId the identifier of the reviewed content
     * @param userId    the identifier of the reviewing user
     * @param rating    the review rating
     * @param body      the review text
     * @return the newly created review
     */
    public static Review create(Long contentId, Long userId, Short rating, String body) {
        Review review = new Review();
        review.contentId = contentId;
        review.userId    = userId;
        review.rating    = rating;
        review.body      = body;
        return review;
    }

    /**
     * Updates the review rating and body and records the modification time.
     *
     * @param rating the updated rating
     * @param body   the updated review body
     */
    public void update(Short rating, String body) {
        this.rating    = rating;
        this.body      = body;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the review as deleted and records the deletion time.
     */
    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }
}
