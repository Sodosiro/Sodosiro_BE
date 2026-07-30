package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ETL이 수집한 카카오 기반 인기 장소. */
@Entity
@Getter
@Table(name = "kakao_spot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KakaoSpot {

    @Id
    private Long id;

    @Column(name = "kakao_place_id")
    private String kakaoPlaceId;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "category_group_code")
    private String categoryGroupCode;

    @Column(name = "category_group_name")
    private String categoryGroupName;

    private Double longitude;
    private Double latitude;

    @Column(name = "popularity_score")
    private Double popularityScore;
}
