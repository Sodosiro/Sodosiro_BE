package com.sodosiro.domain.bingo.service;

import com.sodosiro.domain.bingo.constants.BingoSeasonStatus;
import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.controller.dto.BingoBoardResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoCellCheckResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoSeasonResponse;
import com.sodosiro.domain.bingo.entity.BingoBoard;
import com.sodosiro.domain.bingo.entity.BingoSeason;
import com.sodosiro.domain.bingo.repository.BingoBoardRepository;
import com.sodosiro.domain.bingo.repository.BingoSeasonRepository;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import com.sodosiro.domain.travel.entity.SigunguCode;
import com.sodosiro.global.payload.code.error.BingoErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 빙고판 조회 + 사용자별 달성 상태 계산. 달성 상태는 저장하지 않고 매번 GPS 인증 기록으로 계산한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BingoQueryService {

    private final BingoSeasonRepository bingoSeasonRepository;
    private final BingoBoardRepository bingoBoardRepository;
    private final SigunguCodeRepository sigunguCodeRepository;
    private final GpsRepository gpsRepository;

    /** 지금까지 쌓인 빙고 시즌 전체를 최신순으로 나열한다 (예: 2026 봄, 2025 겨울, 2025 가을 ...). */
    public List<BingoSeasonResponse> listSeasons() {
        return bingoSeasonRepository.findAllByOrderByStartDateDesc().stream()
                .map(BingoSeasonResponse::from)
                .toList();
    }

    /** year+seasonType을 둘 다 생략하면 활성 시즌, 둘 다 주면 그 시즌(과거 포함)의 지역 보드를 조회한다. */
    public BingoBoardResponse getBoardStatus(Long userId, Long sigunguId, Integer year, SeasonType seasonType) {
        BingoSeason season = resolveSeason(year, seasonType);
        BingoBoard board = findBoard(season.getId(), sigunguId);
        return buildResponse(season, board, userId);
    }

    private BingoSeason resolveSeason(Integer year, SeasonType seasonType) {
        if (year == null && seasonType == null) {
            return findActiveSeason();
        }
        if (year == null || seasonType == null) {
            throw new GeneralException(BingoErrorCode._INVALID_SEASON_QUERY);
        }
        return bingoSeasonRepository.findByYearAndSeasonType(year, seasonType)
                .orElseThrow(() -> new GeneralException(BingoErrorCode._SEASON_NOT_FOUND));
    }

    /**
     * GPS 인증 직후 호출한다. 인증한 관광지가 그 지역의 활성 시즌 빙고판 칸이 아니거나 활성 시즌/보드가 없으면
     * null을 반환한다 (GPS 인증 자체는 막지 않는다). 9칸 전체가 아니라 이번에 채워진 칸의 결과만 돌려준다.
     */
    public BingoCellCheckResponse getActiveCellCheckOrNull(Long userId, Long contentId, String ldongSignguCode) {
        SigunguCode sigungu = sigunguCodeRepository.findFirstBySigunguCode(ldongSignguCode).orElse(null);
        if (sigungu == null) {
            return null;
        }
        BingoSeason activeSeason = bingoSeasonRepository.findFirstByStatusOrderByIdDesc(BingoSeasonStatus.ACTIVE).orElse(null);
        if (activeSeason == null) {
            return null;
        }
        BingoBoard board = bingoBoardRepository.findBySeasonIdAndSigunguId(activeSeason.getId(), sigungu.getId()).orElse(null);
        if (board == null) {
            return null;
        }
        BingoBoard.BingoCellSnapshot cell = board.getCells().stream()
                .filter(c -> c.contentId().equals(contentId))
                .findFirst().orElse(null);
        if (cell == null) {
            return null;
        }

        Set<Integer> completedPositions = completedPositions(board, userId);
        int completedLineCount = BingoLineCalculator.countCompletedLines(completedPositions);

        return new BingoCellCheckResponse(board.getId(), cell.position(), completedLineCount, completedLineCount > 0);
    }

    private BingoSeason findActiveSeason() {
        return bingoSeasonRepository.findFirstByStatusOrderByIdDesc(BingoSeasonStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(BingoErrorCode._NO_ACTIVE_SEASON));
    }

    private BingoBoard findBoard(Long seasonId, Long sigunguId) {
        return bingoBoardRepository.findBySeasonIdAndSigunguId(seasonId, sigunguId)
                .orElseThrow(() -> new GeneralException(BingoErrorCode._BOARD_NOT_FOUND));
    }

    private BingoBoardResponse buildResponse(BingoSeason season, BingoBoard board, Long userId) {
        Map<Long, LocalDateTime> verifiedAtByContentId = verifiedAtByContentId(board, userId);

        List<BingoBoardResponse.Cell> cells = board.getCells().stream()
                .map(cell -> new BingoBoardResponse.Cell(
                        cell.position(), cell.contentId(), cell.title(), cell.firstImage(), cell.category(),
                        verifiedAtByContentId.containsKey(cell.contentId()), verifiedAtByContentId.get(cell.contentId())))
                .toList();

        Set<Integer> completedPositions = cells.stream()
                .filter(BingoBoardResponse.Cell::completed)
                .map(BingoBoardResponse.Cell::position)
                .collect(Collectors.toSet());
        int completedLineCount = BingoLineCalculator.countCompletedLines(completedPositions);

        return new BingoBoardResponse(
                board.getId(), season.getId(), season.getYear(), season.getSeasonType(), board.getSigunguId(),
                cells, completedLineCount, completedLineCount > 0);
    }

    private Map<Long, LocalDateTime> verifiedAtByContentId(BingoBoard board, Long userId) {
        List<Long> contentIds = board.getCells().stream().map(BingoBoard.BingoCellSnapshot::contentId).toList();
        return gpsRepository.findByUserIdAndContentIdIn(userId, contentIds).stream()
                .collect(Collectors.toMap(Gps::getContentId, Gps::getVerifiedAt, (first, duplicate) -> first));
    }

    private Set<Integer> completedPositions(BingoBoard board, Long userId) {
        Map<Long, LocalDateTime> verifiedAtByContentId = verifiedAtByContentId(board, userId);
        return board.getCells().stream()
                .filter(cell -> verifiedAtByContentId.containsKey(cell.contentId()))
                .map(BingoBoard.BingoCellSnapshot::position)
                .collect(Collectors.toSet());
    }
}
