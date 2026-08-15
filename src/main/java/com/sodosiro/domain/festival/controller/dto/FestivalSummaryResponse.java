package com.sodosiro.domain.festival.controller.dto;

import com.sodosiro.domain.festival.entity.Festival;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record FestivalSummaryResponse(
        Long festivalId,
        String title,
        String regionName,
        @Schema(description = "해석된 광역시도 코드 (미매칭 시 null)", example = "1")
        String areaCode,
        LocalDate startDate,
        LocalDate endDate,
        String imageUrl,
        String linkUrl,
        @Schema(description = "KST 기준 오늘 날짜로 판정한 진행 상태", example = "ONGOING")
        FestivalStatus status
) {
    public static FestivalSummaryResponse from(Festival festival, LocalDate today) {
        return new FestivalSummaryResponse(
                festival.getFestivalId(), festival.getTitle(), festival.getRegionName(),
                festival.getAreaCode(), festival.getStartDate(), festival.getEndDate(),
                festival.getImageUrl(), festival.getLinkUrl(),
                FestivalStatus.resolve(festival.getStartDate(), festival.getEndDate(), today)
        );
    }
}
