package com.minimax.analytics.service.ingest;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.analytics.entity.IngestTask;
import com.minimax.analytics.mapper.IngestTaskMapper;
import com.minimax.analytics.service.ingest.parser.*;
import com.minimax.analytics.vo.QualityReport;
import com.minimax.common.exception.BizException;
import com.minimax.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件导入服务实现 (V5.31)
 *
 * 流程: 上传 → 写盘 → 异步解析 → 质量报告 → 完成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestServiceImpl implements FileIngestService {

    private final IngestTaskMapper taskMapper;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${analytics.ingest.upload-dir:/tmp/minimax-analytics/uploads}")
    private String uploadDir;

    @Value("${analytics.ingest.max-file-size-mb:100}")
    private int maxFileSizeMb;

    @Value("${analytics.ingest.supported-types:csv,json,log,txt,tsv}")
    private String supportedTypesStr;

    @Override
    public String upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件为空");
        }
        long size = file.getSize();
        if (size > maxFileSizeMb * 1024L * 1024L) {
            throw new BizException(ResultCode.BAD_REQUEST, "文件超过 " + maxFileSizeMb + "MB");
        }
        String origName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = origName.contains(".") ? origName.substring(origName.lastIndexOf('.') + 1).toLowerCase() : "";
        Set<String> supported = Arrays.stream(supportedTypesStr.split(",")).map(String::trim).collect(Collectors.toSet());
        if (!supported.contains(ext)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持的格式: " + ext + ", 支持: " + supported);
        }

        // 建任务
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        IngestTask task = new IngestTask();
        task.setUserId(userId);
        task.setTaskId(taskId);
        task.setFilename(origName);
        task.setFileType(ext);
        task.setFileSize(size);
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        // 写盘
        File target = new File(uploadDir, taskId + "." + ext);
        FileUtil.mkParentDirs(target);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            task.setStatus("FAILED");
            task.setErrorMessage("写盘失败: " + e.getMessage());
            taskMapper.updateById(task);
            throw new BizException(ResultCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        }

        // 异步解析
        asyncParse(taskId, target);
        return taskId;
    }

    @Async
    public void asyncParse(String taskId, File target) {
        IngestTask task = taskMapper.selectOne(new LambdaQueryWrapper<IngestTask>().eq(IngestTask::getTaskId, taskId));
        if (task == null) return;
        task.setStatus("PARSING");
        taskMapper.updateById(task);

        try {
            String encoding = EncodingDetector.detect(target.getAbsolutePath());
            task.setEncoding(encoding);
            Charset cs = EncodingDetector.toCharset(encoding);

            List<String> columns;
            List<Map<String, Object>> rows;
            String sep = null;
            String fmt = null;

            switch (task.getFileType()) {
                case "csv":
                case "tsv": {
                    CsvParser.CsvResult cr = CsvParser.parse(target, cs);
                    columns = cr.columns;
                    rows = cr.rows;
                    sep = cr.separator;
                    break;
                }
                case "json": {
                    JsonParser.JsonResult jr = JsonParser.parse(target);
                    columns = jr.columns;
                    rows = jr.rows;
                    fmt = jr.format;
                    break;
                }
                case "log":
                case "txt": {
                    LogParser.LogResult lr = LogParser.parse(target, cs);
                    columns = lr.columns;
                    rows = lr.rows;
                    fmt = lr.detectedFormat;
                    break;
                }
                default:
                    throw new IllegalArgumentException("不支持: " + task.getFileType());
            }

            if (sep != null) task.setSeparator(sep);
            // 质量报告
            QualityReport qr = computeQuality(columns, rows, encoding, sep);
            task.setQualityJson(json_try(qr));
            task.setColumnsJson(json_try(columns));
            task.setTotalRows((long) rows.size());
            task.setTotalColumns((long) columns.size());
            task.setStatus("READY");
            task.setFinishedAt(LocalDateTime.now());
            // 备注 format
            if (fmt != null) {
                Map<String, Object> extra = new HashMap<>();
                extra.put("format", fmt);
                task.setErrorMessage(json_try(extra));
            }
        } catch (Exception e) {
            log.error("解析失败: {}", e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
        }
        taskMapper.updateById(task);
    }

    @Override
    public IngestTask status(String taskId) {
        return require(taskId);
    }

    @Override
    public QualityReport quality(String taskId) {
        IngestTask task = require(taskId);
        if (task.getQualityJson() == null) throw new BizException(ResultCode.BAD_REQUEST, "暂无质量报告 (任务未完成)");
        try {
            return json.readValue(task.getQualityJson(), QualityReport.class);
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "质量报告解析失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> preview(String taskId, int limit) {
        IngestTask task = require(taskId);
        if (!"READY".equals(task.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "任务未完成: " + task.getStatus());
        }
        File target = new File(uploadDir, task.getTaskId() + "." + task.getFileType());
        if (!target.exists()) throw new BizException(ResultCode.NOT_FOUND, "原文件已删除");
        Charset cs = EncodingDetector.toCharset(task.getEncoding());
        try {
            switch (task.getFileType()) {
                case "csv":
                case "tsv": {
                    CsvParser.CsvResult r = CsvParser.parse(target, cs);
                    return r.rows.stream().limit(limit).collect(Collectors.toList());
                }
                case "json": {
                    JsonParser.JsonResult r = JsonParser.parse(target);
                    return r.rows.stream().limit(limit).collect(Collectors.toList());
                }
                case "log":
                case "txt": {
                    LogParser.LogResult r = LogParser.parse(target, cs);
                    return r.rows.stream().limit(limit).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "预览失败: " + e.getMessage());
        }
        return List.of();
    }

    @Override
    public void reparse(String taskId, String separator, String encoding) {
        IngestTask task = require(taskId);
        if (separator != null) task.setSeparator(separator);
        if (encoding != null) task.setEncoding(encoding);
        task.setStatus("PENDING");
        taskMapper.updateById(task);
        File target = new File(uploadDir, task.getTaskId() + "." + task.getFileType());
        asyncParse(taskId, target);
    }

    @Override
    public List<IngestTask> history(Long userId, int page, int size) {
        return taskMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<IngestTask>().eq(IngestTask::getUserId, userId)
                        .orderByDesc(IngestTask::getCreatedAt)).getRecords();
    }

    // ---- helpers ----

    private IngestTask require(String taskId) {
        IngestTask t = taskMapper.selectOne(new LambdaQueryWrapper<IngestTask>().eq(IngestTask::getTaskId, taskId));
        if (t == null) throw new BizException(ResultCode.NOT_FOUND, "任务不存在");
        return t;
    }

    private QualityReport computeQuality(List<String> columns, List<Map<String, Object>> rows,
                                          String encoding, String separator) {
        QualityReport.QualityReportBuilder b = QualityReport.builder()
                .totalRows((long) rows.size())
                .totalColumns((long) columns.size())
                .encoding(encoding)
                .separator(separator);
        if (rows.isEmpty()) return b.build();

        long totalCells = (long) rows.size() * columns.size();
        long nullCells = 0;
        // 行级去重
        Set<String> rowKeys = new HashSet<>();
        long dupRows = 0;
        for (Map<String, Object> row : rows) {
            int nullsInRow = 0;
            for (String col : columns) if (row.get(col) == null) nullsInRow++;
            nullCells += nullsInRow;
            String key = row.toString();
            if (!rowKeys.add(key)) dupRows++;
        }

        b.nullCellCount(nullCells)
                .nullRate(totalCells > 0 ? (double) nullCells / totalCells : 0.0)
                .duplicateRowCount(dupRows);

        // 每列质量
        List<QualityReport.ColumnQuality> cqs = new ArrayList<>();
        for (String col : columns) {
            cqs.add(computeColumnQuality(col, rows));
        }
        b.columnQualities(cqs);
        return b.build();
    }

    private QualityReport.ColumnQuality computeColumnQuality(String col, List<Map<String, Object>> rows) {
        long nulls = 0;
        Set<Object> distincts = new HashSet<>();
        // 类型推断
        int strCnt = 0, numCnt = 0, dateCnt = 0, boolCnt = 0;
        double sum = 0;
        int numCount = 0;
        Object min = null, max = null;
        Map<String, Integer> valueCount = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Object v = row.get(col);
            if (v == null) {
                nulls++;
                continue;
            }
            distincts.add(v);
            String s = v.toString();
            if (s.matches("-?\\d+(\\.\\d+)?")) {
                numCnt++;
                try {
                    double d = Double.parseDouble(s);
                    sum += d;
                    numCount++;
                    if (min == null || d < (double) min) min = d;
                    if (max == null || d > (double) max) max = d;
                } catch (Exception ignored) {}
            } else if (s.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                dateCnt++;
            } else if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                boolCnt++;
            } else {
                strCnt++;
            }
            valueCount.merge(s, 1, Integer::sum);
        }
        int total = rows.size();
        String type = numCnt > strCnt && numCnt > dateCnt && numCnt > boolCnt ? "NUMBER"
                : dateCnt > strCnt ? "DATE" : boolCnt > strCnt ? "BOOL" : "STRING";
        // top 5
        List<String> top = valueCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return QualityReport.ColumnQuality.builder()
                .name(col)
                .inferredType(type)
                .nullCount(nulls)
                .distinctCount((long) distincts.size())
                .nullRate(total > 0 ? (double) nulls / total : 0.0)
                .minValue(min)
                .maxValue(max)
                .avgValue(numCount > 0 ? sum / numCount : null)
                .topValues(top)
                .build();
    }

    private String json_try(Object o) {
        try { return json.writeValueAsString(o); } catch (Exception e) { return null; }
    }
}
