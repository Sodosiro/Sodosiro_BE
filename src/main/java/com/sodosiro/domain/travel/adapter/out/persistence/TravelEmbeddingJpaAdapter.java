package com.sodosiro.domain.travel.adapter.out.persistence;

import com.sodosiro.domain.dataextract.application.port.out.TravelEmbeddingPort;
import com.sodosiro.domain.travel.entity.SpotEmbedding;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** travel JPA 모델을 dataextract의 임베딩 처리 포트에 연결한다. */
@Component
@RequiredArgsConstructor
public class TravelEmbeddingJpaAdapter implements TravelEmbeddingPort {

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;

    @Override
    public String findSpotTitle(Long contentId) {
        TouristSpot spot = touristSpotRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("관광지를 찾을 수 없습니다: " + contentId));
        return spot.getTitle();
    }

    @Override
    public void saveEmbedding(Long contentId, float[] embedding, String inputText, String keywordText) {
        spotEmbeddingRepository.save(SpotEmbedding.of(contentId, embedding, inputText, keywordText));
    }
}
