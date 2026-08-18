package com.sodosiro.domain.festival.controller.dto;

import com.sodosiro.domain.festival.entity.Festival;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record FestivalDetailResponse(
        Long festivalId,
        String title,
        String regionName,
        @Schema(description = "해석된 광역시도 코드 (미매칭 시 null)", example = "1")
        String areaCode,
        LocalDate startDate,
        LocalDate endDate,
        @Schema(description = "축제 설명 (전문)")
        String description,
        @Schema(description = "축제 태그 목록 (콤마 구분 저장값을 배열로 분리)", example = "[\"여행지\", \"테스트\", \"aa\"]")
        List<String> tags,
        String imageUrl,
        String linkUrl,
        @Schema(description = "KST 기준 오늘 날짜로 판정한 진행 상태", example = "ONGOING")
        FestivalStatus status
) {
    public static FestivalDetailResponse from(Festival festival, LocalDate today) {
        return new FestivalDetailResponse(
                festival.getFestivalId(), festival.getTitle(), festival.getRegionName(),
                festival.getAreaCode(), festival.getStartDate(), festival.getEndDate(),
                festival.getDescription(), festival.getTagList(),
                festival.getImageUrl(), festival.getLinkUrl(),
                FestivalStatus.resolve(festival.getStartDate(), festival.getEndDate(), today)
        );
    }
}
