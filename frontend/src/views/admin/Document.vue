<!--
  @file views/admin/Document.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/admin/Document.vue (V3.5.48)
  @description 文档解析 - 上传 PDF/Word/TXT, 解析 + 关键词提取
  - 3 端点: parse / keywords / formats
-->
<template>
  <div class="page-document page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📄 文档解析 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <el-button @click="loadFormats" :icon="Refresh">支持的格式</el-button>
        </div>
      </template>

      <el-tabs v-model="tab">
        <el-tab-pane label="📤 上传 + 解析" name="parse">
          <el-upload
            :auto-upload="false"
            :on-change="onFileChange"
            :show-file-list="true"
            accept=".pdf,.doc,.docx,.txt,.md,.html"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处, 或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF / Word / TXT / MD / HTML</div>
            </template>
          </el-upload>
          <el-button v-if="file" type="primary" @click="onParse" :loading="parsing" style="margin-top: 16px">
            🔍 解析文档
          </el-button>

          <div v-if="parseResult" class="result-block">
            <h4>解析结果</h4>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="文件名">{{ parseResult.fileName }}</el-descriptions-item>
              <el-descriptions-item label="大小">{{ formatSize(parseResult.size) }}</el-descriptions-item>
              <el-descriptions-item label="页数">{{ parseResult.pageCount || 1 }}</el-descriptions-item>
              <el-descriptions-item label="字符数">{{ parseResult.charCount }}</el-descriptions-item>
              <el-descriptions-item label="段落数">{{ parseResult.paragraphCount }}</el-descriptions-item>
              <el-descriptions-item label="表格数">{{ parseResult.tableCount || 0 }}</el-descriptions-item>
            </el-descriptions>
            <h5 style="margin-top: 16px">📝 提取的文本</h5>
            <el-input v-model="parseResult.text" type="textarea" :rows="10" readonly />
          </div>
        </el-tab-pane>

        <el-tab-pane label="🔑 关键词提取" name="keywords">
          <el-form label-position="top">
            <el-form-item label="输入文本">
              <el-input v-model="kwForm.text" type="textarea" :rows="6" />
            </el-form-item>
            <el-form-item label="提取数量">
              <el-input-number v-model="kwForm.limit" :min="5" :max="50" />
            </el-form-item>
            <el-button type="primary" @click="onExtractKeywords" :loading="extracting">🔍 提取</el-button>
          </el-form>
          <div v-if="keywords.length" class="result-block">
            <h4>提取的关键词 ({{ keywords.length }} 个)</h4>
            <div class="keyword-cloud">
              <el-tag
                v-for="k in keywords"
                :key="k.word"
                :type="k.score > 0.7 ? 'danger' : k.score > 0.4 ? 'warning' : 'info'"
                size="large"
                effect="dark"
                style="margin: 4px"
              >
                {{ k.word }} ({{ (k.score * 100).toFixed(0) }}%)
              </el-tag>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="📋 支持的格式" name="formats">
          <el-table :data="formats" border>
            <el-table-column prop="extension" label="扩展名" width="120" />
            <el-table-column prop="name" label="格式名" />
            <el-table-column prop="mime" label="MIME" width="200" />
            <el-table-column prop="maxSize" label="最大大小" width="120">
              <template #default="{ row }">{{ (row.maxSize / 1024 / 1024).toFixed(0) }} MB</template>
            </el-table-column>
            <el-table-column prop="ocr" label="OCR" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.ocr" type="success" size="small">支持</el-tag>
                <el-tag v-else size="small">—</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { ref, reactive, onMounted, readonly } from 'vue'
import { useToast } from '@/composables/useToast'

import { Refresh, UploadFilled } from '@element-plus/icons-vue'
import { documentParse, documentKeywords, documentFormats } from '@/api/ai'

const tab = ref('parse')
const toast = useToast()
const file = ref(null)
const parsing = ref(false)
const extracting = ref(false)
const parseResult = ref(null)
const keywords = ref([])
const formats = ref([])

const kwForm = reactive({ text: '', limit: 20 })

function onFileChange(f) {
  file.value = f
}

async function onParse() {
  if (!file.value) return
  parsing.value = true
  try {
    // V3.5.48: 简化 - 实际用 multipart/form-data
    const r = await documentParse({
      fileName: file.value.name,
      size: file.value.size,
      // 实际需要读文件内容 base64
      content: await fileToBase64(file.value.raw)
    })
    parseResult.value = r.data
    toast.success('解析完成')
  } catch (e) {} finally { parsing.value = false }
}

function fileToBase64(f) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result.split(',')[1])
    reader.onerror = reject
    reader.readAsDataURL(f)
  })
}

async function onExtractKeywords() {
  if (!kwForm.text) { toast.warning('请输入文本'); return }
  extracting.value = true
  try {
    const r = await documentKeywords({ text: kwForm.text, limit: kwForm.limit })
    keywords.value = r.data || []
    toast.success(`提取 ${keywords.value.length} 个关键词`)
  } catch (e) {} finally { extracting.value = false }
}

async function loadFormats() {
  try {
    const r = await documentFormats()
    formats.value = r.data || []
  } catch (e) {}
}

function formatSize(b) {
  if (b < 1024) return b + ' B'
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + ' KB'
  return (b / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(loadFormats)
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.result-block { margin-top: 16px; padding: 16px; background: #f5f7fa; border-radius: 4px; }
.keyword-cloud { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
