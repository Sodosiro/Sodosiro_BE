package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "spot_ai_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotAiRecommendation {
    @Id
    @Column(name = "content_id")
    private Long contentId;
    @Column(nullable = false, columnDefinition = "text")
    private String reason;
    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static SpotAiRecommendation create(Long contentId, String reason, LocalDateTime now, LocalDateTime expiresAt) {
        SpotAiRecommendation snapshot = new SpotAiRecommendation();
        snapshot.contentId = contentId;
        snapshot.reason = reason;
        snapshot.promptVersion = "v1";
        snapshot.generatedAt = now;
        snapshot.expiresAt = expiresAt;
        return snapshot;
    }
}
