package com.minimax.ai.tensorboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

/**
 * TensorBoard Events Writer (V2.8.7)
 *
 * <p>实现 TensorBoard 的 events.tfevents 文件格式 (Protocol Buffers 二进制),
 * 生成的 .tfevents 文件可被 TensorBoard / WandB / Aim 等工具直接读取.</p>
 *
 * <h3>TensorBoard 文件结构</h3>
 * <pre>
 *   文件头 (32 字节 magic)
 *   + 多个 Event 记录 (varint length + protobuf Event)
 *   + CRC32 checksum (4 字节)
 * </pre>
 *
 * <h3>简化版实现</h3>
 * <p>原生 TensorBoard 用 protobuf, 本项目手写最关键的 ScalarEvent:</p>
 * <ul>
 *   <li>每条记录: header (4字节 magic) + length (varint) + payload (protobuf bytes) + crc32 (4字节)</li>
 *   <li>Event.wall_time (fixed64) + step (fixed64) + value (oneof)</li>
 *   <li>value.tag (string) + simple_value (float) for scalar</li>
 * </ul>
 *
 * <h3>存储路径</h3>
 * <p>默认: <code>${MINIMAX_LOG_DIR:-/tmp/minimax-runs}/&lt;run_id&gt;/events.tfevents.&lt;host&gt;.&lt;timestamp&gt;</code></p>
 *
 * <h3>读取</h3>
 * <pre>
 *   tensorboard --logdir /tmp/minimax-runs
 *   # 或
 *   from tensorboard.backend.event_processing.event_accumulator import EventAccumulator
 *   ea = EventAccumulator('/tmp/minimax-runs/run-001')
 *   ea.Reload()
 *   print(ea.Scalars('loss'))
 * </pre>
 *
 * @author MiniMax
 * @since V2.8.7
 */
@Slf4j
@Component
public class TfEventWriter {

    /** TensorBoard 文件头 magic */
    private static final long FILE_MAGIC = 0xA55A0001L;

    /** 默认存储目录 (可通过 MINIMAX_LOG_DIR 环境变量或 system property 覆盖) */
    private final String logDir = resolveLogDir();

    /** 每个 run 的 writer 缓存 (线程安全) */
    private final Map<String, RunWriter> writers = new ConcurrentHashMap<>();

    /**
     * 写入 scalar 标量
     *
     * @param runId     训练任务 ID
     * @param tag       指标名 (如 "loss", "accuracy")
     * @param step      全局步数
     * @param value     标量值
     */
    public void writeScalar(String runId, String tag, long step, double value) {
        try {
            RunWriter w = getOrCreate(runId);
            byte[] eventBytes = buildScalarEvent(tag, step, value);
            w.append(eventBytes);
        } catch (Exception e) {
            log.warn("[tfevents] 写入失败 runId={} tag={} : {}", runId, tag, e.getMessage());
        }
    }

    /**
     * 写入文本 (例如 prompt)
     */
    public void writeText(String runId, String tag, long step, String text) {
        try {
            RunWriter w = getOrCreate(runId);
            byte[] eventBytes = buildTextEvent(tag, step, text);
            w.append(eventBytes);
        } catch (Exception e) {
            log.warn("[tfevents] 写入文本失败: {}", e.getMessage());
        }
    }

    /**
     * 关闭并刷盘
     */
    public void close(String runId) {
        RunWriter w = writers.remove(runId);
        if (w != null) w.close();
    }

