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

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

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
}
