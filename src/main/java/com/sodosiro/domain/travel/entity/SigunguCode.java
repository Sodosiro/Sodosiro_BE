package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 시군구코드 (지역코드 종속)
 * PK 는 surrogate id(IDENTITY), 자연키 (area_code, sigungu_code) 는 UNIQUE 제약으로 유일성 보장.
 */
@Entity
@Getter
@Table(
        name = "sigungu_code",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sigungu_area_code",
                columnNames = {"area_code", "sigungu_code"}
        )
)
@Comment("시군구코드 (지역코드 종속)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SigunguCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Comment("PK (surrogate)")
    private Long id;

    @Column(name = "area_code", length = 5, nullable = false)
    @Comment("지역코드 (area_code 참조)")
    private String areaCode;

    @Column(name = "sigungu_code", length = 5, nullable = false)
    @Comment("시군구코드")
    private String sigunguCode;

    @Column(name = "name", length = 50, nullable = false)
    @Comment("시군구명")
    private String name;
}
