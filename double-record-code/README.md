# 双录一体化平台 - 代码补全包

本目录包含双录一体化平台的核心代码补全,分为两大部分:

## 目录结构

```
double-record-code/
├── README.md                        # 本文档
├── frontend/                        # 前端代码(Vue 3 + TypeScript)
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── README.md
│   └── src/
│       ├── api/                     # 4 个 API 服务
│       │   ├── order.ts             # 订单 API
│       │   ├── script.ts            # 话术 API
│       │   ├── session.ts           # 双录会话 API
│       │   └── risk.ts              # 风评+合同 API
│       ├── components/              # 7 个业务组件
│       │   ├── ScriptRunner.vue     # 话术执行
│       │   ├── VideoRecorder.vue    # 视频录制
│       │   ├── RiskAssessment.vue   # 风险评估
│       │   ├── ESignature.vue       # 电子签约
│       │   ├── QualityMonitor.vue   # 质检监控
│       │   ├── ExceptionHandler.vue # 异常处理
│       │   └── VerifyIdentity.vue   # 身份核验
│       ├── views/                   # 页面
│       │   └── DualRecord.vue       # 双录主页面(流程编排)
│       ├── store/                   # Pinia 状态
│       │   └── dualRecord.ts
│       ├── router/                  # 路由
│       │   └── index.ts
│       ├── types/                   # TypeScript 类型
│       │   └── index.ts
│       └── utils/                   # 工具类
│           ├── request.ts           # HTTP 封装
│           ├── webrtc.ts            # 音视频录制
│           └── asr.ts               # 语音识别
└── sql/                             # SQL 脚本
    ├── 01_schema.sql                # 8 张核心表
    ├── 02_indexes.sql               # 索引
    ├── 03_init_data.sql             # 字典+话术+风评模板
    ├── 04_test_data.sql             # 测试数据
    └── 05_runbook.sql               # 常用运维查询
```

## SQL 脚本使用说明

按顺序执行:

```bash
mysql -u root -p dual_record < 01_schema.sql
mysql -u root -p dual_record < 02_indexes.sql
mysql -u root -p dual_record < 03_init_data.sql
# 仅开发环境执行
mysql -u root -p dual_record < 04_test_data.sql
```

## SQL 数据模型

### 8 张核心表

| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `t_customer` | 客户主数据 | customer_id, id_no, risk_level, risk_expire_at |
| `t_order` | 订单主表 | order_id, state (0~6), channel, sales_user_id |
| `t_session` | 双录会话 | session_id, order_id, video_url, video_hash |
| `t_script` | 话术模板 | script_id, version, is_active, gray_ratio |
| `t_script_node` | 话术节点 | node_id, script_id, node_seq, content, keywords |
| `t_risk_assess` | 风评问卷 | assess_id, answers(JSON), risk_level, product_match |
| `t_quality` | 质检结果 | qa_id, total_score, verdict, issues(JSON) |
| `t_contract` | 电子合同 | contract_id, file_hash, sign_serial, block_chain_tx |

### 索引策略

- **客户表**: 手机号哈希 + 风险等级 + 软删除
- **订单表**: 客户/产品/状态/客户经理/网点/渠道 + 联合索引
- **会话表**: 订单 + 状态 + 视频哈希
- **风评表**: 客户 + 有效期(用于过期清理)
- **质检表**: 会话 + 状态 + 分数(用于复检筛选)
- **合同表**: 客户 + 状态 + 区块链交易号

### 状态机约定

订单状态机(S0-S6):
- **S0** 已预约 → **S1** 已核验 → **S2** 话术执行中 → **S3** 视频录制中 → **S4** 电子签约 → **S5** 质检通过 → **S6** 订单完成
- 异常状态:-1 已取消 / -2 已失败

## 前端代码使用说明

### 安装与运行

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

### 核心组件使用

```vue
<template>
  <DualRecord :order-id="12345" />
</template>
```

### 单独使用组件

```vue
<template>
  <!-- 话术执行 -->
  <ScriptRunner :script="script" :session-id="sessionId" />

  <!-- 视频录制 -->
  <VideoRecorder :session-id="sessionId" :order-no="..." />

  <!-- 风评问卷 -->
  <RiskAssessment :order-id="..." :session-id="..." />

  <!-- 电子签约 -->
  <ESignature :order-id="..." :customer-id="..." />

  <!-- 质检监控 -->
  <QualityMonitor :session-id="..." :auto-trigger="true" />

  <!-- 异常处理 -->
  <ExceptionHandler :exception-type="'NETWORK'" />
</template>
```

## 关键设计要点

### 1. 话术原子化
话术模板被原子化拆解为 6 类节点(问候/产品/风险/适当性/犹豫期/确认),每类节点独立配置、独立版本管理。强制约束:
- 节点顺序不可跳跃
- 必读/必答/必确认位必须由客户本人口头表达
- 关键词必须命中

### 2. 视频合规
- **录制**:WebRTC + H.264 编码,2Mbps 视频 + 128kbps 音频
- **分片**:每 3 秒一个分片,自动重试 3 次(指数退避)
- **加密**:SM4 国密加密芯片级
- **指纹**:SHA-256 哈希防篡改
- **存证**:Hyperledger Fabric 区块链存证
- **存储**:OSS 3 副本 EC,热/温/冷分层

### 3. 智能质检
- **L1 规则层**(< 0.5s):100+ 规则模板
- **L2 AI 层**(< 30s):ASR + NLP + 情感 + 图像
- **L3 人工层**(T+1):高风险 100% 复检

### 4. 分布式事务
- 6 步事务链 T1-T6
- Saga 模式 + 状态机补偿
- 任一失败自动回滚 + 资源释放

### 5. 数据一致性
- 主键约束 + 唯一索引
- 外键 + 软删除(deleted_at)
- 乐观锁(version 字段)
- JSON 字段存储灵活业务数据
- 区块链哈希保证不可篡改

## 业务规则提示

### 客户保护机制
- 任意异常都需明确恢复路径
- 多端续接、暂存恢复、远程协助
- 二次预约 7 天内
- 人工兜底 955xx

### 监管红线
- 双录视频保存 ≥ 10 年
- 风评与购买必须在同次双录中
- 客户意愿必须明确确认
- 强制区块链存证

### 性能指标
- 单笔双录 5-15 分钟
- API 响应 P95 < 200ms
- 视频上传带宽自适应
- 支持 1000+ TPS

## 后续待补

- [ ] OrderList / OrderCreate 页面
- [ ] Login 页面
- [ ] QA Dashboard 驾驶舱
- [ ] 单元测试(每个组件)
- [ ] E2E 测试(Playwright)
- [ ] Storybook 组件文档
- [ ] Docker 镜像构建
- [ ] CI/CD 配置
- [ ] 移动端 PAD 适配
- [ ] 国际化(i18n)
- [ ] 离线模式支持(IndexedDB)

## 部署清单

1. **前端**:
   - `npm run build` 生成 dist/
   - nginx 静态托管
   - HTTPS 证书(必须)
   - 跨域配置:代理 `/api` 到后端

2. **后端**:
   - Spring Boot 应用 4 实例
   - MySQL 8.0 MGR 3 节点
   - Redis 6.2 Cluster 6 节点
   - Kafka 3 broker
   - OSS + 区块链节点

3. **监控**:
   - Prometheus + Grafana
   - ELK 日志
   - APM 链路追踪
   - 告警:钉钉/企业微信

## 联系方式

代码问题请联系:
- 前端:[前端负责人]
- 后端:[后端负责人]
- DBA:[DBA]
- 架构:[架构师]
