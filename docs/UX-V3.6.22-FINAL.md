# V3.6.22 清理无用文件 (一键)

## 1. 用户反馈

> 标记文件 无用就删除

截图标记的 5 类文件:
- `.view-v3.5.74-backup` 目录 (60 .vue 备份)
- `.view-v3.5.80/81/82/92-backup` 4 个空目录
- `vite.config.js.timestamp-1783916811548-...` Vite 临时
- `.env.development` (V3.6.19 我加的, 跟 .env.production 重复)
- 残留 `Alerts.vue.bak` 等

## 2. V3.6.22 改 (一键清理)

### 2.1 scripts/cleanup-junk.sh (新, 一键清理 5 类)

```bash
$ bash scripts/cleanup-junk.sh
═══════════════════════════════════════════════════════════
  V3.6.22+ 清理无用文件
═══════════════════════════════════════════════════════════

--- 1. .view-*-backup 目录 ---
  ✓ 删 .view-v3.5.74-backup (745K)

--- 2. vite.config.js.timestamp-* ---
  ✓ 删 vite.config.js.timestamp-1783916811548-... (19K)

--- 3. *.bak 文件 ---
  (无)

--- 4. .env.development ---
  ✓ 删 .env.development (Vite 默认加载 .env.development 已生效)

--- 5. .jwt-backup* ---
  (无)

--- 6. 加强 .gitignore ---
  ✓ .gitignore 加 vite.config.js.timestamp-* / .env.development / .eslintcache
```

### 2.2 清理列表

| 文件/目录 | 大小 | 来源 |
|----------|------|------|
| `.view-v3.5.74-backup/` | 745 KB | 60 .vue 历史备份 (V3.5.74 重写时) |
| `.view-v3.5.80-backup/` | 空 | V3.5.80 重写 |
| `.view-v3.5.81-backup/` | 空 | V3.5.81 重写 |
| `.view-v3.5.82-backup/` | 空 | V3.5.82 重写 |
| `.view-v3.5.92-backup/` | 空 | V3.5.92 重写 |
| `vite.config.js.timestamp-...` | 19 KB | Vite 7月生成, 已 stale |
| `.env.development` | 127 B | V3.6.19 我加的, 跟 .env.production 重复 |

### 2.3 .gitignore 加强

```gitignore
# V3.6.22+ 临时文件
vite.config.js.timestamp-*
.env.development
.eslintcache
```

## 3. 验证

| 测试 | 结果 |
|------|------|
| cleanup-junk.sh 跑 | ✅ 5 类全清 |
| .view-*-backup 残留 | ✅ 0 |
| vite.config.js.timestamp 残留 | ✅ 0 |
| .env.development 残留 | ✅ 0 |
| vite build 0 错 | ✅ |
| 21 路由 21/21 200 | ✅ |

## 4. 累计 77 个版本 (V3.5.46-V3.6.22)
