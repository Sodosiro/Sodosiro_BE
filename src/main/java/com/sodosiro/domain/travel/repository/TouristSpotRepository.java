package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.TouristSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long>, TouristSpotQueryRepository {
}
