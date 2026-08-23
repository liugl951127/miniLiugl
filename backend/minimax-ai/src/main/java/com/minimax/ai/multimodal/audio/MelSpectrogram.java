package com.minimax.ai.multimodal.audio;

/**
 * Mel Spectrogram 提取器 (V7.2)
 *
 * <p>Whisper 标准: 16kHz, n_fft=400, hop=160, n_mels=80, 30s 窗口 = 3000 frames.</p>
 *
 * <h3>算法步骤</h3>
 * <ol>
 *   <li>Pre-emphasis: y = x[t] - 0.97 * x[t-1]</li>
 *   <li>Pad 480000 samples (30s) for STFT window (400 samples)</li>
 *   <li>STFT: 400-sample Hann window, hop 160, n_fft 400 → complex spectrum</li>
 *   <li>Power spectrum: |X|^2</li>
 *   <li>Mel filterbank (80 triangular filters, 0-8000Hz)</li>
 *   <li>log10(max(mel, 1e-10))</li>
 *   <li>归一化: (x + 4) / 4 (Whisper 官方做法)</li>
 * </ol>
 *
 * <p>输出: float[80][3000] (mel × frame).</p>
 */
public class MelSpectrogram {

    private static final int SAMPLE_RATE = 16000;
    private static final int N_FFT = 400;
    private static final int HOP = 160;
    private static final int N_MELS = 80;
    private static final int AUDIO_LEN = SAMPLE_RATE * 30;  // 480000
    private static final int N_FRAMES = AUDIO_LEN / HOP;    // 3000

    private final float[] hannWindow;
    private final float[][] melFilterbank;  // [n_mels][n_fft/2+1] = [80][201]

    public MelSpectrogram() {
        this.hannWindow = hannWindow(N_FFT);
        this.melFilterbank = buildMelFilterbank();
    }

    /**
     * 计算 mel spectrogram
     *
     * @param audio 16kHz 单声道, 长度任意 (< 30s 自动 pad, > 30s 截断)
     * @return float[N_MELS][N_FRAMES] = [80][3000]
     */
    public float[][] compute(float[] audio) {
        if (audio == null || audio.length == 0) {
            return new float[N_MELS][N_FRAMES];
        }
        // 1. Pre-emphasis
        float[] preemph = preemphasis(audio, 0.97f);
        // 2. Pad/截断到 30s
        float[] padded = padOrTruncate(preemph, AUDIO_LEN);
        // 3. STFT (一帧一帧)
        float[][] spec = stft(padded);
        // 4. 功率谱
        float[][] power = new float[N_FRAMES][N_FFT / 2 + 1];
        for (int t = 0; t < N_FRAMES; t++) {
            for (int k = 0; k < N_FFT / 2 + 1; k++) {
                float re = spec[t][k * 2];
                float im = spec[t][k * 2 + 1];
                power[t][k] = (float) (Math.log10(Math.max(re * re + im * im, 1e-10f)));
            }
        }
        // 5. Mel filterbank
        float[][] mel = new float[N_MELS][N_FRAMES];
        for (int m = 0; m < N_MELS; m++) {
            for (int t = 0; t < N_FRAMES; t++) {
                float sum = 0;
                for (int k = 0; k < N_FFT / 2 + 1; k++) {
                    sum += melFilterbank[m][k] * power[t][k];
                }
                mel[m][t] = sum;
            }
        }
        // 6. 归一化: clamp max to 8.0, normalize to [-1, 1]
        float max = -Float.MAX_VALUE;
        for (int m = 0; m < N_MELS; m++) {
            for (int t = 0; t < N_FRAMES; t++) {
                if (mel[m][t] > max) max = mel[m][t];
            }
        }
        float clamp = Math.max(max, -8.0f);
        for (int m = 0; m < N_MELS; m++) {
            for (int t = 0; t < N_FRAMES; t++) {
                mel[m][t] = (float) Math.max(mel[m][t], clamp - 8.0);
                mel[m][t] = (mel[m][t] + 4.0f) / 4.0f;
            }
        }
        return mel;
    }

    private float[] preemphasis(float[] x, float coeff) {
        float[] y = new float[x.length];
        y[0] = x[0];
        for (int i = 1; i < x.length; i++) {
            y[i] = x[i] - coeff * x[i - 1];
        }
        return y;
    }

    private float[] padOrTruncate(float[] x, int len) {
        if (x.length == len) return x;
        float[] y = new float[len];
        if (x.length > len) {
            System.arraycopy(x, 0, y, 0, len);
        } else {
            System.arraycopy(x, 0, y, 0, x.length);
        }
        return y;
    }

