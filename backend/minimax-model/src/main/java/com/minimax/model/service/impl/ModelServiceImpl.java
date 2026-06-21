package com.minimax.model.service.impl;

import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.mapper.ModelConfigMapper;
import com.minimax.model.provider.ModelProviderAdapter;
import com.minimax.model.provider.ModelProviderFactory;
import com.minimax.model.quota.QuotaService;
import com.minimax.model.quota.RateLimiter;
import com.minimax.model.service.ApiKeyProviderService;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.vo.ModelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderFactory providerFactory;
    private final RateLimiter rateLimiter;
    private final QuotaService quotaService;
    private final ApiKeyProviderService apiKeyService;

    @Override
    public List<ModelVO> listEnabled() {
        List<Map<String, Object>> rows = modelConfigMapper.selectEnabledWithProvider();
        List<ModelVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            out.add(ModelVO.builder()
                    .id(toLong(r.get("model_id")))
                    .code((String) r.get("model_code"))
                    .displayName((String) r.get("display_name"))
                    .maxContext(toInt(r.get("max_context")))
                    .maxOutput(toInt(r.get("max_output")))
                    .inputPrice(toBigDecimal(r.get("input_price")))
                    .outputPrice(toBigDecimal(r.get("output_price")))
                    .supportsVision(toInt(r.get("supports_vision")) == 1)
                    .supportsTools(toInt(r.get("supports_tools")) == 1)
                    .supportsStream(toInt(r.get("supports_stream")) == 1)
                    .providerId(toLong(r.get("provider_id")))
                    .providerCode((String) r.get("provider_code"))
                    .providerName((String) r.get("provider_name"))
                    .protocol((String) r.get("protocol"))
                    .build());
        }
        return out;
    }

    @Override
    public ChatResponse chat(Long userId, ChatRequest req) {
        if (!rateLimiter.tryAcquire(userId)) {
            throw new BizException(ResultCode.RATE_LIMIT);
        }
        Map<String, Object> model = modelConfigMapper.selectByCode(req.getModel());
        if (model == null) throw new BizException(ResultCode.MODEL_NOT_FOUND);

        String providerCode = (String) model.get("provider_code");
        ModelProviderAdapter adapter = providerFactory.get(providerCode);
        String endpoint = (String) model.get("base_url");
        // V5.18: 优先用环境变量 key (支持多 key 轮询), DB api_key 作为兜底
        String dbApiKey = (String) model.get("api_key");
        String apiKey = resolveApiKey(providerCode, dbApiKey);

        try {
            ChatResponse resp = adapter.chat(endpoint, apiKey, req);
            apiKeyService.reportSuccess(providerCode, apiKey);
            long tokens = (resp.getTotalTokens() != null && resp.getTotalTokens() > 0)
                    ? resp.getTotalTokens() : 1;
            quotaService.record(userId, toLong(model.get("model_id")), tokens);
            return resp;
        } catch (Exception e) {
            apiKeyService.reportFailure(providerCode, apiKey);
            throw e;
        }
    }

    /**
     * V5.18: 解析 API key — 优先环境变量 (支持轮询), DB 兜底
     */
    private String resolveApiKey(String providerCode, String dbApiKey) {
        String envKey = apiKeyService.nextKey(providerCode);
        if (envKey != null && !envKey.isBlank()) return envKey;
        return dbApiKey;
    }

    @Override
    public Flux<String> stream(Long userId, ChatRequest req) {
        if (!rateLimiter.tryAcquire(userId)) {
            return Flux.error(new BizException(ResultCode.RATE_LIMIT));
        }
        Map<String, Object> model = modelConfigMapper.selectByCode(req.getModel());
        if (model == null) return Flux.error(new BizException(ResultCode.MODEL_NOT_FOUND));

        String providerCode = (String) model.get("provider_code");
        ModelProviderAdapter adapter = providerFactory.get(providerCode);
        String endpoint = (String) model.get("base_url");
        String dbApiKey = (String) model.get("api_key");
        String apiKey = resolveApiKey(providerCode, dbApiKey);
        return adapter.stream(endpoint, apiKey, req);
    }

    // ---------- helpers ----------

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }
}
