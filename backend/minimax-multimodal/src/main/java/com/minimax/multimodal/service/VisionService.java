package com.minimax.multimodal.service;

import com.minimax.multimodal.provider.MultimodalModelProvider;
import com.minimax.multimodal.provider.MultimodalModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 视觉服务 (V3.0.1 重构) - 委托给 registry 路由
 *
 * <p>本类不再关心具体模型实现, 仅负责:
 *   1. 从 registry 获取 provider
 *   2. 调用 provider 处理请求
 *   3. 失败时按降级链重试
 *
 * <p>添加新模型只需:
 *   1. 实现 MultimodalModelProvider 接口
 *   2. 加 @Component 注解
 *   3. (可选) 在 application.yml 配置 minimax.multimodal.provider=your-name
 */
@Slf4j
@Service
@RequiredArgsConstructor  // Lombok: 自动生成构造器注入
public class VisionService {

    /** Provider 注册表 (Spring 注入) */
    private final MultimodalModelRegistry registry;

    /**
     * 单图理解 — 委托给指定 provider, 失败时降级
     *
     * @param imageBase64 base64 编码
     * @param mimeType    MIME 类型
     * @param prompt      用户提示
     * @param modelName   模型名 (null 时用默认)
     * @return 描述文本 (所有 provider 都失败时返回 mock 兜底)
     */
    public String describe(String imageBase64, String mimeType, String prompt, String modelName) {
        // 1. 获取降级链 (指定 model 在前, fallback 在后)
        List<MultimodalModelProvider> chain = resolveChain(modelName);
        // 2. 防御: 没有任何 provider 可用
        if (chain.isEmpty()) {
            log.error("[vision] 没有可用的 provider");
            return "无可用视觉模型";
        }
        // 3. 依次尝试, 第一个成功的胜出
        Exception lastError = null;  // 记录最后一个错误
        for (MultimodalModelProvider p : chain) {
            try {
                // 3a. 调用 provider
                String result = p.describe(imageBase64, mimeType, prompt);
                // 3b. 成功: 记录 + 返回
                if (result != null && !result.isBlank()) {
                    log.debug("[vision] 使用 provider '{}' 成功", p.name());
                    return result;
                }
            } catch (Exception e) {
                // 3c. 失败: 记录, 继续降级
                log.warn("[vision] provider '{}' 失败: {}, 尝试下一个", p.name(), e.getMessage());
                lastError = e;
            }
        }
        // 4. 所有都失败: 返回 mock 兜底
        log.error("[vision] 所有 provider 都失败, last error: {}", lastError != null ? lastError.getMessage() : "n/a");
        return "视觉模型暂不可用, 请稍后重试";
    }

    /**
     * 单图理解 (用默认 provider)
     * 重载方法, 保持向后兼容
     */
    public String describe(String imageBase64, String mimeType, String prompt) {
        return describe(imageBase64, mimeType, prompt, null);  // null = 用默认
    }

    /**
     * 多图理解
     */
    public String describeMulti(List<Map<String, String>> images, String prompt, String modelName) {
        // 1. 解析降级链
        List<MultimodalModelProvider> chain = resolveChain(modelName);
        // 2. 空链保护
        if (chain.isEmpty()) return "无可用视觉模型";
        // 3. 依次尝试
        for (MultimodalModelProvider p : chain) {
            try {
                return p.describeMulti(images, prompt);
            } catch (Exception e) {
                log.warn("[vision] provider '{}' 多图失败: {}", p.name(), e.getMessage());
            }
        }
        return "多图理解失败";
    }

    /**
     * 多图理解重载
     */
    public String describeMulti(List<Map<String, String>> images, String prompt) {
        return describeMulti(images, prompt, null);
    }

    /**
     * 解析降级链
     *
     * @param preferredName 用户指定的 provider (可为 null)
     * @return 降级链 (指定 → fallback)
     */
    private List<MultimodalModelProvider> resolveChain(String preferredName) {
        // 1. 准备结果列表
        java.util.List<MultimodalModelProvider> chain = new java.util.ArrayList<>();
        // 2. 优先加入用户指定的
        if (preferredName != null && !preferredName.isBlank()) {
            MultimodalModelProvider p = registry.get(preferredName);
            if (p != null) {
                chain.add(p);
            } else {
                log.warn("[vision] 请求的 provider '{}' 不存在, 使用默认", preferredName);
            }
        }
        // 3. 加入 registry 的降级链 (去重)
        for (MultimodalModelProvider p : registry.getFallbackChain()) {
            if (chain.stream().noneMatch(x -> x.name().equals(p.name()))) {
                chain.add(p);
            }
        }
        return chain;
    }

    /**
     * 图片基础信息 (用第一个可用的 provider)
     */
    public Map<String, Object> inspect(String imageBase64) {
        // 委托给默认 provider 的 inspect
        MultimodalModelProvider p = registry.getDefault();
        return p != null ? p.inspect(imageBase64) : Map.of("error", "no provider");
    }
}
