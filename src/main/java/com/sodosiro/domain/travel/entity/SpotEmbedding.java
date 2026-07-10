package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(name = "embedding")
    @Comment("title + overview 임베딩 벡터 (pgvector, 1536차원)")
    private float[] embedding;

    @Column(name = "input_text", columnDefinition = "text")
    @Comment("임베딩 원문 (title || overview)")
    private String inputText;

    @Column(name = "created_at")
    @Comment("생성시각")
    private LocalDateTime createdAt;
}
