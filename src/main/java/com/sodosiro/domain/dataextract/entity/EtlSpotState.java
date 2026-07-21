package com.sodosiro.domain.dataextract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** ETL이 소유하는 관광지별 후속 처리 상태. Spring은 임베딩 완료 상태만 갱신한다. */
@Entity
@Getter
@Table(name = "etl_spot_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EtlSpotState {

    @Id
    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @Column(name = "embed_pending", nullable = false)
    private boolean embedPending;

    @Column(name = "last_etl_at", nullable = false)
    private LocalDateTime lastEtlAt;

    /**
     * Marks embedding as complete and records the completion time.
     */
    public void completeEmbedding() {
        this.embedPending = false;
        this.lastEtlAt = LocalDateTime.now();
    }
}
