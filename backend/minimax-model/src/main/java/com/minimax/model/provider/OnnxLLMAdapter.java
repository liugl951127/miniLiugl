package com.minimax.model.provider;

import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;

/**
 * 自研 ONNX 推理适配器 (V6.8.2)
 *
 * <h3>职责</h3>
 * 直接加载本地 ONNX 格式的自研模型文件，通过 MiniTransformer 执行推理，
 * 不依赖 Ollama/vLLM 等外部服务器。适用于已导出为 ONNX 的自研模型。
 *
 * <h3>使用方式</h3>
 * <pre>
 * 1. 在 model_provider 表插入:
 *    code = 'onnx', protocol = 'onnx', base_url = '/path/to/models',
 *    enabled = 1
 * 2. 在 model_config 表插入模型:
 *    model_code = 'my-chatgpt', provider_id = <上一步的id>
 *    max_context = 4096, max_output = 2048
 * 3. endpoint = base_url (模型目录)，apiKey = 固定 token 或 null
 * </pre>
 *
 * <h3>endpoint 格式</h3>
 * 约定 endpoint = 模型文件所在目录路径，apiKey = 模型名称（不含 .onnx 后缀）。
 * 实际模型文件路径 = endpoint + "/" + apiKey + ".onnx"
 * 若 apiKey 为空，则 endpoint 直接是 .onnx 文件路径。
 *
 * <h3>Generation</h3>
 * 自回归采样:
 * <pre>
 *   while (generated < maxTokens && !eos):
 *     logits = model.forward(tokens)       // 前向
 *     logits[-1] /= temperature            // 温度
 *     probs = top_p_sample(logits[-1], p)  // nucleus
 *     next_id = multinomial(probs)         // 采样
 *     tokens.append(next_id)
 *     yield decode(tokens)
 * </pre>
 *
 * <h3>V6.8.3 反射调用说明</h3>
 * 由于 minimax-model 与 minimax-ai 存在循环依赖，本类通过反射调用
 * MiniTransformer 和 ChineseTokenizer，避免编译时依赖。
 */
@Slf4j
@Component
public class OnnxLLMAdapter implements ModelProviderAdapter {

    /** 单例 token 缓存 (通过反射加载 ChineseTokenizer) */
    private static Object tokenizer;

    /** endpoint(目录) → 缓存的 MiniTransformer 实例 (通过反射加载) */
    private final Map<String, Object> modelCache = new ConcurrentHashMap<>();

    @Override
    public String code() { return "onnx"; }

