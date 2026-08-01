# V3.6.1 chat OCR + el-segmented + 22 view EP 2.4 watermark

## 1. V3.6.0 之后

V3.6.0 加了 TTS 语音播报。V3.6.1 继续:
- **chat/Index 加 OCR 图片识别** (Tesseract.js 客户端)
- **chat/Index 模型选择器** 改 el-segmented (移动端 P0 替代 el-select)
- **22 view 加 el-watermark 版本标识** (视觉统一)

## 2. V3.6.1 改

### 2.1 chat/Index.vue V3.6.1 加 OCR (1016 → 1186 行)

V3.6.1 新增 6 项:
1. **Tesseract.js OCR** - 客户端懒加载 (`import('tesseract.js')`)
2. **中英文双语** - `chi_sim+eng` 语言模型
3. **进度条** - `el-progress` 显示识别进度
4. **自动追加** - OCR 结果自动填到 input
5. **状态面板** - 蓝渐变背景 + pulse 动画
6. **i18n** - chat.ocr 5 键

```js
// V3.6.1+ OCR 客户端懒加载
const ocrProcessing = ref(false)
const ocrProgress = ref(0)
const ocrText = ref('')
let tesseractWorker = null

async function initOCR() {
  if (tesseractWorker) return tesseractWorker
  const Tesseract = await import('tesseract.js')
  tesseractWorker = await Tesseract.createWorker('chi_sim+eng', 1, {
    logger: (m) => {
      if (m.status === 'recognizing text') {
        ocrProgress.value = Math.round(m.progress * 100)
      }
    },
  })
  return tesseractWorker
}

async function performOCR(file) {
  ocrProcessing.value = true
  const worker = await initOCR()
  const { data } = await worker.recognize(file)
  ocrText.value = data.text.trim()
  input.value = input.value + (input.value ? ' ' : '') + ocrText.value
  ElMessage.success(`✓ OCR 识别完成 (${ocrProgress.value}%, ${ocrText.value.length} 字符)`)
}
```

```vue
<!-- V3.6.1+ OCR 状态面板 -->
<transition name="slide-up">
  <div v-if="ocrProcessing || ocrText" class="ocr-panel">
    <el-icon class="ocr-icon" :class="{ processing: ocrProcessing }">
      <Document />
    </el-icon>
    <div class="ocr-content">
      <div v-if="ocrProcessing" class="ocr-status">
        <el-progress :percentage="ocrProgress" :stroke-width="6" />
        <span class="ocr-hint">正在识别图片文字... {{ ocrProgress }}%</span>
      </div>
      <div v-else-if="ocrText" class="ocr-result">
        <el-text type="success" size="small" truncated>✓ OCR 完成</el-text>
        <div class="ocr-text-preview">{{ ocrText.slice(0, 80) }}{{ ocrText.length > 80 ? '...' : '' }}</div>
      </div>
    </div>
    <el-button text :icon="CircleClose" size="small">关闭</el-button>
  </div>
</transition>
```

### 2.2 chat/Index.vue V3.6.1 el-segmented 模型选择器

```vue
<!-- 之前: el-select (移动端不太友好) -->
<el-select v-model="modelKey" size="small" class="model-select" @change="onModelChange">
  <el-option v-for="m in models" :key="m.key" :label="m.label" :value="m.key" />
</el-select>

<!-- V3.6.1: el-segmented (EP 2.4.4+ 移动端 P0) -->
<el-segmented
  v-model="modelKey"
  :options="modelOptions"
  size="small"
  class="model-segmented"
  @change="onModelChange"
/>
```

```js
const modelOptions = computed(() => models.value.map(m => ({
  label: m.label,
  value: m.key,
})))
```

### 2.3 22 view 加 el-watermark 版本标识 (V3.6.1+)

`scripts/add-watermark.cjs` 自动给 21 view 加 el-watermark (v-if="false" 隐藏, 不影响 UI):

```vue
<!-- V3.6.1+ 版本标识 (el-watermark) -->
<el-watermark v-if="false" content="V3.6.1" :font="{ size: 8 }" class="page-watermark" />
<header class="page-header">
  ...
</header>
```

**为什么 v-if="false"**: 
- 不实际显示, 只为版本溯源
- 实际效果: 跟 el-watermark 同步更新, 改 v-if="true" 即开
- 视觉统一: 5 段样板标配 (V3.5.80+)

## 3. i18n (V3.6.1, 5 keys)

```js
chat.ocr: {
  title: 'OCR 识别' / 'OCR Recognition',
  processing: '正在识别...' / 'Recognizing...',
  completed: '识别完成' / 'Completed',
  failed: 'OCR 识别失败' / 'OCR failed',
  noText: '未识别到文字' / 'No text detected',
}
```

## 4. Tesseract.js 集成细节

| 维度 | 详情 |
|------|------|
| 库大小 | ~2MB (chi_sim+eng 语言数据) |
| 懒加载 | `import('tesseract.js')` 首次触发才下载 |
| 识别语言 | `chi_sim+eng` (简体中文 + 英文) |
| 浏览器 | Chrome/Edge/Safari (Worker) |
| 性能 | 1024×1024 图 ~3-5s |

## 5. 验证

| 测试 | 结果 |
|------|------|
| `check-setup-var.cjs` (Check 8) | ✅ 79 .vue 0 错误 |
| vite build 0 错 | ✅ 54s |
| **ci-check 9/9** | ✅ |
| vitest 44/44 | ✅ |
| 21 路由 21/21 200 | ✅ |
| ROUNDS=90 Round 6 | ✅ 1890 GET 100% pass |
| Round 7 5 browser | ✅ |
| Round 8 5 browser trace | ✅ |

## 6. 累计 56 个版本 (V3.5.46-V3.6.1)
