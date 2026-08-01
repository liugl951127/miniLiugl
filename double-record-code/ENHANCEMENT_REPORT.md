# 双录一体化平台 - 增强交付报告

> 期间: 2026-08-01(第二轮)
> 范围: 链码事件 / WebRTC / AI 质检 / 银保监 / 安全
> 新增: 28 文件 / ~5000 行

## 一、新增模块概览

| # | 模块 | 路径 | 文件数 | 优先级 |
|---|------|------|--------|--------|
| 1 | 链码事件 Kafka 消费者 | `backend/fabric/event/` | 8 | P0 |
| 2 | WebRTC PAD 适配 | `frontend/utils/webrtc/` + `backend/rtc/` + `frontend/components/rtc/` | 5 | P0 |
| 3 | AI 质检 LLM 集成 | `backend/quality/llm/` + `backend/quality/prompt/` | 6 | P1 |
| 4 | 银保监报送 | `backend/compliance/csrc/` | 3 | P1 |
| 5 | 渗透测试报告 | `docs/security/` | 2(MD + DOCX) | P1 |

## 二、链码事件 Kafka 消费者

### 2.1 架构

```
[Fabric Peer] --SetEvent--> [Event Service] --> [Kafka Topic]
                                                  |
                                                  v
                                          [Kafka Consumer]
                                                  |
                                  +---------------+---------------+
                                  v               v               v
                          [Persist t_chain_event] [Handlers ...] [Retry/DLQ]
```

### 2.2 Topic 命名规范

```
fabric.{channel}.{eventName}

fabric.dual-record-channel.EvidenceSubmitted
fabric.dual-record-channel.StateChanged
fabric.dual-record-channel.NodeResultAppended
fabric.dual-record-channel.ContractSigned
fabric.dual-record-channel.AuditRecorded
fabric.dual-record-channel.EvidenceFinalized
```

### 2.3 文件清单

| 文件 | 职责 |
|------|------|
| `ChainEventListener.java` | 事件标准化 |
| `ChainEventConsumer.java` | Kafka 消费 + 分发 |
| `ChainEventAuditService.java` | 落库(幂等) |
| `ChainEventMapper.java` | MyBatis Mapper |
| `ChainEventHandler.java` | 处理器接口 |
| `EvidenceSubmittedHandler.java` | 证据提交 |
| `StateChangedHandler.java` | 状态变更 |
| `ContractSignedHandler.java` | 合同签署 |
| `AuditRecordedHandler.java` | 审计记录 |
| `EvidenceFinalizedHandler.java` | 证据终结(高优先级) |
| `KafkaConfig.java` | Kafka 配置 + DLT |
| `ChainEventListenerTest.java` | 单元测试 |

### 2.4 关键设计

- **幂等保证**:UNIQUE INDEX (chain_tx_id, event_name) 防重复
- **DLT(Dead Letter Topic)**:失败 3 次(1s 间隔)后入死信
- **手动 ACK**:业务处理完才提交 offset
- **多 Handler 责任链**:Single handler 失败不影响其他

## 三、WebRTC PAD 适配

### 3.1 架构

```
[PAD/PAD 浏览器] <--WebSocket 信令--> [后端 STOMP] --> [SFU Server(mediasoup/LiveKit)]
       ↓                                                            ↓
   [MediaRecorder]                                              [录制/LOL]
       ↓
   [分片上传] → 后端 → OSS + 链码
```

### 3.2 文件清单

| 文件 | 职责 |
|------|------|
| `frontend/utils/webrtc/sfu-client.ts` | SFU 客户端(信令 + WebRTC) |
| `frontend/utils/webrtc/media-recorder.ts` | 媒体录制器(分片 + 哈希 + 电平) |
| `frontend/components/rtc/PadRecorder.vue` | PAD 端 Vue 组件 |
| `backend/rtc/RtcConfig.java` | WebSocket STOMP 配置 |
| `backend/rtc/SfuSignalingController.java` | 信令服务(房间管理 + SDP 转发) |

### 3.3 关键特性

- 720P/1080P 视频 + 48kHz 音频
- 设备切换(前置/后置摄像头)
- 静音/关闭视频
- 音频电平可视化
- 分片录制(3s/片)+ 实时哈希
- 多角色:客户/经理/见证人

## 四、AI 质检 LLM 集成

### 4.1 架构

```
[会话完成事件] → Kafka → [AI Quality Service]
                                   ↓
                          [LLM Provider Factory]
                          (Qwen / DeepSeek 主备)
                                   ↓
                          [Prompt Engineering]
                          (系统提示 + 评分标准)
                                   ↓
                          [JSON 解析] → [质检结果]
```

### 4.2 文件清单

| 文件 | 职责 |
|------|------|
| `LlmProvider.java` | Provider 接口 |
| `QwenProvider.java` | 通义千问(主) |
| `DeepSeekProvider.java` | DeepSeek(备) |
| `LlmProviderFactory.java` | 主备切换 + 健康检查 |
| `AiQualityService.java` | 质检服务编排 |
| `QualityCheckPrompt.java` | 提示词模板 |

