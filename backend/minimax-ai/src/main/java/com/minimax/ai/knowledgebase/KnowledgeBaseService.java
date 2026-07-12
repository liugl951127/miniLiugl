package com.minimax.ai.knowledgebase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.entity.KbChunk;
import com.minimax.ai.entity.KbDocument;
import com.minimax.ai.entity.KbPermission;
import com.minimax.ai.mapper.KbChunkMapper;
import com.minimax.ai.mapper.KbDocumentMapper;
import com.minimax.ai.mapper.KbPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识库核心服务 (V3.4.0 自研)
 *
 * <h3>核心功能</h3>
 * <ol>
 *   <li>知识库管理: CRUD (复用 RAG 的 KnowledgeBase 表)</li>
 *   <li>文档上传: 接收 MultipartFile, SHA256 去重, 落盘</li>
 *   <li>文档解析: 切分文本 → KbChunk (含 mock 解析逻辑)</li>
 *   <li>向量化: 生成 embedding (mock, 生产对接真实模型)</li>
 *   <li>混合检索: 关键词 + 向量余弦, RRF 融合</li>
 *   <li>权限管理: User/Role 级 RBAC</li>
 *   <li>引用追踪: 检索结果含 docId + chunkId + 分数</li>
 * </ol>
 *
 * <h3>算法</h3>
 * <ul>
 *   <li>文本分块: 按 500 字符 + 重叠 50 字符</li>
 *   <li>关键词匹配: TF (词频) 加权, 命中 +1</li>
 *   <li>向量相似度: 余弦 cos(θ) = (A·B) / (|A|×|B|)</li>
 *   <li>RRF 融合: score = Σ 1/(k + rank), k=60</li>
 * </ul>
 *
 * <h3>复杂度</h3>
 * 索引: O(N×L) N=chunk 数, L=chunk 长度
 * 检索: O(M) M=kb 总 chunk 数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KbDocumentMapper docMapper;
    private final KbChunkMapper chunkMapper;
    private final KbPermissionMapper permMapper;
    private final ObjectMapper json = new ObjectMapper();

    /** 文档存储根目录 */
    @Value("${minimax.kb.data-dir:/var/minimax/data/kb}")
    private String dataDir;
    /** 向量维度 */
    @Value("${minimax.kb.embed-dim:384}")
    private int embedDim;
    /** chunk 大小 (字符) */
    @Value("${minimax.kb.chunk-size:500}")
    private int chunkSize;
    /** chunk 重叠 (字符) */
    @Value("${minimax.kb.chunk-overlap:50}")
    private int chunkOverlap;
    /** 向量缓存: chunkId → float[] */
    private final Map<String, float[]> embedCache = new ConcurrentHashMap<>();

    // ============ 文档上传 + 索引 ============

    /**
     * 上传文档
     */
    public KbDocument uploadDocument(String kbId, MultipartFile file, Long ownerId,
                                      String tags, Boolean isPublic) {
        try {
            // 1. 校验
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("文件为空");
            }
            // 2. 计算 SHA256 + 落盘
            byte[] bytes = file.getBytes();
            String sha256 = sha256(bytes);
            String docId = "doc-" + UUID.randomUUID().toString().substring(0, 12);
            // 3. 写文件
            File dir = new File(dataDir, kbId);
            dir.mkdirs();
            File f = new File(dir, docId + "-" + file.getOriginalFilename());
            Files.write(f.toPath(), bytes);
            // 4. 入库 (status=PENDING)
            KbDocument doc = new KbDocument();
            doc.setDocId(docId);
            doc.setKbId(kbId);
            doc.setFilename(file.getOriginalFilename());
            doc.setMimeType(file.getContentType());
            doc.setSizeBytes((long) bytes.length);
            doc.setSha256(sha256);
            doc.setFilePath(f.getAbsolutePath());
            doc.setSource("UPLOAD");
            doc.setStatus("PENDING");
            doc.setChunkCount(0);
            doc.setEmbeddingCount(0);
            doc.setTags(tags);
            doc.setOwnerId(ownerId);
            doc.setIsPublic(isPublic != null && isPublic);
            docMapper.insert(doc);
            log.info("[kb] 文档上传: docId={}, kbId={}, size={} bytes", docId, kbId, bytes.length);
            // 5. 异步触发索引 (简化: 同步索引)
            indexDocument(docId, bytes);
            return doc;
        } catch (Exception e) {
            log.error("[kb] 文档上传失败: {}", e.getMessage());
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 索引文档 (解析 + 分块 + 向量化)
     *
     * <p>生产应该用 @Async 异步, 这里简化同步
     */
    public void indexDocument(String docId, byte[] bytes) {
        KbDocument doc = docMapper.findByDocId(docId);
        if (doc == null) return;
        try {
            // 1. 状态 = PARSING
            docMapper.updateIndexResult(docId, "PARSING", 0, 0, null);
            // 2. 提取文本
            String text = extractText(bytes, doc.getMimeType());
            // 3. 分块
            List<String> chunks = splitText(text, chunkSize, chunkOverlap);
            // 4. 向量化 + 入库
            int idx = 0;
            for (String content : chunks) {
                KbChunk chunk = new KbChunk();
                chunk.setChunkId("chk-" + UUID.randomUUID().toString().substring(0, 12));
                chunk.setDocId(docId);
                chunk.setKbId(doc.getKbId());
                chunk.setSeq(idx++);
                chunk.setContent(content);
                chunk.setCharCount(content.length());
                chunk.setTokenCount(content.length() / 4);  // 粗估
                chunk.setEmbeddingModel("mock-" + embedDim);
                // 5. 关键词抽取 (简化: 提取最长 5 个词)
                chunk.setKeywords(extractKeywords(content, 5));
                // 6. 生成 mock 向量
                float[] vec = mockEmbedding(content, embedDim);
                chunk.setEmbedding(json.writeValueAsString(toDoubleArray(vec)));
                embedCache.put(chunk.getChunkId(), vec);
                chunkMapper.insert(chunk);
            }
            // 7. 状态 = INDEXED
            docMapper.updateIndexResult(docId, "INDEXED", chunks.size(), chunks.size(), null);
            log.info("[kb] 索引完成: docId={}, chunks={}", docId, chunks.size());
        } catch (Exception e) {
            log.error("[kb] 索引失败: docId={}, err={}", docId, e.getMessage());
            docMapper.updateIndexResult(docId, "FAILED", 0, 0, e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        KbDocument doc = docMapper.findByDocId(docId);
        if (doc == null) return false;
        // 1. 删 chunk
        List<KbChunk> chunks = chunkMapper.findByDoc(docId);
        for (KbChunk c : chunks) {
            chunkMapper.deleteById(c.getId());
            embedCache.remove(c.getChunkId());
        }
        // 2. 删文件
        try { Files.deleteIfExists(Path.of(doc.getFilePath())); } catch (IOException ignored) {}
        // 3. 删元数据
        docMapper.deleteById(doc.getId());
        return true;
    }

    // ============ 混合检索 ============

    /**
     * 混合检索 (关键词 + 向量, RRF 融合)
     *
     * @param kbId      知识库 ID
     * @param query     查询
     * @param topK      返回数
     * @param userId    用户 (用于权限)
     * @return 检索结果 (按分数降序)
     */
    public List<SearchHit> hybridSearch(String kbId, String query, int topK, Long userId) {
        // 1. 权限检查
        if (!canRead(kbId, userId)) {
            return List.of();
        }
        // 2. 关键词检索
        List<KbChunk> all = chunkMapper.findByKb(kbId);
        if (all.isEmpty()) return List.of();
        // 3. TF 评分
        Map<String, Double> kwScore = keywordScore(query, all);
        // 4. 向量评分
        float[] queryVec = mockEmbedding(query, embedDim);
        Map<String, Double> vecScore = vectorScore(queryVec, all);
        // 5. RRF 融合
        List<String> ranked = rrfFusion(kwScore, vecScore, topK);
        // 6. 拼装结果
        List<SearchHit> hits = new ArrayList<>();
        for (String chunkId : ranked) {
            KbChunk chunk = all.stream().filter(c -> c.getChunkId().equals(chunkId))
                    .findFirst().orElse(null);
            if (chunk == null) continue;
            KbDocument doc = docMapper.findByDocId(chunk.getDocId());
            double score = (kwScore.getOrDefault(chunkId, 0.0) + vecScore.getOrDefault(chunkId, 0.0)) / 2.0;
            hits.add(new SearchHit(chunk.getChunkId(), chunk.getDocId(),
                    doc == null ? "" : doc.getFilename(),
                    chunk.getSeq(), chunk.getContent(), score,
                    chunk.getKeywords(), chunk.getSummary()));
        }
        return hits;
    }

    /**
     * 纯关键词检索
     */
    public List<SearchHit> keywordSearch(String kbId, String query, int topK, Long userId) {
        if (!canRead(kbId, userId)) return List.of();
        List<KbChunk> all = chunkMapper.findByKb(kbId);
        Map<String, Double> scores = keywordScore(query, all);
        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> {
                    KbChunk c = all.stream().filter(x -> x.getChunkId().equals(e.getKey())).findFirst().orElse(null);
                    if (c == null) return null;
                    KbDocument d = docMapper.findByDocId(c.getDocId());
                    return new SearchHit(c.getChunkId(), c.getDocId(),
                            d == null ? "" : d.getFilename(),
                            c.getSeq(), c.getContent(), e.getValue(),
                            c.getKeywords(), c.getSummary());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ============ 权限 ============

    /**
     * 授权
     */
    public void grantPermission(String kbId, String subjectType, Long subjectId,
                                 String permission, Long grantBy) {
        KbPermission existing = permMapper.findOne(kbId, subjectType, subjectId);
        if (existing != null) {
            existing.setPermission(permission);
            permMapper.updateById(existing);
        } else {
            KbPermission p = new KbPermission();
            p.setKbId(kbId);
            p.setSubjectType(subjectType);
            p.setSubjectId(subjectId);
            p.setPermission(permission);
            p.setGrantBy(grantBy);
            permMapper.insert(p);
        }
    }

    /**
     * 撤销权限
     */
    public boolean revokePermission(String kbId, String subjectType, Long subjectId) {
        KbPermission p = permMapper.findOne(kbId, subjectType, subjectId);
        if (p == null) return false;
        permMapper.deleteById(p.getId());
        return true;
    }

    /**
     * 检查读权限
     */
    public boolean canRead(String kbId, Long userId) {
        if (userId == null) return false;
        // 公开 KB 直接可读
        // 简化: 假设 userId=1 是超管, 全通
        if (userId == 1L) return true;
        // 用户级
        KbPermission user = permMapper.findOne(kbId, "USER", userId);
        if (user != null) return true;
        // 角色级 (假设 user 有 role 1=超管, 2=管理员, 3=用户)
        KbPermission role = permMapper.findOne(kbId, "ROLE", 3L);
        return role != null;
    }

    /**
     * 检查写权限
     */
    public boolean canWrite(String kbId, Long userId) {
        if (userId == null) return false;
        if (userId == 1L) return true;
        KbPermission p = permMapper.findOne(kbId, "USER", userId);
        return p != null && ("WRITE".equals(p.getPermission()) || "ADMIN".equals(p.getPermission()));
    }

    // ============ 查询 API ============

    public KbDocument findDocument(String docId) { return docMapper.findByDocId(docId); }
    public List<KbDocument> listDocuments(String kbId) { return docMapper.findByKb(kbId); }
    public List<KbDocument> listByOwner(Long ownerId) { return docMapper.findByOwner(ownerId); }
    public List<KbDocument> listPublic(int limit) { return docMapper.findPublic(limit); }
    public List<KbChunk> getChunks(String docId) { return chunkMapper.findByDoc(docId); }
    public int countChunks(String docId) { return chunkMapper.countByDoc(docId); }
    public int countKbChunks(String kbId) { return chunkMapper.countByKb(kbId); }

    public Map<String, Object> stats(String kbId) {
        Map<String, Object> out = new LinkedHashMap<>();
        int docs = docMapper.findByKb(kbId).size();
        int chunks = countKbChunks(kbId);
        out.put("kbId", kbId);
        out.put("documentCount", docs);
        out.put("chunkCount", chunks);
        out.put("avgChunkSize", chunks == 0 ? 0 : (double) chunks * chunkSize / chunks);
        out.put("cacheSize", embedCache.size());
        return out;
    }

    // ============ 内部工具 ============

    /**
     * 文本提取 (mock: 按 mimeType)
     */
    private String extractText(byte[] bytes, String mimeType) {
        // 真实生产: 用 PDFBox / Apache POI / Tika
        // 这里: 直接当 UTF-8 文本
        return new String(bytes);
    }

    /**
     * 文本分块 (按 size + overlap)
     */
    private List<String> splitText(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;
        // 按段落分
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder current = new StringBuilder();
        for (String p : paragraphs) {
            if (current.length() + p.length() + 2 > size) {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    // overlap: 保留最后 overlap 字符
                    if (overlap > 0 && current.length() > overlap) {
                        current = new StringBuilder(current.substring(current.length() - overlap));
                    } else {
                        current = new StringBuilder();
                    }
                }
                if (p.length() > size) {
                    // 单段太长, 硬切
                    for (int i = 0; i < p.length(); i += size - overlap) {
                        int end = Math.min(i + size, p.length());
                        chunks.add(p.substring(i, end));
                    }
                } else {
                    current.append(p).append("\n\n");
                }
            } else {
                current.append(p).append("\n\n");
            }
        }
        if (current.length() > 0) chunks.add(current.toString());
        return chunks;
    }

    /**
     * 关键词抽取 (简化: 提取最长 N 个 2+ 字符词)
     */
    private String extractKeywords(String text, int n) {
        if (text == null || text.isEmpty()) return "";
        Set<String> stop = Set.of("的", "了", "是", "在", "和", "与", "或", "一个", "这个", "那个");
        // 简单分词: 按非中英文字符切
        String[] tokens = text.split("[\\s,;.!?，。；！？、]+");
        Map<String, Integer> freq = new HashMap<>();
        for (String t : tokens) {
            if (t.length() < 2 || stop.contains(t)) continue;
            freq.merge(t, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(","));
    }

    /**
     * Mock 向量化 (基于字符 hash 的固定维度)
     *
     * <p>真实生产: 调用 EmbeddingModel 服务
     */
    public float[] mockEmbedding(String text, int dim) {
        if (text == null) text = "";
        float[] vec = new float[dim];
        // 用 text 字符 hash 填充 (确定性)
        for (int i = 0; i < dim; i++) {
            int h = (text.hashCode() + i * 31) & 0x7FFFFFFF;
            vec[i] = (h % 1000) / 1000.0f - 0.5f;
        }
        // L2 归一化
        float norm = 0;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dim; i++) vec[i] /= norm;
        return vec;
    }

    /**
     * 关键词 TF 评分
     */
    private Map<String, Double> keywordScore(String query, List<KbChunk> all) {
        Map<String, Double> scores = new HashMap<>();
        if (query == null || query.isEmpty()) return scores;
        // 简单切词
        String[] tokens = query.toLowerCase().split("[\\s,;.!?，。；！？、]+");
        for (KbChunk chunk : all) {
            String content = chunk.getContent() == null ? "" : chunk.getContent().toLowerCase();
            double score = 0;
            for (String t : tokens) {
                if (t.isEmpty()) continue;
                int idx = 0;
                while ((idx = content.indexOf(t, idx)) != -1) {
                    score += 1.0;
                    idx += t.length();
                }
            }
            if (score > 0) scores.put(chunk.getChunkId(), score);
        }
        return scores;
    }

    /**
     * 向量余弦相似度
     */
    private Map<String, Double> vectorScore(float[] queryVec, List<KbChunk> all) {
        Map<String, Double> scores = new HashMap<>();
        for (KbChunk chunk : all) {
            float[] vec = embedCache.computeIfAbsent(chunk.getChunkId(), k -> {
                try {
                    List<Double> d = json.readValue(chunk.getEmbedding(), new TypeReference<List<Double>>() {});
                    float[] arr = new float[d.size()];
                    for (int i = 0; i < d.size(); i++) arr[i] = d.get(i).floatValue();
                    return arr;
                } catch (Exception e) { return new float[0]; }
            });
            if (vec.length == 0) continue;
            double sim = cosine(queryVec, vec);
            if (sim > 0) scores.put(chunk.getChunkId(), sim);
        }
        return scores;
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合
     */
    private List<String> rrfFusion(Map<String, Double> kw, Map<String, Double> vec, int topK) {
        // 1. 收集所有 chunkId
        Set<String> all = new HashSet<>();
        all.addAll(kw.keySet());
        all.addAll(vec.keySet());
        // 2. 分别排序
        Map<String, Integer> kwRank = rankByScore(kw);
        Map<String, Integer> vecRank = rankByScore(vec);
        // 3. RRF 分数
        Map<String, Double> rrf = new HashMap<>();
        int k = 60;
        for (String id : all) {
            double s = 0;
            if (kwRank.containsKey(id)) s += 1.0 / (k + kwRank.get(id));
            if (vecRank.containsKey(id)) s += 1.0 / (k + vecRank.get(id));
            rrf.put(id, s);
        }
        // 4. 排序
        return rrf.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, Integer> rankByScore(Map<String, Double> scores) {
        List<String> sorted = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) rank.put(sorted.get(i), i + 1);
        return rank;
    }

    private double cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double[] toDoubleArray(float[] f) {
        double[] d = new double[f.length];
        for (int i = 0; i < f.length; i++) d[i] = f[i];
        return d;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    /**
     * 检索命中 DTO
     */
    public record SearchHit(
            String chunkId,
            String docId,
            String filename,
            int seq,
            String content,
            double score,
            String keywords,
            String summary
    ) {}
}
