package com.minimax.admin.service;

import com.minimax.admin.client.ServiceClient;
import com.minimax.admin.client.ServiceEndpoints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 跨服务健康检查聚合。
 *
 * 并发 ping 6 个服务, 汇总状态。
 * 单服务超时不影响整体 (异步 + 5s 超时)。
 */
@Service
@RequiredArgsConstructor
public class HealthAggregator {

    private final ServiceClient client;
    private final ServiceEndpoints endpoints;

    public Map<String, Object> aggregate() {
        Instant t0 = Instant.now();
        List<Service> services = List.of(
                new Service("auth",     endpoints.auth()),
                new Service("chat",     endpoints.chat()),
                new Service("model",    endpoints.model()),
                new Service("memory",   endpoints.memory()),
                new Service("rag",      endpoints.rag()),
                new Service("function", endpoints.function())
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", t0.toString());
        int upCount = 0;
        int total = services.size();
        for (Service s : services) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("url", s.baseUrl);
            try {
                CompletableFuture<Boolean> probe = CompletableFuture.supplyAsync(() -> client.isReachable(s.baseUrl));
                Boolean ok = probe.get(3, java.util.concurrent.TimeUnit.SECONDS);
                entry.put("status", ok ? "UP" : "DOWN");
                if (ok) upCount++;
            } catch (Exception e) {
                entry.put("status", "DOWN");
                entry.put("error", e.getMessage());
            }
            result.put(s.name, entry);
        }
        result.put("summary", upCount + "/" + total + " UP");
        result.put("allUp", upCount == total);
        result.put("durationMs", Duration.between(t0, Instant.now()).toMillis());
        return result;
    }

    private record Service(String name, String baseUrl) {}
}
