package com.minimax.rag.parser;

/**
 * 文档解析器接口。
 * 输入: 文件原始字节 + 来源类型
 * 输出: 纯文本 (后续 chunker 切成块)
 */
public interface DocumentParser {

    /** 是否支持该 sourceType: txt/md/docx/pdf */
    boolean supports(String sourceType);

    /** 解析为纯文本。失败抛 RuntimeException。 */
    String parse(byte[] content, String filename);
}
