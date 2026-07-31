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

import java.time.LocalDateTime;

/**
 * 카카오맵 검증을 통과한 인기 장소.
 * ETL(popular_spot_collection DAG)이 삽입·갱신하므로 BE에서는 읽기 전용으로 사용한다.
 * popularity_score가 임계값(0.01) 미만이 되면 popular_spot_decay DAG가 삭제하며,
 * 연관된 spot_like는 ON DELETE CASCADE로 자동 제거된다.
 */
@Entity
@Getter
@Table(
        name = "kakao_spot",
        indexes = {
                @Index(name = "idx_kakao_city_score",  columnList = "city_name, popularity_score DESC"),
                @Index(name = "idx_kakao_category",    columnList = "category_group_code"),
                @Index(name = "idx_kakao_score",       columnList = "popularity_score")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("카카오맵 인기 장소 (popularity_score 감쇠 후 임계값 미만 자동 삭제)")
public class KakaoSpot {

    @Id
    @Column(name = "id")
    @Comment("PK (identity, ETL 관리)")
    private Long id;

    @Column(name = "kakao_place_id", length = 30, nullable = false)
    @Comment("카카오 장소 ID")
    private String kakaoPlaceId;

    @Column(name = "place_name", length = 200, nullable = false)
    @Comment("장소명")
    private String placeName;

    @Column(name = "city_name", length = 20, nullable = false)
    @Comment("도시명")
    private String cityName;

    @Column(name = "address_name", length = 300)
    @Comment("주소")
    private String addressName;

    @Column(name = "category_group_code", length = 10, nullable = false)
    @Comment("카테고리 그룹 코드")
    private String categoryGroupCode;

    @Column(name = "category_group_name", length = 50)
    @Comment("카테고리 그룹명")
    private String categoryGroupName;

    @Column(name = "longitude", nullable = false)
    @Comment("경도")
    private Double longitude;

    @Column(name = "latitude", nullable = false)
    @Comment("위도")
    private Double latitude;

    @Column(name = "blog_mention_count", nullable = false)
    @Comment("블로그 언급 횟수")
    private Integer blogMentionCount;

    @Column(name = "cafe_mention_count", nullable = false)
    @Comment("카페 언급 횟수")
    private Integer cafeMentionCount;

    @Column(name = "popularity_score", nullable = false)
    @Comment("인기도 점수 (일일 감쇠 적용)")
    private Double popularityScore;

    @Column(name = "like_count", nullable = false, columnDefinition = "integer default 0")
    @Comment("좋아요 수 (캐시)")
    private Integer likeCount;

    @Column(name = "phone", length = 30)
    @Comment("전화번호")
    private String phone;

    @Column(name = "collected_at", nullable = false)
    @Comment("수집 시각")
    private LocalDateTime collectedAt;
}
