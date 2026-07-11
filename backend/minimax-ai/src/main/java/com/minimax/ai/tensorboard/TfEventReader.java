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
