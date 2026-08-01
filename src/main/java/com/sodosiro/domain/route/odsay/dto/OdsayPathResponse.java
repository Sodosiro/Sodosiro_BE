package com.sodosiro.domain.route.odsay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record OdsayPathResponse(
        @JsonProperty("result") OdsayResult result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayResult(
            @JsonProperty("path") List<OdsayPath> paths
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayPath(
            @JsonProperty("pathType") Integer pathType,
            @JsonProperty("info") OdsayInfo info,
            @JsonProperty("subPath") List<OdsaySubPath> subPaths
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayInfo(
            @JsonProperty("totalTime") Integer totalTime,
            @JsonProperty("totalDistance") Integer totalDistance,
            @JsonProperty("payment") Integer payment
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsaySubPath(
            @JsonProperty("trafficType") Integer trafficType, // 1: 지하철, 2: 버스, 3: 도보
            @JsonProperty("sectionTime") Integer sectionTime,     // 이 구간 소요 시간 (분)
            @JsonProperty("distance") Integer distance,           // 이 구간 거리 (m)
            @JsonProperty("lane") List<OdsayLane> lanes       // 탈것 정보 (버스 번호 등)
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OdsayLane(
            @JsonProperty("name") String name // 버스 번호 (예: "140", "지선 3412" 등) 또는 지하철 노선명
    ) {}
}