package com.sodosiro.domain.travel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 지역코드 (광역시도) - 17건
 * areaCode2 code (1=서울 … 38=전남)
 */
@Entity
@Getter
@Table(name = "area_code")
@Comment("지역코드 (광역시도) - 17건")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AreaCode {

    @Id
    @Column(name = "area_code", length = 5)
    @Comment("areaCode2 code (1=서울 … 38=전남)")
    private String areaCode;

    @Column(name = "name", length = 50, nullable = false)
    @Comment("지역명")
    private String name;
}
