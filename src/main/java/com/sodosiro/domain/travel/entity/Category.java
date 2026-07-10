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
 * 분류코드 cat1/cat2/cat3 계층
 * code: A01 / A0101 / A01010100
 * parent_code 로 자기참조 (대 > 중 > 소)
 */
@Entity
@Getter
@Table(name = "category")
@Comment("분류코드 cat1/cat2/cat3 계층")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @Column(name = "code", length = 12)
    @Comment("분류코드 (A01 / A0101 / A01010100)")
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    @Comment("분류명")
    private String name;

    @Column(name = "depth", nullable = false)
    @Comment("계층 깊이 (1=cat1, 2=cat2, 3=cat3)")
    private Short depth;

    @Column(name = "parent_code", length = 12)
    @Comment("자기참조 부모 코드 (대 > 중 > 소)")
    private String parentCode;
}
