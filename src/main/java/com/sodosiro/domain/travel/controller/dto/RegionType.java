package com.sodosiro.domain.travel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        여행지 목록 지역 필터.
        ALL: 지역 제한 없음 (기본값)
        SMALL_TOWN: 소도시만 (현재 강원 12곳: 고성군·삼척시·양구군·양양군·영월군·정선군·철원군·태백시·평창군·홍천군·화천군·횡성군)
        """)
public enum RegionType {
    ALL,
    SMALL_TOWN;

    /** 소도시로 결과를 제한해야 하는 타입인지. */
    public boolean smallTownOnly() {
        return this != ALL;
    }
}
