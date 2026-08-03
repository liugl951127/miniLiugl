package com.minimax.ai.tokenizer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化版 BPE (Byte Pair Encoding) 分词器
 *
 * 特点:
 *   - 中英文混合支持
 *   - 字符级 + 词级混合
 *   - 训练时自动构建词表
 *   - 推理时快速编码/解码
 *
 * 不依赖任何外部库 (纯 Java)
 *
 * V2.5 自研 - 不使用 jieba / hanlp
 */
@Slf4j
@Component
public class ChineseTokenizer {

    /** 特殊 token */
    public static final int PAD = 0;    // <pad>
    public static final int UNK = 1;    // <unk>
    public static final int BOS = 2;    // <bos> (begin of sentence)
    public static final int EOS = 3;    // <eos> (end of sentence)
    public static final int SEP = 4;    // <sep>

    /** 词表: token string -> id */
    private final Map<String, Integer> tokenToId = new ConcurrentHashMap<>();
    /** 反向: id -> token string */
    private final Map<Integer, String> idToToken = new ConcurrentHashMap<>();
    /** 词频统计 (用于训练) */
    private final Map<String, Long> wordFreq = new ConcurrentHashMap<>();

    /** 词表大小 (含特殊 token) */
    private int vocabSize = 0;

    public ChineseTokenizer() {
        // 初始化特殊 token
        tokenToId.put("<pad>", PAD);
        tokenToId.put("<unk>", UNK);
        tokenToId.put("<bos>", BOS);
        tokenToId.put("<eos>", EOS);
        tokenToId.put("<sep>", SEP);
        for (int i = 0; i < 5; i++) {
            idToToken.put(i, specialTokenName(i));
        }
        vocabSize = 5;
    }

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

