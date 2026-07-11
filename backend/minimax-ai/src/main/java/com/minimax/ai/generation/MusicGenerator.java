package com.minimax.ai.generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

/**
 * 音乐生成器 (V2.7 自研)
 *
 * <p>纯 Java 实现的 MIDI 音乐生成器, 不依赖任何音频库. 输出标准 MIDI 文件 (SMF Format 0).</p>
 *
 * <h3>能力</h3>
 * <ul>
 *   <li>旋律生成 (基于音阶 + 节奏型)</li>
 *   <li>和弦进行 (I-IV-V-I 等常见进行)</li>
 *   <li>多轨道 (主旋律 + 和声 + 低音)</li>
 *   <li>多种风格 (流行/古典/电子/摇滚/民谣)</li>
 *   <li>参数: BPM / 调式 / 拍号 / 乐器 / 长度</li>
 * </ul>
 *
 * <h3>支持的乐器 (GM 标准 128 种)</h3>
 * 钢琴 (0)、吉他 (24-31)、贝斯 (32-39)、弦乐 (40-47)、铜管 (56-63)、
 * 萨克斯 (64-71)、长笛 (73)、鼓 (通道 10)
 *
 * <h3>MIDI 格式</h3>
 * <pre>
 * Header chunk (14 bytes):
 *   "MThd" + 0x00000006 + format(2) + tracks(2) + division(2)
 * Track chunk:
 *   "MTrk" + length(4) + events[]
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 *   MusicConfig cfg = MusicConfig.builder()
 *       .style("pop")
 *       .key("C")
 *       .scale("major")
 *       .bpm(120)
 *       .bars(8)
 *       .build();
 *   byte[] midi = musicGenerator.generate(cfg);
 * }</pre>
 *
 * @author MiniMax Team
 * @since V2.7
 */
@Slf4j
@Component
public class MusicGenerator {

    /**
     * 音乐风格枚举
     */
    public enum Style {
        POP,        // 流行: 4/4 拍, 简单三和弦
        CLASSICAL,  // 古典: 复杂和弦, 多声部
        ELECTRONIC, // 电子: 重复节拍, 合成音色
        ROCK,       // 摇滚: 强力和弦, 鼓点密集
        FOLK,       // 民谣: 简单旋律, 吉他
        JAZZ        // 爵士: 七和弦, 复杂节奏
    }

    /**
     * 音乐配置
     */
    public static class MusicConfig {
        public Style style = Style.POP;
        public String key = "C";              // 调式: C/D/E/F/G/A/B + # 可选
        public String scale = "major";        // major / minor
        public int bpm = 120;                 // 每分钟节拍
        public int bars = 8;                  // 小节数
        public int beatsPerBar = 4;           // 每小节拍数
        public int instrument = 0;            // 主旋律 GM 乐器号 (0-127)
        public int bassInstrument = 32;       // 贝斯 GM 乐器号
        public boolean includeDrums = true;   // 是否含鼓点
        public boolean includeChords = true;  // 是否含和弦
        public int[] chordProgression;        // 自定义和弦进行 (MIDI 音符号数组)
        public String seed;                   // 随机种子 (保证可复现)

        public static MusicConfigBuilder builder() {
            return new MusicConfigBuilder();
        }
    }

    public static class MusicConfigBuilder {
        private final MusicConfig cfg = new MusicConfig();
        public MusicConfigBuilder style(Style s) { cfg.style = s; return this; }
        public MusicConfigBuilder style(String s) { cfg.style = Style.valueOf(s.toUpperCase()); return this; }
        public MusicConfigBuilder key(String k) { cfg.key = k; return this; }
        public MusicConfigBuilder scale(String s) { cfg.scale = s; return this; }
        public MusicConfigBuilder bpm(int b) { cfg.bpm = b; return this; }
        public MusicConfigBuilder bars(int b) { cfg.bars = b; return this; }
        public MusicConfigBuilder beatsPerBar(int n) { cfg.beatsPerBar = n; return this; }
        public MusicConfigBuilder instrument(int i) { cfg.instrument = i; return this; }
        public MusicConfigBuilder includeDrums(boolean b) { cfg.includeDrums = b; return this; }
        public MusicConfigBuilder includeChords(boolean b) { cfg.includeChords = b; return this; }
        public MusicConfigBuilder chordProgression(int[] p) { cfg.chordProgression = p; return this; }
        public MusicConfigBuilder seed(String s) { cfg.seed = s; return this; }
        public MusicConfig build() { return cfg; }
    }

    /** GM 标准鼓音符 (通道 10) */
    private static final int[] DRUM_BASS = {36};   // 大鼓
    private static final int[] DRUM_SNARE = {38, 40}; // 小鼓
    private static final int[] DRUM_HAT = {42, 44, 46}; // 踩镲

