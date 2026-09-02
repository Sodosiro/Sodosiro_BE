package com.sodosiro.domain.badge.entity;

import com.sodosiro.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** 유저별 뱃지 획득 기록. 지역 최초 방문(GPS 인증) 시 1회만 생성된다. */
@Entity
@Getter
@Table(
        name = "user_badge",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_badge_user_badge", columnNames = {"user_id", "badge_id"}),
        indexes = @Index(name = "idx_user_badge_user", columnList = "user_id")
)
@Comment("유저별 뱃지 획득 기록")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("PK")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("획득한 유저 (users 참조)")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "badge_id", nullable = false)
    @Comment("획득한 뱃지 (badge 참조)")
    private Long badgeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", insertable = false, updatable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    @Comment("획득 시각")
    private LocalDateTime earnedAt;

    public static UserBadge create(Long userId, Long badgeId) {
        UserBadge userBadge = new UserBadge();
        userBadge.userId = userId;
        userBadge.badgeId = badgeId;
        userBadge.earnedAt = LocalDateTime.now();
        return userBadge;
    }
}
