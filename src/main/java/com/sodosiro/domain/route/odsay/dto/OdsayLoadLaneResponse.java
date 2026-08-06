package com.sodosiro.domain.route.odsay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayLoadLaneResponse(
        @JsonProperty("result") OdsayLoadLaneResult result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayLoadLaneResult(
            @JsonProperty("lane") List<OdsayGraphLane> lanes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayGraphLane(
            @JsonProperty("section") List<OdsayGraphSection> sections
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayGraphSection(
            @JsonProperty("graphPos") List<OdsayGraphPos> graphPos
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayGraphPos(
            @JsonProperty("x") BigDecimal x, // 경도
            @JsonProperty("y") BigDecimal y  // 위도
    ) {}
}
