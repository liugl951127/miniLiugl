package com.minimax.pipeline.executor.input;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * FILE INPUT 节点 (V5.32)
 *
 * config: {
 *   path: "/tmp/data.csv",     // 必填
 *   type: "csv" | "json" | "tsv",   // 可选, 自动推断
 *   separator: ",",             // csv 用
 *   encoding: "UTF-8"           // 可选
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileInputNode extends NodeExecutor {

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public NodeType supportedType() { return NodeType.FILE_INPUT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        String path = (String) config.get("path");
        if (path == null) throw new IllegalArgumentException("FILE_INPUT 缺 path");
        File f = new File(path);
        if (!f.exists()) throw new FileNotFoundException("文件不存在: " + path);

        String type = (String) config.get("type");
        if (type == null) {
            String name = f.getName();
            if (name.endsWith(".json")) type = "json";
            else if (name.endsWith(".tsv")) type = "tsv";
            else type = "csv";
        }
        String encoding = (String) config.getOrDefault("encoding", "UTF-8");
        Charset cs = "GBK".equalsIgnoreCase(encoding) ? Charset.forName("GBK") : StandardCharsets.UTF_8;

        log.info("[{}] File input: {} type={} encoding={}", nodeId, path, type, encoding);

        if ("json".equalsIgnoreCase(type)) {
            return readJson(f, cs);
        }
        return readCsv(f, cs, (String) config.get("separator"));
    }

    private List<Map<String, Object>> readCsv(File f, Charset cs, String sep) throws IOException {
        char fieldSep = sep == null ? ',' : sep.charAt(0);
        CsvReadConfig cfg = CsvReadConfig.defaultConfig();
        cfg.setFieldSeparator(fieldSep);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), cs))) {
            CsvData data = CsvUtil.getReader(br, cfg).read();
            if (data == null || data.getRows() == null || data.getRows().isEmpty()) return List.of();
            List<String> headers = data.getRow(0).getRawList();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 1; i < data.getRows().size(); i++) {
                List<String> raw = data.getRow(i).getRawList();
                Map<String, Object> row = new LinkedHashMap<>();
                for (int j = 0; j < headers.size() && j < raw.size(); j++) {
                    row.put(headers.get(j), raw.get(j));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readJson(File f, Charset cs) throws IOException {
        try (Reader r = new InputStreamReader(new FileInputStream(f), cs)) {
            return json.readValue(r, List.class);
        }
    }
}
