package com.sodosiro.domain.course.entity;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.route.dto.TransportMode;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 코스 추천 결과. {@code /recommendations} 호출 시 draft로 저장되고,
 * {@code /confirm/*} 호출 시 같은 row가 확정 상태로 갱신된다 (사용자당 미확정 draft는 1개만 유지).
 */
@Entity
@Getter
@Table(
        name = "course",
        indexes = @Index(name = "idx_course_user_confirmed", columnList = "user_id, is_confirmed")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("AI 코스 추천 결과 (draft/확정 공용)")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("코스 PK")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("소유자 user_id (users 참조)")
    private Long userId;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private User user;

    @Column(name = "start_date", nullable = false)
    @Comment("여행 시작일")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @Comment("여행 종료일")
    private LocalDate endDate;

    @Column(name = "travel_styles")
    @Comment("여행스타일 콤마구분 문자열 (예: CAFE,NATURE), 없으면 null")
    private String travelStyles;

    @Column(name = "must_visit_content_id")
    @Comment("사용자가 지정한 필수 방문지 content_id")
    private Long mustVisitContentId;

    @Column(name = "ai_message", columnDefinition = "text")
    @Comment("AI 한마디 원문")
    private String aiMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", length = 20)
    @Comment("추천 생성 시 선택한 이동수단")
    private TransportMode transportMode;

    @Column(name = "is_confirmed", nullable = false)
    @Comment("확정 여부")
    private Boolean isConfirmed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "days", columnDefinition = "jsonb", nullable = false)
    @Comment("일자별 코스 스냅샷 (추천/확정 시점의 권장 순서, 실제 방문 순서는 별도 GPS 인증 기능에서 추적)")
    private List<DaySnapshot> days;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Comment("여행 상태 (UPCOMING: 예정, IN_PROGRESS: 진행 중, FINISHED: 종료)")
    private CourseStatus status;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static Course createDraft(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            TransportMode transportMode,
            List<TravelStyle> travelStyles,
            Long mustVisitContentId,
            String aiMessage,
            List<DaySnapshot> days) {
        Course course = new Course();
        course.userId = userId;
        course.startDate = startDate;
        course.endDate = endDate;
        course.transportMode = transportMode;
        course.travelStyles = joinTravelStyles(travelStyles);
        course.mustVisitContentId = mustVisitContentId;
        course.aiMessage = aiMessage;
        course.days = days;
        course.isConfirmed = false;
        course.status = CourseStatus.UPCOMING;
        return course;
    }

    public void confirm(TransportMode transportMode, List<DaySnapshot> days) {
        this.transportMode = transportMode;
        this.days = days;
        if (mustVisitContentId != null && !containsMustVisitSpot(days)) {
            this.mustVisitContentId = null;
        }
        this.isConfirmed = true;
        this.updatedAt = LocalDateTime.now();
    }

    /** 확정 과정에서 필수 방문지가 목록에서 빠졌다면 mustVisitContentId도 함께 정리해 스냅샷과 어긋나지 않게 한다 */
    private static boolean containsMustVisitSpot(List<DaySnapshot> days) {
        return days.stream()
                .flatMap(day -> day.spots().stream())
                .anyMatch(SpotSnapshot::mustVisit);
    }

    public List<TravelStyle> travelStylesOrEmpty() {
        if (travelStyles == null || travelStyles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(travelStyles.split(","))
                .map(TravelStyle::valueOf)
                .toList();
    }

    private static String joinTravelStyles(List<TravelStyle> travelStyles) {
        if (travelStyles == null || travelStyles.isEmpty()) {
            return null;
        }
        return travelStyles.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public record DaySnapshot(int day, LocalDate date, List<SpotSnapshot> spots) {
    }

    public record SpotSnapshot(
            Long contentId,
            String title,
            String firstImage,
            BigDecimal mapX,
            BigDecimal mapY,
            Integer category,
            boolean mustVisit) {
    }
}
