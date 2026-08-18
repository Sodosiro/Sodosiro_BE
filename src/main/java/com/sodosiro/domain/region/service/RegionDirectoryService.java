package com.sodosiro.domain.region.service;

import com.sodosiro.domain.region.controller.dto.AreaCodeResponse;
import com.sodosiro.domain.region.controller.dto.RegionCodeResponse;
import com.sodosiro.domain.region.entity.RegionIntro;
import com.sodosiro.domain.region.repository.AreaCodeRepository;
import com.sodosiro.domain.region.repository.RegionIntroRepository;
import com.sodosiro.domain.region.repository.SigunguCodeRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 지역 선택 화면에서 사용하는 시도·시군구 코드 조회 책임. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionDirectoryService {

    private final AreaCodeRepository areaCodeRepository;
    private final SigunguCodeRepository sigunguCodeRepository;
    private final RegionIntroRepository regionIntroRepository;

    public List<AreaCodeResponse> getAreas() {
        return areaCodeRepository.findAllByOrderByNameAsc().stream()
                .map(AreaCodeResponse::from)
                .toList();
    }

    public List<RegionCodeResponse> getRegions(String areaCode) {
        return sigunguCodeRepository.findAllByAreaCode(areaCode).stream()
                .map(RegionCodeResponse::from).toList();
    }
}
