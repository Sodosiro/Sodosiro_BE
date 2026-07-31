package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * kakao_spot 대표 이미지 (장소당 최대 1개).
 * ETL이 관리하므로 BE에서는 읽기 전용으로 사용한다.
 */
@Entity
@Getter
@Table(name = "kakao_spot_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("카카오 인기 장소 대표 이미지")
public class KakaoSpotImage {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "kakao_spot_id", nullable = false)
    @Comment("kakao_spot PK")
    private Long kakaoSpotId;

    @Column(name = "tourist_content_id")
    @Comment("이미지 출처 tourist_spot (TOURIST_SPOT 타입인 경우)")
    private Long touristContentId;

    @Column(name = "image_url", columnDefinition = "text", nullable = false)
    @Comment("이미지 URL")
    private String imageUrl;

    @Column(name = "source_doc_url", columnDefinition = "text")
    @Comment("원문 URL")
    private String sourceDocUrl;

    @Column(name = "source_site_name", length = 50, nullable = false)
    @Comment("출처 사이트명")
    private String sourceSiteName;

    @Column(name = "source_type", length = 30, nullable = false)
    @Comment("이미지 소스 타입 (TOURIST_SPOT / TISTORY)")
    private String sourceType;

    @Column(name = "collected_at", nullable = false)
    @Comment("수집 시각")
    private LocalDateTime collectedAt;
}
