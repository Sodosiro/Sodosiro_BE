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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 관광지 기본정보 (areaBasedList2, contentTypeId=12)
 * content_id 는 외부 API 의 contentid 를 그대로 사용 (자동 생성 아님)
 */
@Entity
@Getter
@Table(
        name = "tourist_spot",
        indexes = {
                @Index(name = "idx_spot_region", columnList = "area_code, sigungu_code"),
                @Index(name = "idx_spot_cat", columnList = "cat1, cat2, cat3")
        }
)
@Comment("관광지 기본정보 (areaBasedList2, contentTypeId=12)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TouristSpot {

    @Id
    @Column(name = "content_id")
    @Comment("contentid (외부 API PK)")
    private Long contentId;

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

    @Column(name = "area_code", length = 5)
    @Comment("지역코드 (areacode)")
    private String areaCode;

    @Column(name = "sigungu_code", length = 5)
    @Comment("시군구코드 (sigungucode)")
    private String sigunguCode;

    @Column(name = "cat1", length = 12)
    @Comment("대분류 (category.code)")
    private String cat1;

    @Column(name = "cat2", length = 12)
    @Comment("중분류 (category.code)")
    private String cat2;

    @Column(name = "cat3", length = 12)
    @Comment("소분류 (category.code)")
    private String cat3;

    @Column(name = "first_image", columnDefinition = "text")
    @Comment("firstimage 대표(원본)")
    private String firstImage;

    @Column(name = "homepage", columnDefinition = "text")
    @Comment("홈페이지")
    private String homepage;

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

    @Column(name = "created_time")
    @Comment("원본 생성시각")
    private LocalDateTime createdTime;

    @Column(name = "collected_at")
    @Comment("수집시각")
    private LocalDateTime collectedAt;
}
