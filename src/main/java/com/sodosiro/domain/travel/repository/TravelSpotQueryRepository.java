package com.sodosiro.domain.travel.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.entity.KakaoSpot;
import com.sodosiro.domain.travel.entity.QKakaoSpot;
import com.sodosiro.domain.travel.entity.QSpotImage;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TravelSpotQueryRepository {

    private static final QTouristSpot touristSpot = QTouristSpot.touristSpot;
    private static final QSpotImage spotImage = QSpotImage.spotImage;
    private static final QKakaoSpot kakaoSpot = QKakaoSpot.kakaoSpot;

    private final JPAQueryFactory queryFactory;

    public List<TouristSpot> findTouristSpots(Long cursor, int size, List<Integer> categories, String keyword) {
        BooleanBuilder conditions = new BooleanBuilder();
        if (cursor != null) {
            conditions.and(touristSpot.contentId.lt(cursor));
        }
        if (categories != null && !categories.isEmpty()) {
            conditions.and(touristSpot.category.in(categories));
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.and(touristSpot.title.likeIgnoreCase("%" + keyword.trim() + "%"));
        }

        return queryFactory.selectFrom(touristSpot)
                .where(conditions)
                .orderBy(touristSpot.contentId.desc())
                .limit(size + 1L)
                .fetch();
    }

    public Optional<TouristSpot> findTouristSpotDetail(Long contentId) {
        return queryFactory.selectFrom(touristSpot)
                .leftJoin(touristSpot.images, spotImage).fetchJoin()
                .where(touristSpot.contentId.eq(contentId))
                .orderBy(spotImage.order.asc())
                .distinct()
                .fetch()
                .stream()
                .findFirst();
    }

    public List<KakaoSpot> findPopularSpots(
            Double cursorScore, Long cursorId, int size, List<String> categoryGroupCodes) {
        BooleanBuilder conditions = new BooleanBuilder();
        if (categoryGroupCodes != null && !categoryGroupCodes.isEmpty()) {
            conditions.and(kakaoSpot.categoryGroupCode.in(categoryGroupCodes));
        }
        if (cursorScore != null && cursorId != null) {
            conditions.and(
                    kakaoSpot.popularityScore.lt(cursorScore)
                            .or(kakaoSpot.popularityScore.eq(cursorScore).and(kakaoSpot.id.lt(cursorId)))
            );
        }

        return queryFactory.selectFrom(kakaoSpot)
                .where(conditions)
                .orderBy(kakaoSpot.popularityScore.desc(), kakaoSpot.id.desc())
                .limit(size + 1L)
                .fetch();
    }
}
