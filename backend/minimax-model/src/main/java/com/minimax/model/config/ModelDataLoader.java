package com.minimax.model.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * V7.0: h2local 沙箱模式数据初始化器。
 * 在应用启动后填充 model_provider 和 model_config 表（Spring SQL init 在 H2 MySQL mode 下有 bug）。
 */
@Slf4j
@Component
@Profile("h2local")
@RequiredArgsConstructor
public class ModelDataLoader {

    private final JdbcTemplate jdbc;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${MINIMAX_API_KEY:}")
    private String minimaxApiKey;

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        log.info("[ModelDataLoader] h2local 模式初始化模型数据...");

        try {
            // 检查是否已有数据
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM model_provider", Integer.class);
            if (count != null && count > 0) {
                log.info("[ModelDataLoader] model_provider 已有 {} 条数据，跳过", count);
                // 打印现有数据
            jdbc.query("SELECT * FROM model_provider", rs -> {
                log.info("[ModelDataLoader] provider: id={}, code={}, name={}", 
                    rs.getLong("id"), rs.getString("code"), rs.getString("name"));
            });
                return;
            }

            // 插入 provider
            jdbc.update("INSERT INTO model_provider (code, name, base_url, api_key, protocol, enabled, deleted, sort) " +
                "VALUES ('deepseek', 'DeepSeek', 'https://api.deepseek.com', ?, 'openai', 1, 0, 10)",
                deepseekApiKey.isEmpty() ? "sk-test-key" : deepseekApiKey);
            jdbc.update("INSERT INTO model_provider (code, name, base_url, api_key, protocol, enabled, deleted, sort) " +
                "VALUES ('minimax', 'MiniMax', 'https://api.minimax.chat', ?, 'openai', 1, 0, 20)",
                minimaxApiKey.isEmpty() ? "sk-test-key" : minimaxApiKey);
            // V7.0 Flow④: 训练模型分类 provider
            jdbc.update("INSERT INTO model_provider (code, name, base_url, api_key, protocol, enabled, deleted, sort) " +
                "VALUES ('trained', '训练模型', 'local://trained', '', 'trained', 1, 0, 99)");

            // 插入 model config
            jdbc.update("INSERT INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_stream, enabled, deleted, sort) " +
                "VALUES (1, 'deepseek-chat', 'DeepSeek Chat', 16384, 4096, 1, 1, 0, 10)");
            jdbc.update("INSERT INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_stream, enabled, deleted, sort) " +
                "VALUES (2, 'MiniMax-Text-01', 'MiniMax Text', 8192, 2048, 1, 1, 0, 20)");
            jdbc.update("INSERT INTO model_config (provider_id, model_code, display_name, max_context, max_output, supports_stream, enabled, deleted, sort) " +
                "VALUES (1, 'deepseek-coder', 'DeepSeek Coder', 16384, 4096, 1, 1, 0, 30)");

            log.info("[ModelDataLoader] 模型数据初始化完成: 3 providers, 3 models");
        } catch (Exception ex) {
            log.error("[ModelDataLoader] 模型数据初始化失败: {}", ex.getMessage());
        }
    }
}
