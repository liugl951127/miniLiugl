package com.minimax.analytics.service.nlsql;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.analytics.dto.Nl2SqlRequest;
import com.minimax.analytics.dto.QueryRequest;
import com.minimax.analytics.entity.Nl2SqlHistory;
import com.minimax.analytics.mapper.Nl2SqlHistoryMapper;
import com.minimax.analytics.service.query.QueryService;
import com.minimax.analytics.service.schema.SchemaService;
import com.minimax.analytics.vo.Nl2SqlResult;
import com.minimax.analytics.vo.QueryResult;
import com.minimax.analytics.vo.TableInfo;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import com.minimax.model.dto.ChatRequest;
import com.minimax.model.vo.ChatResponse;
import com.minimax.model.provider.ModelProviderAdapter;
import com.minimax.model.provider.ModelProviderFactory;
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
 * 复用 minimax-model 模块的 LLM 适配器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Nl2SqlServiceImpl implements Nl2SqlService {

    private final ModelProviderFactory modelFactory;
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
        long t0 = System.currentTimeMillis();
        Nl2SqlHistory history = new Nl2SqlHistory();
        history.setUserId(userId);
        history.setDataSourceId(request.getDataSourceId());
        history.setQuestion(request.getQuestion());
        history.setModel(request.getModel() != null ? request.getModel() : defaultModel);
        history.setCreatedAt(LocalDateTime.now());

        try {
            // 1. 拿 schema (V5.31: 列出所有表, 限制前 20 个避免 prompt 过长)
            List<TableInfo> tables = schemaService.listTables(request.getDataSourceId(), request.getDatabase(), null);
            if (tables.size() > 20) tables = tables.subList(0, 20);
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

            // 3. 调 LLM
            ChatRequest chatReq = new ChatRequest();
            chatReq.setModel(history.getModel());
            chatReq.setMessages(List.of(
                    Map.of("role", "system", "content", sysPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            chatReq.setTemperature(temperature);
            chatReq.setMaxTokens(maxTokens);

            ModelProviderAdapter adapter = modelFactory.get(history.getModel());
            ChatResponse resp = adapter.chat(null, null, chatReq);
            String llmOutput = resp.getContent();

            // 4. 解析 SQL
            String sql = extractSql(llmOutput);
            String explanation = extractExplanation(llmOutput);
            if (sql == null || sql.isBlank()) {
                throw new BizException(ResultCode.SYSTEM_ERROR, "LLM 未生成 SQL");
            }
            history.setGeneratedSql(sql);
            history.setSuccess(true);
            history.setDurationMs(System.currentTimeMillis() - t0);
            if (resp != null) {
                history.setPromptTokens(resp.getPromptTokens());
                history.setCompletionTokens(resp.getCompletionTokens());
            }

            Nl2SqlResult.Nl2SqlResultBuilder result = Nl2SqlResult.builder()
                    .question(request.getQuestion())
                    .generatedSql(sql)
                    .explanation(explanation)
                    .durationMs(history.getDurationMs())
                    .model(history.getModel())
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
        // V5.31 简化: 调 LLM 直接解释
        try {
            ChatRequest chatReq = new ChatRequest();
            chatReq.setModel(defaultModel);
            chatReq.setMessages(List.of(
                    Map.of("role", "system", "content", "你是 SQL 教学助手, 用中文简洁解释用户给的 SQL. 1-3 句话."),
                    Map.of("role", "user", "content", "请解释: " + sql)
            ));
            chatReq.setTemperature(0.1);
            chatReq.setMaxTokens(512);
            ModelProviderAdapter adapter = modelFactory.get(defaultModel);
            return adapter.chat(null, null, chatReq).getContent();
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
