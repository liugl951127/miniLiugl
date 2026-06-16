package com.minimax.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.rag.retriever.Retriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 增强的问答。
 *
 * 流程:
 *  1) 检索 (Retriever) → topK chunks
 *  2) 拼 system prompt + 引用片段
 *  3) 调 model 服务 (OpenAI 兼容) 生成答案
 *  4) 返回 answer + sources[]
 *
 * 失败降级: 检索为空 → 调普通 chat; LLM 失败 → 返回检索内容 + 提示.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    @Value("${minimax.rag.chat.model:MiniMax-Text-01}")
    private String model;
    @Value("${minimax.rag.chat.base-url:http://localhost:8083}")
    private String baseUrl;
    @Value("${minimax.rag.chat.token:}")
    private String token;
    @Value("${minimax.rag.chat.timeout-seconds:30}")
    private int timeout;

    private final Retriever retriever;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public RagAnswer ask(Long kbId, String question, String history, int topK) {
        if (question == null || question.isBlank()) {
            return new RagAnswer("问题不能为空", List.of());
        }
        // 1) 检索
        List<Retriever.Hit> hits = retriever.retrieve(kbId, question, topK);
        if (hits.isEmpty()) {
            log.info("RAG: 检索为空 kbId={} 走普通 chat", kbId);
            String plain = plainChat(question, history);
            return new RagAnswer(plain, List.of());
        }
        // 2) 拼 context
        StringBuilder ctx = new StringBuilder();
        ctx.append("你是基于知识库回答问题的助手。请根据以下参考资料回答，引用处标注 [来源 N]。\n");
        ctx.append("若资料不足以回答，请直接说'我不知道'或'参考资料未覆盖'。\n\n参考资料:\n");
        for (int i = 0; i < hits.size(); i++) {
            Retriever.Hit h = hits.get(i);
            ctx.append("[来源 ").append(i+1).append("] ")
               .append(h.docTitle == null ? "(未知文档)" : h.docTitle)
               .append(" - 片段 #").append(h.chunkIndex)
               .append(" (相似度 ").append(String.format("%.2f", h.score)).append(")\n")
               .append(h.content).append("\n\n");
        }
        // 3) 拼 messages
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content", ctx.toString()));
        if (history != null && !history.isBlank()) {
            // 简化: 把 history 当作 user 补充
            messages.add(Map.of("role","user","content", history));
        }
        messages.add(Map.of("role","user","content", question));

        // 4) 调 LLM
        String answer;
        try {
            answer = callChat(messages);
        } catch (Exception e) {
            log.warn("RAG LLM 调用失败, 降级返回检索内容: {}", e.getMessage());
            StringBuilder sb = new StringBuilder("（LLM 不可用，以下为检索内容）\n\n");
            for (int i = 0; i < hits.size(); i++) {
                sb.append("[").append(i+1).append("] ").append(hits.get(i).content).append("\n");
            }
            answer = sb.toString();
        }

        // 5) sources
        List<Source> sources = new ArrayList<>();
        for (Retriever.Hit h : hits) {
            sources.add(new Source(h.chunkId, h.docId, h.docTitle, h.chunkIndex,
                    truncate(h.content, 200), h.score));
        }
        return new RagAnswer(answer, sources);
    }

    private String plainChat(String question, String history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content","你是助手。"));
        if (history != null && !history.isBlank()) {
            messages.add(Map.of("role","user","content", history));
        }
        messages.add(Map.of("role","user","content", question));
        try { return callChat(messages); }
        catch (Exception e) { return "(无知识库命中 + LLM 不可用) 你的问题: " + question; }
    }

    private String callChat(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", 800);

        HttpRequest.Builder hb = HttpRequest.newBuilder()
                .uri(URI.create(stripSlash(baseUrl) + "/api/v1/models/chat"))
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        if (token != null && !token.isBlank()) hb.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = client.send(hb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(resp.body(), 200));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = json.readValue(resp.body(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return data == null ? "" : (String) data.get("content");
    }

    private String stripSlash(String s) { return s == null ? "" : (s.endsWith("/") ? s.substring(0, s.length()-1) : s); }
    private String truncate(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }

    public record Source(Long chunkId, Long docId, String docTitle, Integer chunkIndex,
                          String snippet, Double score) {}

    public record RagAnswer(String answer, List<Source> sources) {}
}
