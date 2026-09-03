package com.sodosiro.domain.bingo.service;

import com.sodosiro.domain.badge.service.BadgeService;
import com.sodosiro.domain.bingo.constants.BingoSeasonStatus;
import com.sodosiro.domain.bingo.controller.dto.BingoCellCheckResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsRequest;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsVerifyResponse;
import com.sodosiro.domain.bingo.entity.BingoSeason;
import com.sodosiro.domain.bingo.repository.BingoSeasonRepository;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.GpsErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 빙고판 전용 GPS 방문 인증. 코스/일정과 무관하게, 위치 인증은 프론트에서 완료 후 호출하므로 서버는 별도 검증 없이 인증 레코드를 만든다. */
@Service
@RequiredArgsConstructor
@Transactional
public class BingoGpsService {

    private final GpsRepository gpsRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final BingoQueryService bingoQueryService;
    private final BadgeService badgeService;
    private final BingoSeasonRepository bingoSeasonRepository;

    public BingoGpsVerifyResponse verify(Long userId, BingoGpsRequest request) {

        TouristSpot spot = touristSpotRepository.findById(request.contentId())
                .orElseThrow(() -> new GeneralException(GpsErrorCode._SPOT_NOT_FOUND));

        Gps gps = findWithinActiveSeasonOrCreate(request, userId, spot);

        BingoCellCheckResponse bingoCheck = bingoQueryService.getActiveCellCheckOrNull(userId, spot.getContentId(), spot.getLdongSignguCode());
        return BingoGpsVerifyResponse.from(gps, bingoCheck);
    }

    /**
     * 코스 인증에서 만들어진 기록도 여기서 그대로 재사용된다 (userId+contentId 기준, courseId 상관없이 동일 테이블 공유).
     * 다만 시즌이 3개월마다 초기화되므로, 지난 시즌에만 인증한 기록은 재사용하지 않고 새로 인증하게 한다 —
     * 그래야 시즌이 바뀌면 다시 방문해야 빙고 칸이 채워진다. 활성 시즌이 없으면(운영 공백) 예전처럼 아무 기록이나 재사용한다.
     */
    private Gps findWithinActiveSeasonOrCreate(BingoGpsRequest request, Long userId, TouristSpot spot) {
        List<Gps> existing = gpsRepository.findByUserIdAndContentId(userId, request.contentId());
        if (existing.isEmpty()) {
            return createVerification(request, userId, spot);
        }

        Optional<BingoSeason> activeSeason = bingoSeasonRepository.findFirstByStatusOrderByIdDesc(BingoSeasonStatus.ACTIVE);
        if (activeSeason.isEmpty()) {
            return existing.get(0);
        }

        return existing.stream()
                .filter(gps -> activeSeason.get().coversVerification(gps.getVerifiedAt()))
                .findFirst()
                .orElseGet(() -> createVerification(request, userId, spot));
    }

    private Gps createVerification(BingoGpsRequest request, Long userId, TouristSpot spot) {
        Gps gps = gpsRepository.save(Gps.createForBingo(userId, request.contentId()));
        badgeService.awardIfFirstVisit(userId, spot.getLdongSignguCode());
        return gps;
    }
}