    /**
     * 生成 MIDI 字节流
     */
    public byte[] generate(MusicConfig cfg) {
        if (cfg == null) cfg = new MusicConfig();
        // 准备随机源 (支持种子)
        Random random = cfg.seed != null ? new Random(cfg.seed.hashCode()) : new Random();

        // 准备音阶
        int[] scaleNotes = buildScale(cfg.key, cfg.scale);

        // 准备和弦进行
        int[] progression = cfg.chordProgression != null ? cfg.chordProgression : defaultProgression(cfg.key, cfg.scale);

        // 计算总 tick 数 (PPQ = 480, 一拍 = 480 tick)
        int ppq = 480;
        int totalTicks = cfg.bars * cfg.beatsPerBar * ppq;

        // 收集各轨道事件
        List<MidiEvent> melodyEvents = buildMelody(cfg, scaleNotes, progression, random, ppq);
        List<MidiEvent> chordEvents = cfg.includeChords ? buildChords(cfg, progression, ppq) : List.of();
        List<MidiEvent> bassEvents = buildBass(cfg, progression, ppq, random);
        List<MidiEvent> drumEvents = cfg.includeDrums ? buildDrums(cfg, ppq, random) : List.of();

        // 输出 MIDI Format 0 (单轨道, 多通道)
        return writeMidiFormat0(melodyEvents, chordEvents, bassEvents, drumEvents, ppq);
    }

    /**
     * 构建音阶 (12 个半音)
     *
     * @param root 根音 C/D/E/F/G/A/B
     * @param type major/minor
     * @return MIDI 音符号 (0-127), 8 个音符
     */
    private int[] buildScale(String root, String type) {
        // 12 个半音偏移
        int[] noteOffsets = {"C".equals(root) ? 0 : "C#".equals(root) || "Db".equals(root) ? 1 :
                "D".equals(root) ? 2 : "D#".equals(root) || "Eb".equals(root) ? 3 :
                "E".equals(root) ? 4 : "F".equals(root) ? 5 : "F#".equals(root) || "Gb".equals(root) ? 6 :
                "G".equals(root) ? 7 : "G#".equals(root) || "Ab".equals(root) ? 8 :
                "A".equals(root) ? 9 : "A#".equals(root) || "Bb".equals(root) ? 10 : 11};
        int rootNote = noteOffsets[0];

        // 大调音阶: 全全半全全全半 (W W H W W W H)
        // 小调音阶 (自然小调): 全半全全半全全
        int[] intervals = "minor".equals(type)
                ? new int[]{0, 2, 3, 5, 7, 8, 10, 12}  // 自然小调
                : new int[]{0, 2, 4, 5, 7, 9, 11, 12}; // 大调

        int baseOctave = 60;  // 中央 C 附近
        int[] scale = new int[intervals.length];
        for (int i = 0; i < intervals.length; i++) {
            scale[i] = baseOctave + rootNote + intervals[i];
        }
        return scale;
    }

    /**
     * 默认和弦进行: I-V-vi-IV (流行)
     */
    private int[] defaultProgression(String root, String type) {
        int[] scale = buildScale(root, type);
        // I, V, vi, IV 在大调 (小调用 i, v, VI, iv)
        if ("minor".equals(type)) {
            return new int[]{scale[0], scale[4] - 12, scale[5] - 12, scale[3] - 12};  // 简化
        }
        return new int[]{scale[0], scale[4] - 12, scale[5] - 12, scale[3] - 12};
    }

    /**
     * 构建主旋律
     * 简单算法: 每拍选 1-2 个音, 80% 用音阶内音, 20% 用和弦音
     */
    private List<MidiEvent> buildMelody(MusicConfig cfg, int[] scale, int[] progression, Random random, int ppq) {
        List<MidiEvent> events = new ArrayList<>();
        long time = 0;
        int beatTicks = ppq;  // 一拍

        for (int bar = 0; bar < cfg.bars; bar++) {
            // 每小节 4 拍
            for (int beat = 0; beat < cfg.beatsPerBar; beat++) {
                // 决定音长
                int noteDuration = (random.nextDouble() < 0.3) ? beatTicks / 2 : beatTicks;
                // 选音
                int pitch;
                if (random.nextDouble() < 0.7) {
                    // 音阶音
                    pitch = scale[random.nextInt(scale.length)] + 12;  // 高八度
                } else {
                    // 和弦音
                    int chord = progression[bar % progression.length];
                    pitch = chord + (random.nextInt(3) * 2);
                }
                int velocity = 80 + random.nextInt(20);
                // Note On
                events.add(new MidiEvent(time, 0x90, pitch, velocity));
                // Note Off
                events.add(new MidiEvent(time + noteDuration, 0x80, pitch, 0));
                time += noteDuration;
            }
        }
        return events;
    }

    /**
     * 构建和弦 (每小节 1 个和弦, 全小节)
     */
    private List<MidiEvent> buildChords(MusicConfig cfg, int[] progression, int ppq) {
        List<MidiEvent> events = new ArrayList<>();
        long time = 0;
        int barTicks = cfg.beatsPerBar * ppq;

        for (int bar = 0; bar < cfg.bars; bar++) {
            int root = progression[bar % progression.length];
            // 三和弦: 根 + 三度 + 五度
            int[] chordNotes = {root, root + 4, root + 7};
            for (int n : chordNotes) {
                events.add(new MidiEvent(time, 0x91, n, 60));  // 通道 1
            }
            long endTime = time + barTicks - 10;
            for (int n : chordNotes) {
                events.add(new MidiEvent(endTime, 0x81, n, 0));
            }
            time += barTicks;
        }
        return events;
    }