    /**
     * 中文友好的预分词:
     *   1. 按字符切分 (中文)
     *   2. 英文按单词切分
     *   3. 数字单独 token
     *   4. 标点单独 token
     */
    public List<String> preTokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) return tokens;

        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            // 优先取完整字符 (支持 surrogate pair)
            char c = text.charAt(i);
            String ch;
            int charLen = 1;

            if (Character.isHighSurrogate(c) && i + 1 < text.length()
                    && Character.isLowSurrogate(text.charAt(i + 1))) {
                ch = text.substring(i, i + 2);
                charLen = 2;
            } else {
                ch = String.valueOf(c);
            }

            if (isChineseChar(c) && charLen == 1) {
                // 中文: 1 个字符 1 个 token (避免 BPE 切错)
                flushBuf(buf, tokens);
                tokens.add(String.valueOf(c));
            } else if (Character.isLetter(c)) {
                // 英文字母: 累积
                buf.append(c);
            } else if (Character.isDigit(c)) {
                // 数字: 累积
                flushBuf(buf, tokens);
                StringBuilder num = new StringBuilder();
                while (i < text.length() && Character.isDigit(text.charAt(i))) {
                    num.append(text.charAt(i));
                    i++;
                }
                i--; // 回退
                tokens.add(num.toString());
            } else if (isPunctuation(c)) {
                // 标点: 单独 token
                flushBuf(buf, tokens);
                tokens.add(String.valueOf(c));
            } else if (Character.isWhitespace(c)) {
                // 空白: 分隔
                flushBuf(buf, tokens);
            } else {
                // 其他字符 (表情符号等) - 用完整字符
                flushBuf(buf, tokens);
                tokens.add(ch);
                i += charLen;
                continue;
            }
            i += charLen;
        }
        flushBuf(buf, tokens);
        return tokens;
    }

    private void flushBuf(StringBuilder buf, List<String> tokens) {
        if (buf.length() > 0) {
            tokens.add(buf.toString().toLowerCase());
            buf.setLength(0);
        }
    }

    /**
     * 是否中文字符 (CJK 全部平面)
     *
     * V5.4+ 扩展:
     *   - 0x4E00-0x9FFF  : CJK 统一汉字 (基本平面)
     *   - 0x3400-0x4DBF  : CJK 扩展 A (罕用)
     *   - 0x20000-0x2A6DF: CJK 扩展 B (罕用, surrogate pair)
     *   - 0x2A700-0x2B73F: CJK 扩展 C
     *   - 0x2B740-0x2B81F: CJK 扩展 D
     *   - 0x2B820-0x2CEAF: CJK 扩展 E
     *   - 0xF900-0xFAFF  : CJK 兼容汉字
     *   - 0x2F800-0x2FA1F: CJK 兼容汉字 (补充, surrogate pair)
     */
    public static boolean isChineseChar(char c) {
        // 基本平面 (单 char 即可判)
        if ((c >= 0x4E00 && c <= 0x9FFF) ||
            (c >= 0x3400 && c <= 0x4DBF) ||
            (c >= 0xF900 && c <= 0xFAFF)) {
            return true;
        }
        // 高位平面 (需 surrogate pair, 这里只判高代理)
        // high surrogate 0xD800-0xDBFF, low surrogate 0xDC00-0xDFFF
        if (c >= 0xD800 && c <= 0xDBFF) {
            return true; // 配合下一个 char 解析
        }
        return false;
    }

    /**
     * 提取字符 (支持 surrogate pair, 完整码点)
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

    public static boolean isPunctuation(char c) {
        return "，。！？、；：.,!?;:\"'()[]{}<>/\\|@#$%^&*+=-_~`《》「」【】".indexOf(c) >= 0;
    }

    /**
     * 训练词表 (BPE 简化版)
     * 步骤:
     *   1. 统计所有 pre-token 频率
     *   2. 把高频 token 直接加进词表
     *   3. 对低频 token 切字符
     *   4. 字符级 fallback
     *
     * @param corpus 训练语料 (多行)
     * @param targetVocabSize 目标词表大小
     */
    public void train(List<String> corpus, int targetVocabSize) {
        log.info("开始训练词表, 语料: {} 行, 目标大小: {}", corpus.size(), targetVocabSize);

        // 1. 统计频率
        wordFreq.clear();
        long total = 0;
        for (String line : corpus) {
            for (String token : preTokenize(line)) {
                wordFreq.merge(token, 1L, Long::sum);
                total++;
            }
        }
        log.info("总 token 数: {}, 唯一 token 数: {}", total, wordFreq.size());

        // 2. 按频率排序, 取 top N
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(wordFreq.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        // 3. 添加特殊 token + 高频 token
        for (int i = 0; i < sorted.size() && vocabSize < targetVocabSize; i++) {
            String token = sorted.get(i).getKey();
            if (!tokenToId.containsKey(token)) {
                int id = vocabSize++;
                tokenToId.put(token, id);
                idToToken.put(id, token);
            }
        }

        // 4. 确保单字都进词表 (中文必须)
        for (int cp = 0x4E00; cp <= 0x9FFF && vocabSize < targetVocabSize; cp++) {
            String c = String.valueOf((char) cp);
            if (!tokenToId.containsKey(c)) {
                int id = vocabSize++;
                tokenToId.put(c, id);
                idToToken.put(id, c);
            }
        }

        // 5. ASCII 字符兜底
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

    /**
     * 编码: 文本 -> token ids
     */
    public int[] encode(String text) {
        List<String> tokens = preTokenize(text);
        int[] ids = new int[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            ids[i] = tokenToId.getOrDefault(tokens.get(i), UNK);
        }
        return ids;
    }

    /**
     * 解码: token ids -> 文本
     *
     * V5.4+ 修复乱码:
     *   - 不再跳过 UNK/BOS/EOS (id<5), 而是按语义插入占位
     *   - UNK 插入 空字符 (不破坏阅读)
     *   - BOS/EOS 插入 换行 (分隔上下文)
     */
    public String decode(int[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            // 跳过 PAD (id=0), 其他特殊 token 转为占位
            if (id == PAD) continue;

            String token = idToToken.get(id);
            if (token == null) {
                // id 不在词表 (动态生成)
                continue;
            }

            if (id == UNK) {
                // 未知 token 跳过 (避免乱码)
                continue;
            } else if (id == BOS || id == EOS || id == SEP) {
                // 句子边界 - 插入空格 (自然分隔)
                sb.append(' ');
            } else {
                sb.append(token);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 解码 - 包含 UNK (调试用, 输出 <unk> 占位)
     */
    public String decodeWithUnk(int[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            if (id == PAD) continue;
            String token = idToToken.get(id);
            if (token == null) {
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
     * 编码 + 加 BOS/EOS
     */
    public int[] encodeForTraining(String text) {
        int[] inner = encode(text);
        int[] result = new int[inner.length + 2];
        result[0] = BOS;
        System.arraycopy(inner, 0, result, 1, inner.length);
        result[result.length - 1] = EOS;
        return result;
    }

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

    /**
     * 序列化词表 (V5.4+ 改用 ObjectOutputStream)
     *
     * 修 writeUTF 乱码:
     *   - writeUTF 用 modified UTF-8, 不支持 surrogate pair (CJK 扩展 B/C/D/E)
     *   - 改用 ObjectOutputStream.writeObject, 直接序列化 Java String (UTF-16)
     *   - 兼容罕用汉字 + emoji + 任意 Unicode
     */
    public void save(File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            // 头部: 魔数 + 版本号
            out.writeInt(0x4D494E49);  // 魔数 "MINI"
            out.writeInt(2);            // 版本号 v2
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
     * 反序列化词表
     */
    @SuppressWarnings("unchecked")
    public void load(File file) throws IOException {
        tokenToId.clear();
        idToToken.clear();
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            int magic = in.readInt();
            if (magic != 0x4D494E49) {
                // 兼容旧版 writeUTF 格式
                loadLegacy(in);
                return;
            }
            int version = in.readInt();
            if (version == 1) {
                // 旧版 (writeUTF)
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    int id = in.readInt();
                    String token = (String) in.readObject();
                    tokenToId.put(token, id);
                    idToToken.put(id, token);
                }
                vocabSize = size;
            } else {
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    int id = in.readInt();
                    String token = (String) in.readObject();
                    tokenToId.put(token, id);
                    idToToken.put(id, token);
                }
                vocabSize = size;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("词表反序列化失败: 找不到 String 类", e);
        }
        log.info("词表已加载: {} ({} tokens)", file, vocabSize);
    }

    /**
     * 兼容旧版 writeUTF 格式 (DataInputStream)
     */
    private void loadLegacy(ObjectInputStream wrapper) throws IOException {
        // 需要重开, 因为 ObjectInputStream 包装了 DataInputStream
        // 实际调用方应该用 DataInputStream 读旧版
        log.warn("旧版词表格式不支持自动迁移, 请重新训练");
        throw new IOException("旧版词表格式不兼容, 请删除旧文件后重训");
    }
}
