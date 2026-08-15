package com.minimax.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.vo.ModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * ModelService Stub for h2local sandbox mode.
 *
 * V7.0: 改为通过 RestTemplate 代理到真实的 minimax-model 服务 (port 8084)。
 * 不再返回 mock 数据。
 */
@Slf4j
@Service
@Profile("h2local")
public class ModelServiceStub implements ModelService {

    private final RestTemplate modelRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${minimax.model.service-url:http://localhost:8084}")
    private String modelServiceUrl;

    @Autowired
    public ModelServiceStub(@Qualifier("modelRestTemplate") RestTemplate modelRestTemplate) {
        this.modelRestTemplate = modelRestTemplate;
    }

    @Override
    public List<ModelVO> listEnabled() {
        try {
            String url = modelServiceUrl + "/api/v1/models/providers";
            ResponseEntity<String> resp = modelRestTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(resp.getBody(), Map.class);
                Object data = result.get("data");
                if (data instanceof List) {
                    List<ModelVO> vos = new ArrayList<>();
                    for (Object item : (List<?>) data) {
                        if (item instanceof String) {
                            // providers returns List<String> (provider codes)
                            vos.add(ModelVO.builder()
                                .id(0L)
                                .code((String) item)
                                .displayName((String) item)
                                .providerCode((String) item)
                                .build());
                        }
                    }
                    return vos;
                }
            }
        } catch (Exception ex) {
            log.warn("[ModelServiceStub] listEnabled failed: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public ChatResponse chat(Long userId, ChatRequest req) {
        try {
            String url = modelServiceUrl + "/api/v1/models/chat";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("model", req.getModel());
            reqBody.put("messages", req.getMessages());
            reqBody.put("temperature", req.getTemperature());
            reqBody.put("maxTokens", req.getMaxTokens());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            ResponseEntity<String> resp = modelRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(resp.getBody(), Map.class);
                Object data = result.get("data");
                if (data instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) data;
                    return ChatResponse.builder()
                        .id(String.valueOf(m.getOrDefault("id", "")))
                        .model(String.valueOf(m.getOrDefault("model", req.getModel())))
                        .content(String.valueOf(m.getOrDefault("content", "")))
                        .totalTokens(m.get("totalTokens") instanceof Number ? ((Number) m.get("totalTokens")).intValue() : 0)
                        .latencyMs(m.get("latencyMs") instanceof Number ? ((Number) m.get("latencyMs")).longValue() : 0L)
                        .providerCode(String.valueOf(m.getOrDefault("providerCode", "")))
                        .build();
                }
            }
        } catch (Exception ex) {
            log.warn("[ModelServiceStub] chat failed, falling back to mock: {}", ex.getMessage());
        }
        // Fallback to mock if model service is not reachable
        return ChatResponse.builder()
            .id("stub-" + System.currentTimeMillis())
            .model(req.getModel() != null ? req.getModel() : "stub")
            .content("⚠️ AI model service is not available. Please run minimax-model service at port 8084.")
            .build();
    }

    @Override
    public Flux<String> stream(Long userId, ChatRequest req) {
        // Stream 不常用，简单转发
        return Flux.just("⚠️ Streaming not supported in stub mode. Please run minimax-model service.");
    }
}
