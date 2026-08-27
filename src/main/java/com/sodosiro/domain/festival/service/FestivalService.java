package com.sodosiro.domain.festival.service;

import com.sodosiro.domain.festival.controller.dto.FestivalDetailResponse;
import com.sodosiro.domain.festival.controller.dto.FestivalStatus;
import com.sodosiro.domain.festival.controller.dto.FestivalSummaryResponse;
import com.sodosiro.domain.festival.entity.Festival;
import com.sodosiro.domain.festival.repository.FestivalQueryRepository;
import com.sodosiro.domain.festival.repository.FestivalRepository;
import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.global.utils.TimeZones;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalService {

    // status 판정 및 "오늘" 기준은 항상 KST(TimeZones.KST)로 고정한다.
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final FestivalQueryRepository festivalQueryRepository;
    private final FestivalRepository festivalRepository;

    public FestivalDetailResponse getFestival(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 축제입니다."));
        return FestivalDetailResponse.from(festival, LocalDate.now(TimeZones.KST));
    }

    public CursorPageResponse<FestivalSummaryResponse> getFestivals(
            String areaCode, FestivalStatus status, Integer year, String cursor, Integer size) {
        int pageSize = normalizePageSize(size);
        FestivalStatus effectiveStatus = status == null ? FestivalStatus.ALL : status;
        LocalDate today = LocalDate.now(TimeZones.KST);
        FestivalCursor parsedCursor = parseCursor(cursor);

        List<Festival> rows = festivalQueryRepository.findFestivals(
                areaCode, effectiveStatus, year, today,
                parsedCursor.startDate(), parsedCursor.festivalId(), pageSize);
        boolean hasNext = rows.size() > pageSize;

        List<FestivalSummaryResponse> items = rows.stream()
                .limit(pageSize)
                .map(festival -> FestivalSummaryResponse.from(festival, today))
                .toList();
        String nextCursor = hasNext ? encodeCursor(rows.get(pageSize - 1)) : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~" + MAX_PAGE_SIZE + " 사이여야 합니다.");
        }
        return size;
    }

    private FestivalCursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new FestivalCursor(null, null);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] values = decoded.split(":", -1);
            if (values.length != 2) {
                throw new IllegalArgumentException();
            }
            return new FestivalCursor(LocalDate.ofEpochDay(Long.parseLong(values[0])), Long.parseLong(values[1]));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor입니다.");
        }
    }

    private String encodeCursor(Festival festival) {
        String value = festival.getStartDate().toEpochDay() + ":" + festival.getFestivalId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record FestivalCursor(LocalDate startDate, Long festivalId) {
    }
}
