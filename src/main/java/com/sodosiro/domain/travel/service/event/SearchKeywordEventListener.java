package com.sodosiro.domain.travel.service.event;

import com.sodosiro.domain.travel.service.SearchTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SearchKeywordEventListener {

    private final SearchTrendingService searchTrendingService;

    @Async("searchTrendingExecutor")
    @EventListener
    public void onSearch(SearchKeywordSearchedEvent event) {
        try {
            searchTrendingService.countKeyword(event.keyword(), event.userId());
        } catch (Exception e) {
            log.warn("[SearchTrending] 검색어 집계 실패: keyword={}", event.keyword(), e);
        }
    }
}
