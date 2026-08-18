package com.sodosiro.domain.festival.controller.dto;

import com.sodosiro.domain.festival.entity.Festival;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record FestivalSummaryResponse(
        Long festivalId,
        String title,
        String regionName,
        @Schema(description = "해석된 광역시도 코드 (미매칭 시 null)", example = "1")
        String areaCode,
        LocalDate startDate,
        LocalDate endDate,
        @Schema(description = "축제 설명. 15자 초과 시 15자까지만 노출하고 '...'을 붙임", example = "매년 가을 강원도에서...")
        String description,
        String imageUrl,
        String linkUrl,
        @Schema(description = "축제 태그 목록 (콤마 구분 저장값을 배열로 분리)", example = "[\"여행지\", \"테스트\", \"aa\"]")
        List<String> tags,
        @Schema(description = "KST 기준 오늘 날짜로 판정한 진행 상태", example = "ONGOING")
        FestivalStatus status
) {
    private static final int PREVIEW_LENGTH = 15;

    public static FestivalSummaryResponse from(Festival festival, LocalDate today) {
        return new FestivalSummaryResponse(
                festival.getFestivalId(), festival.getTitle(), festival.getRegionName(),
                festival.getAreaCode(), festival.getStartDate(), festival.getEndDate(),
                festival.getDescription(),
                festival.getImageUrl(), festival.getLinkUrl(),
                festival.getTagList(),
                FestivalStatus.resolve(festival.getStartDate(), festival.getEndDate(), today)
        );
    }

    @Deprecated
    private static String truncatePreview(String value) {
        if (value == null || value.length() <= PREVIEW_LENGTH) {
            return value;
        }
        return value.substring(0, PREVIEW_LENGTH) + "...";
    }
}
