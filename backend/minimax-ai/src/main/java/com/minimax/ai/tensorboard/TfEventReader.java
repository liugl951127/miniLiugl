package com.minimax.ai.tensorboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TensorBoard Events Reader (V2.8.7)
 *
 * <p>解析 <code>events.tfevents.*</code> 文件, 提取 scalar / text 事件.</p>
 *
 * <h3>协议格式</h3>
 * <pre>
 *   magic (8 bytes, 0xA55A0001) + crc32 (4 bytes) + padding (20 bytes) = 32 bytes header
 *   record 1: varint length + protobuf Event + crc32 (4 bytes)
 *   record 2: ...
 * </pre>
 *
 * <h3>支持事件类型</h3>
 * <ul>
 *   <li>SCALAR: 数值指标 (loss/accuracy/lr)</li>
 *   <li>TEXT: 文本 (prompt/hyper-params)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Slf4j
@Component
public class TfEventReader {

    private final String logDir = resolveLogDir();

    private static String resolveLogDir() {
        String p = System.getProperty("MINIMAX_LOG_DIR");
        if (p != null && !p.isEmpty()) return p;
        return System.getenv().getOrDefault("MINIMAX_LOG_DIR", "/tmp/minimax-runs");
    }

    // ============= Public API =============

    /**
     * 列出所有 run
     */
    public List<String> listRuns() {
        Path base = Paths.get(logDir);
        if (!Files.exists(base)) return Collections.emptyList();
        try (Stream<Path> stream = Files.list(base)) {
            return stream
                .filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("[tfevents] 读取 runs 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 run 的所有 tag
     */
    public List<String> listTags(String runId) {
        Map<String, List<ScalarPoint>> scalars = readScalars(runId, null);
        return new ArrayList<>(scalars.keySet());
    }

    /**
     * 读取 scalar 数据 (按 tag)
     */
    public Map<String, List<ScalarPoint>> readScalars(String runId, String tagFilter) {
        Map<String, List<ScalarPoint>> result = new LinkedHashMap<>();
        for (TfEvent ev : readAllEvents(runId)) {
            if (ev.type == EventType.SCALAR) {
                if (tagFilter != null && !tagFilter.equals(ev.tag)) continue;
                result.computeIfAbsent(ev.tag, k -> new ArrayList<>())
                    .add(ScalarPoint.builder()
                        .step(ev.step)
                        .value(ev.value)
                        .wallTime(ev.wallTime)
                        .build());
            }
        }
        // 按 step 排序
        for (List<ScalarPoint> list : result.values()) {
            list.sort(Comparator.comparingLong(ScalarPoint::getStep));
        }
        return result;
    }

    /**
     * 读取单个 tag 的最近 N 个点
     */
    public List<ScalarPoint> readLastN(String runId, String tag, int n) {
        Map<String, List<ScalarPoint>> all = readScalars(runId, tag);
        List<ScalarPoint> list = all.getOrDefault(tag, Collections.emptyList());
        int from = Math.max(0, list.size() - n);
        return list.subList(from, list.size());
    }

    /**
     * 健康检查: logDir 可读
     */
    public Map<String, Object> health() {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("logDir", logDir);
        h.put("exists", Files.exists(Paths.get(logDir)));
        h.put("writable", Files.isWritable(Paths.get(logDir)));
        h.put("runs", listRuns().size());
        return h;
    }

    // ============= V2.8.9 统计分析 =============

    /**
     * 计算 run/tag 的统计信息 (min/max/mean/std/percentiles)
     *
     * @param runId run ID
     * @param tag   tag 名
     * @return 统计 Map, 包含 count/min/max/mean/std/median/p25/p75/p95/p99
     */
    public Map<String, Object> computeStats(String runId, String tag) {
        List<TfEventReader.ScalarPoint> points = readScalars(runId, tag).getOrDefault(tag, Collections.emptyList());
        if (points.isEmpty()) {
            return Map.of("runId", runId, "tag", tag, "count", 0);
        }
        double[] values = points.stream().mapToDouble(TfEventReader.ScalarPoint::getValue).toArray();
        java.util.Arrays.sort(values);

        double min = values[0];
        double max = values[values.length - 1];
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / values.length;
        double variance = 0;
        for (double v : values) variance += (v - mean) * (v - mean);
        variance /= values.length;
        double std = Math.sqrt(variance);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("runId", runId);
        stats.put("tag", tag);
        stats.put("count", values.length);
        stats.put("min", min);
        stats.put("max", max);
        stats.put("mean", mean);
        stats.put("std", std);
        stats.put("median", percentile(values, 0.5));
        stats.put("p25", percentile(values, 0.25));
        stats.put("p75", percentile(values, 0.75));
        stats.put("p95", percentile(values, 0.95));
        stats.put("p99", percentile(values, 0.99));
        return stats;
    }

    /**
     * 计算直方图 (N 个 bins)
     *
     * @param runId run ID
     * @param tag   tag 名
     * @param bins  分箱数 (默认 20)
     * @return [binEdges, counts]
     */
    public Map<String, Object> computeHistogram(String runId, String tag, int bins) {
        if (bins <= 0) bins = 20;
        if (bins > 100) bins = 100;
        List<TfEventReader.ScalarPoint> points = readScalars(runId, tag).getOrDefault(tag, Collections.emptyList());
        if (points.isEmpty()) {
            return Map.of("runId", runId, "tag", tag, "bins", new double[0], "counts", new int[0]);
        }
        double[] values = points.stream().mapToDouble(TfEventReader.ScalarPoint::getValue).toArray();
        double min = values[0];
        double max = values[values.length - 1];
        if (min == max) {
            int[] counts = new int[bins];
            counts[0] = values.length;
            return Map.of("runId", runId, "tag", tag,
                "bins", new double[]{min, max}, "counts", counts);
        }
        double step = (max - min) / bins;
        int[] counts = new int[bins];
        double[] binEdges = new double[bins + 1];
        for (int i = 0; i <= bins; i++) binEdges[i] = min + step * i;

        for (double v : values) {
            int idx = (int) ((v - min) / step);
            if (idx >= bins) idx = bins - 1;
            if (idx < 0) idx = 0;
            counts[idx]++;
        }

        Map<String, Object> h = new LinkedHashMap<>();
        h.put("runId", runId);
        h.put("tag", tag);
        h.put("bins", binEdges);
        h.put("counts", counts);
        h.put("min", min);
        h.put("max", max);
        h.put("count", values.length);
        return h;
    }

    /**
     * 多 run 同 tag 对比统计 (用于跨 run 表格)
     */
    public List<Map<String, Object>> compareRunsStats(List<String> runIds, String tag) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String rid : runIds) {
            result.add(computeStats(rid, tag));
        }
        return result;
    }

    private double percentile(double[] sorted, double q) {
        if (sorted.length == 0) return 0;
        if (q <= 0) return sorted[0];
        if (q >= 1) return sorted[sorted.length - 1];
        double pos = q * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted[lo];
        double frac = pos - lo;
        return sorted[lo] * (1 - frac) + sorted[hi] * frac;
    }

    // ============= 内部实现 =============

    private List<TfEvent> readAllEvents(String runId) {
        List<TfEvent> events = new ArrayList<>();
        Path dir = Paths.get(logDir, runId);
        if (!Files.exists(dir)) return events;
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                .filter(p -> p.getFileName().toString().startsWith("events.tfevents"))
                .sorted()
                .collect(Collectors.toList());
            for (Path file : files) {
                events.addAll(parseFile(file));
            }
        } catch (IOException e) {
            log.warn("[tfevents] 列出文件失败: {}", e.getMessage());
        }
        return events;
    }