### 4.3 评分体系

| 维度 | 分值 | 说明 |
|------|------|------|
| 话术完整度 | 30 | 必读节点是否都执行 |
| 风险揭示 | 25 | 产品风险是否清晰 |
| 客户确认 | 20 | 关键问题是否明确 |
| 音视频合规 | 15 | 画面声音是否合规 |
| 流程合规 | 10 | 整体流程规范 |

**评级**:
- 90-100: HIGH_PASS
- 70-89: PASS
- 50-69: REVIEW
- 0-49: FAIL

### 4.4 Prompt 设计要点

- 系统提示词明确角色 + 评分标准 + JSON 输出格式
- 用户提示词注入 4 段:订单信息/话术模板/节点结果/ASR 转写
- 强制 JSON 输出(便于结构化解析)
- 解析失败自动 fallback 到人工复检

## 五、银保监报送

### 5.1 协议

**标准**:中国银保监会《保险销售行为可回溯管理办法》

**格式**:XML(国标 GB/T 25064) + JacksonXmlProperty 注解

**字段**:约 80 个,覆盖:
- 机构信息(代码/名称)
- 订单信息(订单号/产品/金额)
- 客户信息(姓名/证件/手机 - 全部脱敏)
- 销售信息(经理/网点/渠道)
- 双录详情(视频哈希/Merkle 根/话术版本)
- 质检结果(分数/评级)
- 区块链存证(交易号/哈希)

### 5.2 报送频率

| 频率 | 触发 | 内容 |
|------|------|------|
| 实时 | 订单完成 | 单笔完整记录 |
| 日终 | T+1 09:00 | 当日汇总 + 明细 |
| 月度 | 次月 5 日 | 月度统计 + 异常订单 |
| 即时 | 客户投诉 | 投诉工单详情 |

### 5.3 文件清单

| 文件 | 职责 |
|------|------|
| `CsrcReportBuilder.java` | 报文生成器(单笔 + 日报) |
| `CsrcReportController.java` | 报送 API 4 个端点 |
| `XmlMapperConfig.java` | XML 序列化配置 |

### 5.4 API 端点

- `POST /api/compliance/csrc/order` - 单笔实时报送
- `POST /api/compliance/csrc/daily` - 日报批量
- `POST /api/compliance/csrc/complaint` - 投诉举报
- `POST /api/compliance/csrc/monthly` - 月度汇总

## 六、渗透测试报告

### 6.1 文件清单

| 文件 | 类型 | 大小 |
|------|------|------|
| `penetration-test-report.md` | Markdown 源 | 27KB |
| `渗透测试报告.docx` | Word 正式版 | 47KB |
| `generate-report.py` | 生成脚本 | 22KB |

### 6.2 报告结构

1. **执行摘要** - 风险总览 + 关键发现
2. **测试范围** - 7 个系统 + 7 种方法
3. **漏洞汇总** - 17 个发现(1 高 + 4 中 + 12 低)
4. **OWASP Top 10** - A01-A10 逐项评估
5. **国密专项** - SM2/SM3/SM4 算法强度
6. **链码安全** - 共识 + 合约漏洞
7. **API 安全** - OWASP API Top 10
8. **风险评级** - P0-P3 修复优先级
9. **附录** - 测试矩阵 + 工具清单 + 参考标准

### 6.3 关键发现

| 等级 | 数量 | 关键问题 |
|------|------|----------|
| High | 1 | 分布式限流绕过(Guava → Redis 切换) |
| Medium | 4 | JWT 撤销/Actuator 暴露/CORS/链码事件重放 |
| Low | 12 | CSP/HSTS/Cookie/密码策略 等 |

### 6.4 上线前最低要求

- P0 修复完成(分布式限流)
- P1 修复完成(JWT/Actuator/链码 nonce)
- 回归测试通过

## 七、交付物总览(本轮)

| 类别 | 文件 | 行数 |
|------|------|------|
| Kafka 消费者 | 8 | ~1500 |
| WebRTC | 5 | ~1800 |
| AI 质检 | 6 | ~900 |
| 银保监 | 3 | ~700 |
| 渗透测试 | 3 | ~800(含 30+ 页 DOCX) |
| **合计** | **25** | **~5700** |

## 八、待办(下一轮)

- [ ] Kafka 消费者端到端测试(需要真 Kafka + Fabric 集成)
- [ ] SFU 服务选型与部署(mediasoup vs LiveKit)
- [ ] AI 质检模型 fine-tune(银行专属领域)
- [ ] 银保监实际接口联调(需要监管账号)
- [ ] 渗透测试复测
- [ ] 安全加固(修复 17 个发现)

---

**版本**: 1.2.0
**日期**: 2026-08-01
**负责人**: Mavis
**状态**: 持续增强