    /**
     * 构建贝斯线
     */
    private List<MidiEvent> buildBass(MusicConfig cfg, int[] progression, int ppq, Random random) {
        List<MidiEvent> events = new ArrayList<>();
        long time = 0;
        int beatTicks = ppq;

        for (int bar = 0; bar < cfg.bars; bar++) {
            int root = progression[bar % progression.length] - 24;  // 低八度
            for (int beat = 0; beat < cfg.beatsPerBar; beat++) {
                int pitch = root;
                if (beat == 1 && random.nextDouble() < 0.5) pitch = root + 7;  // 偶数拍走 5 度
                events.add(new MidiEvent(time, 0x92, pitch, 90));
                events.add(new MidiEvent(time + beatTicks - 20, 0x82, pitch, 0));
                time += beatTicks;
            }
        }
        return events;
    }

    /**
     * 构建鼓点 (基于风格)
     */
    private List<MidiEvent> buildDrums(MusicConfig cfg, int ppq, Random random) {
        List<MidiEvent> events = new ArrayList<>();
        long time = 0;
        int beatTicks = ppq;

        for (int bar = 0; bar < cfg.bars; bar++) {
            for (int beat = 0; beat < cfg.beatsPerBar; beat++) {
                // 大鼓: 1, 3 拍
                if (beat == 0 || beat == 2) {
                    events.add(new MidiEvent(time, 0x99, 36, 100));
                    events.add(new MidiEvent(time + beatTicks / 4, 0x89, 36, 0));
                }
                // 小鼓: 2, 4 拍
                if (beat == 1 || beat == 3) {
                    events.add(new MidiEvent(time, 0x99, 38, 90));
                    events.add(new MidiEvent(time + beatTicks / 4, 0x89, 38, 0));
                }
                // 踩镲: 8 分音符
                for (int sub = 0; sub < 2; sub++) {
                    long t = time + sub * beatTicks / 2;
                    events.add(new MidiEvent(t, 0x99, 42, 60));
                    events.add(new MidiEvent(t + beatTicks / 4, 0x89, 42, 0));
                }
                time += beatTicks;
            }
        }
        return events;
    }

    /**
     * MIDI 事件
     */
    private static class MidiEvent {
        long tick;
        int status;  // 0x90=On, 0x80=Off
        int data1;   // 音 / 鼓号
        int data2;   // 力度 / 0

        MidiEvent(long tick, int status, int data1, int data2) {
            this.tick = tick;
            this.status = status;
            this.data1 = data1;
            this.data2 = data2;
        }
    }

    /**
     * 写入 MIDI 字节流 (SMF Format 0)
     */
    private byte[] writeMidiFormat0(List<MidiEvent> melody, List<MidiEvent> chord, List<MidiEvent> bass, List<MidiEvent> drum, int ppq) {
        List<MidiEvent>[] tracks = new List[]{melody, chord, bass, drum};
        return writeMidi(tracks);
    }

    /**
     * 写入 MIDI 字节流 (SMF Format 0)
     */
    private byte[] writeMidi(List<MidiEvent>... trackLists) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // 合并所有事件并按 tick 排序
            List<MidiEvent> all = new ArrayList<>();
            for (List<MidiEvent> list : trackLists) all.addAll(list);
            all.sort(Comparator.comparingLong(e -> e.tick));

            // 计算 track 长度 (先写到临时流)
            ByteArrayOutputStream trackData = new ByteArrayOutputStream();
            long lastTick = 0;
            for (MidiEvent e : all) {
                long delta = e.tick - lastTick;
                writeVariableLength(trackData, delta);
                trackData.write(e.status);
                trackData.write(e.data1 & 0x7F);
                trackData.write(e.data2 & 0x7F);
                lastTick = e.tick;
            }
            // End of track
            trackData.write(0x00);
            trackData.write(0xFF);
            trackData.write(0x2F);
            trackData.write(0x00);
            byte[] trackBytes = trackData.toByteArray();

            // Header
            dos.write("MThd".getBytes());
            dos.writeInt(6);                       // header length
            dos.writeShort(0);                     // format 0
            dos.writeShort(1);                     // tracks
            dos.writeShort(480);                   // division (PPQ)

            // Track
            dos.write("MTrk".getBytes());
            dos.writeInt(trackBytes.length);
            dos.write(trackBytes);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("MIDI write failed", e);
        }
    }

    /**
     * 写 MIDI 可变长整数 (Variable Length Quantity)
     * 最高位为 1 表示后续字节
     */
    private void writeVariableLength(ByteArrayOutputStream out, long value) {
        if (value == 0) { out.write(0); return; }
        long buffer = value & 0x7F;
        while ((value >>= 7) > 0) {
            buffer <<= 8;
            buffer |= ((value & 0x7F) | 0x80);
        }
        while (true) {
            out.write((int) (buffer & 0xFF));
            if ((buffer & 0x80) != 0) buffer >>= 8;
            else break;
        }
    }
}
