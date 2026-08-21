package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

/** vertexes는 [x1, y1, x2, y2, ...] 형태의 1차원 배열(X=경도, Y=위도) */
public record KakaoRoad(List<Double> vertexes) {
}
