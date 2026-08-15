package com.minimax.analytics.service.ingest.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

/**
 * JSON 解析器 (V5.31)
 *
 * 支持 2 种格式:
 *  1. JSON 数组: [{...}, {...}, ...]
 *  2. NDJSON: 每行一个 JSON 对象 (大文件友好, 流式)
 */
@Slf4j
public class JsonParser {

    public static class JsonResult {
        public List<String> columns;
        public List<Map<String, Object>> rows;
        public String format;  // "array" / "ndjson"
    }

    public static JsonResult parse(File file) {
        ObjectMapper json = new ObjectMapper();
        JsonResult result = new JsonResult();
        // V5.31 简化: 用 hutool 读行, 逐行解析
        List<String> lines = cn.hutool.core.io.FileUtil.readUtf8Lines(file);
        if (lines.isEmpty()) throw new RuntimeException("文件为空");

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> columnSet = new LinkedHashSet<>();
        boolean isArray = lines.get(0).trim().startsWith("[");
        if (isArray) {
            // 简单: 拼成一行数组
            String content = String.join("", lines);
            try {
                List<?> arr = json.readValue(content, List.class);
                for (Object o : arr) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) o;
                        rows.add(m);
                        columnSet.addAll(m.keySet());
                    }
                }
                result.format = "array";
            } catch (Exception e) {
                throw new RuntimeException("JSON 数组解析失败: " + e.getMessage(), e);
            }
        } else {
            // NDJSON
            for (String line : lines) {
                if (line.isBlank()) continue;
                try {
                    Map<String, Object> m = json.readValue(line, Map.class);
                    rows.add(m);
                    columnSet.addAll(m.keySet());
                } catch (Exception e) {
                    log.warn("NDJSON 单行解析失败: {}", e.getMessage());
                }
            }
            result.format = "ndjson";
        }
        result.columns = new ArrayList<>(columnSet);
        result.rows = rows;
        return result;
    }
}
