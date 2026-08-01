# 双录一体化平台 - 前端代码

## 技术栈
- **Vue 3.4** + Composition API
- **TypeScript 5.3** 强类型
- **Vite 5** 构建
- **Ant Design Vue 4** UI 组件库
- **Pinia 2** 状态管理
- **Vue Router 4** 路由

## 核心功能模块

### 1. 业务编排(`views/DualRecord.vue`)
端到端的双录办理主页面,按 6 步流程编排:身份核验 → 风险评估 → 话术执行 → 视频录制 → 电子签约 → 智能质检。

### 2. 话术执行组件(`components/ScriptRunner.vue`)
- 强制节点顺序执行
- TTS 自动朗读标准话术
- 实时 ASR 监听客户回答
- 关键词命中检测
- 必读/必答/必确认强制控制
- 节点进度可视化

### 3. 视频录制组件(`components/VideoRecorder.vue`)
- WebRTC + MediaRecorder 实现
- 自动 3 秒分片(可配置)
- 分片上传 + 自动重试(指数退避)
- 可信时间戳嵌入(国家授时)
- 视频水印(订单号 + 客户名 + 时间)
- 暂停/恢复/断点续传

### 4. 风险评估组件(`components/RiskAssessment.vue`)
- 动态加载问卷模板
- 单选/多选题型支持
- 实时评分预览
- 风险等级自动匹配 C1-C5

### 5. 电子签约组件(`components/ESignature.vue`)
- CA 数字证书 + 短信验证 + 人脸核身三因子
- Canvas 手写签名
- 区块链存证

### 6. 智能质检监控(`components/QualityMonitor.vue`)
- 自动触发 + 轮询
- 5 维度评分展示
- 问题列表
- ASR 转写文本查看
- 人工复核信息

### 7. 异常处理组件(`components/ExceptionHandler.vue`)
- 7 类异常分类处理
- 多种恢复路径
- 升级机制(转人工/转线下/二次预约)

## 工具层

### `utils/request.ts`
统一 HTTP 请求封装,基于 axios,集成:
- Token 鉴权
- TraceId 链路追踪
- 业务错误码处理
- 401 自动跳转
- 上传进度回调

### `utils/webrtc.ts`
WebRTC 音视频封装:
- `DualRecordRecorder` 类
- 设备权限管理
- 视频快照
- MIME 类型自适应
- 分片上传

### `utils/asr.ts`
ASR 语音识别封装:
- Web Speech API 浏览器原生 ASR
- 关键词命中检测
- 阿里云一句话识别 SDK 接入

## 状态管理(`store/dualRecord.ts`)
Pinia store,管理:
- 客户/订单/会话/话术/风评/质检
- 当前节点 + 进度
- 录制状态(开始/暂停/停止)
- 异常信息

## API 服务层

| 模块 | 路径 | 主要接口 |
|------|------|---------|
| 订单 | `api/order.ts` | 创建/查询/状态推进/回退 |
| 话术 | `api/script.ts` | 拉取/完整性校验/节点提交 |
| 会话 | `api/session.ts` | 启动/上传/合并/暂停/恢复/质检 |
| 风评 | `api/risk.ts` | 问卷/提交/历史/合同签约 |

## 项目结构

```
src/
├── api/             # API 服务层
├── components/      # 业务组件
│   ├── ScriptRunner.vue
│   ├── VideoRecorder.vue
│   ├── RiskAssessment.vue
│   ├── ESignature.vue
│   ├── QualityMonitor.vue
│   ├── ExceptionHandler.vue
│   └── VerifyIdentity.vue
├── router/          # 路由配置
├── store/           # Pinia 状态
├── types/           # TypeScript 类型定义
├── utils/           # 工具类
│   ├── request.ts   # HTTP 封装
│   ├── webrtc.ts    # 音视频
│   └── asr.ts       # 语音识别
├── views/           # 页面
│   ├── DualRecord.vue
│   ├── OrderList.vue
│   ├── OrderCreate.vue
│   └── ...
├── App.vue
└── main.ts
```

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产包
npm run build

# 类型检查
npm run build
```

## 浏览器要求

- Chrome 90+ (推荐)
- Edge 90+
- Safari 14+ (需 Safari 14.1+ 支持 WebRTC)
- Firefox 88+

## 注意事项

1. **HTTPS 要求**: WebRTC + 摄像头权限,生产环境必须使用 HTTPS
2. **国家授时**: 可信时间戳需要后端提供 NTP 校准接口
3. **CA 证书**: 需要对接 CFCA/沃通等 CA 服务商
4. **OSS 上传**: 需要后端提供 STS 临时凭证
5. **ASR 服务**: 建议接入阿里云一句话识别,准确率 > 98%
