package com.sodosiro.domain.dataextract.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 관광지 소개문을 벡터 검색용 색인문(대표 분류 + 키워드 + 설명)으로 변환한다.
 *
 * <p>사용자 질의는 자연어 문장으로 들어오므로, 키워드 목록만 임베딩하면 유사도가 낮다.
 * 분류는 카테고리 필터(6종 중 최대 2개 선택)에, 키워드·설명은 임베딩 원문에 쓴다.
 */
@Service
public class TravelKeywordExtractor {

    private static final int MAX_KEYWORDS = 9;
    private static final Set<String> PRIMARY_CATEGORIES = Set.of(
            "식당", "카페", "쇼핑", "관광지", "자연", "액티비티"
    );
    private static final Pattern LABELED_LINE =
            Pattern.compile("^[*#\\-\\s]*(분류|키워드|설명)\\s*[::]\\s*(.+)$");

    private final ChatClient chatClient;

    public TravelKeywordExtractor(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public Extraction extract(String title, String overview) {
        String response = chatClient.prompt()
                .system("""
                        당신은 한국 여행지 벡터 검색 색인문 작성기입니다.
                        여행지명과 소개문을 읽고 반드시 아래 3줄 형식으로만 출력하세요.

                        분류: <6개 중 정확히 하나>
                        키워드: <쉼표로 구분한 5~9개>
                        설명: <2~3문장>

                        [분류 규칙]
                        반드시 다음 6개 중 하나만: 식당, 카페, 쇼핑, 관광지, 자연, 액티비티
                        - 식당: 식사·음식점·맛집이 주된 방문 목적
                        - 카페: 커피·음료·디저트·베이커리가 주된 방문 목적
                        - 쇼핑: 시장·상점·특산품 구매·쇼핑몰이 주된 방문 목적
                        - 관광지: 역사·문화·전시·건축·사찰·박물관 등 관람이 주된 방문 목적
                        - 자연: 산·바다·해변·호수·계곡·숲·공원 등 자연 감상이 주된 방문 목적
                        - 액티비티: 레저·스포츠·체험·놀이 등 신체 활동이 주된 방문 목적
                        여러 성격이 섞여 있으면 여행자가 그곳에 가는 가장 핵심적인 목적 하나로
                        판단하세요. (예: 해변 옆 베이커리는 카페, 사찰이 있는 산은 관람이
                        핵심이면 관광지, 등산이 핵심이면 자연)

                        [키워드 규칙]
                        여행자가 검색창에 입력할 법한 구체적인 한국어 표현 5~9개.
                        우선순위: ① 무엇을 보고/먹고/할 수 있는가 (대표 메뉴, 경관, 체험 이름)
                        ② 분위기·동반 형태 (가족여행, 아이와 함께, 커플, 반려동물, 혼자 여행)
                        ③ 계절·시간대 (벚꽃, 단풍, 일출, 야경, 피서)
                        ④ 지역 특산물·명물.
                        금지: 여행지명 반복, '좋은 곳'·'명소'·'추천' 같은 추상어, 의미가 겹치는 중복.

                        [설명 규칙]
                        벡터 검색 매칭용 요약 2~3문장. 여행지명을 한 번 포함하고,
                        "~을 즐길 수 있다", "~하기 좋다"처럼 여행자의 질문과 맞닿는 표현으로 쓰세요.
                        소개문에 있는 사실만 사용하고 과장·광고 문구·지어낸 내용은 금지합니다.
                        소개문이 없거나 짧으면 여행지명에서 확실히 알 수 있는 사실만으로 짧게 쓰세요.
                        """)
                .user("여행지명: " + title + "\n소개문: " + (overview == null ? "(없음)" : overview))
                .call()
                .content();

        return parse(response);
    }

    /** 추출 결과 — category 는 카테고리 필터, 나머지는 임베딩 원문 구성에 쓴다. */
    public record Extraction(String category, List<String> keywords, String description) {

        /** 하위 호환 키워드 문자열 — 첫 토큰이 대표 분류다 (카테고리 필터 파싱용). */
        public String keywordText() {
            return category + ", " + String.join(", ", keywords);
        }

        /** 임베딩 원문 — 장소명·분류·설명·키워드를 합친 검색 색인문. */
        public String embeddingText(String title) {
            return title + " (" + category + ")\n"
                    + description + "\n"
                    + "키워드: " + String.join(", ", keywords);
        }
    }

    static Extraction parse(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("키워드 추출 결과가 비어 있습니다.");
        }
        String category = null;
        String keywordLine = null;
        StringBuilder description = new StringBuilder();
        boolean inDescription = false;
        for (String rawLine : response.split("\n")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            Matcher matcher = LABELED_LINE.matcher(line);
            if (matcher.matches()) {
                String value = matcher.group(2).strip();
                inDescription = false;
                switch (matcher.group(1)) {
                    case "분류" -> category = value.replaceAll("[.。,\\s]+$", "");
                    case "키워드" -> keywordLine = value;
                    case "설명" -> {
                        description.append(value);
                        inDescription = true;
                    }
                    default -> { }
                }
            } else if (inDescription) {
                description.append(' ').append(line);
            }
        }
        if (category == null || !PRIMARY_CATEGORIES.contains(category)) {
            throw new IllegalStateException(
                    "분류는 다음 중 하나여야 합니다: " + String.join(", ", PRIMARY_CATEGORIES));
        }
        List<String> keywords = normalizeKeywords(keywordLine, category);
        if (keywords.isEmpty()) {
            throw new IllegalStateException("키워드 추출 결과가 비어 있습니다.");
        }
        String descriptionText = description.toString().strip();
        if (descriptionText.isEmpty()) {
            throw new IllegalStateException("설명 추출 결과가 비어 있습니다.");
        }
        return new Extraction(category, keywords, descriptionText);
    }

    private static List<String> normalizeKeywords(String line, String category) {
        if (line == null) {
            return List.of();
        }
        return Arrays.stream(line.split("[,\\n;|]"))
                .map(keyword -> keyword.replaceFirst("^[#\\-•*\\d.\\s]+", "").strip())
                .map(keyword -> keyword.replaceAll("[.。]+$", "").strip())
                .filter(keyword -> !keyword.isBlank())
                .filter(keyword -> !keyword.equals(category))
                .distinct()
                .limit(MAX_KEYWORDS)
                .toList();
    }
}
