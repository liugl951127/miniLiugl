package com.minimax.ai.distribute.spark;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spark-style 多机并行 (V3.5.4) 单元测试
 */
class SparkTest {

    private SparkContext sc;

    @BeforeEach
    void setUp() {
        sc = new SparkContext(3);
    }

    @AfterEach
    void tearDown() {
        if (sc != null) sc.stop();
    }

    /**
     * 测试 1: SparkContext 初始化
     */
    @Test
    @DisplayName("1. SparkContext 启动 (3 workers)")
    void testInit() {
        assertNotNull(sc);
        assertEquals(3, sc.getWorkers().size());
        for (SparkWorker w : sc.getWorkers()) {
            assertTrue(w.isAlive());
            assertEquals(4, w.getCores());
        }
    }

    /**
     * 测试 2: parallelize
     */
    @Test
    @DisplayName("2. parallelize (100 元素 / 4 partitions)")
    void testParallelize() {
        List<Integer> data = IntStream.range(0, 100).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 4);
        assertEquals(4, ds.getNumPartitions());
        assertEquals(100, ds.getData().size());
    }

    /**
     * 测试 3: map + collect (transform lazy, action eager)
     */
    @Test
    @DisplayName("3. map + collect (1 stage)")
    void testMapCollect() {
        List<Integer> data = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 3);
        List<Integer> doubled = ds.map(x -> x * 2).collect();
        assertEquals(10, doubled.size());
        // 结果应包含 0, 2, 4, ..., 18
        Set<Integer> set = new HashSet<>(doubled);
        for (int i = 0; i < 10; i++) {
            assertTrue(set.contains(i * 2), "缺 " + (i * 2));
        }
    }

    /**
     * 测试 4: filter
     */
    @Test
    @DisplayName("4. filter 偶数")
    void testFilter() {
        List<Integer> data = IntStream.range(0, 20).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 3);
        List<Integer> evens = ds.filter(x -> x % 2 == 0).collect();
        assertEquals(10, evens.size());
        for (int v : evens) {
            assertEquals(0, v % 2, "filter 后应都是偶数: " + v);
        }
    }

    /**
     * 测试 5: reduce 全局聚合
     */
    @Test
    @DisplayName("5. reduce 求和")
    void testReduce() {
        List<Integer> data = IntStream.range(1, 101).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 4);
        Integer sum = ds.reduce(Integer::sum);
        assertEquals(5050, sum, "1+...+100=5050");
    }

    /**
     * 测试 6: groupByKey
     */
    @Test
    @DisplayName("6. groupByKey (shuffle)")
    void testGroupByKey() {
        List<String> words = Arrays.asList("hello", "world", "hello", "spark", "world", "hello");
        DistributedDataset<String> ds = sc.parallelize(words, 3);
        DistributedDataset<Map.Entry<String, List<String>>> grouped =
                ds.groupByKey(w -> w);
        List<Map.Entry<String, List<String>>> result = grouped.collect();
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, List<String>> e : result) {
            counts.put(e.getKey(), e.getValue().size());
        }
        assertEquals(3, counts.get("hello"));
        assertEquals(2, counts.get("world"));
        assertEquals(1, counts.get("spark"));
    }

    /**
     * 测试 7: 多 Stage 链 (map → filter → collect)
     */
    @Test
    @DisplayName("7. 多 Stage 链 (map → filter)")
    void testMultiStage() {
        List<Integer> data = IntStream.range(0, 30).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 3);
        List<Integer> result = ds.map(x -> x * x).filter(x -> x > 100).collect();
        // x*x > 100 → x > 10 → 平方 > 100: 11²=121, 12²=144, ...
        // 0-29 平方 > 100 的: 11-29 → 19 个
        assertEquals(19, result.size());
        for (int v : result) {
            assertTrue(v > 100, "应 > 100, 实际 " + v);
        }
    }

    /**
     * 测试 8: textFile 文本读取
     */
    @Test
    @DisplayName("8. textFile 按行读取 + word count")
    void testTextFile() {
        String text = "hello world\nhello spark\nworld spark spark\n";
        DistributedDataset<String> ds = sc.textFile(text, 2);
        long lineCount = ds.count();
        assertEquals(3, lineCount);
    }

    /**
     * 测试 9: Worker 掉线容错 (测试 kill)
     */
    @Test
    @DisplayName("9. Worker kill (alive=false)")
    void testWorkerKill() {
        SparkWorker w = sc.getWorkers().get(0);
        w.kill();
        assertFalse(w.isAlive());
        w.revive();
        assertTrue(w.isAlive());
    }

    /**
     * 测试 10: Worker 执行 Task
     */
    @Test
    @DisplayName("10. Worker executeTask (map task)")
    void testWorkerExecute() {
        SparkWorker w = sc.getWorkers().get(0);
        SparkTask task = SparkTask.builder()
                .taskId("t1")
                .stageId("s1")
                .jobId("j1")
                .partition(Arrays.asList(1, 2, 3))
                .partitionIdx(0)
                .function(input -> {
                    List<Object> out = new ArrayList<>();
                    for (Object o : input) out.add(((Integer) o) * 10);
                    return out;
                })
                .build();
        TaskResult r = w.executeTask(task);
        assertTrue(r.isSuccess());
        assertEquals(3, ((List<?>) r.getResult()).size());
    }

    /**
     * 测试 11: Shuffle put/get
     */
    @Test
    @DisplayName("11. Shuffle 存读 (mock transport)")
    void testShuffle() {
        SparkWorker w = sc.getWorkers().get(0);
        w.putShuffle("shuffle-1", 0, Arrays.asList("a", "b", "c"));
        w.putShuffle("shuffle-1", 1, Arrays.asList("d"));
        List<List<Object>> data = w.getShuffle("shuffle-1");
        assertEquals(2, data.size());
        assertEquals(3, data.get(0).size());
        assertEquals(1, data.get(1).size());
    }

    /**
     * 测试 12: 统计
     */
    @Test
    @DisplayName("12. SparkContext.Stats (jobs/stages/tasks)")
    void testStats() {
        List<Integer> data = IntStream.range(0, 50).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 3);
        ds.map((Integer x) -> x + 1).filter((Integer x) -> x % 2 == 0).collect();
        Map<String, Object> s = sc.getStats().snapshot();
        assertTrue((long) s.get("jobs") >= 1, "应至少 1 个 job");
        assertTrue((long) s.get("stages") >= 1, "应至少 1 个 stage");
        assertTrue((long) s.get("tasks") >= 3, "应至少 3 个 task (3 partitions)");
    }

    /**
     * 测试 13: 空数据集
     */
    @Test
    @DisplayName("13. 空数据集 collect")
    void testEmpty() {
        DistributedDataset<Integer> ds = sc.parallelize(Collections.emptyList(), 3);
        List<Integer> result = ds.map(x -> x).collect();
        assertEquals(0, result.size());
    }

    /**
     * 测试 14: 大数据量 (10000 元素)
     */
    @Test
    @DisplayName("14. 大数据量 (10000 元素) reduce")
    void testLargeData() {
        List<Integer> data = IntStream.range(1, 10001).boxed().collect(Collectors.toList());
        DistributedDataset<Integer> ds = sc.parallelize(data, 4);
        long sum = ds.reduce(Integer::sum);
        // 1+...+10000 = 10000*10001/2 = 50005000
        assertEquals(50_005_000L, sum);
    }

    /**
     * 测试 15: Submit 多个 Job
     */
    @Test
    @DisplayName("15. 多个 Job 提交 (统计累加)")
    void testMultipleJobs() {
        for (int i = 0; i < 3; i++) {
            List<Integer> data = IntStream.range(0, 10).boxed().collect(Collectors.toList());
            DistributedDataset<Integer> ds = sc.parallelize(data, 2);
            ds.map((Integer x) -> x + 1).collect();
        }
        Map<String, Object> s = sc.getStats().snapshot();
        assertEquals(3L, s.get("jobs"));
    }
}
