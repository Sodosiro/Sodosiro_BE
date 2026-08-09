package com.sodosiro.domain.region.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.entity.QSigunguCode;
import com.sodosiro.domain.travel.entity.QSpotImage;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 시군구에 속한 관광지별 대표 이미지 한 장을 최대 10장 조회한다. */
@Repository
@RequiredArgsConstructor
public class RegionImageQueryRepository {

    private static final int MAX_REGION_IMAGES = 10;

    private static final QSigunguCode sigungu = QSigunguCode.sigunguCode1;
    private static final QTouristSpot spot = QTouristSpot.touristSpot;
    private static final QSpotImage spotImage = QSpotImage.spotImage;
    private static final QSpotImage firstSpotImage = new QSpotImage("firstSpotImage");

    private final JPAQueryFactory queryFactory;

    public List<RegionImageRow> findRepresentativeImages(Long sigunguId) {
        JPQLQuery<Integer> firstImageOrder = JPAExpressions
                .select(firstSpotImage.order.min())
                .from(firstSpotImage)
                .where(firstSpotImage.contentId.eq(spot.contentId), firstSpotImage.imageUrl.isNotNull());
        StringExpression imageUrl = spotImage.imageUrl.coalesce(spot.firstImage);

        List<Tuple> results = queryFactory
                .select(spot.contentId, spot.title, imageUrl)
                .from(sigungu)
                .join(spot).on(
                        spot.ldongRegnCode.eq(sigungu.areaCode),
                        spot.ldongSignguCode.eq(sigungu.sigunguCode))
                .leftJoin(spotImage).on(
                        spotImage.contentId.eq(spot.contentId),
                        spotImage.order.eq(firstImageOrder))
                .where(sigungu.id.eq(sigunguId), imageUrl.isNotNull())
                .orderBy(spot.likeCount.desc(), spot.reviewCount.desc(), spot.contentId.asc())
                .limit(MAX_REGION_IMAGES)
                .fetch();

        return results.stream()
                .map(tuple -> new RegionImageRow(
                        tuple.get(spot.contentId),
                        tuple.get(spot.title),
                        tuple.get(imageUrl)))
                .toList();
    }

    public record RegionImageRow(Long contentId, String title, String imageUrl) {
    }
}
