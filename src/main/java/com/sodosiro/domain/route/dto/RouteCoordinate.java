package com.sodosiro.domain.route.dto;

import java.math.BigDecimal;

/** 지도에 경로선을 그리기 위한 좌표 하나. 자차(카카오 vertexes)/대중교통(카카오 path.points) 공용으로 사용한다. */
public record RouteCoordinate(BigDecimal longitude, BigDecimal latitude) {
}
