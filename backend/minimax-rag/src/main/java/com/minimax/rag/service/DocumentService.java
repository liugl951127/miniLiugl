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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
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
}
