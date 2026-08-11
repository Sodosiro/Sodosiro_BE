package com.sodosiro.domain.review.entity;

import com.sodosiro.domain.review.constants.ReviewVisitType;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
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

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private TouristSpot touristSpot;

    @Column(name = "user_id", nullable = false)
    @Comment("작성자 user_id (users 참조)")
    private Long userId;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(name = "rating", nullable = false, precision = 3, scale = 1)
    @Comment("별점 (1.0~5.0, 소수점 한 자리)")
    private BigDecimal rating;

    @Column(name = "body", columnDefinition = "text")
    @Comment("리뷰 본문")
    private String body;

    @Column(name = "is_deleted", nullable = false)
    @Comment("소프트 딜리트 여부")
    private Boolean isDeleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false, length = 20)
    @Comment("방문 인증 유형")
    private ReviewVisitType visitType = ReviewVisitType.GENERAL;

    @Column(name = "gps_verified_at")
    @Comment("GPS 방문 인증 성공 시각")
    private LocalDateTime gpsVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("작성 일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    public static Review create(Long contentId, Long userId, BigDecimal rating, String body) {
        Review review = new Review();
        review.contentId = contentId;
        review.userId    = userId;
        review.rating    = rating;
        review.body      = body;
        return review;
    }

    public void update(BigDecimal rating, String body) {
        this.rating    = rating;
        this.body      = body;
        this.updatedAt = LocalDateTime.now();
    }

    public void verifyGpsVisit() {
        if (visitType == ReviewVisitType.GPS_VERIFIED) {
            return;
        }
        visitType = ReviewVisitType.GPS_VERIFIED;
        gpsVerifiedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }
}
