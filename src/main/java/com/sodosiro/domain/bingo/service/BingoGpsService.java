package com.sodosiro.domain.bingo.service;

import com.sodosiro.domain.bingo.controller.dto.BingoCellCheckResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsRequest;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsVerifyResponse;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.gps.service.GpsVerifier;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.GpsErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 빙고판 전용 GPS 방문 인증. 코스/일정과 무관하게 관광지 좌표 기준 300m 이내이면 인증 레코드를 새로 만든다. */
@Service
@RequiredArgsConstructor
@Transactional
public class BingoGpsService {

    private final GpsRepository gpsRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final BingoQueryService bingoQueryService;

    public BingoGpsVerifyResponse verify(Long userId, BingoGpsRequest request) {

        TouristSpot spot = touristSpotRepository.findById(request.contentId())
                .orElseThrow(() -> new GeneralException(GpsErrorCode._SPOT_NOT_FOUND));

        // 코스 인증에서 만들어진 기록도 여기서 그대로 재사용된다 (userId+contentId 기준, courseId 상관없이 동일 테이블 공유).
        // 한 유저가 같은 스팟을 여러 코스에 걸쳐 인증하면 로우가 여러 개일 수 있으므로 리스트로 받는다.
        Gps gps = gpsRepository.findByUserIdAndContentId(userId, request.contentId()).stream()
                .findFirst()
                .orElseGet(() -> createVerification(request, userId, spot));

        BingoCellCheckResponse bingoCheck = bingoQueryService.getActiveCellCheckOrNull(userId, spot.getContentId(), spot.getLdongSignguCode());
        return BingoGpsVerifyResponse.from(gps, bingoCheck);
    }

    private Gps createVerification(BingoGpsRequest request, Long userId, TouristSpot spot) {
        if (spot.getMapY() == null || spot.getMapX() == null) {
            throw new GeneralException(GpsErrorCode._SPOT_LOCATION_UNAVAILABLE);
        }
        if (!GpsVerifier.isWithinVerificationRadius(
                spot.getMapY(), spot.getMapX(), request.latitude(), request.longitude())) {
            throw new GeneralException(GpsErrorCode._OUT_OF_VERIFICATION_RANGE);
        }
        return gpsRepository.save(Gps.createForBingo(userId, request.contentId()));
    }
}
