package com.minimax.analytics.service.nlsql;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.analytics.dto.Nl2SqlRequest;
import com.minimax.analytics.dto.QueryRequest;
import com.minimax.analytics.entity.Nl2SqlHistory;
import com.minimax.analytics.feign.ModelChatClient;
import com.minimax.analytics.mapper.Nl2SqlHistoryMapper;
import com.minimax.analytics.service.query.QueryService;
import com.minimax.analytics.service.schema.SchemaService;
import com.minimax.analytics.vo.Nl2SqlResult;
import com.minimax.analytics.vo.QueryResult;
import com.minimax.analytics.vo.TableInfo;
import com.minimax.common.exception.BizException;
import com.minimax.common.feign.model.ChatRequestDTO;
import com.minimax.common.feign.model.ChatResponseDTO;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NL2SQL 服务实现 (V5.31)
 *
 * 流程: 拿 schema → 拼 prompt → 调 LLM → 解析 SQL → 安全校验 → (可选) 执行
 * V6.8.1: 通过 Feign 调用 minimax-model 的内部 chat API（解耦 Maven 依赖）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlServiceImpl implements Nl2SqlService {

    private final ModelChatClient modelChatClient;
    private final com.minimax.common.sdk.LlmClient llmClient;  // V9.1: LLM 兜底
    private final SchemaService schemaService;
    private final SqlSafetyChecker safetyChecker;
    private final QueryService queryService;
    private final Nl2SqlHistoryMapper historyMapper;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${analytics.nlsql.model:MiniMax-Text-01}")
    private String defaultModel;

    @Value("${analytics.nlsql.temperature:0.1}")
    private double temperature;

    @Value("${analytics.nlsql.max-tokens:2048}")
    private int maxTokens;

    /** 从 LLM 响应中提取 SQL 块 */
    private static final Pattern SQL_BLOCK = Pattern.compile("```sql\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Pattern EXPLAIN_LINE = Pattern.compile("解释[::]\\s*(.+?)(?=\\n|$)", Pattern.DOTALL);

    @Override
    public Nl2SqlResult ask(Long userId, Nl2SqlRequest request) {
        if (request == null || request.getDataSourceId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "数据源ID不能为空");
        }
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "问题不能为空");
        }
        long t0 = System.currentTimeMillis();
        Nl2SqlHistory history = new Nl2SqlHistory();
        history.setUserId(userId);
        history.setDataSourceId(request.getDataSourceId());
        history.setQuestion(request.getQuestion());
        history.setModel(request.getModel() != null ? request.getModel() : defaultModel);
        history.setCreatedAt(LocalDateTime.now());

        try {
            // 1. 拿 schema (tableHint 优先只查指定表, 否则列出所有表限制前 20 个)
            List<TableInfo> tables;
            String tableHint = request.getTableHint();
            if (tableHint != null && !tableHint.isBlank()) {
                // 前端选了某张表, 只查这一张
                TableInfo single = schemaService.describeTable(request.getDataSourceId(), request.getDatabase(), tableHint);
                tables = single != null ? List.of(single) : List.of();
            } else {
                tables = schemaService.listTables(request.getDataSourceId(), request.getDatabase(), null);
                if (tables.size() > 20) tables = tables.subList(0, 20);
            }
            List<Map<String, String>> schemas = new ArrayList<>();
            for (TableInfo t : tables) {
                TableInfo detail = schemaService.describeTable(request.getDataSourceId(), request.getDatabase(), t.getName());
                Map<String, String> m = new HashMap<>();
                m.put("name", t.getName());
                m.put("ddl", detail.getDdl() != null ? detail.getDdl() : "/* no ddl */");
                schemas.add(m);
            }

            // 2. 拼 prompt
            String sysPrompt = PromptTemplates.system() + "\n\n" + PromptTemplates.fewShot();
            String userPrompt = PromptTemplates.user(request.getQuestion(), schemas);

            // 3. 调 LLM（V9.1: Feign → minimax-model, 失败时降级到 LlmClient → minimax-ai 的 LLM Gateway）
            ChatRequestDTO chatReq = new ChatRequestDTO();
            chatReq.setModel(history.getModel());
            chatReq.setMessages(List.of(
                    Map.of("role", "system", "content", sysPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            chatReq.setTemperature(temperature);
            chatReq.setMaxTokens(maxTokens);

            String llmOutput = null;
            String llmSource = "CLOUD";  // V9.1: 记录 source
            String llmModel = history.getModel();

            // 3a. 优先走 Feign → minimax-model
            try {
                Result<ChatResponseDTO> respResult = modelChatClient.chat(userId, chatReq);
                if (respResult == null || respResult.getCode() == null || respResult.getCode() != 0 || respResult.getData() == null) {
                    throw new BizException(ResultCode.SYSTEM_ERROR, "Feign 返: " +
                            (respResult != null ? respResult.getMessage() : "null"));
                }
                llmOutput = respResult.getData().getContent();
                log.info("[Nl2Sql] Feign 成功, model={}", llmModel);
            } catch (Exception feignErr) {
                // 3b. V9.1: Feign 失败 → 降级到 LlmClient (走 LLM Gateway, 内部 cloud→local 兜底)
                log.warn("[Nl2Sql] Feign 失败, 降级到 LLM Gateway: {}", feignErr.getMessage());
                com.minimax.common.sdk.LlmClient.LlmResult fallback = llmClient.chat(
                    java.util.List.of(
                        Map.of("role", "system", "content", sysPrompt),
                        Map.of("role", "user", "content", userPrompt)
                    )
                );
                if (fallback.available()) {
                    llmOutput = fallback.content();
                    llmSource = fallback.source().name();
                    llmModel = fallback.model();
                    log.info("[Nl2Sql] 降级成功, source={}, model={}", llmSource, llmModel);
                } else {
                    throw new BizException(ResultCode.SYSTEM_ERROR,
                        "LLM 调用失败 (Feign + 降级): " + fallback.reason());
                }
            }
            if (llmOutput == null || llmOutput.isBlank()) {
                throw new BizException(ResultCode.SYSTEM_ERROR, "LLM 未生成内容");
            }

            // 4. 解析 SQL
            String sql = extractSql(llmOutput);
            String explanation = extractExplanation(llmOutput);
            if (sql == null || sql.isBlank()) {
                throw new BizException(ResultCode.SYSTEM_ERROR, "LLM 未生成 SQL");
            }
            history.setGeneratedSql(sql);
            history.setSuccess(true);
            history.setDurationMs(System.currentTimeMillis() - t0);
            // V9.1: 记 source (Feign 成功时是 CLOUD, 降级时是 LOCAL_FALLBACK)
            // 通过 response 返回, 暂不改 entity (避免 DDL/Mapper 同步)
            // V9.2: 持久化到 history.llm_source 列

            Nl2SqlResult.Nl2SqlResultBuilder result = Nl2SqlResult.builder()
                    .question(request.getQuestion())
                    .generatedSql(sql)
                    .explanation(explanation)
                    .durationMs(history.getDurationMs())
                    .model(llmModel)  // V9.1: 用实际用的模型
                    .llmSource(llmSource)  // V9.1: CLOUD / LOCAL_FALLBACK / LOCAL
                    .promptTokens(history.getPromptTokens())
                    .completionTokens(history.getCompletionTokens());

            // 5. 安全校验
            SqlSafetyChecker.SafetyResult safety = safetyChecker.check(sql, 1000);
            if (!safety.ok()) {
                history.setSuccess(false);
                history.setErrorMessage(safety.reason());
                result.explanation("⚠️ 安全校验未通过: " + safety.reason());
            } else if (Boolean.TRUE.equals(request.getAutoExecute())) {
                // 6. 自动执行
                QueryRequest qreq = new QueryRequest();
                qreq.setDataSourceId(request.getDataSourceId());
                qreq.setSql(sql);
                qreq.setMaxRows(1000);
                try {
                    QueryResult qr = queryService.execute(qreq);
                    result.executed(true).queryResult(qr);
                } catch (Exception e) {
                    history.setSuccess(false);
                    history.setErrorMessage(e.getMessage());
                }
            }

            historyMapper.insert(history);
            return result.build();
        } catch (Exception e) {
            log.error("NL2SQL 失败: {}", e.getMessage(), e);
            history.setSuccess(false);
            history.setErrorMessage(e.getMessage());
            history.setDurationMs(System.currentTimeMillis() - t0);
            historyMapper.insert(history);
            throw new BizException(ResultCode.SYSTEM_ERROR, "NL2SQL 失败: " + e.getMessage());
        }
    }

    @Override
    public String explain(Long userId, Long dataSourceId, String sql) {
        // V6.8.1: 通过 HTTP Feign 调用 model 服务，不再依赖 minimax-model 直接 import
        try {
            ChatRequestDTO chatReq = new ChatRequestDTO();
            chatReq.setModel(defaultModel);
            chatReq.setMessages(List.of(
                    Map.of("role", "system", "content", "你是 SQL 教学助手, 用中文简洁解释用户给的 SQL. 1-3 句话."),
                    Map.of("role", "user", "content", "请解释: " + sql)
            ));
            chatReq.setTemperature(0.1);
            chatReq.setMaxTokens(512);
            Result<ChatResponseDTO> result = modelChatClient.chat(userId, chatReq);
            if (result == null || result.getCode() == null || result.getCode() != 0 || result.getData() == null) {
                return "(模型调用失败: " + (result != null ? result.getMessage() : "null") + ")";
            }
            return result.getData().getContent() != null ? result.getData().getContent() : "(空响应)";
        } catch (Exception e) {
            log.error("SQL 解释失败: {}", e.getMessage());
            return "(解释失败: " + e.getMessage() + ")";
        }
    }

    @Override
    public void feedback(Long userId, Long historyId, String correctedSql, Integer rating) {
        Nl2SqlHistory h = historyMapper.selectById(historyId);
        if (h == null) throw new BizException(ResultCode.NOT_FOUND, "历史不存在");
        if (!h.getUserId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN, "无权");
        h.setCorrectedSql(correctedSql);
        h.setFeedbackRating(rating);
        historyMapper.updateById(h);
    }

    @Override
    public List<Nl2SqlHistory> history(Long userId, int page, int size) {
        return historyMapper.selectPage(new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Nl2SqlHistory>()
                        .eq(Nl2SqlHistory::getUserId, userId)
                        .orderByDesc(Nl2SqlHistory::getCreatedAt)).getRecords();
    }

    // ---- helpers ----

    private String extractSql(String text) {
        if (text == null) return null;
        Matcher m = SQL_BLOCK.matcher(text);
        if (m.find()) return m.group(1).trim();
        // 降级: 找 "SQL:" 后到 "解释" 之间的内容
        int idx = text.toUpperCase().indexOf("SQL");
        if (idx >= 0) {
            String sub = text.substring(idx);
            int end = sub.indexOf("解释");
            if (end < 0) end = sub.length();
            return sub.substring(0, end).replaceAll("^SQL:?\\s*```?sql?\\s*", "").replaceAll("```\\s*$", "").trim();
        }
        return null;
    }

    private String extractExplanation(String text) {
        if (text == null) return null;
        Matcher m = EXPLAIN_LINE.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }
}
