package com.sodosiro.domain.bingo.service;

import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.entity.BingoBoard;
import com.sodosiro.domain.bingo.entity.BingoSeason;
import com.sodosiro.domain.bingo.repository.BingoBoardRepository;
import com.sodosiro.domain.region.entity.RegionIntro;
import com.sodosiro.domain.region.repository.RegionIntroRepository;
import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import com.sodosiro.domain.travel.entity.SigunguCode;
import com.sodosiro.domain.travel.entity.SpotPopularity;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.SpotPopularityRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시즌마다 지역(RegionIntro) 18건 전체를 순회해 9칸 빙고판을 생성한다.
 * 지역 안에서 계절 임베딩 유사도 상위 후보를 뽑은 뒤, 인기 관광지 4개 + 롱테일(비인기) 관광지 5개로 섞는다.
 * 소도시 추천 서비스 취지상 인기 관광지만으로 채우지 않고, 카테고리 다양성(카테고리당 최대 3개)도 지킨다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BingoBoardGenerationService {

    private static final int CANDIDATE_POOL_LIMIT = 60;
    private static final int POPULAR_COUNT = 4;
    private static final int LONGTAIL_COUNT = 5;
    private static final int BOARD_SIZE = POPULAR_COUNT + LONGTAIL_COUNT;
    private static final int MAX_PER_CATEGORY = 3;

    private final RegionIntroRepository regionIntroRepository;
    private final SigunguCodeRepository sigunguCodeRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final SpotPopularityRepository spotPopularityRepository;
    private final BingoBoardRepository bingoBoardRepository;
    private final EmbeddingModel embeddingModel;

    @Transactional
    public void generateAllRegions(BingoSeason season) {
        List<RegionIntro> regions = regionIntroRepository.findAll();
        long generated = regions.stream().filter(region -> generateBoard(season, region)).count();
        log.info("빙고판 생성 완료: {}건 / 전체 {}개 지역 (시즌 {} {})", generated, regions.size(), season.getYear(), season.getSeasonType());
    }

    private boolean generateBoard(BingoSeason season, RegionIntro region) {
        Long sigunguId = region.getSigunguId();
        SigunguCode sigungu = sigunguCodeRepository.findById(sigunguId).orElse(null);
        if (sigungu == null) {
            log.warn("빙고판 생성 스킵: sigunguId={} 시군구코드를 찾을 수 없음", sigunguId);
            return false;
        }

        List<TouristSpot> candidates = fetchCandidates(sigungu.getSigunguCode(), embedSafely(buildSeasonQueryText(region, season.getSeasonType())));
        if (candidates.size() < BOARD_SIZE) {
            log.warn("빙고판 생성 스킵: sigunguId={} 후보 부족({}개)", sigunguId, candidates.size());
            return false;
        }

        Map<Long, Double> popularityById = spotPopularityRepository
                .findAllById(candidates.stream().map(TouristSpot::getContentId).toList()).stream()
                .collect(Collectors.toMap(SpotPopularity::getContentId, SpotPopularity::getPopularityScore));

        List<TouristSpot> popularSorted = candidates.stream()
                .filter(spot -> popularityById.containsKey(spot.getContentId()))
                .sorted(Comparator.comparingDouble((TouristSpot spot) -> popularityById.get(spot.getContentId())).reversed())
                .toList();

        Map<Integer, Integer> categoryCounts = new HashMap<>();
        Set<Long> picked = new LinkedHashSet<>();
        List<TouristSpot> board = new ArrayList<>(BOARD_SIZE);

        pickWithCategoryCap(popularSorted, POPULAR_COUNT, picked, categoryCounts, board);

        List<TouristSpot> longtailPreferred = candidates.stream()
                .filter(spot -> !picked.contains(spot.getContentId()) && !popularityById.containsKey(spot.getContentId()))
                .toList();
        pickWithCategoryCap(longtailPreferred, LONGTAIL_COUNT, picked, categoryCounts, board);

        if (board.size() < BOARD_SIZE) {
            List<TouristSpot> remainder = candidates.stream()
                    .filter(spot -> !picked.contains(spot.getContentId()))
                    .toList();
            pickWithCategoryCap(remainder, BOARD_SIZE - board.size(), picked, categoryCounts, board);
        }

        if (board.size() < BOARD_SIZE) {
            log.warn("빙고판 생성 스킵: sigunguId={} 카테고리 다양성 제약으로 9개 미달({}개)", sigunguId, board.size());
            return false;
        }

        Collections.shuffle(board);
        List<BingoBoard.BingoCellSnapshot> cells = IntStream.range(0, BOARD_SIZE)
                .mapToObj(i -> {
                    TouristSpot spot = board.get(i);
                    return new BingoBoard.BingoCellSnapshot(
                            i + 1, spot.getContentId(), spot.getTitle(), spot.getFirstImage(), spot.getCategory());
                })
                .toList();

        bingoBoardRepository.save(BingoBoard.create(season.getId(), sigunguId, cells));
        return true;
    }

    /** source를 순서대로 훑으며 이미 뽑히지 않았고 카테고리 상한(MAX_PER_CATEGORY)을 넘지 않는 것만 needed개까지 담는다. */
    private void pickWithCategoryCap(
            List<TouristSpot> source, int needed, Set<Long> picked,
            Map<Integer, Integer> categoryCounts, List<TouristSpot> board) {
        int takenNow = 0;
        for (TouristSpot spot : source) {
            if (takenNow >= needed) {
                break;
            }
            if (picked.contains(spot.getContentId())) {
                continue;
            }
            int count = categoryCounts.getOrDefault(spot.getCategory(), 0);
            if (count >= MAX_PER_CATEGORY) {
                continue;
            }
            picked.add(spot.getContentId());
            categoryCounts.put(spot.getCategory(), count + 1);
            board.add(spot);
            takenNow++;
        }
    }

    private List<TouristSpot> fetchCandidates(String sigunguCode, float[] queryEmbedding) {
        List<Long> contentIds = queryEmbedding != null
                ? spotEmbeddingRepository.findNearestContentIds(queryEmbedding, List.of(), sigunguCode, CANDIDATE_POOL_LIMIT)
                : touristSpotRepository.findByLdongSignguCodeOrderByAvgRatingDesc(sigunguCode, PageRequest.of(0, CANDIDATE_POOL_LIMIT))
                        .stream().map(TouristSpot::getContentId).toList();

        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        return contentIds.stream().map(spotsById::get).filter(Objects::nonNull).toList();
    }

    private String buildSeasonQueryText(RegionIntro region, SeasonType seasonType) {
        return region.getDisplayName() + " " + seasonType.koreanLabel() + " " + extractSeasonHint(region.getBestSeason(), seasonType);
    }

    private String extractSeasonHint(Map<String, Object> bestSeason, SeasonType seasonType) {
        if (bestSeason == null) {
            return "";
        }
        Object value = bestSeason.getOrDefault(seasonType.name(), bestSeason.get(seasonType.name().toLowerCase()));
        return value == null ? "" : String.valueOf(value);
    }

    /** AI 임베딩 호출은 외부 API 의존이라 실패할 수 있는데, 실패해도 인기순 폴백으로 보드 생성은 계속되어야 한다. */
    private float[] embedSafely(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return embeddingModel.embed(text);
        } catch (RuntimeException exception) {
            log.warn("빙고판 계절 임베딩 실패: text={}", text, exception);
            return null;
        }
    }
}
