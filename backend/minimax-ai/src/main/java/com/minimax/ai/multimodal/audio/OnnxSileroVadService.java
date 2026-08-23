package com.minimax.ai.multimodal.audio;

import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * ONNX Silero VAD 语音活动检测 (V7.2 多模态)
 *
 * <p>基于 Silero VAD v5 ONNX 模型, 16kHz 单声道, 输出 [batch, 1] 语音概率.</p>
 *
 * <h3>模型规格</h3>
 * <ul>
 *   <li>输入 audio: float32[1, chunk_size] (chunk_size ∈ {512, 1024, 1536} 对应 32/64/96ms @ 16kHz)</li>
 *   <li>输入 state: float32[2, 1, 128] (LSTM 隐藏状态, 初始全 0)</li>
 *   <li>输入 sr: int64[1] = 16000</li>
 *   <li>输出 output: float32[1, 1] - 语音概率 (0-1)</li>
 *   <li>输出 stateN: float32[2, 1, 128] - 新隐藏状态 (下一帧用)</li>
 * </ul>
 *
 * <h3>配置</h3>
 * <pre>
 * minimax.ai.onnx-vision:
 *   silero-vad-path: ${ONNX_SILERO_VAD_PATH:./data/models/silero-vad/model.onnx}
 *   silero-vad-threshold: ${ONNX_SILERO_VAD_THRESH:0.5}
 * </pre>
 */
@Slf4j
@Service
public class OnnxSileroVadService {

    @Value("${minimax.onnx-vision.silero-vad-path:./data/models/silero-vad/model.onnx}")
    private String modelPath;

    @Value("${minimax.onnx-vision.silero-vad-threshold:0.5}")
    private float threshold;

    @Value("${minimax.onnx-vision.silero-vad-chunk:512}")
    private int chunkSize;  // 512 samples = 32ms @ 16kHz

    private OrtEnvironment env;
    private OrtSession session;

    private static final int SAMPLE_RATE = 16000;
    private static final int LSTM_STATE_DIM = 128;
    private static final int LSTM_NUM_LAYERS = 2;

    public boolean isReady() { return session != null; }
    public String getModelPath() { return modelPath; }
    public float getThreshold() { return threshold; }

    @PostConstruct
    public void init() {
        java.io.File f = new java.io.File(modelPath);
        if (!f.exists()) {
            log.warn("[OnnxSileroVad] 模型不存在: {} — VAD 不可用", modelPath);
            return;
        }
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(2);
            session = env.createSession(modelPath, opts);
            log.info("[OnnxSileroVad] ✅ 加载成功: {} (chunk={}ms, threshold={})",
                modelPath, chunkSize * 1000 / SAMPLE_RATE, threshold);
        } catch (Exception e) {
            log.error("[OnnxSileroVad] 加载失败: {}", e.getMessage());
            session = null;
        }
    }

    @PreDestroy
    public void destroy() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (env != null) env.close(); } catch (Exception ignored) {}
    }

    /**
     * 检测音频中的语音段
     *
     * @param audio 16kHz 单声道 PCM (float[] -1..1)
     * @return 语音段列表 [{startSec, endSec, confidence}, ...]
     */
    public List<SpeechSegment> detectSegments(float[] audio) {
        if (!isReady() || audio == null || audio.length < chunkSize) {
            return Collections.emptyList();
        }
        List<SpeechSegment> segments = new ArrayList<>();
        SpeechSegment current = null;
        float[] state = new float[LSTM_NUM_LAYERS * 1 * LSTM_STATE_DIM];  // 2*1*128 = 256

        int total = audio.length;
        int i = 0;
        while (i + chunkSize <= total) {
            float[] chunk = Arrays.copyOfRange(audio, i, i + chunkSize);
            float prob = infer(chunk, state);
            float startSec = (float) i / SAMPLE_RATE;
            boolean voiced = prob >= threshold;
            if (voiced) {
                if (current == null) {
                    current = new SpeechSegment(startSec, startSec, prob);
                } else {
                    current.endSec = (float) (i + chunkSize) / SAMPLE_RATE;
                    current.maxConf = Math.max(current.maxConf, prob);
                }
            } else {
                if (current != null) {
                    segments.add(current);
                    current = null;
                }
            }
            i += chunkSize;
        }
        // 收尾
        if (current != null) {
            current.endSec = (float) total / SAMPLE_RATE;
            segments.add(current);
        }
        return segments;
    }

    /**
     * 单帧推理 (返回概率, 内部更新 state)
     */
    public float infer(float[] chunk, float[] state) {
        if (!isReady()) return 0f;
        try {
            // reshape state [2,1,128] -> flat [256]
            long[] stateShape = {LSTM_NUM_LAYERS, 1, LSTM_STATE_DIM};
            long[] audioShape = {1, chunk.length};
            long[] srShape = {1};

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input", OnnxTensor.createTensor(env, FloatBuffer.wrap(chunk), audioShape));
            inputs.put("state", OnnxTensor.createTensor(env, FloatBuffer.wrap(state), stateShape));
            inputs.put("sr", OnnxTensor.createTensor(env, LongBufferWrap(new long[]{SAMPLE_RATE}), srShape));

            try (OrtSession.Result result = session.run(inputs)) {
                OnnxTensor outTensor = (OnnxTensor) result.get(0);
                float[][] out = (float[][]) outTensor.getValue();
                float prob = out[0][0];

                // 更新 state (output 1)
                OnnxTensor newState = (OnnxTensor) result.get(1);
                float[][][] newStateArr = (float[][][]) newState.getValue();
                int idx = 0;
                for (int l = 0; l < LSTM_NUM_LAYERS; l++) {
                    for (int b = 0; b < 1; b++) {
                        for (int h = 0; h < LSTM_STATE_DIM; h++) {
                            state[idx++] = newStateArr[l][b][h];
                        }
                    }
                }
                return prob;
            }
        } catch (Exception e) {
            log.error("[OnnxSileroVad] 推理失败: {}", e.getMessage());
            return 0f;
        }
    }

    private java.nio.LongBuffer LongBufferWrap(long[] arr) {
        return java.nio.LongBuffer.wrap(arr);
    }

    public record SpeechSegment(float startSec, float endSec, float maxConf) {
        public float duration() { return endSec - startSec; }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("start", startSec);
            m.put("end", endSec);
            m.put("duration", duration());
            m.put("confidence", maxConf);
            return m;
        }
    }
}
