package com.sodosiro.domain.travel.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.controller.dto.TravelSpotSort;
import com.sodosiro.domain.travel.entity.QSpotEmbedding;
import com.sodosiro.domain.travel.entity.QSpotImage;
import com.sodosiro.domain.travel.entity.QSpotPopularity;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import com.sodosiro.domain.travel.entity.SpotPopularity;
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
    private static final QSpotPopularity spotPopularity = QSpotPopularity.spotPopularity;
    private static final QSpotEmbedding spotEmbedding = QSpotEmbedding.spotEmbedding;

    private final JPAQueryFactory queryFactory;

    public List<TouristSpotWithPopularity> findTouristSpots(
            TravelSpotSort sort,
            Long contentIdCursor,
            Double popularityScoreCursor,
            int size,
            List<Integer> categories,
            String keyword,
            String sigunguCode ) {
        BooleanBuilder conditions = new BooleanBuilder();
        if (categories != null && !categories.isEmpty()) {
            conditions.and(touristSpot.category.in(categories));
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.and(touristSpot.title.likeIgnoreCase("%" + keyword.trim() + "%"));
        }
        if (sigunguCode != null && !sigunguCode.isBlank()) {
            conditions.and(touristSpot.ldongSignguCode.eq(sigunguCode));
        }

        if (sort == TravelSpotSort.POPULAR) {
            conditions.and(spotPopularity.rankTag.isNotNull());
            if (popularityScoreCursor != null && contentIdCursor != null) {
                conditions.and(spotPopularity.popularityScore.lt(popularityScoreCursor)
                        .or(spotPopularity.popularityScore.eq(popularityScoreCursor)
                                .and(touristSpot.contentId.lt(contentIdCursor))));
            }
            return queryFactory.select(touristSpot, spotPopularity, spotEmbedding.keywordText)
                    .from(touristSpot)
                    .join(spotPopularity).on(spotPopularity.contentId.eq(touristSpot.contentId))
                    .leftJoin(spotEmbedding).on(spotEmbedding.contentId.eq(touristSpot.contentId))
                    .where(conditions)
                    .orderBy(spotPopularity.popularityScore.desc(), touristSpot.contentId.desc())
                    .limit(size + 1L)
                    .fetch()
                    .stream()
                    .map(tuple -> new TouristSpotWithPopularity(
                            tuple.get(touristSpot), tuple.get(spotPopularity), tuple.get(spotEmbedding.keywordText)))
                    .toList();
        }

        if (contentIdCursor != null) {
            conditions.and(touristSpot.contentId.lt(contentIdCursor));
        }
        return queryFactory.select(touristSpot, spotPopularity, spotEmbedding.keywordText)
                .from(touristSpot)
                .leftJoin(spotPopularity).on(spotPopularity.contentId.eq(touristSpot.contentId))
                .leftJoin(spotEmbedding).on(spotEmbedding.contentId.eq(touristSpot.contentId))
                .where(conditions)
                .orderBy(touristSpot.contentId.desc())
                .limit(size + 1L)
                .fetch()
                .stream()
                .map(tuple -> new TouristSpotWithPopularity(
                        tuple.get(touristSpot), tuple.get(spotPopularity), tuple.get(spotEmbedding.keywordText)))
                .toList();
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

    public record TouristSpotWithPopularity(TouristSpot spot, SpotPopularity popularity, String keywordText) {
    }
}
