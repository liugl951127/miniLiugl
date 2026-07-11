# MiniMax Platform 运维手册 (V2.8.2)

> 日常运维 / 故障处理 / 性能调优

## 1. 日常巡检

### 1.1 健康检查脚本

`/opt/minimax/scripts/health-check.sh`:
```bash
#!/bin/bash
echo "=== 服务健康 ==="
for svc in gateway auth chat ai monitor admin; do
  status=$(docker inspect --format='{{.State.Health.Status}}' minimax-$svc 2>/dev/null)
  echo "  $svc: $status"
done

echo "=== 资源使用 ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"

echo "=== 磁盘 ==="
df -h /opt/minimax

echo "=== 数据库连接数 ==="
docker exec minimax-mariadb mariadb -uroot -proot123456 -e "SHOW STATUS LIKE 'Threads_connected'"

echo "=== Redis 内存 ==="
docker exec minimax-redis redis-cli -a minimax_redis_2024 --no-auth-warning INFO memory | grep used_memory_human
```

### 1.2 关键指标

| 指标 | 阈值 | 监控方式 |
|------|------|---------|
| CPU 使用率 | < 70% | docker stats |
| 内存使用率 | < 80% | docker stats |
| 磁盘使用率 | < 80% | df -h |
| DB 连接数 | < 400 (max 500) | SHOW STATUS |
| API P99 延迟 | < 1s | Prometheus |
| 错误率 | < 1% | 审计日志 |

## 2. 日志管理

### 2.1 日志位置

容器日志: `docker logs <container>`
应用日志: `/opt/minimax/data/<service>/logs/`
Nginx 访问日志: `/var/log/nginx/access.log`
Nginx 错误日志: `/var/log/nginx/error.log`

### 2.2 日志轮转

`/etc/logrotate.d/minimax`:
```
/opt/minimax/data/*/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    missingok
    notifempty
    create 0644 root root
    postrotate
        docker kill --signal=HUP minimax-gateway
    endscript
}
```

### 2.3 关键日志模式

```bash
# 错误日志
docker logs minimax-gateway 2>&1 | grep -E "ERROR|Exception" | tail -50

# 慢请求 (>1s)
docker logs minimax-gateway 2>&1 | grep "cost=[0-9]{4,}ms"

# SSE 连接
docker logs minimax-ai 2>&1 | grep "SSE"

# JWT 验证失败
docker logs minimax-auth 2>&1 | grep "JWT"
```

## 3. 备份策略

### 3.1 备份周期
- 每日凌晨 2:00 全量备份
- 每 6 小时增量备份
- 30 天滚动保留

### 3.2 备份验证
```bash
# 模拟恢复
docker run --rm -v /tmp/test:/backup alpine \
  sh -c "cd /backup && tar tzf latest.tar.gz > /dev/null && echo OK"
```

## 4. 监控告警

### 4.1 告警接收
- 钉钉机器人: `${DINGTALK_WEBHOOK}`
- 邮件: `ops@minimax.com`
- 企业微信: `${WECHAT_WEBHOOK}`

### 4.2 告警级别

| 级别 | 触发条件 | 通知方式 |
|------|---------|---------|
| Critical | 服务不可用 | 钉钉 + 短信 + 邮件 |
| Warning | 指标超阈值 | 钉钉 + 邮件 |
| Info | 重大变更 | 邮件 |

### 4.3 告警抑制
- 同类告警 5min 内不重复
- 维护窗口自动抑制

## 5. 故障应急 Runbook

### 5.1 服务宕机
```bash
# 1. 看状态
docker compose ps

# 2. 看日志
docker logs --tail=100 minimax-<service>

# 3. 重启
docker compose restart minimax-<service>

# 4. 验证
curl http://localhost:7080/actuator/health
```

### 5.2 数据库故障
```bash
# 1. 检查主从
docker exec minimax-mariadb mariadb -uroot -proot123456 -e "SHOW SLAVE STATUS"

# 2. 提升从库为主
docker exec minimax-mariadb mariadb -uroot -proot123456 -e "STOP SLAVE; RESET MASTER"

# 3. 更新应用配置 (Nacos)
# 修改 DB_HOST
```

### 5.3 Redis 故障
```bash
# 1. 重启
docker compose restart minimax-redis

# 2. 如果数据丢失, 重新加载
docker exec minimax-redis redis-cli -a minimax_redis_2024 FLUSHDB
```

