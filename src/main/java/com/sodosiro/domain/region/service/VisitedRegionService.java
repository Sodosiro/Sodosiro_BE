package com.sodosiro.domain.region.service;

import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.region.controller.dto.VisitedRegionResponse;
import com.sodosiro.domain.region.repository.AreaCodeRepository;
import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import com.sodosiro.domain.travel.entity.AreaCode;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitedRegionService {

    /** 서비스가 현재 강원특별자치도만 다루므로 기본값으로 둔다. 추후에-> 확장 가능성떄문에 일부로 하드코딩 지역이 추가될때 제거 */
    private static final String DEFAULT_AREA_CODE = "51";

    private final GpsRepository gpsRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final SigunguCodeRepository sigunguCodeRepository;
    private final AreaCodeRepository areaCodeRepository;

    public VisitedRegionResponse getVisitedRegions(Long userId, String areaCode) {
        String effectiveAreaCode = (areaCode == null || areaCode.isBlank()) ? DEFAULT_AREA_CODE : areaCode;

        Set<Long> visitedContentIds = gpsRepository.findByUserId(userId).stream()
                .map(Gps::getContentId)
                .collect(Collectors.toSet());

        Map<String, Long> visitCountBySigunguCode = touristSpotRepository.findAllById(visitedContentIds).stream()
                .filter(spot -> effectiveAreaCode.equals(spot.getLdongRegnCode()))
                .filter(spot -> spot.getLdongSignguCode() != null)
                .collect(Collectors.groupingBy(TouristSpot::getLdongSignguCode, Collectors.counting()));

        String areaName = areaCodeRepository.findById(effectiveAreaCode)
                .map(AreaCode::getName)
                .orElse(null);

        List<VisitedRegionResponse.VisitedSigungu> visitedSigungus = sigunguCodeRepository
                .findAllByAreaCode(effectiveAreaCode).stream()
                .filter(sigungu -> visitCountBySigunguCode.containsKey(sigungu.getSigunguCode()))
                .map(sigungu -> new VisitedRegionResponse.VisitedSigungu(
                        sigungu.getId(),
                        sigungu.getSigunguCode(),
                        sigungu.getName(),
                        visitCountBySigunguCode.get(sigungu.getSigunguCode()).intValue()))
                .sorted(Comparator.comparing(VisitedRegionResponse.VisitedSigungu::name))
                .toList();

        return new VisitedRegionResponse(effectiveAreaCode, areaName, visitedSigungus);
    }
}
