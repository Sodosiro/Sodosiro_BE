package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.KakaoSpotImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KakaoSpotImageRepository extends JpaRepository<KakaoSpotImage, Long> {

    List<KakaoSpotImage> findByKakaoSpotIdIn(List<Long> kakaoSpotIds);
}
