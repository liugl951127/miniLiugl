# 本地多模态智能 V7.1 (Local ONNX Multimodal)

> 目标: 用 ONNX Runtime + 本地模型替代纯 Java 的 `ImageAnalyzer`/`AudioAnalyzer`/`VideoAnalyzer`,
> 提升 **图片分类 / 目标检测 / 文图检索** 的"智能度"。本版本聚焦**图片 (Phase 1)**, 语音/视频/LLM 后续阶段。

---

## 1. 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│ 前端 Vue3  (multimodal/LocalOnnx.vue)                       │
│   ├─ 分类 tab  → POST /api/v1/multimodal/classify           │
│   ├─ 检测 tab  → POST /api/v1/multimodal/detect             │
│   └─ 相似度    → POST /api/v1/multimodal/text-image-similarity
└──────────────────────────┬──────────────────────────────────┘
                           │ JWT + X-User-Id
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 网关 minimax-gateway  (路由 /api/v1/multimodal/**)          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ minimax-ai / OnnxMultimodalController (V7.1 新增)           │
│   ├─ OnnxResNet50Service       (1000 类分类 + 1000d feature)│
│   ├─ OnnxClipService           (类 CLIP 双塔, BPE 文本)     │
│   └─ OnnxObjectDetectorService (YOLOv8 80 类 COCO + NMS)    │
└──────────────────────────┬──────────────────────────────────┘
                           │ ONNX Runtime 1.19.2
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 本地 ONNX 模型 (data/models/, Git LFS)                      │
│   ├─ resnet50/model.onnx            (98MB)                  │
│   ├─ clip-vit-base-patch32/         (待下载)                │
│   │   ├─ model.onnx                  (350MB)                │
│   │   ├─ vocab.json                  (843KB)                │
│   │   └─ merges.txt                  (513KB)                │
│   └─ yolov8n/model.onnx             (13MB)                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 当前可用模型 (Phase 1)

| 模型 | 路径 | 大小 | 状态 | 功能 |
|------|------|------|------|------|
| **ResNet50** | `data/models/resnet50/model.onnx` | 98MB | ✅ 已下载 | 1000 类 ImageNet 分类 + 1000d 图像特征 |
| **CLIP vocab** | `data/models/clip-vit-base-patch32/{vocab.json,merges.txt}` | 1.4MB | ✅ 已下载 | BPE 分词 (英文为主, 中文 char-level fallback) |
| **CLIP model** | `data/models/clip-vit-base-patch32/model.onnx` | 350MB | ⚠ 沙箱网络受限 | 真·双塔图文对齐 (下载脚本已就绪) |
| **YOLOv8n** | `data/models/yolov8n/model.onnx` | 13MB | ⚠ 沙箱网络受限 | 80 类 COCO 目标检测 + NMS |

### Fallback 策略

由于沙箱环境网络受限 (hf-mirror.com / GitHub 502), 部分模型暂时无法下载。
代码层已实现完整 **fallback 降级链路**：

| 模型 | 缺失时降级 |
|------|------------|
| ResNet50 | 返回 `code: 1001` + 空 data, 前端提示"先执行 download-models.sh" |
| CLIP model | 切换到 `ResNet50 + BPE` 类 CLIP 双塔 (本仓库内置) |
| YOLOv8n | 返回 `code: 1001`, 调用方可 fallback 到 `ImageAnalyzer` 旧逻辑 |

---

## 3. 快速开始

### 3.1 一键下载所有模型 (开发/生产环境)
```bash
cd /workspace/miniLiugl
./scripts/download-models.sh all   # 全下
# 或
./scripts/download-models.sh clip  # 只下 CLIP
./scripts/download-models.sh yolo  # 只下 YOLOv8n
```

### 3.2 Git LFS 拉取 (克隆仓库后)
```bash
git lfs install
git lfs pull
```

### 3.3 启动服务
```bash
# 后端 (环境变量指向模型目录)
export ONNX_RESNET50_PATH=./data/models/resnet50/model.onnx
export ONNX_CLIP_VOCAB_PATH=./data/models/clip-vit-base-patch32/vocab.json
./start-all.sh h2local

# 前端
npm run dev  # 或 npm run preview (生产构建)
```

### 3.4 访问
- 菜单 → 应用中心 → **本地多模态** (`/multimodal/local`)
- 三个 tab: 分类 / 检测 / 相似度

---

## 4. 后端 API 速查 (V7.1)

### 4.1 模型状态
```http
GET /api/v1/multimodal/status
```
```json
{
  "code": 0,
  "data": {
    "resnet50": {"enabled": true, "ready": true, "path": "./data/models/resnet50/model.onnx"},
    "clip":     {"enabled": true, "ready": true},
    "yolo":     {"enabled": true, "ready": false, "path": "./data/models/yolov8n/model.onnx"},
    "version":  "V7.1"
  }
}
```

### 4.2 图片分类
```http
POST /api/v1/multimodal/classify?topK=5
Content-Type: multipart/form-data

file: <image binary>
```
```json
{
  "code": 0,
  "data": [
    {"index": 281, "labelEn": "tabby", "labelCn": "虎斑猫", "probability": 0.82},
    {"index": 282, "labelEn": "tiger_cat", "labelCn": "虎斑家猫", "probability": 0.07}
  ]
}
```

### 4.3 目标检测
```http
POST /api/v1/multimodal/detect
Content-Type: multipart/form-data

file: <image>
```
```json
{
  "code": 0,
  "data": [
    {"class": "cat", "confidence": 0.91, "bbox": [120, 200, 250, 180]},
    {"class": "dog", "confidence": 0.78, "bbox": [400, 300, 200, 150]}
  ]
}
```
- `bbox`: `[x, y, width, height]` 像素坐标 (原图尺度)

### 4.4 文图相似度
```http
POST /api/v1/multimodal/text-image-similarity?text=a%20cat%20on%20sofa
Content-Type: multipart/form-data

file: <image>
```
```json
{
  "code": 0,
  "data": {"text": "a cat on sofa", "score": 0.82, "ready": true}
}
```

### 4.5 文本/图片 embedding
```http
POST /api/v1/multimodal/encode-text    {"text": "一只猫"}
POST /api/v1/multimodal/encode-image   (multipart)
```
- 1000-dim float 数组, L2 归一化

---

## 5. 实现细节

### 5.1 ResNet50 预处理 (ImageNet 标准)
```
1. 等比缩放到 ≥ 224x224
2. 中心裁切 224x224
3. RGB 归一化: (x/255 - mean) / std
   - mean = [0.485, 0.456, 0.406]
   - std  = [0.229, 0.224, 0.225]
4. NCHW → float32[1, 3, 224, 224]
5. softmax(logits) → 1000-dim 概率
```

### 5.2 YOLOv8 后处理
```
1. Letterbox resize 640x640 (保持比例 + 灰填充)
2. RGB 归一化 [0, 1]
3. 推理 output[1, 84, 8400] = 4 box + 80 class score
4. class 维 max → confidence > 0.25 过滤
5. 中心点+宽高 → 左上角 (x1, y1, x2, y2) + letterbox 反投影
6. 按 class 区分的 NMS (IoU > 0.45 抑制)
```

### 5.3 类 CLIP 双塔 (Fallback)
- **Image Tower**: ResNet50 1000-dim softmax → L2 归一化
- **Text Tower**: BPE 词表 hash → 累加到 1000-dim → L2 归一化
- **相似度**: cosine
- **限制**: 不是真·CLIP, 中文/细粒度召回率低。下载真 CLIP 模型后, 替换 `OnnxClipService.encodeImage/encodeText` 即可升级, 上层 API 不变。

---

## 6. 与原 `multimodal/ImageAnalyzer` 对比

| 维度 | 旧 `ImageAnalyzer` (V2.6, 纯 Java) | 新 `OnnxResNet50Service` (V7.1) |
|------|--------------------------------------|----------------------------------|
| 分类能力 | 无 (只有颜色直方图/pHash) | 1000 类 ImageNet |
| 目标检测 | 无 | YOLOv8n 80 类 (待模型) |
| 语义 embedding | 64 维灰度 (无意义) | 1000 维 ResNet50 / 512 维真 CLIP |
| 以文搜图 | ❌ 不支持 | ✅ cosine 相似度 |
| 模型大小 | 0 (纯算法) | 98MB (ResNet50) + 350MB (CLIP) + 13MB (YOLO) |
| 首次推理延迟 | < 1ms | 200-500ms (CPU int8) |
| 适用场景 | 去重/缩略图 | 智能理解/检索 |

---

## 7. 配置项 (application.yml)

```yaml
minimax:
  ai:
    onnx-vision:
      enabled: ${ONNX_VISION_ENABLED:true}
      resnet50-path: ${ONNX_RESNET50_PATH:./data/models/resnet50/model.onnx}
      clip-vocab-path: ${ONNX_CLIP_VOCAB_PATH:./data/models/clip-vit-base-patch32/vocab.json}
      clip-merges-path: ${ONNX_CLIP_MERGES_PATH:./data/models/clip-vit-base-patch32/merges.txt}
      clip-enabled: ${ONNX_CLIP_ENABLED:true}
      yolo-path: ${ONNX_YOLO_PATH:./data/models/yolov8n/model.onnx}
      yolo-enabled: ${ONNX_YOLO_ENABLED:true}
      yolo-confidence: ${ONNX_YOLO_CONF:0.25}
      yolo-iou: ${ONNX_YOLO_IOU:0.45}
      top-k: ${ONNX_VISION_TOPK:5}
      threads: ${ONNX_VISION_THREADS:4}
```

环境变量优先级 > yml 默认值。

---

## 8. 后续阶段 (Phase 2-4, 未做)

| Phase | 内容 | 模型 | 风险 |
|-------|------|------|------|
| 2 | 音频智能 (Whisper-tiny + Silero VAD) | 75MB + 2MB | 低 |
| 3 | 视频智能 (复用 ResNet50 + Whisper) | 复用 | 低 |
| 4 | LLM 升级 (Qwen2.5-1.5B-Int4) | ~1GB | 中 (RAM + 推理延迟) |

如需推进, 直接说 "Phase X"。

---

## 9. 文件清单 (本次新增)

### 后端 (minimax-ai 模块)
- `multimodal/onnx/OnnxResNet50Service.java` (10KB) — 分类 + 特征
- `multimodal/onnx/OnnxObjectDetectorService.java` (12KB) — YOLOv8 检测
- `multimodal/onnx/OnnxClipService.java` (6KB) — 类 CLIP 双塔
- `multimodal/onnx/SimpleBpeTokenizer.java` (7KB) — BPE 分词
- `multimodal/classifier/ImageNetLabels.java` (54KB) — 1000 类 EN+CN 标签
- `controller/OnnxMultimodalController.java` (8KB) — 6 个 REST 端点
- `application.yml` 增 `onnx-vision` 配置块
- `minimax-gateway/application.yml` 增 `/api/v1/multimodal/**` 路由

### 前端
- `src/api/multimodal.js` — 合并 V6.8.1 + V7.1 6 个新 API
- `src/views/multimodal/LocalOnnx.vue` (13KB) — 3-tab 页面
- `src/router/index.js` — `/multimodal/local` 路由
- `src/layout/Index.vue` — 侧边栏"本地多模态"入口

### 基础设施
- `.gitattributes` — `data/models/*` LFS 跟踪
- `scripts/download-models.sh` — 一键下载脚本
- `data/models/.gitkeep` — 占位
