package com.minimax.ai.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 评测运行器 (CLI) - 不依赖 Spring 容器
 *
 * 用法:
 *   java -cp target/classes:... com.minimax.ai.eval.EvalRunner [eval-set-path] [tag|category] [report-output]
 *
 * 示例:
 *   java ... EvalRunner                                       # 跑默认集
 *   java ... EvalRunner eval/regression-set.json              # 指定集
 *   java ... EvalRunner eval/regression-set.json java         # 按 tag 过滤
 *   java ... EvalRunner eval/regression-set.json "" report.json  # 输出报告
 */
@Slf4j
public class EvalRunner {

    public static void main(String[] args) throws Exception {
        String evalSetPath = args.length > 0 ? args[0] : "eval/regression-set.json";
        String filter = args.length > 1 ? args[1] : "";
        String reportPath = args.length > 2 ? args[2] : null;

        log.info("EvalRunner 启动: evalSet={}, filter={}, report={}", evalSetPath, filter, reportPath);

        // 1. 创建独立 Spring 上下文 (或手动 wire)
        // 简化: 反射构造 KnowledgeRetriever
        // 实际生产: 走 SpringBootTest
        EvalService service = createEvalService(evalSetPath);
        if (service == null) {
            System.err.println("无法创建 EvalService (需要 Spring 上下文)");
            System.exit(1);
        }

        // 2. 跑评测
        EvalReport report;
        if (filter.isEmpty()) {
            report = service.runEvaluation();
        } else {
            // 先按 tag 试, 再按 category
            List<EvalCase> cases = service.getCachedCases();
            final String f = filter;
            List<EvalCase> filtered = cases.stream()
                    .filter(c -> (c.getTags() != null && c.getTags().contains(f))
                              || f.equals(c.getCategory()))
                    .toList();
            report = service.runEvaluation(filtered);
        }

        // 3. 输出报告
        printReport(report);
        if (reportPath != null) {
            saveReport(report, reportPath);
            log.info("报告已保存: {}", reportPath);
        }

        // 4. 退出码: 0=通过, 1=失败
        System.exit(report.passed == report.total ? 0 : 1);
    }

    /**
     * 创建 EvalService (简化: 不走 Spring, 反射 wire)
     * 生产环境请用 SpringBootTest 注入
     */
    @SuppressWarnings("unchecked")
    private static EvalService createEvalService(String evalSetPath) {
        try {
            // 反射构造 KnowledgeRetriever
            Class<?> krClass = Class.forName("com.minimax.ai.knowledge.KnowledgeRetriever");
            Class<?> seClass = Class.forName("com.minimax.ai.embedding.SimpleEmbedding");
            Class<?> tkClass = Class.forName("com.minimax.ai.tokenizer.ChineseTokenizer");
            Class<?> mtClass = Class.forName("com.minimax.ai.model.MiniTransformer");

            Object tokenizer = tkClass.getDeclaredConstructor().newInstance();
            Object transformer = mtClass.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class)
                    .newInstance(8192, 64, 4, 2, 128);
            Object embedding = seClass.getDeclaredConstructor(tkClass, mtClass).newInstance(tokenizer, transformer);
            Object kr = krClass.getDeclaredConstructor(seClass, tkClass).newInstance(embedding, tokenizer);
            krClass.getMethod("init").invoke(kr);

            // 构造 EvalService
            EvalService service = (EvalService) EvalService.class
                    .getDeclaredConstructors()[0]
                    .newInstance(kr);
            // 用反射绕过 @PostConstruct
            java.lang.reflect.Method load = EvalService.class.getDeclaredMethod("loadEvalSet", String.class);
            load.setAccessible(true);
            load.invoke(service, evalSetPath);
            return service;
        } catch (Exception e) {
            System.err.println("创建 EvalService 失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void printReport(EvalReport report) {
        System.out.println();
        System.out.println("========== 评测报告 ==========");
        System.out.println("时间: " + report.startedAt);
        System.out.println("耗时: " + report.totalMs + "ms");
        System.out.println("总计: " + report.total + " 条");
        System.out.println("通过: " + report.passed + " 条");
        System.out.println("失败: " + report.failed + " 条");
        System.out.println("通过率: " + String.format("%.1f%%", report.passRate * 100));
        System.out.println("平均分: " + String.format("%.3f", report.avgScore));
        System.out.println("平均延迟: " + String.format("%.0fms", report.avgLatencyMs));
        System.out.println();
        System.out.println("--- 按类别 ---");
        if (report.categoryStats != null) {
            report.categoryStats.values().stream()
                    .sorted((a, b) -> Double.compare(b.passRate, a.passRate))
                    .forEach(s -> System.out.printf("  %-20s %d/%d = %.0f%%%n",
                            s.category, s.passed, s.total, s.passRate * 100));
        }
        System.out.println();
        if (report.failures != null && !report.failures.isEmpty()) {
            System.out.println("--- 失败用例 (前 10) ---");
            report.failures.stream().limit(10).forEach(f -> {
                System.out.printf("  [%-8s] %s%n", f.caseId, f.question);
                System.out.printf("    答: %s%n", truncate(f.answer, 80));
                System.out.printf("    分: %.3f 原因: %s%n%n", f.score, f.reason);
            });
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private static void saveReport(EvalReport report, String path) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // 简化: 直接 toString (不序列化所有字段)
        Files.write(Paths.get(path), report.summary().getBytes());
    }
}
