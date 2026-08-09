package com.sodosiro.domain.region.repository;

import com.sodosiro.domain.travel.entity.SigunguCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SigunguCodeRepository extends JpaRepository<SigunguCode, Long> {

    List<SigunguCode> findAllByAreaCodeOrderByNameAsc(String areaCode);
}
