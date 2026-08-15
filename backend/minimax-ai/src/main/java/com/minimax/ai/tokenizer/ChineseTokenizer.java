package com.minimax.ai.tokenizer;

// Slf4j: Lombok 日志
import lombok.extern.slf4j.Slf4j;
// Component: Spring Bean 注解
import org.springframework.stereotype.Component;

// BufferedInputStream: 缓冲输入流 (加速 IO)
import java.io.BufferedInputStream;
// BufferedOutputStream: 缓冲输出流
import java.io.BufferedOutputStream;
// File: 文件对象
import java.io.File;
// FileInputStream: 文件输入流
import java.io.FileInputStream;
// FileOutputStream: 文件输出流
import java.io.FileOutputStream;
// IOException: IO 异常
import java.io.IOException;
// ObjectInputStream: 对象反序列化流
import java.io.ObjectInputStream;
// ObjectOutputStream: 对象序列化流
import java.io.ObjectOutputStream;
// StandardCharsets: 标准字符集 (UTF_8)
import java.nio.charset.StandardCharsets;
// ArrayList: 动态数组
import java.util.ArrayList;
// 集合
import java.util.*;
// ConcurrentHashMap: 线程安全 Map
import java.util.concurrent.ConcurrentHashMap;
// List: 列表接口
import java.util.List;
// Map: 键值对接口
import java.util.Map;

/**
 * 简化版 BPE (Byte Pair Encoding) 中文分词器 (V6.1 详细注释版)
 *
 * <h2>核心功能</h2>
 * 把文本 (中英文混合) 切成 token 序列, 用于 Transformer 输入
 *
 * <h2>为什么需要分词</h2>
 * Transformer 不直接处理字符, 而是处理 token id (整数)
 * 不同的分词方式影响:
 *   - 词表大小 (BPE 通常 8000-50000)
 *   - 序列长度 (子词短, 整词长)
 *   - 语义粒度 (字级 vs 词级 vs 子词级)
 *
 * <h2>分词策略 (本实现)</h2>
 * <ol>
 *   <li>中文字符: 1 字 1 token (避免 BPE 切错罕见词)</li>
 *   <li>英文: 按单词切 (累积字母)</li>
 *   <li>数字: 连续数字 1 个 token</li>
 *   <li>标点: 1 个标点 1 个 token</li>
 *   <li>空白: 分隔符 (不产出 token)</li>
 *   <li>表情: surrogate pair 整字符</li>
 * </ol>
 *
 * <h2>vs 业界方案</h2>
 * <table>
 *   <tr><th>方案</th><th>粒度</th><th>词表</th><th>特点</th></tr>
 *   <tr><td>本实现 (BPE 简化版)</td><td>字+词</td><td>~8K</td><td>零依赖, 教学</td></tr>
 *   <tr><td>jieba</td><td>词</td><td>~300K</td><td>中文友好, 慢</td></tr>
 *   <tr><td>SentencePiece BPE</td><td>子词</td><td>~32K</td><td>Google, 通用</td></tr>
 *   <tr><td>WordPiece</td><td>子词</td><td>~30K</td><td>BERT, ##前缀</td></tr>
 * </table>
 *
 * <h2>不依赖</h2>
 * 纯 Java 实现, 不依赖 jieba/hanlp/SentencePiece
 */
