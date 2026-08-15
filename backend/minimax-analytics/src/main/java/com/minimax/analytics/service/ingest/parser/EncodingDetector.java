package com.minimax.analytics.service.ingest.parser;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 文件编码检测 (V5.31)
 *
 * V5.31 简化: 读前 64KB 字节, 启发式判断
 *   - 含 GBK 特征字节且 UTF-8 解码失败 → GBK
 *   - 否则 → UTF-8
 */
@Slf4j
public class EncodingDetector {

    public static String detect(String filePath) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buf = new byte[Math.min(64 * 1024, (int) FileUtil.size(new java.io.File(filePath)))];
            int read = bis.read(buf);
            if (read <= 0) return "UTF-8";
            // 启发式 1: 尝试 UTF-8
            try {
                new String(buf, 0, read, StandardCharsets.UTF_8);
                // 进一步: 看是否含 UTF-8 BOM
                if (read >= 3 && (buf[0] & 0xFF) == 0xEF && (buf[1] & 0xFF) == 0xBB && (buf[2] & 0xFF) == 0xBF) {
                    return "UTF-8-BOM";
                }
                return "UTF-8";
            } catch (Exception ignored) {
            }
            // 启发式 2: 看高字节比例 (GBK 中文常用区: 0x81-0xFE)
            int highByte = 0;
            for (int i = 0; i < read - 1; i++) {
                int b = buf[i] & 0xFF;
                if (b >= 0x81 && b <= 0xFE && i + 1 < read) {
                    int b2 = buf[i + 1] & 0xFF;
                    if (b2 >= 0x40 && b2 <= 0xFE) {
                        highByte++;
                        i++;
                    }
                }
            }
            if (highByte > read / 10) return "GBK";
        } catch (Exception e) {
            log.warn("编码检测失败: {}", e.getMessage());
        }
        return "UTF-8";
    }

    public static Charset toCharset(String name) {
        if (name == null) return StandardCharsets.UTF_8;
        try { return Charset.forName(name); } catch (Exception e) { return StandardCharsets.UTF_8; }
    }
}