    @Override
    public ChatResponse chat(String endpoint, String apiKey, ChatRequest req) {
        long start = System.currentTimeMillis();
        String modelPath = resolveModelPath(endpoint, apiKey);
        Object model = loadModel(modelPath);
        if (model == null) {
            return ChatResponse.builder()
                    .content("模型加载失败: " + modelPath)
                    .finishReason("error")
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
        String prompt = buildPrompt(req.getMessages());
        double temperature = getTemperature(req);
        int maxTokens = getMaxTokens(req, getModelMaxSeqLen(model));
        StringBuilder reply = new StringBuilder();
        try {
            Object result = invokeGenerate(model, prompt, temperature, maxTokens, 0.9);
            String text = getResultText(result);
            int promptTokens = getResultPromptTokens(result);
            int completionTokens = getResultCompletionTokens(result);
            boolean eos = getResultEos(result);
            return ChatResponse.builder()
                    .model(req.getModel())
                    .content(text)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .finishReason(eos ? "stop" : "length")
                    .latencyMs(System.currentTimeMillis() - start)
                    .providerCode(code())
                    .build();
        } catch (Exception e) {
            log.error("[OnnxAdapter] 生成失败: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .content("ONNX 推理异常: " + e.getMessage())
                    .finishReason("error")
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @Override
    public OpenAiCompatibleAdapter.StreamResult streamChat(String endpoint, String apiKey, ChatRequest req,
                                   Consumer<String> chunkJsonConsumer,
                                   AtomicBoolean stopFlag) {
        long start = System.currentTimeMillis();
        String modelPath = resolveModelPath(endpoint, apiKey);
        Object model = loadModel(modelPath);
        if (model == null) {
            chunkJsonConsumer.accept("{\"error\":\"模型加载失败: " + modelPath + "\"}");
            return new OpenAiCompatibleAdapter.StreamResult(null, null, null, 0, 0, 0, "error", System.currentTimeMillis() - start);
        }
        String prompt = buildPrompt(req.getMessages());
        double temperature = getTemperature(req);
        int maxTokens = getMaxTokens(req, getModelMaxSeqLen(model));
        StringBuilder full = new StringBuilder();
        int completionTokens = 0;
        try {
            Object result = invokeGenerate(model, prompt, temperature, maxTokens, 0.9);
            String text = getResultText(result);
            int promptTokens = getResultPromptTokens(result);
            for (int i = 0; i < text.length(); i++) {
                if (stopFlag.get()) break;
                char c = text.charAt(i);
                full.append(c);
                completionTokens++;
                String chunk = "{\"choices\":[{\"delta\":{\"content\":\"" + escapeJson(String.valueOf(c)) + "\"},\"index\":0}]}";
                chunkJsonConsumer.accept(chunk);
            }
            return new OpenAiCompatibleAdapter.StreamResult(null, null, full.toString(),
                    promptTokens,
                    completionTokens,
                    promptTokens + completionTokens,
                    "stop",
                    System.currentTimeMillis() - start
            );
        } catch (Exception e) {
            log.error("[OnnxAdapter] 流式推理失败: {}", e.getMessage());
            chunkJsonConsumer.accept("{\"error\":\"ONNX 推理异常: " + escapeJson(e.getMessage()) + "\"}");
            return new OpenAiCompatibleAdapter.StreamResult(null, null, full.toString(), 0, completionTokens, completionTokens, "error", System.currentTimeMillis() - start);
        }
    }

    // ========== 反射调用 MiniTransformer ==========

    private Object loadModel(String modelPath) {
        return modelCache.computeIfAbsent(modelPath, path -> {
            File f = new File(path);
            if (!f.exists()) {
                log.error("[OnnxAdapter] 模型文件不存在: {}", path);
                return null;
            }
            try {
                log.info("[OnnxAdapter] 加载模型: {}", path);
                Class<?> clazz = Class.forName("com.minimax.ai.model.MiniTransformer");
                Constructor<?> ctor = clazz.getConstructor(String.class);
                Object m = ctor.newInstance(path);
                Method getVocabSize = clazz.getMethod("getVocabSize");
                Method getLayers = clazz.getMethod("getLayers");
                log.info("[OnnxAdapter] 模型加载成功: {} (vocab={}, layers={})",
                        path, getVocabSize.invoke(m), getLayers.invoke(m));
                return m;
            } catch (Exception e) {
                log.error("[OnnxAdapter] 模型加载失败: {}: {}", path, e.getMessage());
                return null;
            }
        });
    }

    private int getModelMaxSeqLen(Object model) {
        try {
            Method m = model.getClass().getMethod("getMaxSeqLen");
            return (int) m.invoke(model);
        } catch (Exception e) {
            log.warn("[OnnxAdapter] 获取 maxSeqLen 失败，使用默认值 2048");
            return 2048;
        }
    }

    private Object invokeGenerate(Object model, String prompt, double temperature, int maxTokens, double topP) throws Exception {
        Method generate = model.getClass().getMethod("generate", String.class, double.class, int.class, double.class);
        return generate.invoke(model, prompt, temperature, maxTokens, topP);
    }

    private String getResultText(Object result) throws Exception {
        return (String) result.getClass().getField("text").get(result);
    }

    private int getResultPromptTokens(Object result) throws Exception {
        return (int) result.getClass().getField("promptTokens").get(result);
    }

    private int getResultCompletionTokens(Object result) throws Exception {
        return (int) result.getClass().getField("completionTokens").get(result);
    }

    private boolean getResultEos(Object result) throws Exception {
        return (boolean) result.getClass().getField("eos").get(result);
    }

    // ========== 模型路径解析 ==========

    private String resolveModelPath(String endpoint, String apiKey) {
        if (endpoint == null) return apiKey;
        endpoint = endpoint.trim();
        if (endpoint.endsWith(".onnx")) return endpoint;
        if (apiKey != null && !apiKey.isBlank()) {
            String base = endpoint.replaceAll("/+$", "");
            if (apiKey.endsWith(".onnx")) return base + "/" + apiKey;
            return base + "/" + apiKey + ".onnx";
        }
        return endpoint;
    }

    // ========== Prompt 构建 ==========

    private String buildPrompt(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) return "";
        String roleMap = messages.stream()
                .filter(m -> m.get("role") != null)
                .map(m -> {
                    String role = (String) m.get("role");
                    Object c = m.get("content");
                    String content = c != null ? c.toString() : "";
                    if ("system".equals(role)) return "系统: " + content;
                    if ("user".equals(role)) return "用户: " + content;
                    if ("assistant".equals(role)) return "助手: " + content;
                    return content;
                })
                .collect(Collectors.joining("\n"));
        return roleMap + "\n助手: ";
    }

    private double getTemperature(ChatRequest req) {
        if (req.getTemperature() != null) return req.getTemperature();
        return 0.7;
    }

    private int getMaxTokens(ChatRequest req, int modelMax) {
        if (req.getMaxTokens() != null && req.getMaxTokens() > 0) {
            return Math.min(req.getMaxTokens(), modelMax);
        }
        return Math.min(2048, modelMax);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public Flux<String> stream(String endpoint, String apiKey, ChatRequest req) {
        return Flux.error(new UnsupportedOperationException("ONNX stream 通过 streamChat() 调用"));
    }

    @Override
    public boolean ping(String endpoint, String apiKey) {
        String path = resolveModelPath(endpoint, apiKey);
        File f = new File(path);
        if (!f.exists()) return false;
        try {
            Class<?> clazz = Class.forName("com.minimax.ai.model.MiniTransformer");
            Constructor<?> ctor = clazz.getConstructor(String.class);
            ctor.newInstance(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
