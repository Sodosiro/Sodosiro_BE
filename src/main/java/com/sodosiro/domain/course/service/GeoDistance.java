package com.sodosiro.domain.course.service;

import com.sodosiro.domain.travel.entity.TouristSpot;

/**
 * 두 관광지 사이의 대권 거리(km)를 하버사인 공식으로 계산한다. TourAPI 좌표는 mapX=경도, mapY=위도이다.
 * 위경도 차이를 그대로 제곱해 비교하면 한국 위도(약 35~38도)에서 경도 1도가 위도 1도보다 약 20% 짧은데도
 * 같은 길이로 취급돼 동서 방향 거리가 과대평가되므로, 거리 비교는 이 유틸을 통해서만 한다.
 */
final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    /** 좌표가 없는 스팟이 하나라도 있으면 {@link Double#MAX_VALUE}를 반환해 거리순 정렬에서 맨 뒤로 보낸다. */
    static double km(TouristSpot a, TouristSpot b) {
        if (!hasCoordinates(a) || !hasCoordinates(b)) {
            return Double.MAX_VALUE;
        }
        double lat1 = Math.toRadians(a.getMapY().doubleValue());
        double lat2 = Math.toRadians(b.getMapY().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(b.getMapX().doubleValue() - a.getMapX().doubleValue());

        double h = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    static boolean hasCoordinates(TouristSpot spot) {
        return spot != null && spot.getMapX() != null && spot.getMapY() != null;
    }
}