    private List<TfEvent> parseFile(Path file) {
        List<TfEvent> events = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new FileInputStream(file.toFile()))) {
            // 1. 跳过文件头 32 字节
            byte[] header = new byte[32];
            in.readFully(header);

            // 2. 循环读取记录
            while (in.available() > 0) {
                try {
                    int length = readVarint(in);
                    if (length < 0 || length > 10_000_000) break; // 防止异常
                    byte[] payload = new byte[length];
                    in.readFully(payload);
                    // CRC32 (4 字节, 跳过)
                    in.readInt();
                    // 解析 protobuf (简化)
                    TfEvent ev = parseEvent(payload);
                    if (ev != null) events.add(ev);
                } catch (Exception e) {
                    log.debug("[tfevents] 记录解析失败: {}", e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("[tfevents] 文件读取失败 {}: {}", file, e.getMessage());
        }
        return events;
    }

    /**
     * 极简 protobuf 解析 (只解析 wall_time/step/summary)
     */
    private TfEvent parseEvent(byte[] data) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data);
            double wallTime = 0;
            long step = 0;
            String tag = null;
            double value = 0;
            EventType type = null;
            String text = null;

            while (buf.hasRemaining()) {
                int tagByte = buf.get() & 0xFF;
                int fieldNumber = tagByte >>> 3;
                int wireType = tagByte & 0x07;

                if (wireType == 1) { // fixed64
                    if (fieldNumber == 1) {
                        wallTime = buf.getDouble();
                    } else {
                        buf.getLong();
                    }
                } else if (wireType == 0) { // varint
                    long v = readVarintFromBuffer(buf);
                    if (fieldNumber == 2) {
                        step = v;
                    } else {
                        // skip
                    }
                } else if (wireType == 2) { // length-delimited
                    int len = (int) readVarintFromBuffer(buf);
                    if (len < 0 || len > buf.remaining()) break;
                    byte[] bytes = new byte[len];
                    buf.get(bytes);
                    if (fieldNumber == 9) {
                        // 解析 summary
                        ParseResult pr = parseSummary(bytes);
                        if (pr != null) {
                            tag = pr.tag;
                            value = pr.value;
                            type = EventType.SCALAR;
                        }
                    } else if (text == null && new String(bytes).startsWith("TEXT:")) {
                        // 文本事件
                        String s = new String(bytes);
                        int colon1 = s.indexOf(':');
                        int colon2 = s.indexOf(':', colon1 + 1);
                        if (colon1 > 0 && colon2 > 0) {
                            tag = s.substring(colon1 + 1, colon2);
                            text = s.substring(colon2 + 1);
                            type = EventType.TEXT;
                        }
                    }
                } else {
                    // skip unknown
                    break;
                }
            }

            if (type != null) {
                return TfEvent.builder()
                    .wallTime(wallTime)
                    .step(step)
                    .tag(tag)
                    .value(value)
                    .text(text)
                    .type(type)
                    .build();
            }
        } catch (Exception e) {
            log.debug("[tfevents] Event 解析失败: {}", e.getMessage());
        }
        return null;
    }

    private ParseResult parseSummary(byte[] data) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data);
            String tag = null;
            float simpleValue = 0;
            while (buf.hasRemaining()) {
                int tagByte = buf.get() & 0xFF;
                int fieldNumber = tagByte >>> 3;
                int wireType = tagByte & 0x07;
                if (wireType == 2) {
                    int len = (int) readVarintFromBuffer(buf);
                    if (len < 0 || len > buf.remaining()) return null;
                    byte[] bytes = new byte[len];
                    buf.get(bytes);
                    if (fieldNumber == 1) {
                        // Summary.Value
                        ByteBuffer vb = ByteBuffer.wrap(bytes);
                        while (vb.hasRemaining()) {
                            int vTag = vb.get() & 0xFF;
                            int vField = vTag >>> 3;
                            int vWire = vTag & 0x07;
                            if (vWire == 2) {
                                int vLen = (int) readVarintFromBuffer(vb);
                                if (vField == 1) {
                                    byte[] tagBytes = new byte[vLen];
                                    vb.get(tagBytes);
                                    tag = new String(tagBytes);
                                } else {
                                    vb.position(vb.position() + vLen);
                                }
                            } else if (vWire == 5) {
                                if (vField == 2) {
                                    simpleValue = vb.getFloat();
                                } else {
                                    vb.getFloat();
                                }
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            if (tag != null) {
                return new ParseResult(tag, simpleValue);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private long readVarintFromBuffer(ByteBuffer buf) {
        long result = 0;
        int shift = 0;
        while (buf.hasRemaining()) {
            byte b = buf.get();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
            if (shift > 64) break;
        }
        return result;
    }

    private int readVarint(DataInputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            int b = in.read();
            if (b < 0) return -1;
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
            if (shift > 32) return -1;
        }
    }

    // ============= 内部类型 =============

    private enum EventType { SCALAR, TEXT }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    private static class TfEvent {
        double wallTime;
        long step;
        String tag;
        double value;
        String text;
        EventType type;
    }

    private static class ParseResult {
        String tag;
        double value;
        ParseResult(String t, double v) { tag = t; value = v; }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScalarPoint {
        private long step;
        private double value;
        private double wallTime;
    }
}
