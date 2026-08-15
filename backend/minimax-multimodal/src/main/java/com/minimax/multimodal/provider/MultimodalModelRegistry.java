package com.minimax.multimodal.provider;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模态模型注册表 (V3.0.1)
 *
 * <p>职责:
 *   1. 收集 Spring 容器中所有 MultimodalModelProvider 实现
 *   2. 按 name 索引, 提供 O(1) 查询
 *   3. 暴露 provider 列表 (用于 /api/v1/multimodal/info)
 *
 * <h3>路由逻辑</h3>
 * <pre>
 *   user request: "model": "builtin"
 *     ↓
 *   registry.get("builtin") → BuiltinVisionProvider
 *     ↓
 *   provider.describe(...)
 * </pre>
 *
 * <h3>降级链</h3>
 * <ol>
 *   <li>尝试配置的 provider (e.g. "openai")</li>
 *   <li>失败 → 降级到 "builtin"</li>
 *   <li>builtin 失败 → 降级到 "mock"</li>
 * </ol>
 */
@Slf4j
@Component
public class MultimodalModelRegistry {

    /**
     * 默认 provider 名 (application.yml 配置: minimax.multimodal.provider)
     */
    @Value("${minimax.multimodal.provider:builtin}")
    private String defaultProvider;

    /**
     * 降级顺序 (逗号分隔, 失败时依次尝试)
     * 默认: builtin,mock (内置永远兜底, 不会因外部故障完全不可用)
     */
    @Value("${minimax.multimodal.fallback:builtin,mock}")
    private String fallbackChain;

    /**
     * 已注册的 provider (按 name 索引)
     * 使用 LinkedHashMap 保持插入顺序, 便于 info 接口展示
     */
    private final Map<String, MultimodalModelProvider> providers = new LinkedHashMap<>();

    /**
     * 注入所有 MultimodalModelProvider 实现
     * Spring 自动扫描所有 @Component 标注的 provider
     */
    private final List<MultimodalModelProvider> providerBeans;

    /**
     * 构造器注入 (Spring 4.3+ 可省略 @Autowired)
     * @param providerBeans Spring 自动收集所有 MultimodalModelProvider 类型 Bean
     */
    public MultimodalModelRegistry(List<MultimodalModelProvider> providerBeans) {
        this.providerBeans = providerBeans;
    }

    /**
     * 初始化: 注册所有 provider
     * @PostConstruct 在 Bean 初始化后, 服务可用前执行
     */
    @PostConstruct
    public void init() {
        log.info("[multimodal-registry] 注册 provider, 默认={}, fallback={}", defaultProvider, fallbackChain);
        // 1. 遍历 Spring 注入的 provider beans
        for (MultimodalModelProvider p : providerBeans) {
            // 2. 防止 name 冲突 (后注册的覆盖先注册的)
            if (providers.containsKey(p.name())) {
                log.warn("[multimodal-registry] provider name 冲突: {} 已被 {} 实现占用, 现被 {} 覆盖",
                        p.name(), providers.get(p.name()).getClass().getSimpleName(), p.getClass().getSimpleName());
            }
            // 3. 注册
            providers.put(p.name(), p);
            // 4. 打印就绪状态
            log.info("[multimodal-registry] ✓ {} - {} (ready={})",
                    p.name(), p.description(), p.isReady());
        }
        // 5. 验证默认 provider 存在
        if (!providers.containsKey(defaultProvider)) {
            log.warn("[multimodal-registry] 默认 provider '{}' 未注册, 可用的: {}",
                    defaultProvider, providers.keySet());
        }
    }

    /**
     * 按 name 查找 provider
     *
     * @param name provider 名 (大小写敏感)
     * @return provider 实例, 不存在返回 null
     */
    public MultimodalModelProvider get(String name) {
        if (name == null || name.isBlank()) {
            return null;  // 空值保护
        }
        return providers.get(name);  // O(1) 查找
    }

    /**
     * 获取默认 provider
     */
    public MultimodalModelProvider getDefault() {
        // 1. 尝试配置的默认
        MultimodalModelProvider p = get(defaultProvider);
        if (p != null) return p;
        // 2. 兜底: builtin
        p = get("builtin");
        if (p != null) return p;
        // 3. 最后兜底: mock
        return get("mock");
    }

    /**
     * 获取降级链 (按顺序, 跳过未注册的)
     *
     * @return 降级链列表 (默认 provider 在前, fallback 在后)
     */
    public List<MultimodalModelProvider> getFallbackChain() {
        // 1. 结果列表
        List<MultimodalModelProvider> chain = new ArrayList<>();
        // 2. 先加默认
        MultimodalModelProvider def = getDefault();
        if (def != null) chain.add(def);
        // 3. 再加 fallback 链 (按配置顺序)
        for (String rawName : fallbackChain.split(",")) {
            // 4. trim 去掉空格 (复制到 final 变量供 lambda 使用)
            final String name = rawName.trim();
            // 5. 跳过空和已添加
            if (name.isEmpty() || chain.stream().anyMatch(p -> p.name().equals(name))) {
                continue;
            }
            // 6. 添加 (如果注册了)
            MultimodalModelProvider p = get(name);
            if (p != null) chain.add(p);
        }
        return chain;
    }

    /**
     * 列出所有已注册 provider (用于 /api/v1/multimodal/info)
     */
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MultimodalModelProvider p : providers.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", p.name());                  // 名
            info.put("description", p.description());    // 描述
            info.put("ready", p.isReady());              // 是否就绪
            // 4. 如果是 ONNX provider, 附加模型信息
            if (p instanceof LocalOnnxVisionProvider) {
                info.put("modelInfo", ((LocalOnnxVisionProvider) p).modelInfo());
            }
            list.add(info);
        }
        return list;
    }

    /**
     * 已注册 provider 数量
     */
    public int size() {
        return providers.size();
    }
}
