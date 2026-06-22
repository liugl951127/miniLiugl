package com.minimax.analytics.service.ingest.parser;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志解析器 (V5.31)
 *
 * 识别常见日志格式:
 *   1. Apache/Nginx: IP - - [time] "method path" status size
 *   2. Spring/Tomcat: timestamp LEVEL [thread] logger - msg
 *   3. JSON 格式 (应用日志)
 *   4. 纯文本 (兜底)
 *
 * 输出统一为 [timestamp, level, source, message, extra]
 */
@Slf4j
public class LogParser {

    public static class LogResult {
        public List<String> columns = List.of("timestamp", "level", "source", "message");
        public List<Map<String, Object>> rows = new ArrayList<>();
        public String detectedFormat;
    }

    private static final Pattern APACHE = Pattern.compile(
            "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"(\\S+) (\\S+) [^\"]+\" (\\d+) (\\S+)(?: \"([^\"]*)\" \"([^\"]*)\")?");

    private static final Pattern SPRING = Pattern.compile(
            "^(\\S+\\s+\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+-\\s+(.*)$");

    public static LogResult parse(File file, Charset charset) {
        LogResult result = new LogResult();
        List<String> lines = FileUtil.readLines(file, charset);
        if (lines.isEmpty()) return result;

        // 探测格式
        int apacheCnt = 0, springCnt = 0, jsonCnt = 0;
        for (int i = 0; i < Math.min(20, lines.size()); i++) {
            String line = lines.get(i);
            if (APACHE.matcher(line).find()) apacheCnt++;
            else if (SPRING.matcher(line).find()) springCnt++;
            else if (line.trim().startsWith("{") && line.trim().endsWith("}")) jsonCnt++;
        }
        String fmt = apacheCnt > springCnt && apacheCnt > jsonCnt ? "apache"
                : springCnt > jsonCnt ? "spring" : jsonCnt > 0 ? "json" : "raw";
        result.detectedFormat = fmt;

        for (String line : lines) {
            if (line.isBlank()) continue;
            Map<String, Object> row = parseLine(line, fmt);
            if (row != null) result.rows.add(row);
        }
        return result;
    }

    private static Map<String, Object> parseLine(String line, String fmt) {
        Map<String, Object> row = new LinkedHashMap<>();
        switch (fmt) {
            case "apache": {
                Matcher m = APACHE.matcher(line);
                if (m.find()) {
                    row.put("source", m.group(1));
                    row.put("timestamp", m.group(2));
                    row.put("method", m.group(3));
                    row.put("path", m.group(4));
                    row.put("status", Integer.parseInt(m.group(5)));
                    row.put("size", m.group(6));
                    row.put("message", line);
                    row.put("level", Integer.parseInt(m.group(5)) >= 500 ? "ERROR" : "INFO");
                    return row;
                }
                break;
            }
            case "spring": {
                Matcher m = SPRING.matcher(line);
                if (m.find()) {
                    row.put("timestamp", m.group(1));
                    row.put("level", m.group(2));
                    row.put("source", m.group(5));
                    row.put("message", m.group(6));
                    return row;
                }
                break;
            }
            case "json": {
                try {
                    return new com.fasterxml.jackson.databind.ObjectMapper().readValue(line, Map.class);
                } catch (Exception e) {
                    // fallthrough
                }
                break;
            }
        }
        // 兜底
        row.put("timestamp", "");
        row.put("level", "");
        row.put("source", "");
        row.put("message", line);
        return row;
    }
}
