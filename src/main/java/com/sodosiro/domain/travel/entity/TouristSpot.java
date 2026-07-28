package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 여행 콘텐츠 기본정보 (areaBasedList2)
 * content_id 는 외부 API 의 contentid 를 그대로 사용 (자동 생성 아님)
 */
@Entity
@Getter
@Table(
        name = "tourist_spot",
        indexes = {
                @Index(name = "idx_spot_sigungu", columnList = "sigungu_code"),
                @Index(name = "idx_spot_ldong_region", columnList = "ldong_regn_code, ldong_signgu_code"),
                @Index(name = "idx_spot_category", columnList = "category")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TouristSpot {

    @Id
    @Column(name = "content_id")
    @Comment("contentid (외부 API PK)")
    private Long contentId;

    @Column(name = "content_type_id", length = 5)
    @Comment("TourAPI 콘텐츠 유형 코드")
    private String contentTypeId;

    @Column(name = "title", length = 200, nullable = false)
    @Comment("관광지명")
    private String title;

    @Column(name = "addr1", length = 300)
    @Comment("주소")
    private String addr1;

    @Column(name = "addr2", length = 200)
    @Comment("상세주소")
    private String addr2;

    @Column(name = "zipcode", length = 10)
    @Comment("우편번호")
    private String zipcode;

    @Column(name = "map_x", precision = 13, scale = 10)
    @Comment("mapx 경도")
    private BigDecimal mapX;

    @Column(name = "map_y", precision = 13, scale = 10)
    @Comment("mapy 위도")
    private BigDecimal mapY;

    @Column(name = "map_level")
    @Comment("지도 확대 레벨")
    private Short mapLevel;

    @Column(name = "ldong_regn_code", length = 5)
    @Comment("법정동 시도 코드")
    private String ldongRegnCode;

    @Column(name = "ldong_signgu_code", length = 5)
    @Comment("법정동 시군구 코드")
    private String ldongSignguCode;

    @Column(name = "sigungu_code", length = 5)
    @Comment("시군구코드 (sigungucode)")
    private String sigunguCode;

    @Column(name = "category", nullable = false)
    @Comment("서비스 카테고리 (1=식당, 2=카페, 3=쇼핑, 4=관광지, 5=자연, 6=액티비티, 7=숙박)")
    private Integer category;

    @Column(name = "first_image", columnDefinition = "text")
    @Comment("firstimage 대표(원본)")
    private String firstImage;

    @Column(name = "homepage", columnDefinition = "text")
    @Comment("홈페이지")
    private String homepage;

    @Column(name = "overview", columnDefinition = "text")
    @Comment("detailCommon2 소개글")
    private String overview;

    @Column(name = "infocenter", length = 200)
    @Comment("detailIntro2 문의·안내")
    private String infocenter;

    @Column(name = "usetime", columnDefinition = "text")
    @Comment("이용시간")
    private String usetime;

    @Column(name = "restdate", columnDefinition = "text")
    @Comment("쉬는날")
    private String restdate;

    @Column(name = "parking", columnDefinition = "text")
    @Comment("주차")
    private String parking;

    @Column(name = "expguide", columnDefinition = "text")
    @Comment("체험안내 / AI 메타데이터용")
    private String expguide;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_info", columnDefinition = "jsonb")
    @Comment("detailInfo2 반복 항목 원본 배열")
    private List<Map<String, Object>> detailInfo;

    @Column(name = "created_time")
    @Comment("원본 생성시각")
    private LocalDateTime createdTime;

    @Column(name = "collected_at")
    @Comment("수집시각")
    private LocalDateTime collectedAt;
}
