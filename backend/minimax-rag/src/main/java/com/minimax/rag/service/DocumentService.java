package com.minimax.rag.service;

import com.minimax.rag.chunker.TextChunker;
import com.minimax.rag.embedding.EmbeddingClient;
import com.minimax.rag.entity.Document;
import com.minimax.rag.entity.DocumentChunk;
import com.minimax.rag.entity.KnowledgeBase;
import com.minimax.rag.mapper.DocumentChunkMapper;
import com.minimax.rag.mapper.DocumentMapper;
import com.minimax.rag.parser.ParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /**
     * RAG 上传进度事件 (Day 41)
     *
     * @param stage    当前阶段: UPLOAD | PARSING | CHUNKING | EMBEDDING | INDEXING | DONE | ERROR
     * @param progress 总进度 0-100
     * @param message  阶段描述
     * @param docId    文档 ID（处理完成后有效）
     */
    public record RagProgress(String stage, int progress, String message, Long docId) {}

    private final DocumentMapper docMapper;
    private final DocumentChunkMapper chunkMapper;
    private final KnowledgeBaseService kbService;
    private final ParserRegistry parserRegistry;
    private final TextChunker chunker;
    private final EmbeddingClient embedding;

    /**
     * 上传并处理文档:
     *  1) 计算 checksum
     *  2) dedup (同 owner + 同 kb + 同 checksum = 已存在)
     *  3) parse -> 纯文本
     *  4) chunk -> List<Chunk>
     *  5) embed 每个 chunk
     *  6) 写库 (document + document_chunk)
     *  7) KB 计数 +1
     *
     * @return document id
     */
    @Transactional
    public Long upload(Long ownerId, Long kbId, String title, String sourceType,
                        byte[] rawContent, String filename, String tags) {
        if (rawContent == null || rawContent.length == 0) {
            throw new IllegalArgumentException("content 不能为空");
        }
        KnowledgeBase kb = kbService.get(kbId, ownerId);
        if (kb == null) throw new IllegalArgumentException("知识库不存在或无权访问: " + kbId);

        String st = sourceType == null ? detect(filename) : sourceType.toLowerCase(Locale.ROOT);
        String checksum = sha256(rawContent);

        // dedup
        Document exist = docMapper.selectByChecksum(ownerId, checksum, kbId);
        if (exist != null) {
            log.info("文档去重命中: kbId={} checksum={} existingDocId={}", kbId, checksum, exist.getId());
            return exist.getId();
        }

        // 1) 落 document 初始记录
        Document d = new Document();
        d.setKbId(kbId);
        d.setOwnerId(ownerId);
        d.setTitle(title != null ? title : (filename != null ? filename : "untitled"));
        d.setSourceType(st);
        d.setSourceUri(filename);
        d.setSizeBytes((long) rawContent.length);
        d.setStatus("pending");
        d.setChecksum(checksum);
        d.setTags(tags);
        d.setChunkCount(0);
        docMapper.insert(d);

        // 2) 解析
        String text;
        try {
            text = parserRegistry.resolve(st).parse(rawContent, filename);
            docMapper.updateStatus(d.getId(), "parsing", null);
        } catch (Exception e) {
            log.error("文档解析失败: docId={} err={}", d.getId(), e.getMessage());
            docMapper.updateStatus(d.getId(), "failed", truncate(e.getMessage(), 500));
            throw new RuntimeException("解析失败: " + e.getMessage(), e);
        }
        d.setContent(text);

        // 3) chunk
        List<TextChunker.Chunk> chunks = chunker.chunk(text);
        log.info("文档分块: docId={} chunks={}", d.getId(), chunks.size());

        // 4) embed + 写 chunk
        int count = 0;
        for (int i = 0; i < chunks.size(); i++) {
            TextChunker.Chunk ck = chunks.get(i);
            float[] vec = embedding.embed(ck.content());
            DocumentChunk c = new DocumentChunk();
            c.setDocId(d.getId());
            c.setKbId(kbId);
            c.setOwnerId(ownerId);
            c.setChunkIndex(i);
            c.setContent(ck.content());
            c.setEmbedding(VectorUtils.toBytes(vec));
            c.setDim(vec.length);
            c.setCharCount(ck.charCount());
            c.setStartPos(ck.startPos());
            c.setEndPos(ck.endPos());
            chunkMapper.insert(c);
            count++;
        }

        // 5) 更新 document 状态
        d.setChunkCount(count);
        d.setStatus("chunked");
        docMapper.updateById(d);

        // 6) KB 计数
        kbService.incDocCount(kbId, 1);
        kbService.incChunkCount(kbId, count);
        log.info("文档处理完成: docId={} chunks={}", d.getId(), count);
        return d.getId();
    }

    /**
     * 带 SSE 进度回调的文档上传 (Day 41)
     *
     * 各阶段进度:
     *   5%   - UPLOAD   开始
     *   10%  - PARSING  解析中
     *   35%  - CHUNKING 切片中
     *   70%  - EMBEDDING 向量化中
     *   90%  - INDEXING 索引中
     *   100% - DONE     完成
     *
     * @param progressConsumer 进度回调（可传 null）
     * @return document id
     */
    public Long uploadWithProgress(Long ownerId, Long kbId, String title, String sourceType,
                                   byte[] rawContent, String filename, String tags,
                                   Consumer<RagProgress> progressConsumer) {
        // 1) 基础校验
        if (rawContent == null || rawContent.length == 0) {
            throw new IllegalArgumentException("content 不能为空");
        }
        KnowledgeBase kb = kbService.get(kbId, ownerId);
        if (kb == null) throw new IllegalArgumentException("知识库不存在或无权访问: " + kbId);

        String st = sourceType == null ? detect(filename) : sourceType.toLowerCase(Locale.ROOT);
        String checksum = sha256(rawContent);

        // dedup
        Document exist = docMapper.selectByChecksum(ownerId, checksum, kbId);
        if (exist != null) {
            log.info("文档去重命中: kbId={} existingDocId={}", kbId, exist.getId());
            emit(progressConsumer, new RagProgress("DONE", 100, "文档已存在，无需重复上传", exist.getId()));
            return exist.getId();
        }

        // 2) 落 document 初始记录
        Document d = new Document();
        d.setKbId(kbId);
        d.setOwnerId(ownerId);
        d.setTitle(title != null ? title : (filename != null ? filename : "untitled"));
        d.setSourceType(st);
        d.setSourceUri(filename);
        d.setSizeBytes((long) rawContent.length);
        d.setStatus("pending");
        d.setChecksum(checksum);
        d.setTags(tags);
        d.setChunkCount(0);
        docMapper.insert(d);
        emit(progressConsumer, new RagProgress("UPLOAD", 10, "文件接收完成，开始解析...", d.getId()));

        // 3) 解析
        String text;
        try {
            emit(progressConsumer, new RagProgress("PARSING", 15, "正在解析文档内容...", d.getId()));
            text = parserRegistry.resolve(st).parse(rawContent, filename);
            docMapper.updateStatus(d.getId(), "parsing", null);
            emit(progressConsumer, new RagProgress("PARSING", 25, "文档解析完成，共 " + text.length() + " 字", d.getId()));
        } catch (Exception e) {
            log.error("文档解析失败: docId={} err={}", d.getId(), e.getMessage());
            docMapper.updateStatus(d.getId(), "failed", truncate(e.getMessage(), 500));
            emit(progressConsumer, new RagProgress("ERROR", 0, "解析失败: " + e.getMessage(), d.getId()));
            throw new RuntimeException("解析失败: " + e.getMessage(), e);
        }
        d.setContent(text);

        // 4) 切片
        emit(progressConsumer, new RagProgress("CHUNKING", 30, "正在切片文档...", d.getId()));
        List<TextChunker.Chunk> chunks = chunker.chunk(text);
        log.info("文档分块: docId={} chunks={}", d.getId(), chunks.size());
        emit(progressConsumer, new RagProgress("CHUNKING", 40, "切片完成，共 " + chunks.size() + " 个片段", d.getId()));

        // 5) 向量化 + 写 chunk
        int total = chunks.size();
        int count = 0;
        for (int i = 0; i < total; i++) {
            TextChunker.Chunk ck = chunks.get(i);
            // 每 5 个 chunk 推一次进度（避免推太多事件）
            float[] vec = embedding.embed(ck.content());
            DocumentChunk c = new DocumentChunk();
            c.setDocId(d.getId());
            c.setKbId(kbId);
            c.setOwnerId(ownerId);
            c.setChunkIndex(i);
            c.setContent(ck.content());
            c.setEmbedding(VectorUtils.toBytes(vec));
            c.setDim(vec.length);
            c.setCharCount(ck.charCount());
            c.setStartPos(ck.startPos());
            c.setEndPos(ck.endPos());
            chunkMapper.insert(c);
            count++;

            // 进度: 40% -> 80%
            if (total <= 1 || i % Math.max(1, total / 8) == 0 || i == total - 1) {
                int pct = 40 + (int) ((count * 40.0) / total);
                emit(progressConsumer, new RagProgress("EMBEDDING", pct,
                        "向量化中 " + count + "/" + total + "...", d.getId()));
            }
        }
        emit(progressConsumer, new RagProgress("INDEXING", 85, "正在写入索引...", d.getId()));

        // 6) 更新 document 状态
        d.setChunkCount(count);
        d.setStatus("chunked");
        docMapper.updateById(d);

        // 7) KB 计数
        kbService.incDocCount(kbId, 1);
        kbService.incChunkCount(kbId, count);
        emit(progressConsumer, new RagProgress("INDEXING", 95, "索引写入完成", d.getId()));

        log.info("文档处理完成: docId={} chunks={}", d.getId(), count);
        emit(progressConsumer, new RagProgress("DONE", 100, "处理完成，共 " + count + " 个切片", d.getId()));
        return d.getId();
    }

    private void emit(Consumer<RagProgress> consumer, RagProgress progress) {
        if (consumer != null) {
            try {
                consumer.accept(progress);
            } catch (Exception e) {
                log.warn("进度回调异常，忽略: {}", e.getMessage());
            }
        }
    }

    public Document getById(Long docId) {
        return docMapper.selectById(docId);
    }

    public List<Document> listByKb(Long kbId, int limit) {
        if (limit <= 0 || limit > 200) limit = 50;
        return docMapper.selectByKb(kbId, limit);
    }

    public boolean delete(Long docId, Long ownerId) {
        Document d = docMapper.selectById(docId);
        if (d == null) return false;
        if (!d.getOwnerId().equals(ownerId)) return false;
        chunkMapper.deleteByDoc(docId);
        docMapper.deleteById(docId);
        kbService.incDocCount(d.getKbId(), -1);
        kbService.incChunkCount(d.getKbId(), -d.getChunkCount());
        return true;
    }

    public List<DocumentChunk> chunksOfDoc(Long docId) {
        return chunkMapper.selectByDoc(docId);
    }

    /** V5.33 Day 23: 重命名文档 */
    public Document renameDoc(Long docId, Long ownerId, String newTitle) {
        Document d = docMapper.selectById(docId);
        if (d == null) throw new IllegalArgumentException("文档不存在: " + docId);
        if (!d.getOwnerId().equals(ownerId)) throw new SecurityException("无权修改此文档");
        d.setTitle(newTitle);
        docMapper.updateById(d);
        return d;
    }

    /**
     * Day 45: 在线编辑文档内容
     * - 校验归属
     * - 更新 document.content
     * - 删除旧切片，重新分块 + 向量化 + 写库
     * - 更新 document 状态和 chunkCount
     * - 更新所属 KB 的 chunk 计数
     *
     * @param docId     文档 ID
     * @param ownerId   所有者（归属校验）
     * @param newContent 新内容
     * @return 更新后的 Document
     */
    @Transactional
    public Document updateDocContent(Long docId, Long ownerId, String newContent) {
        Document d = docMapper.selectById(docId);
        if (d == null) throw new IllegalArgumentException("文档不存在: " + docId);
        if (!d.getOwnerId().equals(ownerId)) throw new SecurityException("无权修改此文档");
        if (newContent == null) throw new IllegalArgumentException("内容不能为空");

        Long kbId = d.getKbId();
        int oldChunkCount = d.getChunkCount();

        // 1) 更新 content
        d.setContent(newContent);
        d.setSizeBytes((long) newContent.getBytes().length);
        d.setStatus("chunked");

        // 2) 删除旧切片
        chunkMapper.deleteByDoc(docId);

        // 3) 重新切片
        List<TextChunker.Chunk> chunks = chunker.chunk(newContent);
        log.info("文档重新切片: docId={} oldChunks={} newChunks={}", docId, oldChunkCount, chunks.size());

        // 4) 重新向量化 + 写库
        for (int i = 0; i < chunks.size(); i++) {
            TextChunker.Chunk ck = chunks.get(i);
            float[] vec = embedding.embed(ck.content());
            DocumentChunk c = new DocumentChunk();
            c.setDocId(docId);
            c.setKbId(kbId);
            c.setOwnerId(ownerId);
            c.setChunkIndex(i);
            c.setContent(ck.content());
            c.setEmbedding(VectorUtils.toBytes(vec));
            c.setDim(vec.length);
            c.setCharCount(ck.charCount());
            c.setStartPos(ck.startPos());
            c.setEndPos(ck.endPos());
            chunkMapper.insert(c);
        }

        // 5) 更新 document
        d.setChunkCount(chunks.size());
        docMapper.updateById(d);

        // 6) 调整 KB chunk 计数
        kbService.incChunkCount(kbId, chunks.size() - oldChunkCount);

        log.info("文档内容更新完成: docId={} newChunks={}", docId, chunks.size());
        return d;
    }

    private String detect(String filename) {
        if (filename == null) return "txt";
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md")) return "md";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".pdf")) return "pdf";
        return "txt";
    }

    private String sha256(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private String truncate(String s, int n) {
        if (s == null) return null;
        return s.length() > n ? s.substring(0, n) : s;
    }

    /** V6.8.2: 根据文档ID查找所属知识库ID (用于归属校验) */
    public Long getKbIdByDocId(Long docId) {
        Document d = docMapper.selectById(docId);
        return d != null ? d.getKbId() : null;
    }

    /**
     * Day 46: 批量重新索引多个文档
     * - 校验归属（所有 doc 必须属于同一 owner）
     * - 对每个文档重新切片 + 向量化 + 写库
     * - 返回成功数量和失败列表
     *
     * @param docIds  文档 ID 列表
     * @param ownerId 所有者（归属校验）
     * @return 批量结果：{ succeeded: 成功数, failed: [ { docId, error } ] }
     */
    @Transactional
    public BatchResult batchReindexDocs(List<Long> docIds, Long ownerId) {
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("docIds 不能为空");
        }
        int succeeded = 0;
        List<FailedDoc> failed = new java.util.ArrayList<>();

        for (Long docId : docIds) {
            try {
                Document d = docMapper.selectById(docId);
                if (d == null) {
                    failed.add(new FailedDoc(docId, "文档不存在"));
                    continue;
                }
                if (!d.getOwnerId().equals(ownerId)) {
                    failed.add(new FailedDoc(docId, "无权操作此文档"));
                    continue;
                }
                String content = d.getContent();
                if (content == null || content.isBlank()) {
                    failed.add(new FailedDoc(docId, "文档内容为空，无法重新索引"));
                    continue;
                }
                Long kbId = d.getKbId();
                int oldChunkCount = d.getChunkCount();

                // 1) 删除旧切片
                chunkMapper.deleteByDoc(docId);

                // 2) 重新切片
                List<TextChunker.Chunk> chunks = chunker.chunk(content);
                log.info("批量重索引-重新切片: docId={} oldChunks={} newChunks={}", docId, oldChunkCount, chunks.size());

                // 3) 重新向量化 + 写库
                for (int i = 0; i < chunks.size(); i++) {
                    TextChunker.Chunk ck = chunks.get(i);
                    float[] vec = embedding.embed(ck.content());
                    DocumentChunk c = new DocumentChunk();
                    c.setDocId(docId);
                    c.setKbId(kbId);
                    c.setOwnerId(ownerId);
                    c.setChunkIndex(i);
                    c.setContent(ck.content());
                    c.setEmbedding(VectorUtils.toBytes(vec));
                    c.setDim(vec.length);
                    c.setCharCount(ck.charCount());
                    c.setStartPos(ck.startPos());
                    c.setEndPos(ck.endPos());
                    chunkMapper.insert(c);
                }

                // 4) 更新 document
                d.setChunkCount(chunks.size());
                docMapper.updateById(d);

                // 5) 调整 KB chunk 计数
                kbService.incChunkCount(kbId, chunks.size() - oldChunkCount);
                succeeded++;
            } catch (Exception e) {
                log.error("批量重索引失败: docId={} err={}", docId, e.getMessage());
                failed.add(new FailedDoc(docId, e.getMessage()));
            }
        }
        return new BatchResult(succeeded, failed);
    }

    /**
     * Day 47: 批量删除多个文档
     * - 校验归属（所有 doc 必须属于同一 owner）
     * - 删除 chunks（先删切片再删文档）
     * - 调整所属 KB 的 docCount 和 chunkCount
     * - 返回成功数量和失败列表
     *
     * @param docIds  文档 ID 列表
     * @param ownerId 所有者（归属校验）
     * @return 批量结果：{ succeeded: 成功数, failed: [ { docId, error }] }
     */
    @Transactional
    public BatchResult batchDeleteDocs(List<Long> docIds, Long ownerId) {
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("docIds 不能为空");
        }
        int succeeded = 0;
        List<FailedDoc> failed = new java.util.ArrayList<>();

        for (Long docId : docIds) {
            try {
                Document d = docMapper.selectById(docId);
                if (d == null) {
                    failed.add(new FailedDoc(docId, "文档不存在"));
                    continue;
                }
                if (!d.getOwnerId().equals(ownerId)) {
                    failed.add(new FailedDoc(docId, "无权操作此文档"));
                    continue;
                }
                Long kbId = d.getKbId();
                int chunkCount = d.getChunkCount();

                // 1) 删除切片
                chunkMapper.deleteByDoc(docId);

                // 2) 删除文档
                docMapper.deleteById(docId);

                // 3) 调整 KB 计数
                kbService.incDocCount(kbId, -1);
                kbService.incChunkCount(kbId, -chunkCount);

                log.info("批量删除文档: docId={} kbId={} chunks={}", docId, kbId, chunkCount);
                succeeded++;
            } catch (Exception e) {
                log.error("批量删除失败: docId={} err={}", docId, e.getMessage());
                failed.add(new FailedDoc(docId, e.getMessage()));
            }
        }
        return new BatchResult(succeeded, failed);
    }

    /** 批量结果记录 */
    public record BatchResult(int succeeded, List<FailedDoc> failed) {}
    public record FailedDoc(Long docId, String error) {}

    /**
     * Day 48: 批量导出文档 (PDF / TXT)
     * - 校验归属（所有 doc 必须属于同一 owner）
     * - TXT: 拼接所有 chunk 内容，UTF-8 编码
     * - PDF: 使用 PDFBox 生成多页 PDF
     *
     * @param docIds   文档 ID 列表
     * @param ownerId  所有者
     * @param format   "pdf" | "txt"
     * @return { bytes: byte[], filename: String }
     */
    public ExportResult exportDocs(List<Long> docIds, Long ownerId, String format) {
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("docIds 不能为空");
        }
        String f = (format == null ? "txt" : format.toLowerCase(Locale.ROOT));
        if (!"pdf".equals(f) && !"txt".equals(f)) {
            f = "txt";
        }

        // 1) 收集所有文档内容
        StringBuilder sb = new StringBuilder();
        sb.append("# 文档导出\n");
        sb.append("# 导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        sb.append("# 文档数量: ").append(docIds.size()).append("\n");
        sb.append("\n");

        for (Long docId : docIds) {
            Document d = docMapper.selectById(docId);
            if (d == null) continue;
            if (!d.getOwnerId().equals(ownerId)) continue;

            sb.append("## ").append(d.getTitle()).append("\n");
            sb.append("来源: ").append(d.getSourceUri() != null ? d.getSourceUri() : "未知").append("\n");
            sb.append("类型: ").append(d.getSourceType() != null ? d.getSourceType() : "txt").append("\n");
            sb.append("字数: ").append(d.getContent() != null ? d.getContent().length() : 0).append(" 字\n");
            sb.append("\n---\n\n");

            if (d.getContent() != null && !d.getContent().isBlank()) {
                sb.append(d.getContent());
            } else {
                // 从 chunks 拼接
                List<DocumentChunk> chunks = chunkMapper.selectByDoc(docId);
                for (int i = 0; i < chunks.size(); i++) {
                    DocumentChunk ck = chunks.get(i);
                    sb.append("【片段 ").append(i + 1).append("】\n");
                    sb.append(ck.getContent());
                    sb.append("\n\n");
                }
            }
            sb.append("\n\n");
        }

        String text = sb.toString();

        // 2) 生成文件
        if ("pdf".equals(f)) {
            byte[] pdfBytes = generatePdf(text);
            return new ExportResult(pdfBytes, "documents-export.pdf", "application/pdf");
        } else {
            byte[] txtBytes = text.getBytes(StandardCharsets.UTF_8);
            return new ExportResult(txtBytes, "documents-export.txt", "text/plain; charset=UTF-8");
        }
    }

    private byte[] generatePdf(String text) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PDDocument doc = new PDDocument()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float margin = 50;
            float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
            float pageHeight = PDRectangle.A4.getHeight() - 2 * margin;
            float leading = 14.5f;
            float titleSize = 16f;
            float bodySize = 11f;

            String[] lines = text.split("\n", -1);
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDPageContentStream cos = new PDPageContentStream(doc, page);
            float y = pageHeight + margin;
            boolean inTitle = false;

            for (String line : lines) {
                float fontSize = line.startsWith("# ") ? titleSize : bodySize;
                PDType1Font currentFont = line.startsWith("# ") ? boldFont : font;

                // 去掉 markdown 标记
                String cleanLine = line.replaceAll("^#+\\s*", "").replace("---", "");
                if (cleanLine.trim().isEmpty()) {
                    cleanLine = " ";
                }

                // 计算该行需要的字数（估算，每字符约 6pt 宽）
                int charsPerLine = (int) (pageWidth / (fontSize * 0.5f));
                if (charsPerLine < 10) charsPerLine = 10;

                // 换行
                if (cleanLine.length() <= charsPerLine) {
                    y -= fontSize + 4;
                    if (y < margin) {
                        cos.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cos = new PDPageContentStream(doc, page);
                        y = pageHeight + margin;
                    }
                    cos.beginText();
                    cos.setFont(currentFont, fontSize);
                    cos.newLineAtOffset(margin, y);
                    cos.showText(cleanLine);
                    cos.endText();
                } else {
                    // 长行拆多行
                    int start = 0;
                    while (start < cleanLine.length()) {
                        int end = Math.min(start + charsPerLine, cleanLine.length());
                        y -= fontSize + 2;
                        if (y < margin) {
                            cos.close();
                            page = new PDPage(PDRectangle.A4);
                            doc.addPage(page);
                            cos = new PDPageContentStream(doc, page);
                            y = pageHeight + margin;
                        }
                        cos.beginText();
                        cos.setFont(currentFont, fontSize);
                        cos.newLineAtOffset(margin, y);
                        cos.showText(cleanLine.substring(start, end));
                        cos.endText();
                        start = end;
                    }
                }
            }

            cos.close();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF 生成失败: {}", e.getMessage());
            throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
        }
    }

    /** 导出结果 */
    public record ExportResult(byte[] bytes, String filename, String contentType) {}
}
