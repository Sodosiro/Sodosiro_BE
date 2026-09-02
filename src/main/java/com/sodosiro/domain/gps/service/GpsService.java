package com.sodosiro.domain.gps.service;

import com.sodosiro.domain.badge.service.BadgeService;
import com.sodosiro.domain.bingo.controller.dto.BingoCellCheckResponse;
import com.sodosiro.domain.bingo.service.BingoQueryService;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.gps.controller.dto.request.GpsRequest;
import com.sodosiro.domain.gps.controller.dto.response.GpsResponse;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.GpsErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코스 스팟 GPS 방문 인증. 사용자 좌표가 스팟 반경 300m 이내인 경우에만 인증 레코드를 새로 만든다. */
@Service
@RequiredArgsConstructor
@Transactional
public class GpsService {

    private final GpsRepository gpsRepository;
    private final CourseRepository courseRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final BingoQueryService bingoQueryService;
    private final BadgeService badgeService;

    public GpsResponse verify(Long userId, GpsRequest request) {

        Course course = courseRepository.findByIdAndUserId(request.courseId(), userId)
                .orElseThrow(() -> new GeneralException(GpsErrorCode._COURSE_NOT_FOUND));

        validateSpotInItinerary(course, request.contentId(), request.day());

        TouristSpot spot = touristSpotRepository.findById(request.contentId())
                .orElseThrow(() -> new GeneralException(GpsErrorCode._SPOT_NOT_FOUND));

        // 같은 스팟을 여러 번 인증 시도해도 항상 같은 결과를 반환한다 (버튼 연타 대응).
        Gps gps = gpsRepository.findByCourseIdAndContentIdAndDay(request.courseId(), request.contentId(), request.day())
                .orElseGet(() -> createVerification(request, userId, spot));

        BingoCellCheckResponse bingoCheck = bingoQueryService.getActiveCellCheckOrNull(userId, spot.getContentId(), spot.getLdongSignguCode());
        return GpsResponse.from(gps, bingoCheck);
    }

    /**
     * 이 유저가 이 스팟을 (다른 코스에서든) 이미 한 번이라도 인증한 적 있으면 실제 방문이 증명된 것으로 보고
     * 거리 체크 없이 이 코스·날짜용 레코드만 새로 만든다. 처음 인증하는 경우에만 반경 300m를 확인한다.
     */
    private Gps createVerification(GpsRequest request, Long userId, TouristSpot spot) {
        boolean alreadyVerifiedElsewhere = !gpsRepository.findByUserIdAndContentId(userId, request.contentId()).isEmpty();
        if (!alreadyVerifiedElsewhere) {
            if (spot.getMapY() == null || spot.getMapX() == null) {
                throw new GeneralException(GpsErrorCode._SPOT_LOCATION_UNAVAILABLE);
            }
            if (!GpsVerifier.isWithinVerificationRadius(
                    spot.getMapY(), spot.getMapX(), request.latitude(), request.longitude())) {
                throw new GeneralException(GpsErrorCode._OUT_OF_VERIFICATION_RANGE);
            }
        }
        Gps gps = gpsRepository.save(Gps.create(request.courseId(), userId, request.contentId(), request.day()));
        badgeService.awardIfFirstVisit(userId, spot.getLdongSignguCode());
        return gps;
    }

    private void validateSpotInItinerary(Course course, Long contentId, Integer day) {

        boolean inItinerary = course.getDays().stream()
                .filter(daySnapshot -> daySnapshot.day() == day)
                .flatMap(daySnapshot -> daySnapshot.spots().stream())
                .anyMatch(spot -> spot.contentId().equals(contentId));

        if (!inItinerary) {
            throw new GeneralException(GpsErrorCode._COURSE_SPOT_NOT_FOUND);
        }
    }
}
