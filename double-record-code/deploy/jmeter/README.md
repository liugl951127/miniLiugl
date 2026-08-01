# 双录一体化平台 - 压测方案

> 🎯 10 场景压测 · 目标 1000 QPS · P95 < 500ms · 错误率 < 0.1%

## 🎯 压测目标

| 指标 | 目标 | 实际(SLA 基准) |
|------|------|---------------|
| 峰值 QPS | ≥ 1000 | - |
| P95 响应时间 | < 500ms | - |
| P99 响应时间 | < 1s | - |
| 错误率 | < 0.1% | - |
| CPU 使用率 | < 70% | - |
| 内存使用率 | < 80% | - |
| 数据库连接 | < 80% | - |
| 0 数据丢失 | 100% | - |

## 📋 压测场景

| 场景 | 并发 | 持续 | 验证 |
|------|------|------|------|
| **1. 登录** | 20 | 5min | 限流 + JWT 签发 |
| **2. 订单查询** | 50 | 5min | 缓存命中率 |
| **3. 链上查询** | 30 | 5min | Fabric SDK 性能 |
| **4. 并发写订单** | 10 | - | 分布式锁 + 唯一约束 |
| **5. AI 质检** | 5 | - | LLM 限流 + 降级 |
| **6. CSRC 报送** | 2 | - | 大报文 + 字段加密 |
| **7. 文件上传** | 5 | - | 大文件分片 |
| **8. 限流压测** | 100 | - | 429 触发 |
| **9. 故障注入** | - | - | Redis 不可用降级 |
| **10. 长时间稳态** | 100 | 24h | 内存泄漏 / 慢 GC |

## 🚀 执行方式

### 方式 A:JMeter GUI(调试用)

```bash
# 1. 启动 JMeter
cd /opt/jmeter/bin
./jmeter

# 2. 打开 dual-record-test-plan.jmx
# 3. 配置目标地址(host/port/protocol)
# 4. 点击"运行"开始压测
```

### 方式 B:JMeter 命令行(压测执行)

```bash
# 1. 基础压测(100 用户,30 分钟)
jmeter -n -t dual-record-test-plan.jmx \
  -Jhost=dual-record.bank.com \
  -Jport=443 \
  -Jprotocol=https \
  -l results.jtl \
  -e -o report/

# 2. 高并发压测(1000 用户)
jmeter -n -t dual-record-test-plan.jmx \
  -Jhost=dual-record.bank.com \
  -l results_1k.jtl \
  -e -o report_1k/ \
  -Gthreads=1000 \
  -Gduration=1800

# 3. 分布式压测(多机)
# master:
jmeter -n -t dual-record-test-plan.jmx -r -l results.jtl
# slave:
jmeter-server
```

### 方式 C:持续压测(自动化)

```bash
# 每周日凌晨 02:00 跑 24h 稳态压测
0 2 * * 0 /opt/dual-record/scripts/stress-test.sh
```

## 📊 报告解读

### 关键指标

```bash
# 从 jtl 生成 HTML 报告
jmeter -g results.jtl -o report/

# 关键数字
Throughput:        1000 req/s        ← 目标 ≥ 1000
Average:           150 ms            ← 平均响应
90% Line:          300 ms            ← 90% 请求 < 300ms
95% Line:          450 ms            ← 目标 < 500ms
99% Line:          800 ms            ← 99% 请求 < 1s
Error %:           0.05%             ← 目标 < 0.1%
```

### 瓶颈识别

| 现象 | 瓶颈 | 解决方案 |
|------|------|----------|
| CPU > 80% | 应用计算 | 优化 SQL / 加 JVM 调优 |
| DB 连接满 | 数据库 | 加大连接池 / 读写分离 |
| GC 频繁 | JVM | 调大堆 / 换 ZGC |
| Redis CPU 高 | 序列化 | 用二进制协议 |
| Kafka 积压 | 消费者 | 增加 partition / 扩容 |
| 链上慢 | Fabric | 增大 gRPC 限流 / 优化链码 |

## 🔧 调优清单

### 应用层

- [ ] 启用 G1GC 或 ZGC
- [ ] 连接池调优(HikariCP max=30)
- [ ] MyBatis Plus 二级缓存
- [ ] 启用 HTTP/2 + gzip
- [ ] 异步处理非关键路径

### 数据库层

- [ ] 索引覆盖所有查询条件
- [ ] 慢查询 > 100ms 全部优化
- [ ] 启用 binlog + 归档
- [ ] 读写分离(读 > 写时)

### 缓存层

- [ ] 热点数据 Redis 缓存
- [ ] 多级缓存(Caffeine + Redis)
- [ ] 缓存预热(启动时加载)

### 区块链层

- [ ] SDK 连接池(10-20)
- [ ] 事件监听异步化
- [ ] 大数据上链改用 IPFS 存证

## 📈 容量规划

| 业务量 | Pod 数 | MySQL | Redis | Kafka | Fabric Peer |
|--------|--------|-------|-------|-------|-------------|
| 100 QPS | 2 | 2 核 4G | 1G | 3 节点 | 4 节点 |
| 500 QPS | 4 | 4 核 8G | 4G | 3 节点 | 4 节点 |
| 1000 QPS | 8 | 8 核 16G | 8G | 5 节点 | 8 节点 |
| 5000 QPS | 20 | 16 核 32G | 16G | 9 节点 | 12 节点 |

## 🆘 压测异常处理

### 502/504 错误

```bash
# 1. 检查 Pod 状态
kubectl get pods -n dual-record-prod

# 2. 检查 Ingress
kubectl describe ingress dual-record -n dual-record-prod

# 3. 调大 nginx 超时
kubectl edit ingress dual-record -n dual-record-prod
# 添加: nginx.ingress.kubernetes.io/proxy-read-timeout: "1200"
```

### OOM

```bash
# 1. 查看 JVM 堆
kubectl exec -it <pod> -- jmap -heap 1

# 2. 临时调大
kubectl set resources deployment/dual-record -n dual-record-prod \
  --limits=memory=8Gi --requests=memory=4Gi
```

### 数据库连接耗尽

```bash
# 1. 查看连接
mysql -e "SHOW PROCESSLIST" | wc -l

# 2. 调大 HikariCP
helm upgrade dual-record ./helm --reuse-values \
  --set env.HIKARI_MAX=50
```

## 📝 压测报告模板

```markdown
# 压测报告 - 2026-08-XX

## 测试环境
- 时间: 2026-08-XX 02:00-04:00
- 版本: dual-record v1.2.0
- 环境: 生产预发(staging-prod)

## 测试场景
[列出执行的场景]

## 关键结果
| 指标 | 目标 | 实际 | 通过 |
| QPS | ≥ 1000 | 1245 | ✅ |
| P95 | < 500ms | 425ms | ✅ |
| 错误率 | < 0.1% | 0.03% | ✅ |

## 发现的瓶颈
1. 订单详情接口 P95 = 480ms(主要来自数据库)
   - 优化: 加 Redis 缓存
2. 链上查询 P99 = 1.2s(网络延迟)
   - 优化: 增加 SDK 连接池

## 建议
1. 生产环境建议 8 副本起步
2. 数据库启用读写分离
3. 考虑 CDN 缓存静态资源
```

---

**版本**: 1.0  
**最后更新**: 2026-08-01  
**下次压测**: 2026-08-15(版本发布前)
