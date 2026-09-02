package com.sodosiro.domain.badge.entity;

import com.sodosiro.domain.travel.entity.SigunguCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/** 강원 소도시(인구감소지역) 지역별 뱃지. 지역 하나당 뱃지 하나. */
@Entity
@Getter
@Table(
        name = "badge",
        uniqueConstraints = @UniqueConstraint(name = "uk_badge_sigungu", columnNames = "sigungu_id")
)
@Comment("소도시 지역 뱃지")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("뱃지 PK")
    private Long id;

    @Column(name = "sigungu_id", nullable = false)
    @Comment("연결된 시군구 (sigungu_code 참조)")
    private Long sigunguId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sigungu_id", insertable = false, updatable = false)
    private SigunguCode sigunguCode;

    @Column(name = "name", nullable = false, length = 50)
    @Comment("뱃지 이름 (지역명)")
    private String name;
}
