package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 관광지 이미지 (detailImage2) - tourist_spot 과 1:다
 * PK 는 surrogate id(IDENTITY), 자연키 (content_id, order) 는 UNIQUE 제약으로 중복 방지.
 */
@Entity
@Getter
@Table(
        name = "spot_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_spot_image_content_order",
                columnNames = {"content_id", "`order`"}
        ),
        indexes = {
                @Index(name = "idx_image_content", columnList = "content_id")
        }
)
@Comment("관광지 이미지 (detailImage2) - 1:다")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("PK (surrogate)")
    private Long id;

    @Column(name = "content_id", nullable = false)
    @Comment("관광지 contentid (tourist_spot 참조)")
    private Long contentId;

    /** 이미지 순서 ("order" 는 예약어라 백틱으로 이스케이프) */
    @Column(name = "`order`", nullable = false)
    @Comment("이미지 순서")
    private Integer order;

    @Column(name = "type", length = 200)
    @Comment("이미지 타입")
    private String type;

    @Column(name = "image_url", columnDefinition = "text")
    @Comment("이미지 URL")
    private String imageUrl;
}
