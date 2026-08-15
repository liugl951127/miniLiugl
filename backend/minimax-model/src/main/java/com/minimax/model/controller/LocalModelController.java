package com.minimax.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.common.result.Result;
import com.minimax.model.entity.ModelConfig;
import com.minimax.model.entity.ModelProvider;
import com.minimax.model.mapper.ModelConfigMapper;
import com.minimax.model.mapper.ModelProviderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * 本地 / 自研模型 管理 (V6.8.1+)
 *
 * 功能:
 *   1. 注册本地模型服务商 (base_url + api_key)
 *   2. 从本地推理服务器发现可用模型 (/v1/models)
 *   3. 将模型加入 model_config 供前端调用
 *
 * 支持协议: OpenAI 兼容 API (Ollama / vLLM / FastAPI / 自研)
 *
 * @since 2026-08-12
 */
@Slf4j
@Tag(name = "本地模型", description = "注册并调用本地/自研推理服务器的模型")
@RestController
@RequestMapping("/api/v1/models/local")
@RequiredArgsConstructor
public class LocalModelController {

    private final ModelProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final RestTemplate restTemplate;

    // ==================== 服务商管理 ====================

    @Operation(summary = "列出所有本地模型服务商")
    @GetMapping("/providers")
    public Result<List<ModelProvider>> listProviders() {
        List<ModelProvider> providers = providerMapper.selectList(
                new LambdaQueryWrapper<ModelProvider>()
                        .eq(ModelProvider::getProtocol, "local")
                        .orderByDesc(ModelProvider::getCreatedAt)
        );
        return Result.ok(providers);
    }

    @Operation(summary = "注册本地模型服务商")
    @PostMapping("/providers")
    public Result<ModelProvider> registerProvider(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String baseUrl = (String) body.get("baseUrl");
        String apiKey = (String) body.getOrDefault("apiKey", "");
        String description = (String) body.getOrDefault("description", "");

        if (name == null || name.isBlank()) return Result.fail("name 不能为空");
        if (baseUrl == null || baseUrl.isBlank()) return Result.fail("baseUrl 不能为空");

        // SSRF 防护: 去掉末尾斜杠并校验 URL 安全性
        baseUrl = baseUrl.trim().replaceAll("/+$", "");
        String ssrfCheck = validateWebhookUrl(baseUrl);
        if (ssrfCheck != null) {
            log.warn("[LocalModel] 注册被 SSRF 拦截: {} -> {}", baseUrl, ssrfCheck);
            return Result.fail("baseUrl 不安全: " + ssrfCheck);
        }

        ModelProvider provider = new ModelProvider();
        provider.setCode("local-" + Math.abs((name + System.currentTimeMillis()).hashCode()));
        provider.setName(name);
        provider.setBaseUrl(baseUrl);
        provider.setApiKey(apiKey);
        provider.setProtocol("local");
        provider.setEnabled(1);
        provider.setSort(0);
        provider.setDescription(description);
        providerMapper.insert(provider);

        log.info("[LocalModel] 注册本地服务商: name={} url={}", name, baseUrl);
        return Result.ok(provider);
    }

