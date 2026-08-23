package com.minimax.ai.multimodal.audio;

import ai.onnxruntime.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.*;

/**
 * ONNX Whisper-tiny 语音转文字 (V7.2 多模态)
 *
 * <p>支持中英 STT, 基于 onnx-community/whisper-tiny 的 decoder_model_merged (单文件推理).</p>
 *
 * <h3>模型规格</h3>
 * <ul>
 *   <li>输入 audio_features: float32[1, 80, 3000] (mel spectrogram)</li>
 *   <li>输入 encoder_hidden_states: 同 audio_features (首次自回归)</li>
 *   <li>输入 tokens: int64[1, seqLen] - 已生成的 token 序列 (含 SOT/语言/任务)</li>
 *   <li>输出 logits: float32[1, seqLen, 51865] (vocab size = 51865 for whisper-tiny)</li>
 * </ul>
 *
 * <h3>流程</h3>
 * <ol>
 *   <li>音频 → 16kHz PCM (重采样)</li>
 *   <li>PCM → mel spectrogram [80][3000]</li>
 *   <li>构造初始 tokens: [SOT, language, task, NOTIMESTAMPS]</li>
 *   <li>循环: 推理 → argmax → 拼接到 tokens → 直到 EOT 或 max=200</li>
 *   <li>token ids → text (Whisper BPE decoder)</li>
 * </ol>
 *
 * <h3>限制</h3>
 * <ul>
 *   <li>仅支持 30s 内音频 (超出截断)</li>
 *   <li>无时间戳输出 (V1, 后续可加)</li>
 *   <li>CPU int8 推理, 延迟 ~2-5s/30s 音频</li>
 * </ul>
 */
@Slf4j
@Service
public class OnnxWhisperService {

    @Value("${minimax.onnx-vision.whisper-path:./data/models/whisper-tiny/decoder.onnx}")
    private String modelPath;

    @Value("${minimax.onnx-vision.whisper-tokenizer:./data/models/whisper-tiny/tokenizer.json}")
    private String tokenizerPath;

    @Value("${minimax.onnx-vision.whisper-max-tokens:200}")
    private int maxTokens;

    private OrtEnvironment env;
    private OrtSession session;
    private final MelSpectrogram mel = new MelSpectrogram();
    private final WhisperTokenizer tokenizer = new WhisperTokenizer();

    private int vocabSize = 51865;
    private int numMelBins = 80;
    private int numFrames = 3000;

    public boolean isReady() { return session != null && tokenizer.vocabSize() > 0; }
    public String getModelPath() { return modelPath; }
    public int getVocabSize() { return vocabSize; }

    @PostConstruct
    public void init() {
        // 1. 加载 tokenizer
        File tokFile = new File(tokenizerPath);
        if (tokFile.exists()) {
            tokenizer.loadFromFile(tokenizerPath);
            vocabSize = tokenizer.vocabSize();
        } else {
            log.warn("[OnnxWhisper] tokenizer.json 不存在: {}", tokenizerPath);
        }
        // 2. 加载模型
        File f = new File(modelPath);
        if (!f.exists()) {
            log.warn("[OnnxWhisper] 模型不存在: {} — STT 不可用", modelPath);
            return;
        }
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(4);
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.EXTENDED_OPT);
            session = env.createSession(modelPath, opts);
            log.info("[OnnxWhisper] ✅ 加载成功: {} (vocab={}, threads=4)", modelPath, vocabSize);

            // 从 config.json 读 mel bins / frames
            File configFile = new File(modelPath.replace("decoder.onnx", "config.json"));
            if (configFile.exists()) {
                try {
                    JsonNode cfg = new ObjectMapper().readTree(configFile);
                    numMelBins = cfg.path("num_mel_bins").asInt(80);
                    // numFrames 由音频长度决定, 固定 3000 (30s)
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("[OnnxWhisper] 加载失败: {}", e.getMessage());
            session = null;
        }
    }

