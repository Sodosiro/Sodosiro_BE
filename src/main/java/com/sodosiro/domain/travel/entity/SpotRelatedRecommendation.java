package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상세 화면의 연관 관광지 추천 스냅샷. 후보 ID의 순서를 24시간 동안 보존한다. */
@Entity
@Getter
@Table(name = "spot_related_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotRelatedRecommendation {

    @Id
    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "related_content_ids", nullable = false, columnDefinition = "text")
    private String relatedContentIds;

    @Column(name = "scoring_version", nullable = false, length = 30)
    private String scoringVersion;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private SpotRelatedRecommendation(Long contentId, String relatedContentIds,
            LocalDateTime generatedAt, LocalDateTime expiresAt) {
        this.contentId = contentId;
        this.relatedContentIds = relatedContentIds;
        this.scoringVersion = "v1";
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }

    public static SpotRelatedRecommendation create(Long contentId, String relatedContentIds,
            LocalDateTime generatedAt, LocalDateTime expiresAt) {
        return new SpotRelatedRecommendation(contentId, relatedContentIds, generatedAt, expiresAt);
    }
}
