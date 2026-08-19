package com.sodosiro.domain.gps.entity;

import com.sodosiro.domain.course.entity.Course;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * 확정된 코스(단일 여행)에 속한 스팟별 GPS 방문 인증 기록.
 * 사용자의 현재 좌표가 스팟 좌표 기준 반경 300m 이내로 들어온 경우에만 생성된다 (원본 좌표는 저장하지 않음).
 * row가 존재한다는 것 자체가 인증 성공을 의미하므로 별도의 verified 플래그는 두지 않는다.
 */
@Entity
@Getter
@Table(
        name = "gps",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gps_verification_course_content_day",
                columnNames = {"course_id", "content_id", "day"}
        ),
        indexes = @Index(name = "idx_gps_verification_user", columnList = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("코스 스팟별 GPS 방문 인증 기록")
public class Gps {

    public static final double VERIFICATION_RADIUS_METERS = 300.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("GPS 인증 PK")
    private Long id;

    @Column(name = "course_id", nullable = false)
    @Comment("소속 코스(단일 여행) course_id")
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private Course course;

    @Column(name = "user_id", nullable = false)
    @Comment("인증 소유자 user_id (course.user_id와 동일, 조회 편의용)")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    @Column(name = "content_id", nullable = false)
    @Comment("방문 대상 관광지 content_id (tourist_spot 참조)")
    private Long contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private TouristSpot touristSpot;

    @Column(name = "day", nullable = false)
    @Comment("코스 내 방문 예정 일자 (Course.DaySnapshot.day)")
    private Integer day;

    @Column(name = "verified_at", nullable = false)
    @Comment("GPS 방문 인증 성공 시각")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /** 반경 300m 판정을 통과한 시점에만 호출된다. */
    public static Gps create(Long courseId, Long userId, Long contentId, Integer day) {
        Gps gps = new Gps();
        gps.courseId = courseId;
        gps.userId = userId;
        gps.contentId = contentId;
        gps.day = day;
        gps.verifiedAt = LocalDateTime.now();
        return gps;
    }
}
