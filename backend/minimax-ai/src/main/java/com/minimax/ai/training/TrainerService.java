package com.minimax.ai.training;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * 训练服务 (V2.7.5)
 * 包装 MiniTrainer, 把训练过程指标打到 TrainingTracker
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainingTracker tracker;
    private final ChineseTokenizer tokenizer;

    /**
     * 训练一个完整的 task, 同步阻塞 (简单实现, 不开线程)
     */
    public void train(String taskId, MiniTransformer model, List<String> corpus,
                      int totalEpochs, double learningRate) {
        try {
            tracker.start(taskId);
            MiniTrainer trainer = new MiniTrainer(model, tokenizer);
            long start = System.currentTimeMillis();
            Random rng = new Random(42);

            for (int epoch = 1; epoch <= totalEpochs; epoch++) {
                double avgLoss = trainer.trainEpoch(corpus, learningRate);
                long elapsed = System.currentTimeMillis() - start;

                TrainingTracker.MetricPoint p = new TrainingTracker.MetricPoint(
                        epoch, epoch * corpus.size(),
                        avgLoss, avgLoss * (1 + rng.nextDouble() * 0.1),  // val_loss 模拟
                        Math.max(0, 1 - avgLoss / 8),                    // accuracy 估算
                        learningRate,
                        elapsed
                );
                tracker.record(taskId, p);
                log.info("[{}] epoch {}/{} loss={}", taskId, epoch, totalEpochs, avgLoss);
            }

            tracker.complete(taskId);
        } catch (Exception e) {
            log.error("Training failed: {}", taskId, e);
            tracker.fail(taskId, e.getMessage());
        }
    }
}
