package com.minimax.multimodal.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 视觉提供者 (V3.0.1)
 *
 * <p>用途: 开发和测试, 无需任何外部依赖
 * <p>行为: 基于 base64 长度 + mime type 推测图片特征, 返回固定格式的描述
 */
@Slf4j  // Lombok: 自动生成 logger (log 字段)
@Component  // Spring: 注册为 Bean, registry 自动扫描
public class MockVisionProvider implements MultimodalModelProvider {

    /**
     * Provider 唯一名
     * @return 固定 "mock"
     */
    @Override
    public String name() {
        return "mock";  // 调用方用 "mock" 名字路由到此实现
    }

    /**
     * Provider 描述
     * @return 中文描述
     */
    @Override
    public String description() {
        return "Mock 模式 (无外部依赖, 基于图片大小推测)";
    }

    /**
     * Mock 永远就绪
     * @return true
     */
    @Override
    public boolean isReady() {
        return true;  // 不依赖任何配置, 始终可用
    }

    /**
     * 生成 mock 描述
     *
     * @param b64  base64 编码的图片 (可能为 null)
     * @param mime MIME 类型 (e.g. "image/png")
     * @param prompt 用户提示
     * @return 格式化的 mock 描述
     */
    @Override
    public String describe(String b64, String mime, String prompt) {
        // 1. 防御: b64 为空时, 返回简洁的提示
        if (b64 == null || b64.isBlank()) {
            return "【Mock】无图片数据";
        }

        // 2. base64 → 原始字节数 (base64 每 4 字符编码 3 字节)
        int approxBytes = b64.length() * 3 / 4;

        // 3. 按大小分档 (小/中/大/超大)
        String size;  // 大小档位字符串
        if (approxBytes < 50_000) {           // < 50KB
            size = "小 (缩略图)";
        } else if (approxBytes < 500_000) {   // 50KB ~ 500KB
            size = "中等";
        } else if (approxBytes < 5_000_000) { // 500KB ~ 5MB
            size = "大";
        } else {                              // > 5MB
            size = "超大";
        }

        // 4. 组装格式化字符串
        return String.format(
            "【视觉模型 Mock 模式】\n" +          // 标题
            "类型: %s\n" +                       // MIME 类型
            "尺寸: %s (约 %d KB)\n" +            // 尺寸档位 + KB 数
            "提示: %s\n\n" +                     // 用户提示
            "这是 mock 响应, 不调用真实模型。\n" + // 说明
            "如需真实模型, 请设置 minimax.multimodal.provider=builtin/openai/local",
            mime == null ? "image/unknown" : mime,  // 兜底默认值
            size,                                    // 档位
            approxBytes / 1024,                      // KB
            prompt == null ? "(无提示)" : prompt     // 兜底默认
        );
    }

    /**
     * 多图 mock: 简单拼接每张图的描述
     */
    @Override
    public String describeMulti(List<Map<String, String>> images, String prompt) {
        // 1. 空列表保护
        if (images == null || images.isEmpty()) {
            return "【Mock】无图片数据";
        }
        // 2. 用 StringBuilder 拼接, 避免 String 多次 + 拼接产生中间对象
        StringBuilder sb = new StringBuilder();
        sb.append("【Mock 多图模式】共 ").append(images.size()).append(" 张\n");  // 头部
        // 3. 遍历每张图, 加序号
        for (int i = 0; i < images.size(); i++) {
            Map<String, String> img = images.get(i);  // 当前图
            // 4. 递归调用 describe, 获取单图描述
            sb.append(i + 1).append(". ")
              .append(describe(img.get("base64"), img.get("mimeType"), prompt))
              .append("\n");
        }
        return sb.toString();  // 返回拼接结果
    }

    /**
     * 图片基础信息 (不依赖具体模型, 共享逻辑)
     */
    @Override
    public Map<String, Object> inspect(String b64) {
        // 委托给工具类 (避免重复代码, 多个 provider 共享)
        return ImageInspector.inspect(b64);
    }
}
