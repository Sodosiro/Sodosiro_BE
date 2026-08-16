package com.sodosiro.domain.region.service;

import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import com.sodosiro.domain.travel.entity.SigunguCode;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 여행지의 법정동 지역코드(ldong_regn_code = area_code, ldong_signgu_code = sigungu_code)를
 * 시군구명(예: "원주시")으로 해석한다. 시군구가 없으면 광역시도명으로 폴백한다.
 *
 * <p>area_code(약 17건)·sigungu_code(약 250건)는 런타임에 거의 변하지 않는 정적 참조 데이터라
 * 전체를 한 번만 메모리에 적재해두고, 요청당 조회는 HashMap 조회(0 쿼리)로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class RegionNameResolver {

    private final SigunguCodeRepository sigunguCodeRepository;

    /** "areaCode|sigunguCode" -> 시군구명 */
    private volatile Map<String, String> regionNameByPair;
    /** "areaCode" -> "광역시도명" (시군구 미매칭 시 폴백) */
    private volatile Map<String, String> areaNameByCode;

    /** 여행지들의 지역명을 contentId 기준 맵으로 반환한다. 해석되지 않는 여행지는 맵에 포함하지 않는다. */
    public Map<Long, String> resolveByContentId(Collection<TouristSpot> spots) {
        if (spots.isEmpty()) {
            return Map.of();
        }
        ensureLoaded();
        Map<Long, String> result = new HashMap<>();
        for (TouristSpot spot : spots) {
            String name = regionName(spot);
            if (name != null) {
                result.put(spot.getContentId(), name);
            }
        }
        return result;
    }

    /** 지역 데이터가 갱신되면 호출해 캐시를 다시 적재한다. */
    public void refresh() {
        load();
    }

    private String regionName(TouristSpot spot) {
        String areaCode = spot.getLdongRegnCode();
        if (areaCode == null) {
            return null;
        }
        String sigunguCode = spot.getLdongSignguCode();
        if (sigunguCode != null) {
            String combined = regionNameByPair.get(pairKey(areaCode, sigunguCode));
            if (combined != null) {
                return combined;
            }
        }
        return areaNameByCode.get(areaCode); // 시군구 미매칭 시 광역시도명만, 없으면 null
    }

    private void ensureLoaded() {
        if (regionNameByPair == null) {
            synchronized (this) {
                if (regionNameByPair == null) {
                    load();
                }
            }
        }
    }

    private synchronized void load() {
        List<SigunguCode> all = sigunguCodeRepository.findAllWithArea();
        Map<String, String> pairMap = new HashMap<>();
        Map<String, String> areaMap = new HashMap<>();
        for (SigunguCode s : all) {
            pairMap.put(pairKey(s.getAreaCode(), s.getSigunguCode()), s.getName());
            areaMap.putIfAbsent(s.getAreaCode(), s.getArea().getName());
        }
        this.areaNameByCode = areaMap;
        this.regionNameByPair = pairMap; // 발행 플래그: 반드시 마지막에 세팅
    }

    private static String pairKey(String areaCode, String sigunguCode) {
        return areaCode + "|" + sigunguCode;
    }
}
