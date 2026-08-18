package com.sodosiro.domain.festival.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 지역 축제. 원천 데이터셋의 고유 ID를 그대로 PK로 사용한다.
 * 지역명(region_name)은 원본 텍스트를 보존하고, 적재 시점에 광역시도 코드(area_code)로 해석해 함께 저장한다.
 */
@Entity
@Getter
@Table(
        name = "festival",
        indexes = {
                @Index(name = "idx_festival_area_start", columnList = "area_code, start_date"),
                @Index(name = "idx_festival_period", columnList = "start_date, end_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Festival {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "festival_id")
    @Comment("외부 데이터셋 고유 ID")
    private Long festivalId;

    @Column(name = "title", length = 200, nullable = false)
    @Comment("축제명")
    private String title;

    @Column(name = "region_name", length = 100, nullable = false)
    @Comment("지역명(원본 텍스트)")
    private String regionName;

    @Column(name = "area_code", length = 5)
    @Comment("해석된 광역시도 코드 (매칭 실패 시 null)")
    private String areaCode;

    @Column(name = "start_date", nullable = false)
    @Comment("시작일")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @Comment("끝일")
    private LocalDate endDate;

    @Column(name = "description", columnDefinition = "text")
    @Comment("축제 설명")
    private String description;

    @Column(name = "tag", length = 100)
    @Comment("축제 태그")
    private String tag;

    @Column(name = "image_url", columnDefinition = "text")
    @Comment("축제 이미지")
    private String imageUrl;

    @Column(name = "link_url", columnDefinition = "text")
    @Comment("축제 링크")
    private String linkUrl;

    @Column(name = "collected_at")
    @Comment("수집 시각")
    private LocalDateTime collectedAt;

    private Festival(Long festivalId, String title, String regionName, String areaCode,
            LocalDate startDate, LocalDate endDate, String description, String tag, String imageUrl,
            String linkUrl, LocalDateTime collectedAt) {
        this.festivalId = festivalId;
        this.title = title;
        this.regionName = regionName;
        this.areaCode = areaCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.tag = tag;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.collectedAt = collectedAt;
    }

    public static Festival create(Long festivalId, String title, String regionName, String areaCode,
            LocalDate startDate, LocalDate endDate, String description, String tag, String imageUrl,
            String linkUrl, LocalDateTime collectedAt) {
        return new Festival(festivalId, title, regionName, areaCode, startDate, endDate,
                description, tag, imageUrl, linkUrl, collectedAt);
    }

    public List<String> getTagList() {
        if (tag == null || tag.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tag.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
