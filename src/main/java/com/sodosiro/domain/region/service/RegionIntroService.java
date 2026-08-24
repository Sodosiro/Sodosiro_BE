package com.sodosiro.domain.region.service;

import com.sodosiro.domain.region.controller.dto.RegionIntroductionResponse;
import com.sodosiro.domain.region.entity.RegionIntro;
import com.sodosiro.domain.region.repository.RegionIntroRepository;
import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import com.sodosiro.domain.region.repository.RegionImageQueryRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.entity.SigunguCode;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionIntroService {

    private final RegionIntroRepository regionIntroRepository;
    private final SigunguCodeRepository sigunguCodeRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final RegionImageQueryRepository regionImageQueryRepository;

    public RegionIntroductionResponse getIntroduction(Long sigunguId) {
        RegionIntro region = findRegionIntro(sigunguId);
        SigunguCode sigungu = findSigunguCode(sigunguId);
        List<RegionIntroductionResponse.FeaturedSpot> featuredSpots = findFeaturedSpots(region);
        List<RegionIntroductionResponse.RegionImage> images = findRegionImages(sigunguId);

        return toResponse(region, sigungu, images, featuredSpots);
    }

    private RegionIntro findRegionIntro(Long sigunguId) {
        return regionIntroRepository.findById(sigunguId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지역 소개를 찾을 수 없습니다."));
    }

    private SigunguCode findSigunguCode(Long sigunguId) {
        return sigunguCodeRepository.findById(sigunguId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시군구 코드를 찾을 수 없습니다."));
    }

    private List<RegionIntroductionResponse.FeaturedSpot> findFeaturedSpots(RegionIntro region) {
        List<Long> contentIds = region.getFeaturedContentIds() == null
                ? List.of() : region.getFeaturedContentIds();
        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        return contentIds.stream()
                .map(spotsById::get)
                .filter(java.util.Objects::nonNull)
                .map(spot -> new RegionIntroductionResponse.FeaturedSpot(
                        spot.getContentId(), spot.getTitle(), spot.getAddr1(), spot.getOverview(), spot.getCategory()
                        ,spot.getFirstImage()))
                .toList();
    }

    private List<RegionIntroductionResponse.RegionImage> findRegionImages(Long sigunguId) {
        return regionImageQueryRepository
                .findRepresentativeImages(sigunguId).stream()
                .map(image -> new RegionIntroductionResponse.RegionImage(
                        image.contentId(), image.title(), image.imageUrl()))
                .toList();
    }

    private RegionIntroductionResponse toResponse(
            RegionIntro region,
            SigunguCode sigungu,
            List<RegionIntroductionResponse.RegionImage> images,
            List<RegionIntroductionResponse.FeaturedSpot> featuredSpots) {
        return new RegionIntroductionResponse(
                region.getSigunguId(), sigungu.getAreaCode(), sigungu.getSigunguCode(), region.getDisplayName(),
                region.getIntro(),
                nullToEmpty(region.getThemeTags()), nullToEmpty(region.getRecommendationReasons()),
                region.getBestSeason() == null ? Map.of() : region.getBestSeason(),
                nullToEmpty(region.getFoodTags()), images, featuredSpots);
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
