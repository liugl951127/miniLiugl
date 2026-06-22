package com.minimax.analytics.service.ingest.parser;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvData;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * CSV 解析器 (V5.31) - 用 hutool CsvUtil
 *
 * 自动推断分隔符: 优先逗号 → 失败回退 \t
 * 第一行作为表头
 */
@Slf4j
public class CsvParser {

    public static class CsvResult {
        public List<String> columns;
        public List<Map<String, Object>> rows;
        public String separator;
    }

    public static CsvResult parse(File file, Charset charset) {
        CsvResult result = new CsvResult();
        char[] seps = new char[]{',', '\t', ';', '|'};
        for (char sep : seps) {
            try {
                CsvReadConfig cfg = CsvReadConfig.defaultConfig();
                cfg.setFieldSeparator(sep);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), charset))) {
                    CsvReader reader = CsvUtil.getReader(br, cfg);
                    CsvData data = reader.read();
                    if (data == null || data.getRows() == null || data.getRows().isEmpty()) {
                        continue;
                    }
                    // 第一行作为表头
                    List<String> headers = data.getRow(0).getRawList();
                    if (headers == null || headers.isEmpty()) continue;
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (int i = 1; i < data.getRows().size(); i++) {
                        List<String> raw = data.getRow(i).getRawList();
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int j = 0; j < headers.size() && j < raw.size(); j++) {
                            row.put(headers.get(j), raw.get(j));
                        }
                        rows.add(row);
                    }
                    if (!headers.isEmpty() && !rows.isEmpty()) {
                        result.columns = headers;
                        result.rows = rows;
                        result.separator = String.valueOf(sep);
                        return result;
                    }
                }
            } catch (Exception e) {
                log.debug("分隔符 {} 失败: {}", sep, e.getMessage());
            }
        }
        throw new RuntimeException("CSV 解析失败, 所有分隔符都试过");
    }
}
