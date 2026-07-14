package com.minimax.monitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.monitor.entity.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 告警实时推送注册表 (Day 27).
 * AlertEngine 触发新告警时调用 {@link #broadcast(AlertEvent)} 向所有在线前端推送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertStreamRegistry {

    private final ObjectMapper objectMapper;

    private static AlertStreamRegistry INSTANCE;

    @jakarta.annotation.PostConstruct
    public void init() {
        AlertStreamRegistry.INSTANCE = this;
    }

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void register(SseEmitter emitter) {
        emitters.add(emitter);
        log.debug("[AlertStream] registered, total={}", emitters.size());
    }

    public void unregister(SseEmitter emitter) {
        emitters.remove(emitter);
        log.debug("[AlertStream] unregistered, total={}", emitters.size());
    }

    /**
     * 广播告警事件给所有在线前端。
     * 由 AlertEngine 调用。
     */
    public void broadcast(AlertEvent event) {
        if (emitters.isEmpty()) return;

        Map<String, Object> payload = Map.of(
                "type", "alert_fired",
                "alert", Map.of(
                        "id", event.getId(),
                        "ruleName", event.getRuleName(),
                        "severity", event.getSeverity(),
                        "message", event.getMessage(),
                        "metricName", event.getMetricName(),
                        "metricValue", event.getMetricValue(),
                        "threshold", event.getThreshold(),
                        "firedAt", event.getFiredAt() != null ? event.getFiredAt().toString() : null
                )
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[AlertStream] serialize error: {}", e.getMessage());
            return;
        }

        for (SseEmitter em : List.copyOf(emitters)) {
            try {
                em.send(SseEmitter.event().name("alert").data(json));
            } catch (Exception e) {
                log.debug("[AlertStream] send failed, removing emitter: {}", e.getMessage());
                emitters.remove(em);
            }
        }
        log.info("[AlertStream] broadcasted alert: {} (subscribers={})", event.getRuleName(), emitters.size());
    }
}
