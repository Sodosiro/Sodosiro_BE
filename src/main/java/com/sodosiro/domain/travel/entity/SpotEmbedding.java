package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 관광지 임베딩 (pgvector) — 코사인 유사도 추천용
 * tourist_spot 과 1:1 (content_id 공유)
 */
@Entity
@Getter
@Table(name = "spot_embedding")
@Comment("관광지 임베딩 (pgvector) — 코사인 유사도 추천용")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotEmbedding {

    @Id
    @Column(name = "content_id")
    @Comment("관광지 contentid (tourist_spot 와 1:1)")
    private Long contentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private TouristSpot touristSpot;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(name = "embedding")
    @Comment("AI가 추출한 여행지 키워드의 임베딩 벡터 (pgvector, 1536차원)")
    private float[] embedding;

    @Column(name = "input_text", columnDefinition = "text")
    @Comment("임베딩 원문 (장소명·대표분류·설명·키워드를 합친 검색 색인문)")
    private String inputText;

    @Column(name = "keyword_text", columnDefinition = "text")
    @Comment("정규화 키워드 목록 — 첫 토큰이 대표 분류 (카테고리 필터용)")
    private String keywordText;

    @Column(name = "created_at")
    @Comment("생성시각")
    private LocalDateTime createdAt;

    private SpotEmbedding(Long contentId, float[] embedding, String inputText, String keywordText) {
        this.contentId = contentId;
        this.embedding = embedding;
        this.inputText = inputText;
        this.keywordText = keywordText;
        this.createdAt = LocalDateTime.now();
    }

    public static SpotEmbedding of(Long contentId, float[] embedding, String inputText, String keywordText) {
        return new SpotEmbedding(contentId, embedding, inputText, keywordText);
    }
}
