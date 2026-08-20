package com.sodosiro.domain.route.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 카카오 길찾기 API는 예상 주유비를 제공하지 않아 거리 기반으로 근사치를 추정한다.
 * 실제 차종별 연비나 실시간 유가는 반영하지 않는 참고용 수치다.
 */
@Component
public class CarFuelCostEstimator {

    @Value("${route.fuel.price-per-liter-won}")
    private double pricePerLiterWon;

    @Value("${route.fuel.efficiency-km-per-liter}")
    private double efficiencyKmPerLiter;

    public long estimate(long distanceMeters) {
        double distanceKm = distanceMeters / 1000.0;
        double liters = distanceKm / efficiencyKmPerLiter;
        return Math.round(liters * pricePerLiterWon);
    }
}
