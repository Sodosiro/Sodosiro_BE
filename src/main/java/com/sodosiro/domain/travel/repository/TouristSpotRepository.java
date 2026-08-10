package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long>, TouristSpotQueryRepository {

    List<TouristSpot> findTop200ByCategoryInOrderByAvgRatingDesc(List<Integer> categories);

    List<TouristSpot> findTop200ByOrderByAvgRatingDesc();
}
