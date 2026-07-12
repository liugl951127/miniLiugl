package com.minimax.model.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.minimax.common.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.provider.MockAdapter;
import com.minimax.model.provider.ModelProviderAdapter;
import com.minimax.model.provider.ModelProviderFactory;
import com.minimax.model.provider.OpenAiCompatibleAdapter;
import com.minimax.model.quota.QuotaService;
import com.minimax.model.quota.RateLimiter;
import com.minimax.model.service.ModelService;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.vo.ModelVO;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import com.minimax.model.mapper.ModelConfigMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Tag(name = "模型管理")
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;
    private final ModelProviderFactory providerFactory;
    private final ModelConfigMapper modelConfigMapper;
    private final RateLimiter rateLimiter;
    private final QuotaService quotaService;
    private final MockAdapter mockAdapter;
    private final OpenAiCompatibleAdapter openAiAdapter;

    /** 用于跟踪活跃流式 session → stopFlag，支持取消。 */
    private final Map<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();

    @Operation(summary = "列出所有可用模型")
    @GetMapping
    public Result<List<ModelVO>> list() {
        return Result.ok(modelService.listEnabled());
    }

    @Operation(summary = "列出支持的模型提供商")
    @GetMapping("/providers")
    public Result<List<String>> providers() {
        return Result.ok(List.of("openai", "minimax", "ollama", "zhipu", "qwen", "deepseek"));
    }

    @Operation(summary = "非流式对话请求")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @Valid @RequestBody ChatRequest req) {
        return Result.ok(modelService.chat(principal.id(), req));
    }

    @Operation(summary = "流式对话请求（SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody streamChat(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @RequestBody ChatRequest req,
                                            @RequestParam(required = false) String streamId) {
        // 限流
        if (!rateLimiter.tryAcquire(principal.id())) {
            throw new BizException(ResultCode.RATE_LIMIT);
        }
        // 模型存在
        Map<String, Object> model = modelConfigMapper.selectByCode(req.getModel());
        if (model == null) throw new BizException(ResultCode.MODEL_NOT_FOUND);

        // 选 provider
        ModelProviderAdapter adapter = providerFactory.get((String) model.get("provider_code"));
        String endpoint = (String) model.get("base_url");
        String apiKey   = (String) model.get("api_key");
        String id = streamId != null ? streamId : ("s_" + System.currentTimeMillis());
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        stopFlags.put(id, stopFlag);

        Long uid = principal.id();
        Long modelId = ((Number) model.get("model_id")).longValue();
        String providerCode = (String) model.get("provider_code");

        return output -> {
            try {
                OpenAiCompatibleAdapter.StreamResult result;
                if (adapter instanceof MockAdapter) {
                    result = ((MockAdapter) adapter).streamChat(req, chunk -> {
                        try { writeSse(output, chunk); } catch (IOException e) { throw new RuntimeException(e); }
                    }, stopFlag);
                } else if (adapter instanceof OpenAiCompatibleAdapter) {
                    result = ((OpenAiCompatibleAdapter) adapter).streamChat(endpoint, apiKey, req,
                            chunk -> {
                                try { writeSse(output, chunk); } catch (IOException e) { throw new RuntimeException(e); }
                            }, stopFlag);
                } else {
                    // 兜底：调阻塞 chat
                    ChatResponse cr = adapter.chat(endpoint, apiKey, req);
                    try {
                        writeSse(output, "{\"choices\":[{\"index\":0,\"delta\":{\"content\":\"" + escapeJson(cr.getContent()) + "\"}}]}");
                    } catch (IOException e) { throw new RuntimeException(e); }
                    result = new OpenAiCompatibleAdapter.StreamResult("s", req.getModel(), cr.getContent(),
                            cr.getPromptTokens(), cr.getCompletionTokens(), cr.getTotalTokens(),
                            cr.getFinishReason(), 0);
                }
                // 写 [DONE] 标记
                output.write("data: [DONE]\n\n".getBytes());
                output.flush();
                // 计配额
                quotaService.record(uid, modelId, result.totalTokens());
                log.info("stream done: id={} user={} model={} provider={} tokens={} finish={}",
                        id, uid, req.getModel(), providerCode, result.totalTokens(), result.finishReason());
            } catch (Exception e) {
                log.error("stream error", e);
                writeSse(output, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } finally {
                stopFlags.remove(id);
            }
        };
    }

    @Operation(summary = "取消流式生成")
    @PostMapping("/chat/cancel")
    public Result<Void> cancel(@RequestParam String streamId) {
        AtomicBoolean flag = stopFlags.remove(streamId);
        if (flag != null) {
            flag.set(true);
            return Result.ok();
        }
        return Result.fail(ResultCode.NOT_FOUND.getCode(), "streamId 不存在或已结束");
    }

    private void writeSse(java.io.OutputStream out, String json) throws IOException {
        out.write(("data: " + json + "\n\n").getBytes());
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