### 5.4 磁盘满
```bash
# 1. 清理日志
docker system prune -a --volumes
journalctl --vacuum-time=2d

# 2. 清理备份
find /opt/minimax/backups -mtime +7 -delete

# 3. 扩容
df -h /opt/minimax
```

### 5.5 Nacos 故障
```bash
# 1. 重启
docker compose restart minimax-nacos

# 2. 备份配置
docker exec minimax-nacos bash -c "cd /home/nacos/conf && tar czf /tmp/conf.tar.gz ."

# 3. 恢复
docker cp /tmp/conf.tar.gz minimax-nacos:/home/nacos/conf/
```

## 6. 容量规划

### 6.1 用户数与资源
| 用户数 | CPU | 内存 | 存储 |
|--------|-----|------|------|
| 100 | 4 核 | 8 GB | 50 GB |
| 1000 | 8 核 | 16 GB | 200 GB |
| 10000 | 16 核 | 32 GB | 1 TB |
| 100000 | 64 核 | 128 GB | 10 TB |

### 6.2 数据库容量
- 用户表: 每用户 1KB, 100万用户 ≈ 1GB
- 聊天记录: 每条 5KB, 日均 100条/用户, 100万用户 ≈ 500GB/年
- 审计日志: 每条 2KB, 6个月 ≈ 100GB

## 7. 升级流程

### 7.1 应用升级
1. 备份 (4.2)
2. 拉新代码: `git pull`
3. 编译: `mvn package -DskipTests`
4. 滚动重启 (从 gateway 开始)
5. 验证: `/actuator/health`
6. 监控 30min

### 7.2 数据库迁移
1. 备份当前库
2. 写迁移脚本 (V*.sql)
3. 在测试环境演练
4. 生产库执行: `mariadb < V*.sql`
5. 验证表结构: `DESCRIBE table_name`

### 7.3 重大变更
- 提前 24h 通知所有用户
- 选择低峰期 (凌晨 2-6 点)
- 准备回滚方案
- 运维在场

## 8. 安全运维

### 8.1 定期审计
- 每周: 查看异常登录, 异常 IP
- 每月: 审计日志分析, 权限复查
- 每季: 密码轮换, 漏洞扫描

### 8.2 漏洞响应
1. 收到漏洞报告 → 评估严重性
2. Critical (24h) / High (7d) / Medium (30d)
3. 修复 → 测试 → 生产部署
4. 用户告知 (如数据泄露)

### 8.3 密钥轮换
- JWT Secret: 每年
- 数据库密码: 每季
- 钉钉 Webhook: 每年
- TLS 证书: 90天 (Let's Encrypt 自动)

## 9. 应急预案

### 9.1 通讯录
- 技术负责人: <name> <phone>
- DBA: <name> <phone>
- 运维: <name> <phone>
- 客服: <name> <phone>
- 7x24 紧急: <hotline>

### 9.2 升级路径
P0 (Critical): 5min 决策, 30min 修复
P1 (High): 1h 决策, 4h 修复
P2 (Medium): 4h 决策, 24h 修复
P3 (Low): 24h 决策, 7d 修复

### 9.3 沟通模板
```
【MiniMax P0 事故】 2026-07-12 04:30

现象: xxx
影响: 100% 用户无法登录
当前: 已止血 (gateway 重启), 根因排查中
负责人: @张三 @李四
预计恢复: 30min
下次更新: 5min 后
```

## 10. 性能调优

### 10.1 慢 SQL
```sql
-- 开启慢日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- 查慢 SQL
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 20;
```

### 10.2 热点接口
```bash
# Nginx access 日志分析
awk '{print $7}' /var/log/nginx/access.log | sort | uniq -c | sort -rn | head -20
```

### 10.3 JVM 调优
```bash
# 远程调试
docker exec minimax-gateway jcmd 1 VM.flags

# 堆转储
docker exec minimax-gateway jcmd 1 GC.heap_dump /tmp/heap.hprof
docker cp minimax-gateway:/tmp/heap.hprof .
```

## 11. 常用命令

```bash
# 查看所有容器
docker ps -a

# 资源使用
docker stats

# 实时日志
docker compose logs -f

# 进入容器
docker exec -it minimax-gateway sh

# 镜像列表
docker images | grep minimax

# 删除悬空镜像
docker image prune

# 磁盘占用
docker system df
```

---

**版本**: V2.8.2
**最后更新**: 2026-07-12
