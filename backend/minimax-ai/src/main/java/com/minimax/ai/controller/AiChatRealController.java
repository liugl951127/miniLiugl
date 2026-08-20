package com.minimax.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.AiChatSession;
import com.minimax.ai.entity.AiChatMessage;
import com.minimax.ai.mapper.AiChatSessionMapper;
import com.minimax.ai.mapper.AiChatMessageMapper;
import com.minimax.ai.service.AgentClient;
import com.minimax.common.feign.model.ChatRequestDTO;
import com.minimax.common.feign.model.ChatResponseDTO;
import com.minimax.common.result.Result;
import com.minimax.ai.service.ModelClient;
import com.minimax.ai.service.OnnxLLMService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * AI Chat 真实业务控制器 (V6.5+, V6.8.2 SSE 真实推理)
 *
 * <h2>真实推理</h2>
 * <p>V6.8.2: /stream 改为 SSE 流式输出（text/event-stream），
 * 匹配前端 useSSEStream 协议：data:{"type":"content","content":"..."}</p>
 *
 * @author Mavis
 * @since V6.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/chat")
public class AiChatRealController {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final ModelClient modelClient;
    private final RestTemplate ragRestTemplate;
    private final AgentClient agentClient;
    private final OnnxLLMService onnxLLMService;

    @Value("${minimax.rag.service-url:http://localhost:8085}")
    private String ragServiceUrl;

