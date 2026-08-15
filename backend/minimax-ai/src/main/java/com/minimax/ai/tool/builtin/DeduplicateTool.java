package com.minimax.ai.tool.builtin;

import com.minimax.ai.datasource.MultiDataSourceManager;
import com.minimax.ai.entity.AiTool;
import com.minimax.ai.entity.DbDataSource;
import com.minimax.ai.mapper.DataSourceMapper;
import com.minimax.ai.tool.AiToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据去重工具 (V2.7)
 *
 * <p>基于指定列去除重复行, 支持:
 *   - 单列去重
 *   - 多列组合去重
 *   - 返回去重后的数据 (内存模式, 适合小数据集)
 *   - 返回统计 (不去重, 仅统计每组数量)
 *   - 支持 LIMIT 防止 OOM</p>
 *
 * <h3>算法</h3>
 * <ol>
 *   <li>查 SELECT * FROM table LIMIT n</li>
 *   <li>HashMap&lt;key, row&gt; 记录首个出现</li>
 *   <li>key 由指定列拼接 (用 \u0001 分隔避免冲突)</li>
 *   <li>返回 unique rows + 重复统计</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeduplicateTool implements AiToolExecutor {

    private final DataSourceMapper dataSourceMapper;
    private final MultiDataSourceManager dsManager;

    @Override
    public String getCode() {
        return "data.clean.deduplicate";
    }

    @Override
    public Object execute(AiTool tool, Map<String, Object> input) throws Exception {
        Long dataSourceId = ((Number) input.get("dataSourceId")).longValue();
        String table = (String) input.get("table");
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) input.get("columns");
        Integer limit = input.get("limit") != null ? ((Number) input.get("limit")).intValue() : 10000;

        if (table == null) throw new IllegalArgumentException("table is required");
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("columns is required (at least one column to dedup by)");
        }

        DbDataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) throw new IllegalArgumentException("Data source not found: " + dataSourceId);

        long start = System.currentTimeMillis();

        // 1. 查数据
        String sql = "SELECT * FROM " + table + " LIMIT " + limit;
        List<Map<String, Object>> rows = new ArrayList<>();
        javax.sql.DataSource ds_ = dsManager.getDataSource(ds);
        try (Connection conn = ds_.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }

        // 2. 去重
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        Map<String, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String key = buildKey(row, columns);
            countMap.merge(key, 1, Integer::sum);
            unique.putIfAbsent(key, row);
        }

        // 3. 返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", rows.size());
        result.put("uniqueRows", unique.size());
        result.put("duplicatesRemoved", rows.size() - unique.size());
        result.put("duplicates", countMap.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new)));
        result.put("data", new ArrayList<>(unique.values()));
        result.put("durationMs", System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 用指定列拼成 key (用 \u0001 避免冲突)
     */
    private String buildKey(Map<String, Object> row, List<String> columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append('\u0001');
            Object v = row.get(columns.get(i));
            sb.append(v == null ? "NULL" : v.toString());
        }
        return sb.toString();
    }
}
