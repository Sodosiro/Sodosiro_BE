package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long>, TouristSpotQueryRepository {

    List<TouristSpot> findByCategoryInAndSigunguCodeOrderByAvgRatingDesc(
            List<Integer> categories, String sigunguCode, Pageable pageable);

    List<TouristSpot> findBySigunguCodeOrderByAvgRatingDesc(String sigunguCode, Pageable pageable);
}
