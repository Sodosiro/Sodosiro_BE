package com.sodosiro.domain.gps.service;

import com.sodosiro.domain.gps.entity.Gps;
import java.math.BigDecimal;

/** 코스 스팟과 사용자 좌표 사이의 GPS 방문 인증 거리 판정 (반경 {@link Gps#VERIFICATION_RADIUS_METERS}). */
public final class GpsVerifier {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GpsVerifier() {
    }

    public static boolean isWithinVerificationRadius(
            BigDecimal spotLatitude, BigDecimal spotLongitude,
            BigDecimal userLatitude, BigDecimal userLongitude) {
        return distanceMeters(spotLatitude, spotLongitude, userLatitude, userLongitude)
                <= Gps.VERIFICATION_RADIUS_METERS;
    }

    static double distanceMeters(
            BigDecimal latitude1, BigDecimal longitude1,
            BigDecimal latitude2, BigDecimal longitude2) {

        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lon1 = Math.toRadians(longitude1.doubleValue());
        double lat2 = Math.toRadians(latitude2.doubleValue());
        double lon2 = Math.toRadians(longitude2.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
