package com.sodosiro.domain.dataextract.entity;

import com.sodosiro.domain.travel.entity.TouristSpot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private TouristSpot touristSpot;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @Column(name = "embed_pending", nullable = false)
    private boolean embedPending;

    @Column(name = "last_etl_at", nullable = false)
    private LocalDateTime lastEtlAt;

    public void completeEmbedding() {
        this.embedPending = false;
        this.lastEtlAt = LocalDateTime.now();
    }
}