    @Operation(summary = "删除本地模型服务商 (同时删除关联模型)")
    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable Long id) {
        ModelProvider p = providerMapper.selectById(id);
        if (p == null) return Result.fail("服务商不存在: " + id);

        // 删除关联的 model_config
        modelConfigMapper.delete(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getProviderId, id));
        providerMapper.deleteById(id);
        log.info("[LocalModel] 删除本地服务商: id={} name={}", id, p.getName());
        return Result.ok();
    }

    @Operation(summary = "切换本地服务商启用状态")
    @PostMapping("/providers/{id}/toggle")
    public Result<Void> toggleProvider(@PathVariable Long id) {
        ModelProvider p = providerMapper.selectById(id);
        if (p == null) return Result.fail("服务商不存在");
        p.setEnabled(p.getEnabled() != null && p.getEnabled() == 1 ? 0 : 1);
        providerMapper.updateById(p);
        return Result.ok();
    }

    // ==================== 模型发现 ====================

    @Operation(summary = "从本地服务器发现可用模型 (GET /v1/models)")
    @GetMapping("/providers/{id}/discover")
    public Result<List<String>> discoverModels(@PathVariable Long id) {
        ModelProvider p = providerMapper.selectById(id);
        if (p == null) return Result.fail("服务商不存在: " + id);

        String url = p.getBaseUrl() + "/v1/models";
        try {
            Object response = restTemplate.getForObject(url, Object.class);
            List<String> models = parseModelIds(response);
            log.info("[LocalModel] 发现模型: provider={} count={} models={}", p.getName(), models.size(), models);
            return Result.ok(models);
        } catch (Exception e) {
            log.warn("[LocalModel] 模型发现失败: provider={} url={} error={}", p.getName(), url, e.getMessage());
            return Result.fail("无法连接或解析: " + e.getMessage());
        }
    }

    // ==================== 添加模型 ====================

    @Operation(summary = "添加本地模型到 model_config (在本地服务器注册后可调用)")
    @PostMapping("/providers/{id}/models")
    public Result<ModelConfig> addModel(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ModelProvider p = providerMapper.selectById(id);
        if (p == null) return Result.fail("服务商不存在: " + id);

        String modelCode = (String) body.get("modelCode");
        String displayName = (String) body.getOrDefault("displayName", modelCode);
        Integer maxContext = body.get("maxContext") != null ? ((Number) body.get("maxContext")).intValue() : 4096;
        Integer maxOutput = body.get("maxOutput") != null ? ((Number) body.get("maxOutput")).intValue() : 2048;
        Integer supportsVision = body.get("supportsVision") != null ? (((Boolean) body.get("supportsVision")) ? 1 : 0) : 0;
        Integer supportsTools = body.get("supportsTools") != null ? (((Boolean) body.get("supportsTools")) ? 1 : 0) : 0;

        if (modelCode == null || modelCode.isBlank()) return Result.fail("modelCode 不能为空");

        // 检查是否已存在
        ModelConfig existing = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getModelCode, modelCode)
                .eq(ModelConfig::getProviderId, id));
        if (existing != null) {
            return Result.fail("模型已存在: " + modelCode);
        }

        ModelConfig model = new ModelConfig();
        model.setProviderId(id);
        model.setModelCode(modelCode);
        model.setDisplayName(displayName);
        model.setMaxContext(maxContext);
        model.setMaxOutput(maxOutput);
        model.setInputPrice(java.math.BigDecimal.ZERO);
        model.setOutputPrice(java.math.BigDecimal.ZERO);
        model.setSupportsVision(supportsVision);
        model.setSupportsTools(supportsTools);
        model.setSupportsStream(1); // 本地模型默认支持流式
        model.setEnabled(1);
        modelConfigMapper.insert(model);

        log.info("[LocalModel] 添加本地模型: provider={} model={}", p.getName(), modelCode);
        return Result.ok(model);
    }

    @Operation(summary = "一键同步: 从服务器发现模型并全部添加到 model_config")
    @PostMapping("/providers/{id}/sync")
    public Result<Map<String, Object>> syncModels(@PathVariable Long id) {
        ModelProvider p = providerMapper.selectById(id);
        if (p == null) return Result.fail("服务商不存在: " + id);

        String url = p.getBaseUrl() + "/v1/models";
        List<String> discovered;
        try {
            Object response = restTemplate.getForObject(url, Object.class);
            discovered = parseModelIds(response);
        } catch (Exception e) {
            return Result.fail("无法连接 " + url + ": " + e.getMessage());
        }

        int added = 0, skipped = 0;
        for (String code : discovered) {
            ModelConfig existing = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                    .eq(ModelConfig::getModelCode, code)
                    .eq(ModelConfig::getProviderId, id));
            if (existing != null) { skipped++; continue; }

            ModelConfig model = new ModelConfig();
            model.setProviderId(id);
            model.setModelCode(code);
            model.setDisplayName(code);
            model.setMaxContext(4096);
            model.setMaxOutput(2048);
            model.setInputPrice(java.math.BigDecimal.ZERO);
            model.setOutputPrice(java.math.BigDecimal.ZERO);
            model.setSupportsVision(0);
            model.setSupportsTools(0);
            model.setSupportsStream(1);
            model.setEnabled(1);
            modelConfigMapper.insert(model);
            added++;
        }

        log.info("[LocalModel] 同步完成: provider={} added={} skipped={}", p.getName(), added, skipped);
        return Result.ok(Map.of("provider", p.getName(), "discovered", discovered.size(), "added", added, "skipped", skipped));
    }

    // ==================== Utils ====================

    /**
     * 解析 /v1/models 返回的模型 ID 列表
     * 兼容格式:
     *   { "data": [{ "id": "llama3:8b" }, ...] }
     *   { "object": "list", "data": [...] }
     *   ["llama3:8b", "mistral:7b", ...]  (直接数组)
     */
    @SuppressWarnings("unchecked")
    private List<String> parseModelIds(Object response) {
        List<String> result = new ArrayList<>();
        if (response == null) return result;

        try {
            if (response instanceof List) {
                // 直接数组格式
                for (Object item : (List<?>) response) {
                    if (item instanceof String) result.add((String) item);
                    else if (item instanceof Map) result.add((String) ((Map<?, ?>) item).get("id"));
                }
            } else if (response instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) response;
                Object data = map.get("data");
                if (data instanceof List) {
                    for (Object item : (List<?>) data) {
                        if (item instanceof Map) {
                            Object id = ((Map<?, ?>) item).get("id");
                            if (id != null) result.add(id.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[LocalModel] 解析模型列表失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * V7.0 Flow④关联Flow②: 注册训练产出的模型
     * AI 模块训练完成后调用此接口，将 trained-{taskId} 注册到 model_config
     */
    @Operation(summary = "注册训练模型 (V7.0: AI训练完成后调用)")
    @PostMapping("/models")
    public Result<ModelConfig> registerTrainedModel(@RequestBody Map<String, Object> body) {
        String modelCode = (String) body.get("modelCode");
        String displayName = (String) body.getOrDefault("displayName", modelCode);
        if (modelCode == null || modelCode.isBlank()) {
            return Result.fail("modelCode 不能为空");
        }
        // 检查是否已存在
        ModelConfig existing = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getModelCode, modelCode));
        if (existing != null) {
            // 已存在则启用并返回
            existing.setEnabled(1);
            modelConfigMapper.updateById(existing);
            log.info("[Model] 训练模型已启用: {}", modelCode);
            return Result.ok(existing);
        }
        // 创建新模型 (使用 "trained" provider)
        ModelProvider trainedProvider = providerMapper.selectOne(
                new LambdaQueryWrapper<ModelProvider>().eq(ModelProvider::getCode, "trained"));
        if (trainedProvider == null) {
            return Result.fail("训练模型分类 provider 不存在，请联系管理员");
        }
        ModelConfig model = new ModelConfig();
        model.setProviderId(trainedProvider.getId());
        model.setModelCode(modelCode);
        model.setDisplayName(displayName != null ? displayName : modelCode);
        model.setMaxContext(body.get("maxContext") != null ? ((Number) body.get("maxContext")).intValue() : 4096);
        model.setMaxOutput(body.get("maxOutput") != null ? ((Number) body.get("maxOutput")).intValue() : 2048);
        model.setInputPrice(java.math.BigDecimal.ZERO);
        model.setOutputPrice(java.math.BigDecimal.ZERO);
        model.setSupportsVision(0);
        model.setSupportsTools(1);
        model.setSupportsStream(1);
        model.setEnabled(1);
        modelConfigMapper.insert(model);
        log.info("[Model] 注册训练模型: {} -> {}", modelCode, displayName);
        return Result.ok(model);
    }

    /**
     * SSRF 防护（本地模型服务器专用版）:
     * - 允许 localhost/127.0.0.1 (本地推理服务器)
     * - 允许私有内网地址 (10.x / 172.16-31.x / 192.168.x)
     * - 仍禁止云元数据地址
     * - 仅允许 http/https
     */
    private String validateWebhookUrl(String urlStr) {
        try {
            URI uri = new URI(urlStr);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return "仅支持 http/https 协议";
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) return "主机名不能为空";
            InetAddress addr = InetAddress.getByName(host);
            // 本地/内网允许 (本地模型服务器通常运行在 localhost 或 LAN)
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()) return null;
            if (addr.isLinkLocalAddress()) return "禁止链路本地地址 (169.254.x.x)";
            // 禁止云元数据地址 (这些才是真正的 SSRF 风险)
            String h = host.toLowerCase();
            if (h.equals("metadata.google.internal")
                    || h.equals("metadata.tencentyun.com")
                    || h.equals("169.254.169.254")) {
                return "禁止云元数据地址";
            }
            return null;  // 通过
        } catch (URISyntaxException | java.net.UnknownHostException e) {
            return "URL 格式错误: " + e.getMessage();
        }
    }
}
