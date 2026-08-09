package com.sodosiro.domain.region.repository;

import com.sodosiro.domain.travel.entity.AreaCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaCodeRepository extends JpaRepository<AreaCode, String> {

    List<AreaCode> findAllByOrderByNameAsc();
}
