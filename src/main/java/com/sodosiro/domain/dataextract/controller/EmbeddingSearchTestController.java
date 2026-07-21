package com.sodosiro.domain.dataextract.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * [임시] 벡터 검색 유사도 수동 테스트용 컨트롤러.
 *
 * <p>검증이 끝나면 이 파일 하나만 삭제하면 된다 — 다른 클래스와 의존 관계 없음.
 *
 * <pre>
 * GET /internal/test/travel/search?query=아이랑 걷기 좋은 바닷가&categories=자연,관광지&limit=20
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/test/travel")
public class EmbeddingSearchTestController {

    private static final Set<String> CATEGORIES = Set.of(
            "식당", "카페", "쇼핑", "관광지", "자연", "액티비티"
    );

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public record SearchResult(
            Long contentId,
            String title,
            String category,
            double similarity,
            String keywordText,
            String inputText
    ) { }

    public record SearchResponse(String embeddedQuery, int count, List<SearchResult> results) { }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(defaultValue = "20") int limit
    ) {
        // "자연, 식당"처럼 쉼표 뒤 공백이 있어도 허용한다.
        List<String> selected = categories == null
                ? List.<String>of()
                : categories.stream().map(String::strip).filter(c -> !c.isBlank()).toList();
        if (selected.size() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리는 최대 2개까지 선택할 수 있습니다.");
        }
        for (String category : selected) {
            if (!CATEGORIES.contains(category)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "알 수 없는 카테고리: " + category + " (허용: " + String.join(", ", CATEGORIES) + ")");
            }
        }
        boolean hasQuery = query != null && !query.isBlank();
        if (!hasQuery && selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query 또는 categories 중 하나는 필요합니다.");
        }

        // 색인문이 "장소명 (분류) + 설명 + 키워드" 형식이므로 질의에도 카테고리를 붙여 대칭을 맞춘다.
        // 질의 없이 카테고리만 선택한 경우 카테고리 텍스트 자체를 임베딩한다.
        String embeddedQuery;
        if (hasQuery && !selected.isEmpty()) {
            embeddedQuery = String.join(", ", selected) + ". " + query;
        } else if (hasQuery) {
            embeddedQuery = query;
        } else {
            embeddedQuery = String.join(", ", selected);
        }
        String vectorLiteral = toVectorLiteral(embeddingModel.embed(embeddedQuery));

        StringBuilder sql = new StringBuilder("""
                SELECT e.content_id,
                       s.title,
                       split_part(e.keyword_text, ',', 1) AS category,
                       1 - (e.embedding <=> ?::vector) AS similarity,
                       e.keyword_text,
                       e.input_text
                FROM spot_embedding e
                JOIN tourist_spot s ON s.content_id = e.content_id
                """);
        List<Object> params = new ArrayList<>();
        params.add(vectorLiteral);
        if (!selected.isEmpty()) {
            sql.append(" WHERE split_part(e.keyword_text, ',', 1) = ANY(string_to_array(?, ','))\n");
            params.add(String.join(",", selected));
        }
        sql.append(" ORDER BY e.embedding <=> ?::vector LIMIT ?");
        params.add(vectorLiteral);
        params.add(limit);

        List<SearchResult> results = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new SearchResult(
                rs.getLong("content_id"),
                rs.getString("title"),
                rs.getString("category"),
                Math.round(rs.getDouble("similarity") * 10000) / 10000.0,
                rs.getString("keyword_text"),
                rs.getString("input_text")
        ), params.toArray());

        return new SearchResponse(embeddedQuery, results.size(), results);
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.append(']').toString();
    }
}
