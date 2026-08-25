package com.sodosiro.domain.region.repository;

import com.sodosiro.domain.travel.entity.SigunguCode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SigunguCodeRepository extends JpaRepository<SigunguCode, Long> {

    List<SigunguCode> findAllByAreaCode(String areaCode);

    @Query("select s from SigunguCode s join fetch s.area")
    List<SigunguCode> findAllWithArea();

    Optional<SigunguCode> findFirstBySigunguCode(String sigunguCode);
}
