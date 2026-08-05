package com.sodosiro.domain.route;

import com.sodosiro.domain.route.dto.TransportMode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RouteSearchClientFactory {

    private final Map<TransportMode, RouteSearchClient> clientsByMode;

    public RouteSearchClientFactory(List<RouteSearchClient> clients) {
        this.clientsByMode = clients.stream()
                .collect(Collectors.toMap(RouteSearchClient::supports, Function.identity()));
    }

    public RouteSearchClient getClient(TransportMode mode) {
        RouteSearchClient client = clientsByMode.get(mode);
        if (client == null) {
            throw new IllegalArgumentException("지원하지 않는 이동수단입니다: " + mode);
        }
        return client;
    }
}