    @PreDestroy
    public void destroy() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (env != null) env.close(); } catch (Exception ignored) {}
    }

    /**
     * 语音转文字 (主入口)
     *
     * @param audio 16kHz 单声道 PCM, 任意长度 (≤ 30s)
     * @param lang  语言 ("en" / "zh"), 留空则自动 (默认 zh)
     * @return 转写结果
     */
    public TranscribeResult transcribe(float[] audio, String lang) {
        if (!isReady() || audio == null || audio.length == 0) {
            return new TranscribeResult("", "模型未就绪或音频为空", 0);
        }
        long start = System.currentTimeMillis();
        try {
            // 1. mel
            float[][] melSpec = mel.compute(audio);
            // [80][3000] -> [1, 80, 3000]
            float[] melFlat = new float[1 * numMelBins * numFrames];
            for (int m = 0; m < numMelBins; m++) {
                for (int t = 0; t < numFrames; t++) {
                    melFlat[m * numFrames + t] = melSpec[m][t];
                }
            }
            long[] melShape = {1, numMelBins, numFrames};

            // 2. 初始 tokens: [SOT, lang, TRANSCRIBE, NOTIMESTAMPS]
            int languageToken = "en".equalsIgnoreCase(lang) ? WhisperTokenizer.ENGLISH
                                : WhisperTokenizer.CHINESE;
            List<Long> tokens = new ArrayList<>();
            tokens.add((long) WhisperTokenizer.SOT);
            tokens.add((long) languageToken);
            tokens.add((long) WhisperTokenizer.TRANSCRIBE);
            tokens.add((long) WhisperTokenizer.NOTIMESTAMPS);

            // 3. 自回归
            int step = 0;
            while (step < maxTokens) {
                long[] tokenArr = tokens.stream().mapToLong(Long::longValue).toArray();
                long[] tokenShape = {1, tokenArr.length};

                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("audio_features", OnnxTensor.createTensor(env,
                    FloatBuffer.wrap(melFlat), melShape));
                inputs.put("tokens", OnnxTensor.createTensor(env,
                    LongBuffer.wrap(tokenArr), tokenShape));

                int nextToken;
                try (OrtSession.Result result = session.run(inputs)) {
                    OnnxTensor logitsTensor = (OnnxTensor) result.get(0);
                    float[][][] logits = (float[][][]) logitsTensor.getValue();
                    int lastIdx = tokenArr.length - 1;
                    // argmax over vocab
                    int best = 0;
                    float bestScore = -Float.MAX_VALUE;
                    float[] lastLogits = logits[0][lastIdx];
                    for (int v = 0; v < lastLogits.length; v++) {
                        if (lastLogits[v] > bestScore) {
                            bestScore = lastLogits[v];
                            best = v;
                        }
                    }
                    nextToken = best;
                }
                tokens.add((long) nextToken);
                if (nextToken == WhisperTokenizer.EOT) break;
                step++;
            }

            // 4. decode
            int[] tokenIds = tokens.stream().mapToInt(Long::intValue).toArray();
            String text = tokenizer.decode(tokenIds);
            long cost = System.currentTimeMillis() - start;
            return new TranscribeResult(text, null, cost);
        } catch (Exception e) {
            log.error("[OnnxWhisper] 推理失败: {}", e.getMessage(), e);
            return new TranscribeResult("", "推理失败: " + e.getMessage(),
                System.currentTimeMillis() - start);
        }
    }

    /**
     * 解码 MP3/WAV → 16kHz mono PCM.  委托给 {@link WavReader}.
     */
    public static float[] decodeWavToPcm16k(byte[] wav) {
        return WavReader.readAsMonoFloat16k(wav);
    }

    public record TranscribeResult(String text, String error, long costMs) {
        public boolean isSuccess() { return error == null; }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("text", text);
            m.put("error", error);
            m.put("costMs", costMs);
            m.put("ready", error == null);
            return m;
        }
    }
}
