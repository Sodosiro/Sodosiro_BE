package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.travel.entity.SpotRelatedRecommendation;
import com.sodosiro.domain.travel.repository.SpotRelatedRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연관 추천 스냅샷을 별도의 쓰기 트랜잭션으로 저장한다.
 * 상세 조회(readOnly)에서 스냅샷 INSERT가 flush되도록 REQUIRES_NEW로 분리하며,
 * 이 트랜잭션은 캐시 미스일 때만 열린다.
 */
@Component
@RequiredArgsConstructor
class SpotRelatedRecommendationSnapshotWriter {

    private final SpotRelatedRecommendationRepository snapshotRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(SpotRelatedRecommendation snapshot) {
        snapshotRepository.saveAndFlush(snapshot);
    }
}
