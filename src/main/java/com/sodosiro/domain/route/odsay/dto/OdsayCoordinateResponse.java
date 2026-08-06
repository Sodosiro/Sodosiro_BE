package com.sodosiro.domain.route.odsay.dto;

import java.math.BigDecimal;

public record OdsayCoordinateResponse(
        BigDecimal longitude,
        BigDecimal latitude
) {
}
