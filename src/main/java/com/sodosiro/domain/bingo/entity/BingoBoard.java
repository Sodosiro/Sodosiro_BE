package com.sodosiro.domain.bingo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 시즌 x 지역(sigunguId)별 9칸 빙고판. 달성 여부는 저장하지 않고 조회 시점에 GPS 인증 기록으로 계산한다. */
@Entity
@Getter
@Table(
        name = "bingo_board",
        uniqueConstraints = @UniqueConstraint(name = "uk_bingo_board_season_sigungu", columnNames = {"season_id", "sigungu_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("시즌 x 지역별 빙고판 (모든 사용자 공용)")
public class BingoBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "sigungu_id", nullable = false)
    @Comment("region_intro / sigungu_code 참조")
    private Long sigunguId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cells", columnDefinition = "jsonb", nullable = false)
    @Comment("9칸 관광지 스냅샷 (position 1~9)")
    private List<BingoCellSnapshot> cells;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static BingoBoard create(Long seasonId, Long sigunguId, List<BingoCellSnapshot> cells) {
        BingoBoard board = new BingoBoard();
        board.seasonId = seasonId;
        board.sigunguId = sigunguId;
        board.cells = cells;
        return board;
    }

    /** position: 1~9 (1행 1-3, 2행 4-6, 3행 7-9) */
    public record BingoCellSnapshot(
            int position,
            Long contentId,
            String title,
            String firstImage,
            Integer category) {
    }
}
