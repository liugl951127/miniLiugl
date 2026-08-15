package com.minimax.multimodal.provider;

import java.util.List;
import java.util.Map;

/**
 * 多模态模型提供者统一接口 (V3.0.1 自研模型适配器)
 *
 * <p>设计目的: 让多模态模块支持插件化接入任意模型, 包括但不限于:
 *   - 自研像素分析 (BuiltinPixelProvider, 纯 Java 实现)
 *   - OpenAI gpt-4o / gpt-4-vision (HTTP 协议)
 *   - 本地 ONNX 模型 (LocalOnnxProvider, JNI 调用)
 *   - Mock 模式 (用于开发/测试)
 *
 * <p>调用方 (VisionService) 不感知具体实现, 仅通过 name 选择 provider.
 *
 * <h3>扩展方式</h3>
 * <ol>
 *   <li>实现本接口</li>
 *   <li>加 {@code @Component} 注解</li>
 *   <li>Spring 自动扫描, registry 自动注册</li>
 *   <li>请求参数 {@code "model": "your-name"} 即可使用</li>
 * </ol>
 *
 * <h3>线程安全</h3>
 * 实现必须是线程安全的, registry 在多线程请求下会调用同一 provider 实例.
 * 建议: 无状态, 或使用 ThreadLocal / synchronized 块.
 *
 * @author MiniMax
 * @since V3.0.1
 */
public interface MultimodalModelProvider {

    /**
     * Provider 唯一名 (registry 用此名路由)
     *
     * <p>命名规范: 小写 + 连字符, e.g. "builtin", "openai", "local-onnx", "mock"
     *
     * @return provider name
     */
    String name();

    /**
     * 描述 (用于 /api/v1/multimodal/info 接口展示)
     *
     * @return 简短描述, e.g. "内置像素分析 (无外部依赖)"
     */
    String description();

    /**
     * 是否就绪 (e.g. API key 是否配置, 模型文件是否存在)
     *
     * @return true 表示可以处理请求
     */
    boolean isReady();

    /**
     * 图片理解 (单图)
     *
     * @param imageBase64 原始图片 base64 (无 data: 前缀)
     * @param mimeType    image/png / image/jpeg
     * @param prompt      用户文字提示
     * @return 模型生成的描述文本
     * @throws Exception 处理失败时抛异常, 调用方会降级到 fallback provider
     */
    String describe(String imageBase64, String mimeType, String prompt) throws Exception;

    /**
     * 多图理解 (可选实现, 默认降级到单图)
     *
     * @param images 图片列表, 每项含 base64 + mimeType
     * @param prompt 用户提示
     * @return 模型生成的描述
     * @throws Exception 处理失败
     */
    default String describeMulti(List<Map<String, String>> images, String prompt) throws Exception {
        // 默认实现: 取第一张图, 走单图接口
        if (images == null || images.isEmpty()) {
            return "(无图片)";
        }
        Map<String, String> first = images.get(0);
        return describe(first.get("base64"), first.get("mimeType"), prompt);
    }

    /**
     * 图片基础信息 (从原始字节解析, 不依赖具体模型)
     *
     * @param imageBase64 base64 字符串
     * @return { format, sizeBytes, magic, ... }
     */
    Map<String, Object> inspect(String imageBase64);
}
