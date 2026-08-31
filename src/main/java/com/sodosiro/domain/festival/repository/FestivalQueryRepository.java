package com.sodosiro.domain.festival.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.festival.controller.dto.FestivalStatus;
import com.sodosiro.domain.festival.entity.Festival;
import com.sodosiro.domain.festival.entity.QFestival;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalQueryRepository {

    private static final QFestival festival = QFestival.festival;

    private final JPAQueryFactory queryFactory;


    public List<Festival> findFestivals(
            String areaCode,
            FestivalStatus status,
            String regionName,
            Integer year,
            LocalDate today,
            LocalDate startDateCursor,
            Long festivalIdCursor,
            int size) {
        BooleanBuilder conditions = new BooleanBuilder();
        if (areaCode != null && !areaCode.isBlank()) {
            conditions.and(festival.areaCode.eq(areaCode));
        }

        if (regionName != null && !regionName.isBlank()) {
            conditions.and(festival.regionName.containsIgnoreCase(regionName.strip()));
        }
        applyYear(conditions, year);
        applyStatus(conditions, status, today);
        if (startDateCursor != null && festivalIdCursor != null) {
            conditions.and(festival.startDate.gt(startDateCursor)
                    .or(festival.startDate.eq(startDateCursor)
                            .and(festival.festivalId.gt(festivalIdCursor))));
        }
        return queryFactory.selectFrom(festival)
                .where(conditions)
                .orderBy(festival.startDate.asc(), festival.festivalId.asc())
                .limit(size + 1L)
                .fetch();
    }

    private void applyYear(BooleanBuilder conditions, Integer year) {
        if (year == null) {
            return;
        }
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        conditions.and(festival.startDate.loe(yearEnd).and(festival.endDate.goe(yearStart)));
    }

    private void applyStatus(BooleanBuilder conditions, FestivalStatus status, LocalDate today) {
        switch (status) {
            case ONGOING -> conditions.and(festival.startDate.loe(today)).and(festival.endDate.goe(today));
            case UPCOMING -> conditions.and(festival.startDate.gt(today));
            case ACTIVE -> conditions.and(festival.endDate.goe(today));
            case ENDED -> conditions.and(festival.endDate.lt(today));
            case ALL -> { /* 조건 없음 */ }
        }
    }
}
