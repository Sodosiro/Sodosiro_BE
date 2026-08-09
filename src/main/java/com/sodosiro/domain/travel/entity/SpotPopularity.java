package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * tourist_spot 기반 인기도 점수 및 카테고리별 순위.
 * ETL(popular_spot_collection DAG)이 매 시간 갱신하며 BE에서는 읽기 전용으로 사용한다.
 */
@Entity
@Getter
@Table(
        name = "spot_popularity",
        indexes = {
                @Index(name = "idx_spot_popularity_score", columnList = "popularity_score DESC"),
                @Index(name = "idx_spot_popularity_rank", columnList = "category_rank")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("tourist_spot 기반 인기도 점수 및 카테고리별 순위")
public class SpotPopularity {

    @Id
    @Column(name = "content_id")
    @Comment("tourist_spot PK (1:1)")
    private Long contentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private TouristSpot touristSpot;

    @Column(name = "mention_score", nullable = false)
    @Comment("카카오 블로그·카페 언급 누적 점수 (시간당 감쇠 적용)")
    private Double mentionScore;

    @Column(name = "popularity_score", nullable = false)
    @Comment("종합 인기도 점수 (mention + like + review + rating 가중 합산)")
    private Double popularityScore;

    @Column(name = "category_rank")
    @Comment("카테고리 내 순위 (10위 초과 시 NULL)")
    private Integer categoryRank;

    @Column(name = "rank_tag", length = 30)
    @Comment("인기 순위 태그 (예: '인기 카페 1위'). 10위 초과 시 NULL")
    private String rankTag;

    @Column(name = "calculated_at", nullable = false)
    @Comment("마지막 순위 계산 시각")
    private LocalDateTime calculatedAt;
}
