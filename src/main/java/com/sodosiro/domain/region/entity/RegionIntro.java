package com.sodosiro.domain.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 지역 소개 카드용 소규모 큐레이션 데이터. */
@Entity
@Getter
@Table(name = "region_intro")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegionIntro {

    @Id
    @Column(name = "sigungu_id")
    private Long sigunguId;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "intro", nullable = false, columnDefinition = "text")
    private String intro;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_tags", nullable = false, columnDefinition = "jsonb")
    private List<String> themeTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_reasons", nullable = false, columnDefinition = "jsonb")
    private List<String> recommendationReasons;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_season", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> bestSeason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "food_tags", nullable = false, columnDefinition = "jsonb")
    private List<String> foodTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "featured_content_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> featuredContentIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
