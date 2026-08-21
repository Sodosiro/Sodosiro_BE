package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

/** points 내부 배열은 [x, y](경도, 위도) 형식의 좌표 쌍 */
public record KakaoTransitPath(List<List<Double>> points) {
}
