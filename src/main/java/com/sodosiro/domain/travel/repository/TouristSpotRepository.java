package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long>, TouristSpotQueryRepository {

    List<TouristSpot> findByCategoryInAndLdongSignguCodeOrderByAvgRatingDesc(List<Integer> categories, String ldongSignguCode, Pageable pageable);

    List<TouristSpot> findByLdongSignguCodeOrderByAvgRatingDesc(String ldongSignguCode, Pageable pageable);

    boolean existsByLdongSignguCode(String ldongSignguCode);
}