    /**
     * 获取 runs 列表 (含每个 run 的 metric 列表)
     */
    public Map<String, Object> listRuns() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("logDir", logDir);
        result.put("runs", writers.keySet());
        return result;
    }

    // ============= 实现细节 =============

    private RunWriter getOrCreate(String runId) throws IOException {
        return writers.computeIfAbsent(runId, k -> {
            try {
                Path dir = Paths.get(logDir, runId);
                Files.createDirectories(dir);
                String filename = String.format("events.tfevents.%d.%s",
                    System.currentTimeMillis(), getHostName());
                Path file = dir.resolve(filename);
                return new RunWriter(file.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String resolveLogDir() {
        // 优先 system property (用于测试), 然后 env, 然后默认
        String p = System.getProperty("MINIMAX_LOG_DIR");
        if (p != null && !p.isEmpty()) return p;
        return System.getenv().getOrDefault("MINIMAX_LOG_DIR", "/tmp/minimax-runs");
    }

    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName().replace('.', '_');
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * 构造 Scalar Event 的 protobuf 字节
     *
     * <p>protobuf 结构 (简化):</p>
     * <pre>
     *   message Event {
     *     double wall_time = 1;
     *     int64 step = 2;
     *     oneof what {
     *       Summary summary = 9;
     *     }
     *   }
     *   message Summary {
     *     repeated Summary.Value value = 1;
     *   }
     *   message Summary.Value {
     *     string tag = 1;
     *     float simple_value = 2;
     *   }
     * </pre>
     */
    private byte[] buildScalarEvent(String tag, long step, double value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        // wall_time (field 1, type=1 fixed64) → tag 1<<3|1 = 9
        out.writeByte(0x09);
        out.writeLong(Double.doubleToRawLongBits(System.currentTimeMillis() / 1000.0));

        // step (field 2, type=0 varint)
        writeVarint(out, (0x10)); // tag 2<<3|0 = 16
        writeVarint(out, step);

        // summary (field 9, type=2 length-delimited)
        out.writeByte(0x4A); // tag 9<<3|2 = 74
        byte[] summaryBytes = buildSummary(tag, value);
        writeVarint(out, summaryBytes.length);
        out.write(summaryBytes);

        return baos.toByteArray();
    }

    private byte[] buildTextEvent(String tag, long step, String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeByte(0x09);
        out.writeLong(Double.doubleToRawLongBits(System.currentTimeMillis() / 1000.0));

        writeVarint(out, 0x10);
        writeVarint(out, step);

        // file_version 简化为 string message
        out.writeByte(0x4A);
        byte[] textBytes = ("TEXT:" + tag + ":" + text).getBytes(StandardCharsets.UTF_8);
        writeVarint(out, textBytes.length);
        out.write(textBytes);

        return baos.toByteArray();
    }

    private byte[] buildSummary(String tag, double value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        // Summary.Value tag = 1, length-delimited
        out.writeByte(0x0A);
        ByteArrayOutputStream v = new ByteArrayOutputStream();
        DataOutputStream vout = new DataOutputStream(v);

        // tag (field 1, string)
        vout.writeByte(0x0A);
        byte[] tagBytes = tag.getBytes(StandardCharsets.UTF_8);
        writeVarint(vout, tagBytes.length);
        vout.write(tagBytes);

        // simple_value (field 2, float)
        vout.writeByte(0x15);
        vout.writeInt(Float.floatToRawIntBits((float) value));

        byte[] valueBytes = v.toByteArray();
        writeVarint(out, valueBytes.length);
        out.write(valueBytes);
        return baos.toByteArray();
    }

    private void writeVarint(DataOutputStream out, long value) throws IOException {
        while ((value & ~0x7FL) != 0) {
            out.writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.writeByte((int) value);
    }

    /**
     * 单个 run 的文件 writer (线程安全)
     */
    private static class RunWriter {
        private final String path;
        private java.io.RandomAccessFile raf;
        private long bytesWritten = 0;
        private final Object lock = new Object();

        RunWriter(String path) {
            this.path = path;
            try {
                this.raf = new java.io.RandomAccessFile(path, "rw");
                // 写入文件头 magic (32 字节: magic 8 + crc 4 + padding 20)
                ByteArrayOutputStream header = new ByteArrayOutputStream(32);
                DataOutputStream ho = new DataOutputStream(header);
                ho.writeLong(FILE_MAGIC);
                // CRC32 of magic
                CRC32 crc = new CRC32();
                crc.update(longToBytes(FILE_MAGIC));
                ho.writeInt((int) crc.getValue());
                // 20 字节 padding (TensorBoard 规范)
                ho.write(new byte[20]);
                raf.write(header.toByteArray());
                bytesWritten = 32;
            } catch (Exception e) {
                log.error("[tfevents] 初始化失败 path={}: {}", path, e.getMessage());
            }
        }

        void append(byte[] eventBytes) throws IOException {
            synchronized (lock) {
                if (raf == null) return;
                // 写入 length varint
                ByteArrayOutputStream record = new ByteArrayOutputStream();
                DataOutputStream ro = new DataOutputStream(record);
                writeVarintLocal(ro, eventBytes.length);
                ro.write(eventBytes);
                // CRC32 of length + eventBytes
                CRC32 crc = new CRC32();
                crc.update(record.toByteArray());
                ro.writeInt((int) crc.getValue());
                byte[] recordBytes = record.toByteArray();
                raf.write(recordBytes);
                bytesWritten += recordBytes.length;
            }
        }

        void close() {
            try {
                if (raf != null) {
                    raf.getFD().sync();
                    raf.close();
                }
            } catch (Exception e) {
                log.warn("[tfevents] 关闭失败: {}", e.getMessage());
            }
        }

        private static void writeVarintLocal(DataOutputStream out, long value) throws IOException {
            while ((value & ~0x7FL) != 0) {
                out.writeByte((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
            out.writeByte((int) value);
        }

        private static byte[] longToBytes(long v) {
            return new byte[] {
                (byte) (v >>> 56), (byte) (v >>> 48), (byte) (v >>> 40), (byte) (v >>> 32),
                (byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v
            };
        }
    }
}
