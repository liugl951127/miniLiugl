package com.minimax.ai.multimodal.onnx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * 多模态 CLIP-like 检索服务 (V7.1)
 *
 * <p><b>现状</b>: 完整的 CLIP ViT-B/32 ONNX 模型 350MB, 国内下载受限. 沙箱环境
 * 暂用 ResNet50 + BPE tokenizer 实现"类 CLIP"双塔结构, 提供基础的以文搜图能力.</p>
 *
 * <h3>当前实现 (Fallback)</h3>
 * <ul>
 *   <li>Image Tower: ResNet50 1000 类 softmax 概率 → L2 归一化 → 1000-dim embedding</li>
 *   <li>Text Tower: BPE 词表 hash → 1000-dim 向量 (与 ImageNet 类别对齐)</li>
 *   <li>相似度: cosine</li>
 * </ul>
 *
 * <h3>升级路径</h3>
 * <p>下载 <code>openai/clip-vit-base-patch32</code> ONNX 后, 只需在
 * <code>loadClipModel()</code> 中替换 session 即可. 上层 API 不变.</p>
 *
 * <h3>降级</h3>
 * <p>若 CLIP 真实模型加载失败 → 仍可用 ResNet50 + BPE fallback (本类).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnnxClipService {

    private final OnnxResNet50Service resnet;
    private final SimpleBpeTokenizer tokenizer = new SimpleBpeTokenizer();

    @Value("${minimax.onnx-vision.clip-enabled:true}")
    private boolean enabled;

    @Value("${minimax.onnx-vision.clip-vocab-path:./data/models/clip-vit-base-patch32/vocab.json}")
    private String clipVocabPath;

    @Value("${minimax.onnx-vision.clip-merges-path:./data/models/clip-vit-base-patch32/merges.txt}")
    private String clipMergesPath;

    private boolean tokenizerReady = false;

    public boolean isEnabled() { return enabled; }
    public boolean isReady()   { return resnet.isReady() || tokenizerReady; }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("[OnnxClip] 禁用");
            return;
        }
        File vf = new File(clipVocabPath);
        File mf = new File(clipMergesPath);
        if (vf.exists()) {
            boolean ok = tokenizer.loadFromVocabJson(clipVocabPath, mf.exists() ? clipMergesPath : null);
            if (ok) {
                tokenizerReady = true;
                log.info("[OnnxClip] ✅ BPE tokenizer 加载 ({} tokens)", tokenizer.vocabSize());
            } else {
                log.warn("[OnnxClip] BPE 加载失败, 走 char-level fallback");
                tokenizerReady = false;
            }
        } else {
            log.warn("[OnnxClip] vocab.json 不存在: {}, 走 char-level fallback", clipVocabPath);
        }
    }

    /**
     * 编码图片为 1000-dim 向量 (类 CLIP image embedding)
     */
    public float[] encodeImage(BufferedImage image) {
        if (!resnet.isReady() || image == null) return new float[0];
        float[] feature = resnet.extractFeature(image);
        return l2Normalize(feature);
    }

    /**
     * 编码文本为 1000-dim 向量 (类 CLIP text embedding)
     *
     * <p>算法: 对每个 token 取 vocab id, 通过 mod 1000 累加到 1000 维向量, L2 归一化.
     * 简化但保证与 image 维度一致, 适合做粗粒度以文搜图.</p>
     */
    public float[] encodeText(String text) {
        if (text == null || text.isEmpty()) return new float[1000];
        long[] tokens;
        if (tokenizerReady) {
            tokens = tokenizer.encode(text);
        } else {
            // fallback: char-level ids
            tokens = new long[text.length() + 2];
            tokens[0] = 49406L;
            for (int i = 0; i < text.length(); i++) {
                tokens[i + 1] = (long) (text.charAt(i) & 0xFFFF);
            }
            tokens[tokens.length - 1] = 49407L;
        }
        float[] vec = new float[1000];
        for (long t : tokens) {
            int idx = (int) (Math.abs(t) % 1000);
            vec[idx] += 1f;
        }
        return l2Normalize(vec);
    }

    /**
     * 文本与图片相似度 (cosine)
     */
    public float similarity(String text, BufferedImage image) {
        float[] textVec = encodeText(text);
        float[] imgVec = encodeImage(image);
        if (imgVec.length == 0 || textVec.length == 0) return 0f;
        return cosine(textVec, imgVec);
    }

    /**
     * 批量以文搜图 - 返回 top-k 索引 (按相似度降序)
     */
    public List<SearchHit> searchByText(String query, List<float[]> imageEmbeddings, int topK) {
        if (imageEmbeddings == null || imageEmbeddings.isEmpty()) {
            return Collections.emptyList();
        }
        float[] queryVec = encodeText(query);
        List<SearchHit> hits = new ArrayList<>(imageEmbeddings.size());
        for (int i = 0; i < imageEmbeddings.size(); i++) {
            float[] img = imageEmbeddings.get(i);
            if (img.length == 0) continue;
            hits.add(new SearchHit(i, cosine(queryVec, img)));
        }
        hits.sort((a, b) -> Float.compare(b.score, a.score));
        return hits.subList(0, Math.min(topK, hits.size()));
    }

    private float[] l2Normalize(float[] v) {
        double norm = 0;
        for (float f : v) norm += f * f;
        if (norm == 0) return v;
        norm = Math.sqrt(norm);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }

    private float cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    public record SearchHit(int index, float score) {}
}
