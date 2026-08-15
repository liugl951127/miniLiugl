package com.minimax.multimodal.controller;

import com.minimax.common.result.Result;
import com.minimax.multimodal.provider.MultimodalModelRegistry;
import com.minimax.multimodal.service.VisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 多模态控制器 (V3.0.1 升级: 支持多 provider)
 *
 * <p>API 列表 (统一 /api/v1/multimodal 前缀):
 * <ul>
 *   <li>POST /upload          上传图片 (返回 base64 + 元信息)</li>
 *   <li>POST /describe        文字+图片 → 描述 (可指定 model)</li>
 *   <li>POST /describe/multi  多图对比</li>
 *   <li>GET  /info            所有可用 provider 列表</li>
 *   <li>GET  /providers       provider 详情</li>
 * </ul>
 *
 * <h3>model 参数</h3>
 * 请求体可指定 {@code "model": "builtin|openai|local-onnx|mock"}
 * 不传时用 application.yml 中 minimax.multimodal.provider 配置
 */
@Tag(name = "多模态")
@RestController
@RequestMapping("/api/v1/multimodal")  // 统一 /api/v1 前缀
@RequiredArgsConstructor  // Lombok: 构造器注入所有 final 字段
public class MultimodalController {

    /** 视觉服务 (委托给 registry) */
    private final VisionService vision;

    /** Provider 注册表 (用于 /info 接口) */
    private final MultimodalModelRegistry registry;

    /**
     * 上传图片
     *
     * @param file Multipart 文件
     * @return Result {filename, mimeType, sizeBytes, base64, info, dataUrl}
     */
    @Operation(summary = "上传图片（返回 base64 + 元信息）")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        // 1. 空文件保护
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        // 2. 读取字节
        byte[] bytes = file.getBytes();
        // 3. 大小限制: 20MB (硬编码, 也可通过 @Value 注入)
        if (bytes.length > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("文件超过 20MB (实际: " + bytes.length + " 字节)");
        }
        // 4. 推断 MIME (优先用上传时的 Content-Type, 兜底 image/png)
        String mime = file.getContentType() == null ? "image/png" : file.getContentType();
        // 5. base64 编码
        String b64 = Base64.getEncoder().encodeToString(bytes);
        // 6. 调用 vision.inspect 获取图片元信息
        Map<String, Object> info = vision.inspect(b64);

        // 7. 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("filename", file.getOriginalFilename());   // 原始文件名
        data.put("mimeType", mime);                          // MIME 类型
        data.put("sizeBytes", bytes.length);                 // 字节数
        data.put("sizeKB", bytes.length / 1024);             // KB
        data.put("base64", b64);                             // base64 字符串
        data.put("info", info);                              // 图片元信息
        data.put("dataUrl", "data:" + mime + ";base64," + b64);  // data URL (可直接放 <img src>)
        return Result.ok(data);
    }

    /**
     * 单图理解 (支持指定 model)
     *
     * @param body { imageBase64, mimeType?, prompt?, model? }
     * @return { description, durationMs, model, info }
     */
    @Operation(summary = "图片理解（文字+图片→描述）")
    @PostMapping("/describe")
    public Result<Map<String, Object>> describe(@RequestBody Map<String, String> body) {
        // 1. 解析请求参数
        String b64 = body.get("imageBase64");                    // base64
        String mime = body.getOrDefault("mimeType", "image/png"); // MIME (默认 png)
        String prompt = body.getOrDefault("prompt", "请描述这张图片"); // 用户提示
        String model = body.get("model");                        // 指定 provider (可选)

        // 2. 计时
        long t0 = System.currentTimeMillis();
        // 3. 调用 vision (内部按降级链尝试)
        String description = vision.describe(b64, mime, prompt, model);
        long dur = System.currentTimeMillis() - t0;

        // 4. 推断实际使用的 provider (从 description 前缀识别)
        String usedModel = inferProvider(description);

        // 5. 组装返回
        Map<String, Object> r = new HashMap<>();
        r.put("description", description);                        // 描述文本
        r.put("durationMs", dur);                                 // 耗时 (ms)
        r.put("prompt", prompt);                                  // 原始 prompt
        r.put("model", model == null ? usedModel : model);        // 使用的 provider
        r.put("info", vision.inspect(b64));                        // 图片元信息
        return Result.ok(r);
    }

    /**
     * 多图理解
     *
     * @param body { images: [{base64, mimeType}], prompt?, model? }
     */
    @Operation(summary = "多图理解（对比多张图片）")
    @PostMapping("/describe/multi")
    public Result<Map<String, Object>> describeMulti(@RequestBody Map<String, Object> body) {
        // 1. 解析参数
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> images =
                (java.util.List<Map<String, String>>) body.get("images");
        String prompt = (String) body.getOrDefault("prompt", "请对比这些图片");
        String model = (String) body.get("model");

        // 2. 调用
        long t0 = System.currentTimeMillis();
        String result = vision.describeMulti(images, prompt, model);
        long dur = System.currentTimeMillis() - t0;

        // 3. 返回
        Map<String, Object> r = new HashMap<>();
        r.put("description", result);
        r.put("durationMs", dur);
        r.put("imageCount", images == null ? 0 : images.size());
        return Result.ok(r);
    }

    /**
     * 多模态模型信息 (所有 provider + 当前默认)
     */
    @Operation(summary = "多模态模型信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        // 1. 返回结构
        Map<String, Object> r = new HashMap<>();
        // 2. 所有 provider 列表
        r.put("providers", registry.listAll());
        // 3. provider 总数
        r.put("totalProviders", registry.size());
        // 4. 兜底链
        r.put("fallbackChain", registry.getFallbackChain().stream()
                .map(p -> Map.of("name", p.name(), "ready", p.isReady()))
                .toList());
        // 5. 支持的格式
        r.put("supportedFormats", java.util.List.of("png", "jpeg", "gif", "webp", "bmp"));
        // 6. 大小限制
        r.put("maxSize", "20MB");
        return Result.ok(r);
    }

    /**
     * 当前默认 provider 详情
     */
    @Operation(summary = "当前默认 provider")
    @GetMapping("/providers/default")
    public Result<Map<String, Object>> defaultProvider() {
        var p = registry.getDefault();
        Map<String, Object> r = new HashMap<>();
        if (p == null) {
            r.put("error", "no provider available");
        } else {
            r.put("name", p.name());
            r.put("description", p.description());
            r.put("ready", p.isReady());
        }
        return Result.ok(r);
    }

    /**
     * 从描述前缀推断 provider (兼容旧版本 API)
     *
     * <p>各 provider 描述以 "【xxx】" 开头, 此方法提取 xxx
     */
    private String inferProvider(String desc) {
        if (desc == null || !desc.startsWith("【")) {
            return "unknown";
        }
        int end = desc.indexOf("】");
        if (end < 0) return "unknown";
        return desc.substring(1, end);
    }
}