@Slf4j
/**
 * ChineseTokenizer (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 分词器 - ChineseTokenizer.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 ChineseTokenizer 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Component
public class ChineseTokenizer {

    // ============== 特殊 Token ==============
    /**
     * <pad>: 填充 token (序列对齐时用)
     * id=0 是惯例, 多数框架默认 padding 是 0
     */
    public static final int PAD = 0;
    /**
     * <unk>: 未知 token (词表外字符)
     * 用于: 训练时没见过 / 推理时遇到生僻字
     */
    public static final int UNK = 1;
    /**
     * <bos>: Begin Of Sentence (句子开始)
     * 训练时标记输入起点
     */
    public static final int BOS = 2;
    /**
     * <eos>: End Of Sentence (句子结束)
     * 训练时标记输出终点
     */
    public static final int EOS = 3;
    /**
     * <sep>: Separator (分隔符)
     * 预留, 暂未使用
     */
    public static final int SEP = 4;

    // ============== 词表 ==============
    /**
     * 正向: token string → id
     * 例: {"<pad>": 0, "<unk>": 1, "Java": 100, ...}
     * ConcurrentHashMap 线程安全
     */
    private final Map<String, Integer> tokenToId = new ConcurrentHashMap<>();
    /**
     * 反向: id → token string
     * 解码时用
     */
    private final Map<Integer, String> idToToken = new ConcurrentHashMap<>();
    /**
     * 词频统计 (训练时维护)
     * 例: {"Java": 50, "Python": 30, ...}
     */
    private final Map<String, Long> wordFreq = new ConcurrentHashMap<>();

    /** 当前词表大小 (含特殊 token, 默认 5) */
    private int vocabSize = 0;

    /**
     * 构造器: 初始化 5 个特殊 token
     */
    public ChineseTokenizer() {
        // 1. 添加 5 个特殊 token
        tokenToId.put("<pad>", PAD);
        tokenToId.put("<unk>", UNK);
        tokenToId.put("<bos>", BOS);
        tokenToId.put("<eos>", EOS);
        tokenToId.put("<sep>", SEP);
        // 2. 反向映射
        for (int i = 0; i < 5; i++) {
            idToToken.put(i, specialTokenName(i));
        }
        // 3. 词表大小 = 5
        vocabSize = 5;
    }

    /**
     * id → 特殊 token 名称
     * 用 switch 表达式 (Java 14+)
     */
    private static String specialTokenName(int id) {
        return switch (id) {
            case PAD -> "<pad>";
            case UNK -> "<unk>";
            case BOS -> "<bos>";
            case EOS -> "<eos>";
            case SEP -> "<sep>";
            default -> "?";
        };
    }

    // ============== 预分词 ==============
    /**
     * 中文友好的预分词
     *
     * <h2>策略</h2>
     * <ol>
     *   <li>中文字符: 1 字 1 token (避免 BPE 切错罕见词)</li>
     *   <li>英文字母: 累积成单词, 转小写</li>
     *   <li>数字: 连续数字 1 个 token</li>
     *   <li>标点: 1 个标点 1 个 token</li>
     *   <li>空白: 分隔符 (不产出 token)</li>
     *   <li>其他 (emoji/罕用字): 整字符 (支持 surrogate pair)</li>
     * </ol>
     *
     * <h2>示例</h2>
     * <pre>
     *   "Java 是面向对象的" → ["java", "是", "面", "向", "对", "象", "的"]
     *   "Hello World 2026!"  → ["hello", "world", "2026", "!"]
     *   "你好World"          → ["你", "好", "world"]
     * </pre>
     *
     * @param text 输入文本
     * @return token 列表
     */
    public List<String> preTokenize(String text) {
        // 结果列表
        List<String> tokens = new ArrayList<>();
        // 空文本返回空列表
        if (text == null || text.isEmpty()) return tokens;

        // 英文/数字缓冲 (累积成单词)
        StringBuilder buf = new StringBuilder();
        int i = 0;
        // 主循环
        while (i < text.length()) {
            // 取当前字符 (支持 surrogate pair)
            char c = text.charAt(i);
            String ch;  // 完整字符 (1 char 或 2 char surrogate)
            int charLen = 1;  // 字符长度 (1 或 2)

            // surrogate pair 处理: 罕用汉字 / emoji 需 2 个 char
            if (Character.isHighSurrogate(c) && i + 1 < text.length()
                    && Character.isLowSurrogate(text.charAt(i + 1))) {
                ch = text.substring(i, i + 2);
                charLen = 2;
            } else {
                ch = String.valueOf(c);
            }

            // 1. 中文字符 (单 char): 1 字 1 token
            if (isChineseChar(c) && charLen == 1) {
                // 先 flush 英文缓冲
                flushBuf(buf, tokens);
                tokens.add(String.valueOf(c));
            }
            // 2. 英文字母: 累积成单词
            else if (Character.isLetter(c)) {
                buf.append(c);
            }
            // 3. 数字: 连续数字合并
            else if (Character.isDigit(c)) {
                // 先 flush 字母缓冲
                flushBuf(buf, tokens);
                // 累积连续数字
                StringBuilder num = new StringBuilder();
                while (i < text.length() && Character.isDigit(text.charAt(i))) {
                    num.append(text.charAt(i));
                    i++;
                }
                i--;  // 回退 1 (因为外层 i++ 会再加)
                tokens.add(num.toString());
            }
            // 4. 标点: 1 个标点 1 个 token
            else if (isPunctuation(c)) {
                flushBuf(buf, tokens);
                tokens.add(String.valueOf(c));
            }
            // 5. 空白: 分隔符
            else if (Character.isWhitespace(c)) {
                flushBuf(buf, tokens);
                // 不产出 token
            }
            // 6. 其他 (emoji/罕用字): 整字符
            else {
                flushBuf(buf, tokens);
                tokens.add(ch);
                i += charLen;  // surrogate pair 占 2 位
                continue;       // 跳过外层 i++
            }
            i += charLen;
        }
        // 循环结束, flush 最后缓冲
        flushBuf(buf, tokens);
        return tokens;
    }

    /**
     * Flush 英文缓冲 (小写化)
     */
    private void flushBuf(StringBuilder buf, List<String> tokens) {
        if (buf.length() > 0) {
            tokens.add(buf.toString().toLowerCase());  // 英文统一小写
            buf.setLength(0);  // 清空
        }
    }

    /**
     * 是否中文字符 (CJK 全部平面)
     *
     * <h2>覆盖范围</h2>
     * - 0x4E00-0x9FFF: CJK 统一汉字 (基本平面, 20K+ 字)
     * - 0x3400-0x4DBF: CJK 扩展 A (罕用)
     * - 0xF900-0xFAFF: CJK 兼容汉字
     * - 0xD800-0xDBFF: 高位代理 (CJK 扩展 B-E, 罕用)
     *
     * <h2>V5.4+ 扩展</h2>
     * 之前只覆盖基本平面, 导致罕用字乱码
     * 现在覆盖 CJK 全平面 + 兼容汉字
     */
    public static boolean isChineseChar(char c) {
        // 基本平面 + 扩展 A + 兼容 (单 char 即可判)
        if ((c >= 0x4E00 && c <= 0x9FFF) ||    // CJK 统一
            (c >= 0x3400 && c <= 0x4DBF) ||    // CJK 扩展 A
            (c >= 0xF900 && c <= 0xFAFF)) {    // CJK 兼容
            return true;
        }
        // 高位平面 (需 surrogate pair, 这里只判高代理)
        // 完整判: 需要 low surrogate (0xDC00-0xDFFF) 一起
        if (c >= 0xD800 && c <= 0xDBFF) {
            return true;  // 配合下一个 char 解析
        }
        return false;
    }

    /**
     * 取下一个完整字符 (支持 surrogate pair)
     * @return 字符串 (1 或 2 char), null 表示越界
     */
    private static String nextChar(String text, int i) {
        if (i >= text.length()) return null;
        char c = text.charAt(i);
        if (Character.isHighSurrogate(c) && i + 1 < text.length()
                && Character.isLowSurrogate(text.charAt(i + 1))) {
            return text.substring(i, i + 2);
        }
        return String.valueOf(c);
    }

    /**
     * 是否中英文标点
     * 包含: 中英标点 + 常用符号
     */
    public static boolean isPunctuation(char c) {
        return "，。！？、；：.,!?;:\"'()[]{}<>/\\|@#$%^&*+=-_~`《》「」【】".indexOf(c) >= 0;
    }

    // ============== 词表训练 ==============
    /**
     * 训练词表 (BPE 简化版)
     *
     * <h2>算法步骤</h2>
     * <ol>
     *   <li>遍历语料, 预分词 + 统计词频</li>
     *   <li>按频率倒序排</li>
     *   <li>取 top N (高频 token) 加进词表</li>
     *   <li>补 CJK 基本平面单字 (确保中文能编码)</li>
     *   <li>补 ASCII (确保英文/数字能编码)</li>
     * </ol>
     *
     * <h2>简化点</h2>
     * 真实 BPE: 迭代合并最高频的 byte pair
     * 本实现: 直接按词频排序, 取 top N
     * 优势: 简单快; 劣势: 不捕获词内结构
     *
     * @param corpus 训练语料 (多行文本)
     * @param targetVocabSize 目标词表大小 (e.g. 8192)
     */
    public void train(List<String> corpus, int targetVocabSize) {
        log.info("开始训练词表, 语料: {} 行, 目标大小: {}", corpus.size(), targetVocabSize);

        // ====== 1. 统计词频 ======
        wordFreq.clear();
        long total = 0;
        for (String line : corpus) {
            for (String token : preTokenize(line)) {
                // merge: 词频累加
                wordFreq.merge(token, 1L, Long::sum);
                total++;
            }
        }
        log.info("总 token 数: {}, 唯一 token 数: {}", total, wordFreq.size());

        // ====== 2. 按频率倒序排 ======
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(wordFreq.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        // ====== 3. 取 top N 加进词表 ======
        for (int i = 0; i < sorted.size() && vocabSize < targetVocabSize; i++) {
            String token = sorted.get(i).getKey();
            // 跳过已存在 (理论上不会, 但防御)
            if (!tokenToId.containsKey(token)) {
                int id = vocabSize++;
                tokenToId.put(token, id);
                idToToken.put(id, token);
            }
        }

        // ====== 4. 补 CJK 基本平面单字 (确保中文) ======
        // 0x4E00-0x9FFF 覆盖 20K+ 汉字
        for (int cp = 0x4E00; cp <= 0x9FFF && vocabSize < targetVocabSize; cp++) {
            String c = String.valueOf((char) cp);
            if (!tokenToId.containsKey(c)) {
                int id = vocabSize++;
                tokenToId.put(c, id);
                idToToken.put(id, c);
            }
        }

        // ====== 5. ASCII 字符兜底 ======
        // 32-126 是可打印 ASCII
        for (char c = 32; c < 127 && vocabSize < targetVocabSize; c++) {
            String s = String.valueOf(c);
            if (!tokenToId.containsKey(s)) {
                int id = vocabSize++;
                tokenToId.put(s, id);
                idToToken.put(id, s);
            }
        }

        log.info("词表训练完成, 最终大小: {}", vocabSize);
    }

    // ============== 编码/解码 ==============
    /**
     * 编码: 文本 → token ids
     * @param text 输入文本
     * @return token id 数组
     */
    public int[] encode(String text) {
        // 1. 预分词
        List<String> tokens = preTokenize(text);
        // 2. 查词表 (不在则用 UNK)
        int[] ids = new int[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            ids[i] = tokenToId.getOrDefault(tokens.get(i), UNK);
        }
        return ids;
    }

    /**
     * 解码: token ids → 文本
     *
     * <h2>V5.4+ 修复乱码</h2>
     * 之前: 跳过 UNK/BOS/EOS (id<5), 导致输出大量空白
     * 现在:
     *   - PAD (id=0): 跳过
     *   - UNK (id=1): 跳过 (不破坏阅读)
     *   - BOS/EOS/SEP (id=2-4): 插入空格 (自然分隔)
     *   - 普通 token: 拼接
     */
    public String decode(int[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            // 跳过 PAD (id=0)
            if (id == PAD) continue;

            // 查反向词表
            String token = idToToken.get(id);
            // id 不在词表 (动态生成): 跳过
            if (token == null) continue;

            // 按 id 类型处理
            if (id == UNK) {
                // UNK: 跳过 (避免乱码)
                continue;
            } else if (id == BOS || id == EOS || id == SEP) {
                // 句子边界: 插入空格
                sb.append(' ');
            } else {
                // 普通 token: 拼接
                sb.append(token);
            }
        }
        return sb.toString().trim();  // 去首尾空格
    }

    /**
     * 解码 - 包含 UNK (调试用)
     * 输出 <unk> 占位而不是跳过
     */
    public String decodeWithUnk(int[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            if (id == PAD) continue;
            String token = idToToken.get(id);
            if (token == null) {
                // 动态 id: 标 [?id?]
                sb.append("[?").append(id).append("?]");
                continue;
            }
            if (id == UNK) {
                sb.append("<unk>");
            } else if (id == BOS || id == EOS) {
                sb.append(' ');
            } else {
                sb.append(token);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 编码 + 加 BOS/EOS (用于训练)
     * 输出: [BOS, ...ids, EOS]
     */
    public int[] encodeForTraining(String text) {
        int[] inner = encode(text);
        int[] result = new int[inner.length + 2];
        result[0] = BOS;  // 开头
        System.arraycopy(inner, 0, result, 1, inner.length);  // 主体
        result[result.length - 1] = EOS;  // 结尾
        return result;
    }

    // ============== Getters ==============
    /**
     * 词表大小
     */
    public int getVocabSize() {
        return vocabSize;
    }

    /**
     * 设置词表大小 (用于反序列化)
     */
    public void setVocabSize(int size) {
        this.vocabSize = size;
    }

    // ============== 序列化 ==============
    /**
     * 保存词表到文件 (V5.4+ 改用 ObjectOutputStream)
     *
     * <h2>修 writeUTF 乱码</h2>
     * 之前用 DataOutputStream.writeUTF, 但 modified UTF-8 不支持 surrogate pair
     * (CJK 扩展 B/C/D/E 是 4 字节 UTF-8, writeUTF 拒绝)
     * 改: ObjectOutputStream.writeObject → 走 Java String 默认 UTF-16
     * 兼容罕用字 + emoji + 任意 Unicode
     *
     * <h2>文件格式</h2>
     * <pre>
     *   int   magic    0x4D494E49 ("MINI")
     *   int   version  2
     *   int   size     词表大小
     *   for i in 0..size:
     *     int     id     0, 1, 2, ...
     *     String  token  词条
     * </pre>
     *
     * @param file 目标文件
     */
    public void save(File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            // 头部: 魔数 + 版本
            out.writeInt(0x4D494E49);  // "MINI"
            out.writeInt(2);            // 版本 v2
            out.writeInt(vocabSize);
            // 顺序写 id + token
            for (int i = 0; i < vocabSize; i++) {
                String token = idToToken.get(i);
                if (token == null) token = "<unk>";  // 兜底
                out.writeInt(i);
                out.writeObject(token);
            }
        }
        log.info("词表已保存: {} ({} tokens)", file, vocabSize);
    }

    /**
     * 从文件加载词表
     *
     * <h2>兼容</h2>
     * 旧版 (DataInputStream + writeUTF): 不支持, 提示重新训练
     * 新版 (ObjectOutputStream + writeObject): 正常加载
     *
     * @param file 源文件
     */
    @SuppressWarnings("unchecked")
    public void load(File file) throws IOException {
        // 清空旧词表
        tokenToId.clear();
        idToToken.clear();
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            // 读魔数
            int magic = in.readInt();
            if (magic != 0x4D494E49) {
                // 不是 MINI 魔数, 走 legacy 路径
                loadLegacy(in);
                return;
            }
            // 读版本
            int version = in.readInt();
            int size = in.readInt();
            // 读每个 token
            for (int i = 0; i < size; i++) {
                int id = in.readInt();
                String token = (String) in.readObject();
                tokenToId.put(token, id);
                idToToken.put(id, token);
            }
            vocabSize = size;
        } catch (ClassNotFoundException e) {
            // ObjectInputStream 反序列化时可能找不到 String (但实际上 String 一定在)
            throw new IOException("词表反序列化失败: 找不到 String 类", e);
        }
        log.info("词表已加载: {} ({} tokens)", file, vocabSize);
    }

    /**
     * 兼容旧版 writeUTF 格式
     * 旧版无魔数, 这里只能猜
     * 实际: 旧版用 DataInputStream, 跟 ObjectInputStream 不兼容
     * 简单处理: 抛错让用户重训
     */
    private void loadLegacy(ObjectInputStream wrapper) throws IOException {
        log.warn("旧版词表格式不支持自动迁移, 请重新训练");
        throw new IOException("旧版词表格式不兼容, 请删除旧文件后重训");
    }
}
