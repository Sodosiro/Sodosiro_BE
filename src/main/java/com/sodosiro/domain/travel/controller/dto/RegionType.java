package com.sodosiro.domain.travel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

@Schema(description = """
        여행지 목록 지역 필터.
        ALL: 지역 제한 없음 (기본값)
        GANGWON_SMALL_TOWN: 강원 소도시 12곳 (고성군·삼척시·양구군·양양군·영월군·정선군·철원군·태백시·평창군·홍천군·화천군·횡성군)
        """)
public enum RegionType {
    ALL,
    GANGWON_SMALL_TOWN;

    public static final String GANGWON_LDONG_REGN_CODE = "51";


    private static final Set<String> GANGWON_SMALL_TOWN_SIGNGU_CODES = Set.of(
            "190",  // 태백시
            "230",  // 삼척시
            "720",  // 홍천군
            "730",  // 횡성군
            "750",  // 영월군
            "760",  // 평창군
            "770",  // 정선군
            "780",  // 철원군
            "790",  // 화천군
            "800",  // 양구군
            "820",  // 고성군
            "830"   // 양양군
    );

    public List<String> signguCodes() {
        return this == GANGWON_SMALL_TOWN ? List.copyOf(GANGWON_SMALL_TOWN_SIGNGU_CODES) : List.of();
    }
}
