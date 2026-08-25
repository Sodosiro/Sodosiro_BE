package com.sodosiro.domain.bingo.service;

import com.sodosiro.domain.bingo.constants.BingoSeasonStatus;
import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.entity.BingoSeason;
import com.sodosiro.domain.bingo.repository.BingoSeasonRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계절이 바뀌는 3/1, 6/1, 9/1, 12/1 자정에 이전 시즌을 종료하고 새 시즌 + 18개 지역 빙고판을 생성한다.
 * 같은 시즌 키(year, seasonType)로 이미 생성된 적 있으면 건너뛴다 (재배포로 중복 실행돼도 안전) — 그 덕분에
 * 앱 기동 시에도 같은 로직을 한 번 더 실행해, 최초 배포 시점이나 크론 실행 시각에 서버가 내려가 있었던 경우를
 * 다음 재시작 때 스스로 채워 넣는다(self-healing).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BingoSeasonScheduler {

    private final BingoSeasonRepository bingoSeasonRepository;
    private final BingoBoardGenerationService bingoBoardGenerationService;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 1 3,6,9,12 *")
    @Transactional
    public void rolloverSeason() {
        LocalDate today = LocalDate.now();
        SeasonType seasonType = SeasonType.of(today);
        int seasonYear = SeasonType.seasonYearOf(today);

        if (bingoSeasonRepository.existsByYearAndSeasonType(seasonYear, seasonType)) {
            log.info("빙고 시즌 이미 존재해 스킵: year={}, seasonType={}", seasonYear, seasonType);
            return;
        }

        bingoSeasonRepository.findByStatus(BingoSeasonStatus.ACTIVE).forEach(BingoSeason::end);

        BingoSeason newSeason = bingoSeasonRepository.save(BingoSeason.createActive(
                seasonYear, seasonType, seasonType.startDateOf(seasonYear), seasonType.endDateOf(seasonYear)));

        bingoBoardGenerationService.generateAllRegions(newSeason);
        log.info("빙고 시즌 롤오버 완료: year={}, seasonType={}", seasonYear, seasonType);
    }
}
