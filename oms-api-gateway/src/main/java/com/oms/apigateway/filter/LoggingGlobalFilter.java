package com.oms.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter = runs for EVERY route. Centralizes the cross-cutting "log every request"
 * concern in one place. Reactive: returns a Mono and logs on the way back via .then(...).
 */
@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = System.currentTimeMillis();
        log.info("Gateway IN  -> {} {}", request.getMethod(), request.getURI().getPath());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long tookMs = System.currentTimeMillis() - start;
            log.info("Gateway OUT <- {} {} ({} ms)",
                    exchange.getResponse().getStatusCode(),
                    request.getURI().getPath(), tookMs);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // run early
    }
}
