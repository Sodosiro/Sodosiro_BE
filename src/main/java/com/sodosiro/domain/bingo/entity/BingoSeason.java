package com.sodosiro.domain.bingo.entity;

import com.sodosiro.domain.bingo.constants.BingoSeasonStatus;
import com.sodosiro.domain.bingo.constants.SeasonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** 3개월 주기 빙고 시즌. 사용자당이 아니라 전체 공용으로 하나만 ACTIVE 상태를 가진다. */
@Entity
@Getter
@Table(
        name = "bingo_season",
        uniqueConstraints = @UniqueConstraint(name = "uk_bingo_season_year_type", columnNames = {"year", "season_type"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("빙고 시즌 (계절 3개월 주기)")
public class BingoSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "year", nullable = false)
    @Comment("시즌 연도 (WINTER는 12월 기준 연도)")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", length = 10, nullable = false)
    private SeasonType seasonType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private BingoSeasonStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static BingoSeason createActive(int year, SeasonType seasonType, LocalDate startDate, LocalDate endDate) {
        BingoSeason season = new BingoSeason();
        season.year = year;
        season.seasonType = seasonType;
        season.startDate = startDate;
        season.endDate = endDate;
        season.status = BingoSeasonStatus.ACTIVE;
        return season;
    }

    public void end() {
        this.status = BingoSeasonStatus.ENDED;
    }

    /** verifiedAt이 이 시즌의 완료로 인정되는지: 시즌 기간(startDate~endDate, 당일 포함) 안에 인증했을 때만 인정한다. */
    public boolean coversVerification(LocalDateTime verifiedAt) {
        LocalDate date = verifiedAt.toLocalDate();
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
