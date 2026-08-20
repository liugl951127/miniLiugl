package com.minimax.model.service.impl;

import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import com.minimax.model.config.OnnxModelConfig;
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
import java.util.LinkedHashMap;
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
    private final OnnxModelConfig onnxConfig;

    @Override
    public List<ModelVO> listEnabled() {
        List<Map<String, Object>> rows;
        try {
            rows = modelConfigMapper.selectEnabledWithProvider();
        } catch (Exception e) {
            log.warn("[ModelService] DB listEnabled failed: {}", e.getMessage());
            rows = List.of();
        }
        List<ModelVO> out = new ArrayList<>(rows.size() + 1);
        for (Map<String, Object> r : rows) {
            String providerCode = (String) r.get("provider_code");
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
                    .providerCode(providerCode)
                    .providerName((String) r.get("provider_name"))
                    .protocol((String) r.get("protocol"))
                    .category(classifyCategory(providerCode))
                    .build());
        }
        // V7.0: 如果本地 ONNX 启用，在列表末尾追加
        if (onnxConfig.isEnabled()) {
            out.add(ModelVO.builder()
                    .id(0L)
                    .code("onnx")
                    .displayName(onnxConfig.getDisplayName())
                    .maxContext(onnxConfig.getMaxContext())
                    .maxOutput(onnxConfig.getMaxOutput())
                    .inputPrice(BigDecimal.ZERO)
                    .outputPrice(BigDecimal.ZERO)
                    .supportsVision(false)
                    .supportsTools(onnxConfig.isSupportsTools())
                    .supportsStream(onnxConfig.isSupportsStream())
                    .providerId(0L)
                    .providerCode("onnx")
                    .providerName("本地 ONNX")
                    .protocol("local")
                    .category("self")
                    .build());
        }
        return out;
    }

    /**
     * V7.1: 根据 provider code 分类
     * self      — 自研模型
     * commercial — 商业模型
     */
    private String classifyCategory(String providerCode) {
        if (providerCode == null) return "commercial";
        String p = providerCode.toLowerCase();
        if ("self-trained".equals(p) || "onnx".equals(p) || "ollama".equals(p)) {
            return "self";
        }
        return "commercial";
    }

    @Override
    public ChatResponse chat(Long userId, ChatRequest req) {
        if (!rateLimiter.tryAcquire(userId)) {
            throw new BizException(ResultCode.RATE_LIMIT);
        }
        Map<String, Object> model = resolveModel(req.getModel());

        String providerCode = (String) model.get("provider_code");
        ModelProviderAdapter adapter = providerFactory.get(providerCode);
        String endpoint = (String) model.get("base_url");
        String protocol = (String) model.get("protocol");
        // V5.18: 优先用环境变量 key (支持多 key 轮询), DB api_key 作为兜底
        // 本地模型 (protocol=local) 使用 model_code 作为 apiKey 拼接模型文件路径
        String dbApiKey = (String) model.get("api_key");
        String apiKey = "local".equals(protocol)
                ? (String) model.get("model_code")
                : resolveApiKey(providerCode, dbApiKey);

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
        Map<String, Object> model = resolveModel(req.getModel());
        if (model == null) return Flux.error(new BizException(ResultCode.MODEL_NOT_FOUND));

        String providerCode = (String) model.get("provider_code");
        ModelProviderAdapter adapter = providerFactory.get(providerCode);
        String endpoint = (String) model.get("base_url");
        String dbApiKey = (String) model.get("api_key");
        String apiKey = resolveApiKey(providerCode, dbApiKey);
        return adapter.stream(endpoint, apiKey, req);
    }

    // ---------- 本地 ONNX 模型解析 ----------

    /**
     * V7.0: 解析模型配置 — 数据库优先，回退到本地 ONNX 配置
     */
    private Map<String, Object> resolveModel(String modelCode) {
        Map<String, Object> model = tryDbLookup(modelCode);
        if (model != null) return model;

        // DB 无结果，尝试本地 ONNX 配置
        if (onnxConfig.isEnabled() && isOnnxModel(modelCode)) {
            return buildOnnxModelMap();
        }

        throw new BizException(ResultCode.MODEL_NOT_FOUND);
    }

    private Map<String, Object> tryDbLookup(String modelCode) {
        try {
            return modelConfigMapper.selectByCode(modelCode);
        } catch (Exception e) {
            log.warn("[ModelService] DB lookup failed for {}: {}", modelCode, e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否为 ONNX 模型标识
     * 匹配: "onnx", "onnx-local", "local-onnx", 以 "onnx-" 开头
     */
    private boolean isOnnxModel(String modelCode) {
        if (modelCode == null) return false;
        String lower = modelCode.toLowerCase();
        return lower.equals("onnx")
                || lower.equals("onnx-local")
                || lower.equals("local-onnx")
                || lower.startsWith("onnx-");
    }

    /**
     * 根据本地 ONNX 配置构建模型 Map（模拟 DB 返回格式）
     */
    private Map<String, Object> buildOnnxModelMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("model_id", 0L);
        m.put("model_code", "onnx");
        m.put("display_name", onnxConfig.getDisplayName());
        m.put("provider_code", "onnx");
        m.put("provider_name", "本地 ONNX");
        // endpoint: 优先用配置的推理地址，否则指向 minimax-ai 本地推理端点
        String endpoint = onnxConfig.getInferenceUrl();
        m.put("base_url", (endpoint == null || endpoint.isBlank())
                ? "http://localhost:8094" : endpoint);
        m.put("api_key", onnxConfig.getModelName());
        m.put("protocol", "local");
        m.put("max_context", onnxConfig.getMaxContext());
        m.put("max_output", onnxConfig.getMaxOutput());
        m.put("supports_vision", 0);
        m.put("supports_tools", onnxConfig.isSupportsTools() ? 1 : 0);
        m.put("supports_stream", onnxConfig.isSupportsStream() ? 1 : 0);
        log.info("[ModelService] 使用本地 ONNX 模型: {}", onnxConfig.getModelName());
        return m;
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
