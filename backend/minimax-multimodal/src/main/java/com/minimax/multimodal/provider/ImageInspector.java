package com.minimax.multimodal.provider;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片基础信息解析工具 (V3.0.1)
 *
 * <p>多个 Provider 共享的 inspect 逻辑: 解析 magic number 识别格式 + 估算大小
 * <p>无状态, 静态方法, 线程安全
 */
public final class ImageInspector {

    /**
     * 私有构造器, 禁止实例化 (工具类)
     */
    private ImageInspector() {}

    /**
     * 从 base64 解析图片信息
     *
     * @param b64 base64 字符串 (无 data: 前缀)
     * @return { format, magic, sizeBytes, sizeKB, error? }
     */
    public static Map<String, Object> inspect(String b64) {
        // 1. 创建返回 map
        Map<String, Object> info = new HashMap<>();

        // 2. 空值保护
        if (b64 == null || b64.isBlank()) {
            info.put("error", "empty");  // 错误标记
            return info;                  // 提前返回
        }

        try {
            // 3. base64 → 字节数组 (前 64 字符足够解析 magic number)
            //    Math.min 防止下标越界
            int sampleLen = Math.min(64, b64.length());
            byte[] bytes = Base64.getDecoder().decode(b64.substring(0, sampleLen));

            // 4. 从前 8 字节提取 magic (可显示字符串)
            String magic = new String(bytes, 0, Math.min(8, bytes.length), java.nio.charset.StandardCharsets.ISO_8859_1);

            // 5. 按文件头 magic number 识别格式
            String format = detectFormat(bytes);

            // 6. 估算原始字节数 (base64 长度 × 3/4)
            int approxBytes = b64.length() * 3 / 4;

            // 7. 填充信息
            info.put("format", format);                          // 图片格式
            info.put("magic", sanitize(magic));                  // magic 字符串 (清洗过)
            info.put("sizeBytes", approxBytes);                  // 估算字节数
            info.put("sizeKB", approxBytes / 1024);              // 估算 KB
        } catch (Exception e) {
            // 8. 解析失败 (如 base64 非法), 记录错误但不抛
            info.put("error", "decode failed: " + e.getMessage());
        }
        return info;
    }

    /**
     * 检测图片格式 (基于 magic number)
     *
     * <p>magic number 是文件开头的固定字节序列, 用于识别文件类型:
     * <ul>
     *   <li>PNG: 89 50 4E 47 0D 0A 1A 0A (即 \x89PNG\r\n\x1a\n)</li>
     *   <li>JPEG: FF D8 FF</li>
     *   <li>GIF: GIF87a 或 GIF89a</li>
     *   <li>WEBP: RIFF....WEBP</li>
     *   <li>BMP: BM</li>
     * </ul>
     *
     * @param bytes 文件前 N 字节
     * @return 格式名 (png/jpeg/gif/webp/bmp/unknown)
     */
    private static String detectFormat(byte[] bytes) {
        // 1. 长度保护, 避免空数组下标越界
        if (bytes == null || bytes.length < 2) {
            return "unknown";
        }

        // 2. PNG: 首字节 0x89, 第二个字符 'P' (0x50)
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P') {
            return "png";
        }
        // 3. JPEG: FF D8 FF
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        // 4. GIF: 'G' 'I' 'F'
        if (bytes.length >= 3 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "gif";
        }
        // 5. WEBP: 'R' 'I' 'F' 'F' .... 'W' 'E' 'B' 'P' (前 4 字节 + 8-11 字节)
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        // 6. BMP: 'B' 'M'
        if (bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M') {
            return "bmp";
        }
        // 7. 其它
        return "unknown";
    }

    /**
     * 清洗 magic 字符串 (只保留可打印 ASCII, 不可打印字符替换为 ?)
     *
     * @param raw 原始字符串 (可能含二进制)
     * @return 清洗后的可显示字符串
     */
    private static String sanitize(String raw) {
        // 1. 空值保护
        if (raw == null) return "";
        // 2. 替换非可打印字符 (空格 ~ ~ 之外) 为 ?
        return raw.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
