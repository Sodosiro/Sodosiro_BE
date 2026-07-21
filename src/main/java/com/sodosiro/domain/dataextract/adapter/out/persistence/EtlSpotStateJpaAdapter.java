package com.sodosiro.domain.dataextract.adapter.out.persistence;

import com.sodosiro.domain.dataextract.application.port.out.EmbeddingStatePort;
import com.sodosiro.domain.dataextract.entity.EtlSpotState;
import com.sodosiro.domain.dataextract.repository.EtlSpotStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JPA 기반 ETL 상태 어댑터. */
@Component
@RequiredArgsConstructor
public class EtlSpotStateJpaAdapter implements EmbeddingStatePort {

    private final EtlSpotStateRepository etlSpotStateRepository;

    /**
     * Retrieves the overview associated with an ETL content state.
     *
     * @param contentId the identifier of the content whose overview is retrieved
     * @return the content state's overview
     */
    @Override
    public String findOverview(Long contentId) {
        return findState(contentId).getOverview();
    }

    /**
     * Marks embedding as complete for the ETL state associated with the content.
     *
     * @param contentId the identifier of the content whose embedding is completed
     */
    @Override
    public void completeEmbedding(Long contentId) {
        EtlSpotState state = findState(contentId);
        state.completeEmbedding();
        etlSpotStateRepository.save(state);
    }

    /**
     * Loads the ETL state associated with a content identifier.
     *
     * @param contentId the identifier of the content
     * @return the associated ETL state
     * @throws IllegalArgumentException if no ETL state exists for the content identifier
     */
    private EtlSpotState findState(Long contentId) {
        return etlSpotStateRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("ETL 상태를 찾을 수 없습니다: " + contentId));
    }
}
