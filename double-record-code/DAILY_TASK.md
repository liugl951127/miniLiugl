# 双录一体化平台 - 每日定时任务

> 🤖 自动化健康检查 · 每天 22:00 跑一次 · 当前已运行(PID 1282)

## 当前状态

```
✓ 守护进程: 运行中(PID 1282)
✓ 间隔: 86400 秒(24 小时)
✓ 首次执行: 2026-08-01 14:18:34(已成功)
✓ 下次执行: 2026-08-02 14:18:34
```

## 每日任务内容(10 步)

| # | 步骤 | 内容 | 输出 |
|---|------|------|------|
| 1 | 环境检查 | java/mvn/node/git/docker/python3/mysql 版本 | 报告 |
| 2 | Git 状态 | working tree + 最近 5 commit + remote | 报告 |
| 3 | Java 链码测试 | `mvn test`(53 用例) | 报告 |
| 4 | 后端测试 | `mvn test`(6+ 用例) | 报告 |
| 5 | TypeScript SDK 测试 | `jest`(62 用例) | 报告 |
| 6 | SQL 验证 | 表数量/今日订单/事件/状态分布 | 报告 |
| 7 | 代码体积 | 5 模块文件/行数 | 报告 |
| 8 | GitHub 同步 | 自动 commit + push | 日志 |
| 9 | 待办跟踪 | 17 个发现 + P0-P3 状态 | 报告 |
| 10 | 报告输出 | `.daily-reports/YYYY-MM-DD.md` | 文件 |

## 报告位置

- 报告:`double-record-code/.daily-reports/2026-08-01.md`
- 日志:`double-record-code/.daily-logs/2026-08-01.log`
- 守护进程日志:`double-record-code/.cron-daemon.log`
- 保留期:30 天(自动清理)

## 手动控制命令

```bash
# 查看状态
/workspace/double-record-code/cron-daemon.sh status

# 立即跑一次(测试)
/workspace/double-record-code/cron-daemon.sh run-once

# 停止
/workspace/double-record-code/cron-daemon.sh stop

# 重启
/workspace/double-record-code/cron-daemon.sh restart

# 自定义间隔(秒)
/workspace/double-record-code/cron-daemon.sh stop
DAILY_INTERVAL=3600 nohup /workspace/double-record-code/cron-daemon.sh start &
```

## 生产部署方案

4 种部署方式,详见 [`daily-task-deploy.md`](./daily-task-deploy.md):

| 方案 | 适用 | 难度 |
|------|------|------|
| **A. 常驻进程** | 沙箱/容器/无 crontab | ⭐ 最简单 |
| **B. Crontab** | 传统服务器 | ⭐⭐ |
| **C. Systemd Timer** | 现代 Linux | ⭐⭐ |
| **D. K8s CronJob** | 容器化生产 | ⭐⭐⭐ |

## 报告样例

```markdown
# 双录一体化平台 - 每日健康检查报告

> 日期: 2026-08-01 14:17:59

## 1. 环境检查
| 工具 | 版本 | 状态 |
| java | OpenJDK 11 | ✓ |
| mvn | 3.9 | ✓ |
| node | v22.17.0 | ✓ |
| ...

## 2. Git 状态
最近 5 次提交:
e5ed51c chore(gitignore): ...
f6c09fd chore(daily): ...
baa6820 feat(双录-增强): ...
...

## 3-7. 测试结果
✓ Java 链码 53/53 通过
✓ 后端 6/6 通过
✓ TypeScript SDK 62/62 通过
✓ SQL 17 张表,0 张异常
✓ 代码 67412 行(5 模块)

## 8. GitHub 同步
✅ 推送成功

## 9. 待办跟踪
P0: 1 项(限流)
P1: 4 项(JWT/Actuator/链码 nonce)
P2-P3: 12 项
```

## 监控告警集成

日报可对接 Prometheus + AlertManager:

```yaml
# prometheus rules
- alert: DailyTaskFailed
  expr: time() - daily_task_last_success_timestamp > 90000
  for: 1h
  labels:
    severity: critical
```

## 任务收益

- 🛡️ **早发现**:每天 22:00 自动捕获构建/测试/Git 异常
- 📊 **可量化**:连续日报形成趋势分析(测试通过率/代码增长/部署频率)
- 🤖 **零人工**:无需人工跑脚本,自动 commit + push
- 🔍 **可追溯**:30 天报告存档,任何问题可回溯

---

**版本**: 1.0
**最后更新**: 2026-08-01
**运行状态**: ✅ 已调度
**下次执行**: 2026-08-02 14:18:34
