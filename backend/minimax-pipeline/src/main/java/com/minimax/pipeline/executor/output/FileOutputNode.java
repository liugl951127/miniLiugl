package com.minimax.pipeline.executor.output;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.text.csv.CsvWriteConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.common.exception.BizException;
import com.minimax.pipeline.enums.NodeType;
import com.minimax.pipeline.executor.ExecutionContext;
import com.minimax.pipeline.executor.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * FILE OUTPUT 节点 (V5.32) - 导出 CSV/JSON
 *
 * config: {
 *   path: "/tmp/result.csv",     // 可选, 默认 {outputDir}/pipeline_{runId}_{nodeId}.{format}
 *   format: "csv" | "json"       // 默认 csv
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileOutputNode extends NodeExecutor {

    @Value("${pipeline.storage.output-dir:/tmp/minimax-pipeline/outputs}")
    private String outputDir;

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public NodeType supportedType() { return NodeType.FILE_OUTPUT; }

    @Override
    protected List<Map<String, Object>> doExecute(String nodeId, Map<String, Object> config,
                                                   Map<String, List<Map<String, Object>>> inputs,
                                                   ExecutionContext ctx) throws Exception {
        List<Map<String, Object>> rows = inputs.values().iterator().next();
        String format = ((String) config.getOrDefault("format", "csv")).toLowerCase();
        String path = (String) config.get("path");
        if (path == null) {
            new File(outputDir).mkdirs();
            path = outputDir + "/pipeline_" + ctx.getRunId() + "_" + nodeId + "." + format;
        }
        log.info("[{}] File output: {} rows -> {} ({})", nodeId, rows.size(), path, format);

        if ("json".equals(format)) {
            json.writerWithDefaultPrettyPrinter().writeValue(new File(path), rows);
        } else {
            CsvWriteConfig cfg = CsvWriteConfig.defaultConfig();
            try (CsvWriter w = CsvUtil.getWriter(new File(path), StandardCharsets.UTF_8, false, cfg)) {
                if (!rows.isEmpty()) {
                    List<String> cols = new ArrayList<>(rows.get(0).keySet());
                    w.writeHeaderLine(cols.toArray(new String[0]));
                    for (Map<String, Object> row : rows) {
                        List<String> vals = new ArrayList<>();
                        for (String c : cols) {
                            Object v = row.get(c);
                            vals.add(v == null ? "" : v.toString());
                        }
                        w.writeLine(vals.toArray(new String[0]));
                    }
                }
            }
        }
        // V5.32 简化: 把 path 写回 rows[0] 作为标记, RunDetailVo 读这个
        if (!rows.isEmpty()) {
            Map<String, Object> first = new LinkedHashMap<>(rows.get(0));
            first.put("_output_path", path);
            List<Map<String, Object>> out = new ArrayList<>();
            out.add(first);
            out.addAll(rows.subList(1, rows.size()));
            return out;
        }
        return rows;
    }
}
