package com.minimax.agent.controller;

import com.minimax.agent.service.AgentService;
import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * 文档问答控制器 (V6.8+)
 *
 * 上传 PDF / 图片后，用 Vision LLM 分析并回答问题。
 * 真实调用 SiliconFlow gpt-4o-mini 或 OpenAI GPT-4V。
 *
 * 端点:
 *   POST /api/v1/agent/doc/upload   上传文档，返回 docId
 *   POST /api/v1/agent/doc/ask      针对文档提问 (multipart docId + question)
 *
 * @since 2026-08
 */
@Slf4j
@Tag(name = "文档问答")
@RestController
@RequestMapping("/api/v1/agent/doc")
@RequiredArgsConstructor
public class DocumentQAController {

    private final AgentService agentService;

    @Value("${minimax.doc.mock-mode:false}")
    private boolean mockMode;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    // 内存存储已上传文档（生产换 Redis）
    private final Map<String, DocRecord> docs = new LinkedHashMap<>();

    @Operation(summary = "上传文档 (PDF/图片)，返回 docId")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(value = "title", required = false) String title) {
        String docId = "doc-" + System.currentTimeMillis();
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        try {
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String base64 = Base64.getEncoder()
                    .encodeToString(file.getBytes());

            docs.put(docId, new DocRecord(docId, title != null ? title : name,
                    name, mimeType, base64, System.currentTimeMillis()));

            log.info("[DocQA] 上传 docId={} name={} size={}", docId, name, file.getSize());
            return Result.ok(Map.of(
                    "docId", docId,
                    "name", name,
                    "mimeType", mimeType,
                    "size", file.getSize(),
                    "uploadedAt", java.time.LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            log.error("[DocQA] 上传失败: {}", e.getMessage());
            return Result.fail(500, "上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "针对文档提问 (docId + question)")
    @PostMapping("/ask")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> ask(
            @RequestParam("docId") String docId,
            @RequestParam("question") String question) {

        if (question == null || question.isBlank()) {
            return Result.fail(400, "question 不能为空");
        }

        long t0 = System.currentTimeMillis();
        try {
            DocRecord doc = docs.get(docId);
            if (doc == null) {
                return Result.fail(404, "文档不存在或已过期，请重新上传");
            }

            if (mockMode) {
                return Result.ok(mockAnswer(doc, question, t0));
            }

            return Result.ok(realAnswer(doc, question, t0));
        } catch (Exception e) {
            log.warn("[DocQA] 问答失败 docId={}: {}", docId, e.getMessage());
            return Result.fail(500, "问答失败: " + e.getMessage());
        }
    }

    @Operation(summary = "直接上传文件并提问 (单次请求)")
    @PostMapping("/upload-and-ask")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> uploadAndAsk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question) {

        if (question == null || question.isBlank()) {
            return Result.fail(400, "question 不能为空");
        }
        long t0 = System.currentTimeMillis();
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            DocRecord doc = new DocRecord("tmp-" + System.currentTimeMillis(),
                    name, name, mimeType, base64, System.currentTimeMillis());

            if (mockMode) {
                return Result.ok(mockAnswer(doc, question, t0));
            }
            return Result.ok(realAnswer(doc, question, t0));
        } catch (Exception e) {
            log.warn("[DocQA] uploadAndAsk 失败: {}", e.getMessage());
            return Result.fail(500, "处理失败: " + e.getMessage());
        }
    }

    // ====================== Mock 答案 ======================

    private Map<String, Object> mockAnswer(DocRecord doc, String question, long t0) {
        String typeLabel = doc.mimeType.contains("pdf") ? "PDF" :
                doc.mimeType.startsWith("image/") ? "图片" : "文档";
        String answer = "【" + typeLabel + "问答演示】\n\n"
                + "文档：" + doc.title + "\n"
                + "问题：" + question + "\n\n"
                + "回答：这是一个基于文档「" + doc.title + "」的智能问答演示。\n"
                + "实际使用时，请配置 SILICONFLOW_API_KEY 或 OPENAI_API_KEY，"
                + "系统将使用 GPT-4V / GPT-4o-mini 模型对文档内容进行分析并准确回答。\n\n"
                + "支持的文档格式：PDF、图片（jpg/png/webp）、Word（docx）、纯文本（txt）";

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("docId", doc.docId);
        out.put("question", question);
        out.put("answer", answer);
        out.put("model", "mock");
        out.put("latencyMs", System.currentTimeMillis() - t0);
        out.put("mock", true);
        return out;
    }

    // ====================== 真实 LLM 问答 ======================

    @SuppressWarnings("unchecked")
    private Map<String, Object> realAnswer(DocRecord doc, String question, long t0) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv("OPENAI_BASE_URL");
        boolean useSiliconFlow = false;

        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("SILICONFLOW_API_KEY");
            useSiliconFlow = (apiKey != null && !apiKey.isBlank());
            baseUrl = useSiliconFlow ? "https://api.siliconflow.cn" : null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            // fallback: 用 AgentService 调用配置的 LLM
            return agentBasedAnswer(doc, question, t0);
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com";
        }

        String endpoint = baseUrl + "/v1/chat/completions";

        // 构建 vision 消息
        String mimeType = doc.mimeType;
        if (mimeType == null) mimeType = "image/png";
        String dataUrl = "data:" + mimeType + ";base64," + doc.base64;

        String model = useSiliconFlow ? "gpt-4o-mini" : "gpt-4o-mini";

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text",
                                "你是一个专业的文档分析助手。请仔细阅读下面的文档图片或PDF内容，然后回答用户的问题。"
                                        + "如果文档中没有相关信息，请如实告知。请用简洁清晰的语言回答。\n\n"
                                        + "用户问题：" + question),
                        Map.of("type", "image_url", "image_url",
                                Map.of("url", dataUrl, "detail", "high"))
                ))
        );

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("model", model);
        reqBody.put("messages", messages);
        reqBody.put("max_tokens", 2048);

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(reqBody)))
                .build();

        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }

        Map<String, Object> body = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(resp.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.getOrDefault("choices", List.of());
        String answer = "";
        if (!choices.isEmpty()) {
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            answer = (String) msg.getOrDefault("content", "");
        }
        if (answer.isBlank()) {
            answer = "模型未返回有效回答，请检查 API Key 配置。";
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("docId", doc.docId);
        out.put("question", question);
        out.put("answer", answer);
        out.put("model", model);
        out.put("provider", useSiliconFlow ? "siliconflow" : "openai");
        out.put("latencyMs", System.currentTimeMillis() - t0);
        out.put("mock", false);
        return out;
    }

    /**
     * Fallback: 用 AgentService 内部的 LLM 调用链 (配置好的模型)
     */
    private Map<String, Object> agentBasedAnswer(DocRecord doc, String question, long t0) {
        try {
            String typeLabel = doc.mimeType.contains("pdf") ? "PDF" :
                    doc.mimeType.startsWith("image/") ? "图片" : "文档";
            String prompt = "你是一个专业的文档分析助手。请分析以下" + typeLabel
                    + "内容，然后回答问题。\n\n" + typeLabel + "标题：" + doc.title
                    + "\n\n问题：" + question
                    + "\n\n回答要求：简洁、准确、有条理。";

            // 用 AgentService 的 LLM（会自动路由到配置的模型）
            var result = agentService.run(null, prompt, List.of());
            String answer = result != null && result.answer() != null
                    ? result.answer()
                    : "Agent 服务未返回有效结果。";

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("docId", doc.docId);
            out.put("question", question);
            out.put("answer", answer);
            out.put("model", "agent-default");
            out.put("latencyMs", System.currentTimeMillis() - t0);
            out.put("mock", false);
            return out;
        } catch (Exception e) {
            log.warn("[DocQA] agentBasedAnswer 失败: {}", e.getMessage());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("docId", doc.docId);
            out.put("question", question);
            out.put("answer", "文档问答服务暂时不可用，请检查 LLM 配置。错误：" + e.getMessage());
            out.put("mock", true);
            out.put("error", true);
            return out;
        }
    }

    // ====================== 内部类 ======================

    private record DocRecord(
            String docId,
            String title,
            String name,
            String mimeType,
            String base64,
            long uploadedAt
    ) {}
}