    /** STFT: 返回 [N_FRAMES][N_FFT + 2] (实部交错虚部) - 这里用 complex[] */
    private float[][] stft(float[] x) {
        int halfFft = N_FFT / 2;
        float[][] result = new float[N_FRAMES][(halfFft + 1) * 2];  // 实部 + 虚部
        for (int t = 0; t < N_FRAMES; t++) {
            int start = t * HOP;
            // 1. 帧 + window
            float[] frame = new float[N_FFT];
            for (int i = 0; i < N_FFT; i++) {
                int idx = start + i;
                if (idx < x.length) {
                    frame[i] = x[idx] * hannWindow[i];
                } else {
                    frame[i] = 0;
                }
            }
            // 2. FFT
            fftInPlace(frame);
            // 3. 取前 N_FFT/2+1 个 (实 + 虚 交错)
            for (int k = 0; k <= halfFft; k++) {
                result[t][k * 2] = frame[k * 2];
                result[t][k * 2 + 1] = frame[k * 2 + 1];
            }
        }
        return result;
    }

    /** In-place radix-2 FFT, frame 长度必须为 2 的幂 */
    private void fftInPlace(float[] a) {
        int n = a.length / 2;  // 复数个数
        if (n == 0) return;
        // bit reversal
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                int i2 = i * 2, j2 = j * 2;
                float tr = a[i2]; a[i2] = a[j2]; a[j2] = tr;
                float ti = a[i2 + 1]; a[i2 + 1] = a[j2 + 1]; a[j2 + 1] = ti;
            }
        }
        // butterfly
        for (int len = 1; len < n; len <<= 1) {
            float ang = (float) (-Math.PI / len);
            float wlenR = (float) Math.cos(ang);
            float wlenI = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len << 1) {
                float wR = 1, wI = 0;
                for (int k = 0; k < len; k++) {
                    int uR = (i + k) * 2;
                    int vR = (i + k + len) * 2;
                    float tR = wR * a[vR] - wI * a[vR + 1];
                    float tI = wR * a[vR + 1] + wI * a[vR];
                    a[vR] = a[uR] - tR;
                    a[vR + 1] = a[uR + 1] - tI;
                    a[uR] += tR;
                    a[uR + 1] += tI;
                    float nwR = wR * wlenR - wI * wlenI;
                    float nwI = wR * wlenI + wI * wlenR;
                    wR = nwR; wI = nwI;
                }
            }
        }
    }

    private float[] hannWindow(int n) {
        float[] w = new float[n];
        for (int i = 0; i < n; i++) {
            w[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)));
        }
        return w;
    }

    private float[][] buildMelFilterbank() {
        // Slaney mel scale
        float fMin = 0f;
        float fMax = SAMPLE_RATE / 2f;  // 8000
        // mel = 2595 * log10(1 + f/700), inverse: f = 700 * (10^(m/2595) - 1)
        float melMin = hzToMel(fMin);
        float melMax = hzToMel(fMax);
        int nFreqs = N_FFT / 2 + 1;  // 201

        float[] melPoints = new float[N_MELS + 2];
        for (int i = 0; i < N_MELS + 2; i++) {
            melPoints[i] = melMin + (melMax - melMin) * i / (N_MELS + 1);
        }
        float[] hzPoints = new float[N_MELS + 2];
        for (int i = 0; i < N_MELS + 2; i++) {
            hzPoints[i] = melToHz(melPoints[i]);
        }
        int[] bin = new int[N_MELS + 2];
        for (int i = 0; i < N_MELS + 2; i++) {
            bin[i] = (int) Math.floor((N_FFT + 1) * hzPoints[i] / SAMPLE_RATE);
        }

        float[][] fbank = new float[N_MELS][nFreqs];
        for (int m = 0; m < N_MELS; m++) {
            int left = bin[m];
            int center = bin[m + 1];
            int right = bin[m + 2];
            for (int k = left; k < center; k++) {
                if (k < nFreqs) fbank[m][k] = (k - left) / (float) (center - left);
            }
            for (int k = center; k < right; k++) {
                if (k < nFreqs) fbank[m][k] = (right - k) / (float) (right - center);
            }
        }
        return fbank;
    }

    private float hzToMel(float hz) {
        return (float) (2595.0 * Math.log10(1.0 + hz / 700.0));
    }

    private float melToHz(float mel) {
        return (float) (700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0));
    }

    public int getNumFrames() { return N_FRAMES; }
    public int getNumMels() { return N_MELS; }
}
