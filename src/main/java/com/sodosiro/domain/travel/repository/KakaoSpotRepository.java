package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.KakaoSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KakaoSpotRepository extends JpaRepository<KakaoSpot, Long>, KakaoSpotQueryRepository {
}
