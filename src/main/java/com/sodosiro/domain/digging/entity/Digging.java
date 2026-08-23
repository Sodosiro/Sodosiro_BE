package com.sodosiro.domain.digging.entity;

import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * 완료된 코스에 포함된 여행지 1곳에 대해 남기는 회고 포스팅.
 * 같은 코스의 같은 여행지에는 활성 디깅 1건만 작성할 수 있다.
 */
@Entity
@Getter
@Table(
        name = "digging",
        indexes = {
                @Index(name = "idx_digging_user", columnList = "user_id"),
                @Index(name = "idx_digging_content", columnList = "content_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("코스 완료 후 여행지 회고 포스팅")
public class Digging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("디깅 PK")
    private Long id;

    @Column(name = "course_id", nullable = false)
    @Comment("대상 코스 course_id")
    private Long courseId;

    @Column(name = "content_id", nullable = false)
    @Comment("대상 관광지 content_id (tourist_spot 참조)")
    private Long contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private TouristSpot touristSpot;

    @Column(name = "user_id", nullable = false)
    @Comment("작성자 user_id (users 참조)")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    @Column(name = "body", length = 300)
    @Comment("감성 한마디 (최대 300자)")
    private String body;

    @Column(name = "like_count", nullable = false)
    @Comment("좋아요 수 (캐시)")
    private Integer likeCount;

    @Column(name = "is_deleted", nullable = false)
    @Comment("소프트 딜리트 여부")
    private Boolean isDeleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("작성 일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static Digging create(Long courseId, Long contentId, Long userId, String body) {
        Digging digging = new Digging();
        digging.courseId = courseId;
        digging.contentId = contentId;
        digging.userId = userId;
        digging.body = body;
        digging.likeCount = 0;
        digging.isDeleted = false;
        return digging;
    }

    public void update(String body) {
        this.body = body;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseLikeCount() {
        this.likeCount = this.likeCount + 1;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }
}