    // 手动构造器注入（包含 @Qualifier("ragRestTemplate")）
    public AiChatRealController(
            AiChatSessionMapper sessionMapper,
            AiChatMessageMapper messageMapper,
            ModelClient modelClient,
            @Qualifier("ragRestTemplate") RestTemplate ragRestTemplate,
            AgentClient agentClient,
            OnnxLLMService onnxLLMService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.modelClient = modelClient;
        this.ragRestTemplate = ragRestTemplate;
        this.agentClient = agentClient;
        this.onnxLLMService = onnxLLMService;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * V7.0: 从 RAG 服务检索知识上下文。
     * 如果 kbId 为 null 或 RAG 服务不可用，返回空串。
     */
    private String buildRagContext(Long kbId, String query) {
        if (kbId == null || kbId <= 0) return "";
        if (ragRestTemplate == null) {
            log.warn("[Chat/RAG] RestTemplate 未配置，跳过 RAG");
            return "";
        }
        try {
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("kbId", kbId);
            reqBody.put("query", query);
            reqBody.put("topK", 5);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            String url = ragServiceUrl + "/api/v1/rag/retrieve";
            ResponseEntity<String> resp = ragRestTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                // 解析 Result<List<Retriever.Hit>>
                Map<String, Object> result = objectMapper.readValue(resp.getBody(), Map.class);
                Object data = result.get("data");
                if (data instanceof List<?> hits) {
                    StringBuilder ctx = new StringBuilder();
                    ctx.append("\n\n【知识库参考内容】\n");
                    int idx = 1;
                    for (Object h : hits) {
                        if (h instanceof Map) {
                            Map<?, ?> hit = (Map<?, ?>) h;
                            Object content = hit.get("content");
                            Object source = hit.get("source");
                            if (content != null) {
                                ctx.append(String.format("[%d] %s%s\n",
                                    idx++,
                                    source != null ? "(" + source + ") " : "",
                                    content.toString().trim()
                                ));
                            }
                        }
                    }
                    String context = ctx.toString();
                    if (context.length() > 30) {
                        log.info("[Chat/RAG] kbId={} 检索到 {} 条结果", kbId, hits.size());
                        return context;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[Chat/RAG] kbId={} 检索失败: {}", kbId, ex.getMessage());
        }
        return "";
    }

    /** 列出聊天会话 */
    @GetMapping("/sessions")
    public Result<List<AiChatSession>> listSessions(@RequestParam(required = false) Long userId) {
        QueryWrapper<AiChatSession> qw = new QueryWrapper<>();
        if (userId != null) qw.eq("user_id", userId);
        qw.orderByDesc("created_at").last("LIMIT 50");
        return Result.ok(sessionMapper.selectList(qw));
    }

    /** 获取单个会话 */
    @GetMapping("/sessions/{id}")
    public Result<AiChatSession> getSession(@PathVariable Long id) {
        AiChatSession s = sessionMapper.selectById(id);
        if (s == null) return Result.error(404, "会话不存在");
        return Result.ok(s);
    }

    /** 获取会话消息 (V7.0: 用 sessionId 字符串查) */
    @GetMapping("/sessions/{id}/messages")
    public Result<List<AiChatMessage>> getMessages(@PathVariable Long id) {
        AiChatSession session = sessionMapper.selectById(id);
        if (session == null) return Result.ok(List.of());
        QueryWrapper<AiChatMessage> qw = new QueryWrapper<>();
        qw.eq("session_id", session.getSessionId()).orderByAsc("created_at");
        return Result.ok(messageMapper.selectList(qw));
    }

    /** 创建会话 (V7.0 支持 kbId + agentId) */
    @PostMapping("/sessions")
    public Result<AiChatSession> createSession(@RequestBody AiChatSession session) {
        session.setId(null);
        // sessionId 格式: 时间戳 + 4位随机数
        session.setSessionId(String.valueOf(System.currentTimeMillis() % 1000000));
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        if (session.getStatus() == null) session.setStatus(1);
        sessionMapper.insert(session);
        return Result.ok(session);
    }

    /** 更新会话 (改名) */
    @PutMapping("/sessions/{id}")
    public Result<Void> updateSession(@PathVariable Long id, @RequestBody AiChatSession session) {
        AiChatSession existing = sessionMapper.selectById(id);
        if (existing == null) return Result.error(404, "会话不存在");
        if (session.getTitle() != null) existing.setTitle(session.getTitle());
        existing.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(existing);
        return Result.ok();
    }

    /** 删除会话 */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        sessionMapper.deleteById(id);
        return Result.ok();
    }

    /** 停止聊天 */
    @PostMapping("/stop")
    public Result<Map<String, Object>> stopChat(@RequestBody Map<String, Object> body) {
        Long sessionId = ((Number) body.getOrDefault("sessionId", 0)).longValue();
        log.info("[Chat] 停止 session={}", sessionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionId", sessionId);
        resp.put("status", "stopped");
        resp.put("stoppedAt", LocalDateTime.now());
        return Result.ok(resp);
    }

    /**
     * 流式聊天 (V6.8.2 → SSE 真实推理, V7.0 +RAG +Agent)。
     *
     * <p>前端 useSSEStream 期望 text/event-stream:
     *   data: {"type":"content","content":"xxx"}
     *   data: {"type":"agent_result","content":"..."}   // V7.0 Agent 结果
     *   data: {"type":"done"}
     *
     * <p>请求体: { message, sessionId, model?, systemPrompt?, kbId?, agentId? }
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, Object> body) {
        String message = (String) body.getOrDefault("message", "");
        String model = (String) body.getOrDefault("model", "deepseek-chat");
        String systemPrompt = (String) body.getOrDefault("systemPrompt",
            "你是一个有帮助的 AI 助手，请简洁，专业地回答用户问题。");
        String streamId = (String) body.getOrDefault("streamId", UUID.randomUUID().toString());

        // V7.0: kbId → RAG 上下文 (必须是 effectively final 才能在 lambda 中使用)
        Object kbIdObj = body.get("kbId");
        final Long kbId;
        if (kbIdObj == null) {
            kbId = null;
        } else if (kbIdObj instanceof Number) {
            kbId = ((Number) kbIdObj).longValue();
        } else {
            Long parsed = null;
            try { parsed = Long.parseLong(kbIdObj.toString()); } catch (Exception ignored) {}
            kbId = parsed;
        }
        final String kbName = body.get("kbName") != null ? body.get("kbName").toString() : null;

        // V7.0: agentId → Agent 委托执行
        final String agentId = (String) body.get("agentId");
        final String agentName = (String) body.get("agentName");

        // 同步构建 RAG 上下文（避免 lambda 捕获非 final 变量）
        final String ragContext;
        if (kbId != null && kbId > 0) {
            ragContext = buildRagContext(kbId, message);
        } else {
            ragContext = "";
        }

        SseEmitter emitter = new SseEmitter(120_000L);  // V7.0: Agent 执行可能需要更长时间

        CompletableFuture.runAsync(() -> {
            // V7.0 Flow③: 查找 session，持久化消息
            String sessIdStr = body.get("sessionId") != null ? body.get("sessionId").toString() : null;
            AiChatSession session = sessIdStr != null ? sessionMapper.findBySessionId(sessIdStr) : null;
            // session_id 现在存 sessionId 字符串 (VARCHAR)
            final String msgSessionId = session != null ? session.getSessionId() : null;

            // V7.0 Flow③: 保存用户消息
            if (msgSessionId != null && message != null && !message.isBlank()) {
                try {
                    AiChatMessage userMsg = new AiChatMessage();
                    userMsg.setSessionId(msgSessionId);
                    userMsg.setRole("user");
                    userMsg.setContent(message);
                    userMsg.setCreatedAt(LocalDateTime.now());
                    messageMapper.insert(userMsg);
                } catch (Exception ex) {
                    log.warn("[Chat/Flow③] 保存用户消息失败: {}", ex.getMessage());
                }
            }

            try {
                // V7.0: 构建增强 system prompt
                StringBuilder fullSystem = new StringBuilder(systemPrompt);

                if (!ragContext.isEmpty()) {
                    fullSystem.append("\n\n请结合以下【知识库参考内容】回答用户问题。如果参考内容不相关，请忽略并基于你的知识回答。\n")
                              .append(ragContext);
                }

                // V7.0: Flow②+③: 如果指定了 agentId，调用 Agent 服务获取增强结果
                // 使用 AtomicReference 让 lambda 内可写
                final String finalAgentId = agentId;
                final String finalAgentName = agentName;
                java.util.concurrent.atomic.AtomicReference<String> agentResultRef =
                    new java.util.concurrent.atomic.AtomicReference<>();
                if (finalAgentId != null && !finalAgentId.isBlank()) {
                    try {
                        emitter.send(SseEmitter.event()
                            .name("message")
                            .data(Map.of("type", "agent_status", "content", "🤖 Agent 正在执行...")));
                        String agResult = agentClient.callAgentSync(0L, finalAgentId, message, null, kbId, kbName);
                        agentResultRef.set(agResult);
                        if (agResult != null && !agResult.isBlank()) {
                            fullSystem.append("\n\n【Agent 执行结果】\n").append(agResult);
                            emitter.send(SseEmitter.event()
                                .name("message")
                                .data(Map.of("type", "agent_result", "content", agResult)));
                            // V7.0 Flow③: 保存 Agent 结果消息
                            if (msgSessionId != null) {
                                try {
                                    AiChatMessage agentMsg = new AiChatMessage();
                                    agentMsg.setSessionId(msgSessionId);
                                    agentMsg.setRole("assistant");
                                    agentMsg.setContent("🤖 【" + (finalAgentName != null ? finalAgentName : finalAgentId) + " 执行结果】\n" + agResult);
                                    agentMsg.setToolCode("agent:" + finalAgentId);
                                    agentMsg.setToolOutput(agResult);
                                    agentMsg.setCreatedAt(LocalDateTime.now());
                                    messageMapper.insert(agentMsg);
                                } catch (Exception ex) {
                                    log.warn("[Chat/Flow③] 保存 Agent 消息失败: {}", ex.getMessage());
                                }
                            }
                            log.info("[Chat/Agent] agentId={} 执行成功, 结果长度={}", finalAgentId, agResult.length());
                        }
                    } catch (Exception e) {
                        log.warn("[Chat/Agent] agentId={} 执行失败: {}", finalAgentId, e.getMessage());
                        emitter.send(SseEmitter.event()
                            .name("message")
                            .data(Map.of("type", "agent_status", "content", "⚠️ Agent 执行失败，继续使用 LLM 回答")));
                    }
                }

                ChatRequestDTO req = new ChatRequestDTO();
                req.setModel(model);
                req.setTemperature(0.7);
                req.setMaxTokens(1024);
                req.setMessages(List.of(
                    Map.of("role", "system", "content", fullSystem.toString()),
                    Map.of("role", "user", "content", message)
                ));

                ChatResponseDTO resp = modelClient.chat(0L, req);
                String content = resp.getContent() != null ? resp.getContent() : "";

                // 分块推送（每 20 字符一个 SSE chunk）
                int chunkSize = 20;
                for (int i = 0; i < content.length(); i += chunkSize) {
                    String chunk = content.substring(i, Math.min(i + chunkSize, content.length()));
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new HashMap<String, Object>() {{ put("type", "content"); put("content", chunk); }}));
                }

                // V7.0 Flow③: 保存 LLM 响应消息
                if (msgSessionId != null && content != null && !content.isBlank()) {
                    try {
                        AiChatMessage llmMsg = new AiChatMessage();
                        llmMsg.setSessionId(msgSessionId);
                        llmMsg.setRole("assistant");
                        llmMsg.setContent(content);
                        llmMsg.setCreatedAt(LocalDateTime.now());
                        messageMapper.insert(llmMsg);
                    } catch (Exception ex) {
                        log.warn("[Chat/Flow③] 保存 LLM 消息失败: {}", ex.getMessage());
                    }
                }

                // 完成
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("type", "done");
                doneData.put("streamId", streamId);
                doneData.put("model", resp.getModel() != null ? resp.getModel() : model);
                doneData.put("provider", resp.getProviderCode() != null ? resp.getProviderCode() : "unknown");
                doneData.put("totalTokens", resp.getTotalTokens() != null ? resp.getTotalTokens() : 0);
                doneData.put("latencyMs", resp.getLatencyMs() != null ? resp.getLatencyMs() : 0);
                emitter.send(SseEmitter.event().name("done").data(doneData));
                emitter.complete();
                log.info("[Chat/SSE] streamId={} model={} tokens={} agentId={} done",
                        streamId, model, resp.getTotalTokens(), agentId);

            } catch (Exception ex) {
                log.warn("[Chat/SSE] streamId={} error: {}", streamId, ex.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data(new HashMap<String, Object>() {{
                            put("type", "error");
                            put("message", "LLM 调用失败: " + ex.getMessage());
                        }}));
                } catch (Exception ignored) {}
                emitter.completeWithError(ex);
            }
        });

        emitter.onCompletion(() -> log.debug("[Chat/SSE] streamId={} completed", streamId));
        emitter.onTimeout(() -> log.warn("[Chat/SSE] streamId={} timeout", streamId));
        emitter.onError(e -> log.warn("[Chat/SSE] streamId={} error: {}", streamId, e.getMessage()));

        return emitter;
    }

    /**
     * 多模型投票 (V6.8.2 真实推理)。
     *
     * <p>并发调用多个真实 LLM，取多数答案。</p>
     */
    @PostMapping("/voting")
    public Result<Map<String, Object>> voting(@RequestBody Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        if (text == null || text.isBlank()) {
            return Result.error(400, "text 不能为空");
        }

        log.info("[Chat/voting] text={}", text.substring(0, Math.min(60, text.length())));

        String[] models = {"gpt-4o-mini", "deepseek-chat"};
        String systemPrompt = "你是投票专家。请根据知识回答用户问题，只给出答案选项（如：A / B / C），不需要解释。";

        List<Map<String, Object>> votes = new ArrayList<>();
        for (String model : models) {
            try {
                ChatRequestDTO req = new ChatRequestDTO();
                req.setModel(model);
                req.setTemperature(0.1);
                req.setMaxTokens(50);
                List<Map<String, Object>> msgs = new ArrayList<>();
                msgs.add(Map.of("role", "system", "content", systemPrompt));
                msgs.add(Map.of("role", "user", "content", text));
                req.setMessages(msgs);

                ChatResponseDTO resp = modelClient.chat(0L, req);
                String answer = resp.getContent();
                votes.add(Map.of(
                    "model", model,
                    "answer", answer != null ? answer.trim() : "unknown",
                    "tokens", resp.getTotalTokens() != null ? resp.getTotalTokens() : 0
                ));
            } catch (Exception ex) {
                log.warn("[Chat/voting] model={} failed: {}", model, ex.getMessage());
                votes.add(Map.of("model", model, "answer", "ERROR: " + ex.getMessage()));
            }
        }

        // 简单多数票
        Map<String, Long> countMap = new HashMap<>();
        for (var v : votes) {
            String ans = String.valueOf(v.get("answer"));
            if (!ans.startsWith("ERROR")) {
                countMap.merge(ans, 1L, Long::sum);
            }
        }
        String finalAnswer = countMap.isEmpty() ? "无法确定"
                : countMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + e.getValue() + " 票)")
                    .orElse("无法确定");

        return Result.ok(Map.of(
            "text", text,
            "triggered", true,
            "votes", votes,
            "final", finalAnswer
        ));
    }

    /** 投票配置 */
    @GetMapping("/voting-info")
    public Result<Map<String, Object>> votingInfo() {
        return Result.ok(Map.of(
            "enabled", true,
            "threshold", 0.5,
            "models", Arrays.asList("gpt-4o-mini", "deepseek-chat"),
            "strategy", "majority"
        ));
    }

    // ==================== ONNX 自研模型推理 (V7.0) ====================

    /**
     * ONNX 模型文本生成 (V7.0)
     *
     * minimax-model 服务的 OnnxLLMAdapter 通过此端点调用真实 ONNX 推理。
     *
     * POST /api/v1/ai/chat/onnx/generate
     * Body: {
     *   "prompt": "用户输入",
     *   "modelPath": "/workspace/onnx-models/mini-transformer.onnx",  (可选)
     *   "temperature": 0.7,
     *   "maxTokens": 512,
     *   "topP": 0.9
     * }
     */
    @PostMapping("/onnx/generate")
    public Result<Map<String, Object>> onnxGenerate(@RequestBody Map<String, Object> body) {
        long start = System.currentTimeMillis();
        String prompt = (String) body.getOrDefault("prompt", "");
        String modelPath = (String) body.getOrDefault("modelPath", null);
        Double temperature = body.get("temperature") instanceof Number t ? t.doubleValue() : 0.7;
        Integer maxTokens = body.get("maxTokens") instanceof Number m ? m.intValue() : 512;
        Double topP = body.get("topP") instanceof Number p ? p.doubleValue() : 0.9;

        if (prompt.isBlank()) {
            return Result.error(400, "prompt 不能为空");
        }

        // 动态加载模型（如果指定了新路径）
        if (modelPath != null && !modelPath.isBlank()) {
            boolean loaded = onnxLLMService.loadModelPath(modelPath);
            log.info("[Onnx/generate] 动态加载模型: path={}, success={}", modelPath, loaded);
        }

        if (!onnxLLMService.isEnabled()) {
            return Result.error(503, "ONNX 推理未启用 (minimax.onnx.enabled=false)，请配置 minimax.onnx.model-dir");
        }

        OnnxLLMService.GeneratedResult result = onnxLLMService.generate(prompt, temperature, maxTokens, topP);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("text", result.text);
        resp.put("eos", result.eos);
        resp.put("promptTokens", result.promptTokens);
        resp.put("completionTokens", result.completionTokens);
        resp.put("totalTokens", result.promptTokens + result.completionTokens);
        resp.put("latencyMs", System.currentTimeMillis() - start);
        resp.put("modelPath", onnxLLMService.getActiveModelPath());

        log.info("[Onnx/generate] prompt={}, tokens={}, latency={}ms",
                prompt.substring(0, Math.min(30, prompt.length())),
                result.completionTokens,
                System.currentTimeMillis() - start);

        return Result.ok(resp);
    }

    /**
     * V7.1: 列出所有本地/自研模型（含 ONNX），供前端模型选择器使用。
     * 调用 minimax-model /models/local/providers，适配前端 trainedModels 数据结构。
     */
    @GetMapping("/training/models")
    public Result<List<Map<String, Object>>> listTrainedModels() {
        List<Map<String, Object>> providers = modelClient.listLocalProviders();
        List<Map<String, Object>> models = new java.util.ArrayList<>();
        for (Map<String, Object> p : providers) {
            String providerCode = String.valueOf(p.getOrDefault("code", ""));
            String providerName = String.valueOf(p.getOrDefault("name", "训练模型"));
            Object modelsObj = p.get("models");
            if (modelsObj instanceof List<?> list) {
                for (Object m : list) {
                    if (m instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> modelMap = (Map<String, Object>) m;
                        Map<String, Object> model = new java.util.LinkedHashMap<>();
                        model.put("code", modelMap.getOrDefault("modelCode", modelMap.getOrDefault("model_code", "")));
                        model.put("name", modelMap.getOrDefault("displayName", modelMap.getOrDefault("modelCode", "")));
                        model.put("provider", providerName);
                        model.put("providerCode", providerCode);
                        model.put("accuracy", modelMap.getOrDefault("accuracy", 0));
                        // 自动判断类型
                        String code = String.valueOf(model.get("code")).toLowerCase();
                        model.put("vision", code.contains("vision") || code.contains("vl") || code.contains("图像"));
                        model.put("audio", code.contains("audio") || code.contains("tts") || code.contains("asr"));
                        model.put("trained", true);
                        model.put("category", "self");
                        models.add(model);
                    }
                }
            }
        }
        log.info("[Training/Models] 返回本地模型 {} 个", models.size());
        return Result.ok(models);
    }

    /**
     * ONNX 模型状态查询
     */
    @GetMapping("/onnx/status")
    public Result<Map<String, Object>> onnxStatus() {
        return Result.ok(Map.of(
            "enabled", onnxLLMService.isEnabled(),
            "modelPath", onnxLLMService.getActiveModelPath() != null
                    ? onnxLLMService.getActiveModelPath() : "未加载",
            "message", onnxLLMService.isEnabled()
                    ? "ONNX 推理服务就绪" : "ONNX 未启用，请设置 minimax.onnx.enabled=true 和 minimax.onnx.model-dir"
        ));
    }
}
