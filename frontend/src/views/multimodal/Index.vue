<!-- @file multimodal/Index.vue - 多模态能力中心 V7.1 (每个模块独立选择模型) -->
<template>
  <div class="page-card">
    <div class="page-header">
      <h2>🎨 多模态能力中心</h2>
      <div style="display:flex;align-items:center;gap:8px">
        <span class="service-badge" :class="servicesOk ? 'ok' : 'loading'">
          {{ serviceCount }}/{{ totalServices }} 服务就绪
        </span>
        <el-button size="small" @click="loadServices">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button v-if="activeModule" size="small" @click="activeModule = null">
          返回概览
        </el-button>
      </div>
    </div>

    <!-- 服务状态栏 -->
    <div class="service-row">
      <div v-for="svc in serviceList" :key="svc.key"
           class="svc-chip" :class="svc.ok ? 'chip-ok' : 'chip-fail'"
           :title="svc.label + ': ' + (svc.ok ? '正常' : '离线')">
        <span>{{ svc.icon }}</span>
        <span>{{ svc.label }}</span>
      </div>
    </div>

    <!-- ========== 模块网格 (概览) ========== -->
    <div v-if="!activeModule" class="mod-grid">
      <el-card v-for="m in modules" :key="m.key" shadow="hover" class="mod-card"
        :class="{ active: false }"
        @click="selectModule(m)">
        <div style="text-align:center">
          <div style="font-size:36px;margin-bottom:8px">{{ m.icon }}</div>
          <div style="font-weight:600;font-size:14px;margin-bottom:4px">{{ m.name }}</div>
          <div style="font-size:11px;color:#909399;margin-bottom:8px">{{ m.desc }}</div>
          <!-- 当前使用模型 -->
          <div class="cur-model-tag">
            <span v-if="selectedModuleModel[m.key]">
              🏷️ {{ selectedModuleModel[m.key].displayName || selectedModuleModel[m.key].modelCode }}
            </span>
            <span v-else style="color:#c0c4cc;font-size:10px">未选择模型</span>
          </div>
          <el-tag v-if="m.beta" size="small" type="warning" style="margin-top:6px">Beta</el-tag>
        </div>
      </el-card>
    </div>

    <!-- ========== 图片生成 ========== -->
    <el-card v-if="activeModule?.key === 'image-gen'" body-style="padding:20px" v-loading="imgLoading">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>🎨 图片生成</span>
          <el-tag size="small" type="success">真实 API</el-tag>
          <!-- 模型选择器 -->
          <el-select v-model="selectedModuleModel['image-gen']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('image-gen', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.imageGen" :key="m.modelCode"
                :label="m.displayName || m.modelCode"
                :value="m">
                <span>{{ m.displayName || m.modelCode }}</span>
                <span v-if="m.accuracy" style="float:right;color:#67c23a;font-size:11px">
                  {{ (m.accuracy * 100).toFixed(0) }}%
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.imageGen" :key="m.modelCode"
                :label="m.displayName || m.modelCode"
                :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-form ref="imgFormRef" :model="imgForm" :rules="imgFormRules" label-width="80px" @submit.prevent>
        <el-form-item label="描述" prop="prompt">
          <el-input v-model="imgForm.prompt" type="textarea" :rows="3"
            placeholder="描述你想要的图片，越详细越好" />
        </el-form-item>
        <el-form-item label="比例">
          <el-radio-group v-model="imgForm.ratio">
            <el-radio value="1:1">1:1</el-radio>
            <el-radio value="16:9">16:9</el-radio>
            <el-radio value="9:16">9:16</el-radio>
            <el-radio value="4:3">4:3</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分辨率">
          <el-select v-model="imgForm.resolution" style="width:200px">
            <el-option label="1K（快速）" value="1K" />
            <el-option label="2K（标准）" value="2K" />
            <el-option label="4K（高清）" value="4K" />
          </el-select>
        </el-form-item>
        <el-form-item label="参考图">
          <el-upload :before-upload="uploadRefImage" :show-file-list="false" accept="image/*">
            <el-button size="small">上传参考图</el-button>
          </el-upload>
          <img v-if="imgForm.refImage" :src="imgForm.refImage" loading="lazy" style="width:80px;height:80px;border-radius:8px;margin-left:8px;object-fit:cover" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="imgLoading" @click="generateImage">
            <el-icon><MagicStick /></el-icon>生成图片
          </el-button>
          <el-button v-if="imgResult.length" size="small" link type="danger" @click="clearImageResult" style="margin-left:8px">
            清空结果
          </el-button>
        </el-form-item>
      </el-form>

      <div style="margin-top:20px">
        <div style="font-weight:600;font-size:13px;margin-bottom:12px">生成结果 ({{ imgResult.length }})</div>
        <el-empty v-if="!imgLoading && imgResult.length === 0" description="暂无生成结果，请填写描述后点击生成" :image-size="80" />
        <div v-else class="img-grid">
          <div v-for="(img, i) in imgResult" :key="i" class="img-item">
            <el-image :src="img.url" fit="cover" style="width:100%;height:200px;border-radius:8px"
              :preview-src-list="imgResult.map(x=>x.url)" />
            <div class="img-actions">
              <el-button size="small" link @click="copyUrl(img.url)">复制链接</el-button>
              <el-button size="small" link type="primary" @click="downloadImg(img.url)">下载</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ========== 图片理解 ========== -->
    <el-card v-if="activeModule?.key === 'image-understand'" body-style="padding:20px" v-loading="analyzeLoading && !analyzeImgUrl">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>🔍 图片理解</span>
          <el-tag size="small" type="success">真实 API</el-tag>
          <el-select v-model="selectedModuleModel['image-understand']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('image-understand', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.vision" :key="m.modelCode"
                :label="m.displayName || m.modelCode"
                :value="m">
                <span>{{ m.displayName || m.modelCode }}</span>
                <span v-if="m.accuracy" style="float:right;color:#67c23a;font-size:11px">
                  {{ (m.accuracy * 100).toFixed(0) }}%
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.vision" :key="m.modelCode"
                :label="m.displayName || m.modelCode"
                :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-upload :before-upload="uploadAnalyzeImg" :show-file-list="false" drag accept="image/*" style="width:100%">
            <div style="padding:40px;text-align:center;color:#909399">
              <el-icon :size="40"><UploadFilled /></el-icon>
              <div style="margin-top:8px">拖拽图片或点击上传</div>
            </div>
          </el-upload>
          <img v-if="analyzeImgUrl" :src="analyzeImgUrl" loading="lazy" style="width:100%;margin-top:12px;border-radius:8px" />
        </el-col>
        <el-col :span="12">
          <el-input v-model="analyzePrompt" type="textarea" :rows="2"
            placeholder="可选：输入具体问题，如：这张图里有什么？" style="margin-bottom:8px" />
          <el-button type="primary" :loading="analyzeLoading" @click="analyzeImage" style="margin-bottom:12px">
            <el-icon><Search /></el-icon>分析图片
          </el-button>
          <el-card v-if="analyzeResult" body-style="padding:12px" shadow="hover">
            <pre style="white-space:pre-wrap;font-size:13px;margin:0">{{ analyzeResult }}</pre>
          </el-card>
          <el-empty v-else-if="!analyzeLoading" description="尚未分析，上传图片后点击「分析图片」" :image-size="60" />
        </el-col>
      </el-row>
    </el-card>

    <!-- ========== 语音合成 ========== -->
    <el-card v-if="activeModule?.key === 'tts'" body-style="padding:20px" v-loading="ttsLoading && !ttsUrl">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>🔊 语音合成 (TTS)</span>
          <el-tag size="small" type="success">真实 API</el-tag>
          <el-select v-model="selectedModuleModel['tts']"
            value-key="voiceId"
            placeholder="选择音色" style="width:220px;margin-left:auto"
            @change="onModelChange('tts', $event)">
            <el-option-group label="🏷️ 自研音色">
              <el-option v-for="m in selfModels.tts" :key="m.voiceId"
                :label="m.name" :value="m">
                <span>{{ m.name }}</span>
                <span style="float:right;font-size:10px;color:#909399">{{ m.language }}</span>
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端音色">
              <el-option v-for="m in cloudModels.tts" :key="m.voiceId"
                :label="m.name" :value="m">
                <span>{{ m.name }}</span>
                <span style="float:right;font-size:10px;color:#909399">{{ m.language }}</span>
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-form ref="ttsFormRef" :model="ttsForm" :rules="ttsFormRules" label-width="80px" @submit.prevent>
        <el-form-item label="文本" prop="text">
          <el-input v-model="ttsForm.text" type="textarea" :rows="4"
            placeholder="输入要转为语音的文本…" show-word-limit maxlength="2000" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="语速">
              <el-slider v-model="ttsForm.speed" :min="0.5" :max="2" :step="0.1" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="音调">
              <el-slider v-model="ttsForm.pitch" :min="-12" :max="12" :step="1" show-input />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="ttsLoading" @click="doTts">
            <el-icon><VideoPlay /></el-icon>合成语音
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="ttsUrl" style="margin-top:16px">
        <el-card body-style="padding:12px" shadow="hover">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">🎵 合成结果</div>
          <audio :src="ttsUrl" controls style="width:100%" />
          <div style="margin-top:8px;display:flex;gap:8px">
            <el-button size="small" @click="copyUrl(ttsUrl)">复制链接</el-button>
            <el-button size="small" type="primary" @click="downloadImg(ttsUrl)">下载 MP3</el-button>
          </div>
        </el-card>
      </div>
      <el-empty v-else-if="!ttsLoading" description="尚未合成，请输入文本后点击「合成语音」" :image-size="60" />
    </el-card>

    <!-- ========== 语音识别 ========== -->
    <el-card v-if="activeModule?.key === 'asr'" body-style="padding:20px" v-loading="asrLoading && !asrResult">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>🎤 语音识别 (ASR)</span>
          <el-tag size="small" type="success">真实 API</el-tag>
          <el-select v-model="selectedModuleModel['asr']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('asr', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.asr" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.asr" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-upload :before-upload="uploadAsrAudio" :show-file-list="false" accept="audio/*,video/*">
        <el-button type="primary" size="large">
          <el-icon><Microphone /></el-icon>上传音频文件
        </el-button>
      </el-upload>
      <div v-if="asrLoading" style="margin-top:12px;color:#909399">
        <el-icon class="is-loading"><Loading /></el-icon> 识别中…
      </div>
      <div v-else-if="!asrResult" style="margin-top:16px">
        <el-empty description="尚未识别，请上传音频文件" :image-size="60" />
      </div>
      <div v-if="asrResult" style="margin-top:16px">
        <el-card body-style="padding:12px" shadow="hover">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">📝 识别结果</div>
          <pre style="white-space:pre-wrap;font-size:13px;margin:0">{{ asrResult }}</pre>
        </el-card>
        <div style="margin-top:8px;display:flex;gap:8px">
          <el-button size="small" type="primary" @click="copyUrl(asrResult)">复制文本</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========== 人脸识别 (摄像头拍照 + 人脸分析, V7.3) ========== -->
    <el-card v-if="activeModule?.key === 'face-recognition'" body-style="padding:20px" v-loading="faceLoading">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px">
          <span>👤 人脸识别</span>
          <el-tag size="small" type="success">实时拍摄</el-tag>
          <el-select v-model="selectedModuleModel['face-recognition']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('face-recognition', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.vision" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.vision" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>

      <!-- 摄像头区域 -->
      <div class="camera-area">
        <video ref="faceVideoEl" autoplay muted playsinline
          :class="{ 'camera-active': faceCameraOn }"
          style="width:100%;max-width:480px;border-radius:8px;background:#000;display:block" />
        <canvas ref="faceCanvasEl" style="display:none" />
      </div>

      <div style="margin-top:12px;display:flex;gap:8px;flex-wrap:wrap">
        <el-button v-if="!faceCameraOn" type="primary" @click="startCamera">
          📷 开启摄像头
        </el-button>
        <el-button v-else type="danger" @click="stopCamera">
          ⏹ 关闭摄像头
        </el-button>
        <el-button v-if="faceCameraOn" type="success" :loading="faceLoading" @click="captureAndAnalyze">
          📸 拍照分析
        </el-button>
        <el-button type="info" @click="facePrompt = '请描述这张图片中的人物外貌特征'">
          👤 人物描述
        </el-button>
        <el-button type="info" @click="facePrompt = '请分析这张图片中的人物表情和情绪'">
          😊 情绪分析
        </el-button>
        <el-button type="info" @click="facePrompt = '请检查这张图片中是否有安全隐患或异常情况'">
          ⚠️ 安全检查
        </el-button>
      </div>

      <!-- 提示词自定义 -->
      <div style="margin-top:12px">
        <el-input v-model="facePrompt" placeholder="自定义分析提示词…" size="small">
          <template #prepend>提示词</template>
        </el-input>
      </div>

      <!-- 分析结果 -->
      <div v-if="faceLoading" style="margin-top:16px;color:#909399">
        <el-icon class="is-loading"><Loading /></el-icon> 人脸分析中…
      </div>
      <el-empty v-else-if="!faceResult" description="尚未分析，请开启摄像头后点击「拍照分析」" :image-size="80" style="margin-top:16px" />
      <div v-if="faceResult" style="margin-top:16px">
        <el-card body-style="padding:16px" shadow="hover">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
            <el-tag type="success">✅ 分析完成</el-tag>
            <span style="font-size:12px;color:#909399">
              耗时 {{ faceResult.durationMs || 0 }}ms · 模型: {{ faceResult.model || '-' }}
            </span>
          </div>
          <pre style="white-space:pre-wrap;font-size:13px;line-height:1.6">{{ faceResult.content || faceResult.description || JSON.stringify(faceResult, null, 2) }}</pre>
        </el-card>
        <div v-if="faceResult.base64" style="margin-top:12px">
          <img :src="'data:image/jpeg;base64,' + faceResult.base64" loading="lazy" style="max-width:300px;border-radius:8px" />
        </div>
        <div style="margin-top:8px;display:flex;gap:8px">
          <el-button size="small" @click="copyUrl(JSON.stringify(faceResult, null, 2))">复制结果</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========== 视频理解 (上传视频 + LLM 内容分析, V7.3) ========== -->
    <el-card v-if="activeModule?.key === 'video-understand'" body-style="padding:20px" v-loading="videoUnderstLoading">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px">
          <span>🎥 视频内容理解</span>
          <el-tag size="small" type="success">AI 分析</el-tag>
          <el-select v-model="selectedModuleModel['video-understand']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('video-understand', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.vision" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.vision" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>

      <!-- 提示词 -->
      <div style="margin-bottom:12px">
        <el-input v-model="videoPrompt" type="textarea" :rows="2"
          placeholder="分析提示词，如: 请详细描述这段视频的内容，包括场景、人物、动作、对话等"
          size="large" />
      </div>

      <!-- 上传区 -->
      <el-upload
        ref="videoUploadEl"
        :before-upload="uploadVideoFile"
        :show-file-list="true"
        accept="video/*"
        drag
        style="margin-bottom:12px"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽视频文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 mp4/mov/m4v/3gp/mkv，最大 100MB。系统会提取 3 个关键帧进行分析。</div>
        </template>
      </el-upload>

      <!-- 进度 -->
      <div v-if="videoUnderstLoading" style="margin-top:12px">
        <el-progress :percentage="videoProgress" :stroke-width="10"
          :color="videoProgressColorFn" />
        <p style="color:#909399;font-size:13px;margin-top:4px">
          <el-icon class="is-loading"><Loading /></el-icon>
          视频分析中… (提取关键帧 + LLM 视觉理解)
        </p>
      </div>

      <!-- 元数据 -->
      <div v-if="videoMeta" style="margin-top:16px">
        <el-card body-style="padding:12px" shadow="never">
          <div style="font-size:12px;color:#606266">
            <el-tag size="small" type="info" style="margin-right:8px">
              ⏱ {{ videoMeta.durationMs ? (videoMeta.durationMs / 1000).toFixed(1) + 's' : '-' }}
            </el-tag>
            <el-tag size="small" type="info" style="margin-right:8px">
              📐 {{ videoMeta.width }}×{{ videoMeta.height }}
            </el-tag>
            <el-tag size="small" type="info" style="margin-right:8px">
              📦 {{ videoMeta.format }}
            </el-tag>
            <el-tag v-if="videoMeta.audioTracks > 0" size="small" type="info">
              🔊 含音频
            </el-tag>
          </div>
        </el-card>
      </div>

      <!-- 分析结果 -->
      <el-empty v-if="!videoUnderstLoading && !videoResult" description="尚未分析，请上传视频文件并填写提示词" :image-size="80" style="margin-top:16px" />
      <div v-if="videoResult" style="margin-top:16px">
        <el-card body-style="padding:16px" shadow="hover">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px">
            <el-tag type="success">✅ 分析完成</el-tag>
            <span style="font-size:12px;color:#909399">
              {{ videoResult.frameCount || 0 }} 帧 · 耗时 {{ videoResult.analysisTimeMs || 0 }}ms
            </span>
          </div>
          <div v-for="(frame, idx) in (videoResult.frames || [])" :key="idx"
            style="margin-bottom:16px;padding:12px;background:#f5f7fa;border-radius:8px">
            <div style="font-size:12px;color:#909399;margin-bottom:6px">
              📸 第 {{ frame.frameIndex || idx + 1 }} 帧 (at {{ frame.frameAt || '-' }})
            </div>
            <pre style="white-space:pre-wrap;font-size:13px;line-height:1.6">{{ frame.content || frame.description || JSON.stringify(frame, null, 2) }}</pre>
          </div>
        </el-card>
        <div style="margin-top:8px;display:flex;gap:8px">
          <el-button size="small" @click="copyUrl(JSON.stringify(videoResult, null, 2))">复制结果</el-button>
        </div>
      </div>
    </el-card>

    <!-- ========== 视频生成 ========== -->
    <el-card v-if="activeModule?.key === 'video-gen'" body-style="padding:20px" v-loading="vidLoading && !vidResult">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>🎬 视频生成</span>
          <el-tag size="small" type="warning">Beta</el-tag>
          <el-select v-model="selectedModuleModel['video-gen']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('video-gen', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.video" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                <span>{{ m.displayName || m.modelCode }}</span>
                <span v-if="m.accuracy" style="float:right;color:#67c23a;font-size:11px">
                  {{ (m.accuracy * 100).toFixed(0) }}%
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.video" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-form ref="vidFormRef" :model="vidForm" :rules="vidFormRules" label-width="80px" @submit.prevent>
        <el-form-item label="描述" prop="prompt">
          <el-input v-model="vidForm.prompt" type="textarea" :rows="3" placeholder="描述你想生成的视频内容…" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="时长">
              <el-select v-model="vidForm.duration" style="width:100%">
                <el-option label="6 秒" value="6" />
                <el-option label="10 秒" value="10" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分辨率">
              <el-select v-model="vidForm.resolution" style="width:100%">
                <el-option label="768P" value="768P" />
                <el-option label="1080P" value="1080P" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="vidLoading" @click="generateVideo">
            <el-icon><VideoPlay /></el-icon>生成视频
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="vidResult" style="margin-top:16px">
        <el-card body-style="padding:12px" shadow="hover">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">🎬 生成结果</div>
          <video :src="vidResult" controls style="width:100%;border-radius:8px" />
          <div style="margin-top:8px;display:flex;gap:8px">
            <el-button size="small" @click="copyUrl(vidResult)">复制链接</el-button>
            <el-button size="small" type="primary" @click="downloadImg(vidResult)">下载视频</el-button>
          </div>
        </el-card>
      </div>
      <el-empty v-else-if="!vidLoading" description="尚未生成，请填写描述后点击「生成视频」" :image-size="60" />
    </el-card>

    <!-- ========== 文档理解 ========== -->
    <el-card v-if="activeModule?.key === 'doc'" body-style="padding:20px" v-loading="docLoading && !docResult">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>📄 文档理解</span>
          <el-tag size="small" type="warning">Beta</el-tag>
          <el-select v-model="selectedModuleModel['doc']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('doc', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.doc" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                <span>{{ m.displayName || m.modelCode }}</span>
                <span v-if="m.accuracy" style="float:right;color:#67c23a;font-size:11px">
                  {{ (m.accuracy * 100).toFixed(0) }}%
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.doc" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-upload :before-upload="uploadDoc" :show-file-list="false"
        accept=".pdf,.jpg,.jpeg,.png,.docx,.txt" style="margin-bottom:16px">
        <el-button type="primary"><el-icon><Upload /></el-icon>上传文档 / 图片</el-button>
      </el-upload>
      <div v-if="docFile" style="margin-bottom:12px;display:flex;align-items:center;gap:8px">
        <el-icon><Document /></el-icon>
        <span style="font-size:13px">{{ docFile.name }}</span>
        <el-tag size="small">{{ docFile.type }}</el-tag>
      </div>
      <el-input v-model="docQuestion" type="textarea" :rows="2"
        placeholder="针对文档内容提问，如：总结这份文档的主要内容？" />
      <div style="margin-top:12px">
        <el-button type="primary" :loading="docLoading" @click="askDoc">
          <el-icon><Search /></el-icon>提问
        </el-button>
      </div>
      <div v-if="docResult" style="margin-top:16px">
        <el-card body-style="padding:12px" shadow="hover">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">💡 回答</div>
          <pre style="white-space:pre-wrap;font-size:13px;margin:0">{{ docResult }}</pre>
        </el-card>
        <div style="margin-top:8px">
          <el-button size="small" @click="copyUrl(docResult)">复制文本</el-button>
        </div>
      </div>
      <el-empty v-else-if="!docLoading" description="尚未提问，请先上传文档并输入问题" :image-size="60" />
    </el-card>

    <!-- ========== 音乐生成 ========== -->
    <el-card v-if="activeModule?.key === 'music'" body-style="padding:20px" v-loading="musicLoading && !musicResult">
      <template #header>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
          <span>🎵 音乐生成</span>
          <el-tag size="small" type="warning">Beta</el-tag>
          <el-select v-model="selectedModuleModel['music']"
            value-key="modelCode"
            placeholder="选择模型" style="width:220px;margin-left:auto"
            @change="onModelChange('music', $event)">
            <el-option-group label="🏷️ 自研模型">
              <el-option v-for="m in selfModels.music" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                <span>{{ m.displayName || m.modelCode }}</span>
                <span v-if="m.accuracy" style="float:right;color:#67c23a;font-size:11px">
                  {{ (m.accuracy * 100).toFixed(0) }}%
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="☁️ 云端模型">
              <el-option v-for="m in cloudModels.music" :key="m.modelCode"
                :label="m.displayName || m.modelCode" :value="m">
                {{ m.displayName || m.modelCode }}
              </el-option>
            </el-option-group>
          </el-select>
        </div>
      </template>
      <el-form ref="musicFormRef" :model="musicForm" :rules="musicFormRules" label-width="80px" @submit.prevent>
        <el-form-item label="描述" prop="prompt">
          <el-input v-model="musicForm.prompt" type="textarea" :rows="3"
            placeholder="描述你想要的音乐，如：轻快的钢琴曲，适合咖啡厅背景音乐" />
        </el-form-item>
        <el-form-item label="歌词">
          <el-input v-model="musicForm.lyrics" type="textarea" :rows="3" placeholder="可选：填写歌词" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="musicLoading" @click="generateMusic">
            <el-icon><VideoPlay /></el-icon>生成音乐
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="musicResult" style="margin-top:16px">
        <el-card body-style="padding:12px" shadow="hover">
          <div style="font-weight:600;font-size:13px;margin-bottom:8px">🎵 生成结果</div>
          <audio :src="musicResult" controls style="width:100%" />
          <div style="margin-top:8px;display:flex;gap:8px">
            <el-button size="small" @click="copyUrl(musicResult)">复制链接</el-button>
            <el-button size="small" type="primary" @click="downloadImg(musicResult)">下载音频</el-button>
          </div>
        </el-card>
      </div>
      <el-empty v-else-if="!musicLoading" description="尚未生成，请填写描述后点击「生成音乐」" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  imageGenGenerate, imageGenModels,
  visionAnalyze,
  audioTts, audioTtsVoices,
  audioAsr,
  videoGenerate, videoModels,
  musicGenerate, musicModels,
  docUpload, docAsk,
  uploadImage,
} from '@/api/multimodal'
import { faceAnalyze, videoUnderstand } from '@/api/ai'
import { listEnabledModels } from '@/api/model'
import {
  Close, MagicStick, UploadFilled, Search, VideoPlay,
  Microphone, Upload, Document, Refresh, Loading,
  VideoCamera,
} from '@element-plus/icons-vue'

// ============ 模块定义 ============
const modules = [
  { key: 'image-gen', name: '图片生成', icon: '🎨', desc: '文生图 / 图生图', beta: false },
  { key: 'image-understand', name: '图片理解', icon: '🔍', desc: '上传图片 AI 分析问答', beta: false },
  { key: 'tts', name: '语音合成', icon: '🔊', desc: '文本转语音 (TTS)', beta: false },
  { key: 'asr', name: '语音识别', icon: '🎤', desc: '音频转文本 (ASR)', beta: false },
  { key: 'face-recognition', name: '人脸识别', icon: '👤', desc: '摄像头拍照 · 人脸检测', beta: false },
  { key: 'video-understand', name: '视频理解', icon: '🎥', desc: '上传视频 · AI 内容分析', beta: false },
  { key: 'video-gen', name: '视频生成', icon: '🎬', desc: '文本生成视频', beta: true },
  { key: 'music', name: '音乐生成', icon: '🎵', desc: '文本生成音乐', beta: true },
  { key: 'doc', name: '文档理解', icon: '📄', desc: 'PDF/图片问答', beta: true },
]

// ============ 模型加载 ============
// 每个模块当前选中的模型 (key = 模块 key)
const selectedModuleModel = reactive({})

// 分类后的模型列表
const selfModels = reactive({
  imageGen: [], vision: [], tts: [], asr: [],
  video: [], music: [], doc: [],
  'face-recognition': [], 'video-understand': [],
})
const cloudModels = reactive({
  imageGen: [], vision: [], tts: [], asr: [],
  video: [], music: [], doc: [],
  'face-recognition': [], 'video-understand': [],
})

/** 加载所有模块的模型选项 */
async function loadModuleModels() {
  try {
    // 1. 从 model_config 加载自研模型
    const allModels = await listEnabledModels()
    const list = Array.isArray(allModels) ? allModels : (allModels.data || [])

    // 重置
    Object.keys(selfModels).forEach(k => selfModels[k] = [])
    Object.keys(cloudModels).forEach(k => cloudModels[k] = [])

    for (const m of list) {
      const code = m.modelCode || ''
      const providerCode = m.providerCode || ''
      const isSelf = providerCode.startsWith('local-') ||
                     providerCode.startsWith('self') ||
                     providerCode === 'trained' ||
                     providerCode.includes('onnx')

      // 按 modality 分类
      const modality = inferModality(code, m)

      if (isSelf) {
        if (modality.imageGen) selfModels.imageGen.push(m)
        if (modality.vision)   selfModels.vision.push(m)
        if (modality.tts)      selfModels.tts.push(m)
        if (modality.asr)      selfModels.asr.push(m)
        if (modality.video)    selfModels.video.push(m)
        if (modality.music)    selfModels.music.push(m)
        if (modality.doc)      selfModels.doc.push(m)
        // V7.3: 人脸识别 + 视频理解复用 vision 模型
        if (modality.vision) {
          selfModels['face-recognition'].push(m)
          selfModels['video-understand'].push(m)
        }
      } else {
        if (modality.imageGen) cloudModels.imageGen.push(m)
        if (modality.vision)   cloudModels.vision.push(m)
        if (modality.tts)      cloudModels.tts.push(m)
        if (modality.asr)      cloudModels.asr.push(m)
        if (modality.video)    cloudModels.video.push(m)
        if (modality.music)    cloudModels.music.push(m)
        if (modality.doc)      cloudModels.doc.push(m)
        // V7.3: 人脸识别 + 视频理解复用 vision 模型
        if (modality.vision) {
          cloudModels['face-recognition'].push(m)
          cloudModels['video-understand'].push(m)
        }
      }
    }

    // 2. 补充云端默认值 (model_config 没有时)
    fillCloudDefaults()

    // 3. 自动选中第一个可用模型
    autoSelectFirstModel()
  } catch (e) {
    ElMessage.warning('加载模型列表失败，已使用默认云端模型')
    fillCloudDefaults()
    autoSelectFirstModel()
  }
}

/** 根据 modelCode 和字段推断支持的模态 */
function inferModality(code, m) {
  const c = code.toLowerCase()
  const hasVision  = m.supportsVision === 1 || c.includes('vision') || c.includes('vl')
  const hasEmbedding = c.includes('embedding') || c.includes('vector')
  const hasAudio  = c.includes('tts') || c.includes('speech') || c.includes('audio')
  const hasAsr    = c.includes('asr') || c.includes('whisper') || c.includes('stt')
  const hasVideo  = c.includes('video') || c.includes('hunyuan') || c.includes('sora')
  const hasMusic  = c.includes('music') || c.includes('audio')
  const hasDoc    = !hasEmbedding && !hasVision && !hasAudio && !hasVideo && !hasMusic

  // 通用大模型支持图片理解 + 文档
  const isGeneralLLM = !hasVision && !hasEmbedding && !hasAudio && !hasVideo && !hasMusic

  return {
    imageGen: c.includes('image') || c.includes('diffusion') || c.includes('stable'),
    vision:   hasVision || isGeneralLLM,
    tts:      hasAudio,
    asr:      hasAsr || isGeneralLLM,
    video:    hasVideo,
    music:    hasMusic,
    doc:      hasDoc && !c.includes('embedding'),
  }
}

/** 补充云端默认模型 (model_config 为空时兜底) */
function fillCloudDefaults() {
  if (!cloudModels.imageGen.find(m => m.modelCode === 'stable-diffusion-xl')) {
    cloudModels.imageGen.push({ modelCode: 'stable-diffusion-xl', displayName: 'Stable Diffusion XL', providerCode: 'siliconflow' })
  }
  if (!cloudModels.imageGen.find(m => m.modelCode === 'dall-e-3')) {
    cloudModels.imageGen.push({ modelCode: 'dall-e-3', displayName: 'DALL-E 3', providerCode: 'openai' })
  }
  if (!cloudModels.vision.find(m => m.modelCode === 'gpt-4o')) {
    cloudModels.vision.push({ modelCode: 'gpt-4o', displayName: 'GPT-4o (视觉)', providerCode: 'openai' })
  }
  if (!cloudModels.vision.find(m => m.modelCode === 'minimamax-vl')) {
    cloudModels.vision.push({ modelCode: 'minimax-vl', displayName: 'MiniMax-VL (视觉)', providerCode: 'minimax' })
  }
  if (!cloudModels.tts.find(m => m.voiceId === 'zh-CN-XiaoxiaoNeural')) {
    cloudModels.tts.push({ voiceId: 'zh-CN-XiaoxiaoNeural', name: '晓晓 (女声)', language: '中文', providerCode: 'azure' })
    cloudModels.tts.push({ voiceId: 'zh-CN-YunxiNeural', name: '云希 (男声)', language: '中文', providerCode: 'azure' })
    cloudModels.tts.push({ voiceId: 'en-US-JennyNeural', name: 'Jenny (US)', language: '英文', providerCode: 'azure' })
  }
  if (!cloudModels.asr.find(m => m.modelCode === 'whisper-1')) {
    cloudModels.asr.push({ modelCode: 'whisper-1', displayName: 'Whisper (ASR)', providerCode: 'openai' })
  }
  if (!cloudModels.video.find(m => m.modelCode === 'hunyuan-video')) {
    cloudModels.video.push({ modelCode: 'hunyuan-video', displayName: '腾讯混元视频', providerCode: 'siliconflow' })
  }
  if (!cloudModels.music.find(m => m.modelCode === 'music-gen')) {
    cloudModels.music.push({ modelCode: 'music-gen', displayName: 'MusicGen', providerCode: 'siliconflow' })
  }
  if (!cloudModels.doc.find(m => m.modelCode === 'gpt-4o-mini')) {
    cloudModels.doc.push({ modelCode: 'gpt-4o-mini', displayName: 'GPT-4o-mini (文档问答)', providerCode: 'openai' })
    cloudModels.doc.push({ modelCode: 'MiniMax-Text-01', displayName: 'MiniMax-Text-01', providerCode: 'self' })
  }
}

/** 自动选中第一个可用模型 (优先自研) */
function autoSelectFirstModel() {
  // V7.3: 新增 face-recognition / video-understand (均复用 vision 模型)
  const moduleKeys = ['image-gen', 'image-understand', 'tts', 'asr', 'video-gen', 'music', 'doc',
    'face-recognition', 'video-understand']
  const mapKey = {
    'image-gen': 'imageGen', 'image-understand': 'vision', 'video-gen': 'video', 'doc': 'doc',
    'face-recognition': 'vision', 'video-understand': 'vision',
  }
  for (const mk of moduleKeys) {
    if (selectedModuleModel[mk]) continue // 已有选择，不覆盖
    const sk = mapKey[mk] || mk
    const first = selfModels[sk]?.[0] || cloudModels[sk]?.[0]
    if (first) selectedModuleModel[mk] = first
  }
}

/** 模型切换回调 */
function onModelChange(moduleKey, model) {
  selectedModuleModel[moduleKey] = model
}

// ============ 服务状态 ============
const serviceList = ref([])
const totalServices = modules.length
const serviceCount = computed(() => serviceList.value.filter(s => s.ok).length)
const servicesOk = computed(() => serviceCount.value > 0)

async function loadServices() {
  const checks = [
    { key: 'image-gen', label: '图片生成', icon: '🎨', fn: () => imageGenModels().then(() => true).catch(() => false) },
    { key: 'image-understand', label: '图片理解', icon: '🔍', fn: () => Promise.resolve(true) },
    { key: 'tts', label: 'TTS', icon: '🔊', fn: () => audioTtsVoices().then(() => true).catch(() => false) },
    { key: 'asr', label: 'ASR', icon: '🎤', fn: () => Promise.resolve(true) },
    { key: 'video-gen', label: '视频生成', icon: '🎬', fn: () => videoModels().then(() => true).catch(() => false) },
    { key: 'music', label: '音乐生成', icon: '🎵', fn: () => musicModels().then(() => true).catch(() => false) },
    { key: 'doc', label: '文档理解', icon: '📄', fn: () => Promise.resolve(true) },
  ]
  const results = await Promise.allSettled(checks.map(async c => ({ ...c, ok: await c.fn() })))
  serviceList.value = results.map(r => r.value)
}

onMounted(async () => {
  await Promise.all([loadModuleModels(), loadServices()])
})

// P0 内存泄漏修复: 组件卸载时清理所有活跃的 setInterval
const activeTimers = new Set()
function trackInterval(fn, delay) {
  const id = setInterval(fn, delay)
  activeTimers.add(id)
  return id
}
function untrackInterval(id) {
  if (id && activeTimers.has(id)) {
    clearInterval(id)
    activeTimers.delete(id)
  }
}
onBeforeUnmount(() => {
  activeTimers.forEach(id => clearInterval(id))
  activeTimers.clear()
})

// ============ 模块切换 ============
const activeModule = ref(null)
function selectModule(m) {
  activeModule.value = m
}
function resetState() {
  imgResult.value = []
  analyzeResult.value = ''
  analyzeImgUrl.value = ''
  ttsUrl.value = ''
  asrResult.value = ''
  vidResult.value = ''
  docResult.value = ''
  docFile.value = null
  musicResult.value = ''
}

// ============ 图片生成 ============
const imgFormRef = ref(null)
const imgForm = reactive({ prompt: '', ratio: '1:1', resolution: '1K', refImage: '' })
const imgLoading = ref(false)
const imgResult = ref([])

const imgFormRules = {
  prompt: [
    { required: true, message: '请输入图片描述', trigger: 'blur' },
    { min: 2, max: 1000, message: '描述长度 2-1000 字符', trigger: 'blur' }
  ]
}

async function uploadRefImage(file) {
  const fd = new FormData(); fd.append('file', file)
  try {
    const r = await uploadImage(fd)
    imgForm.refImage = r.data?.url || r.url || ''
    ElMessage.success('参考图已上传')
  } catch (e) { ElMessage.error('参考图上传失败: ' + (e.message || '')) }
  return false
}

async function generateImage() {
  if (!imgFormRef.value) return
  await imgFormRef.value.validate(async (valid) => {
    if (!valid) return
    imgLoading.value = true
    try {
      const sizeMap = { '1:1': '1024x1024', '16:9': '1024x576', '9:16': '768x1024', '4:3': '1024x768' }
      const modelCode = selectedModuleModel['image-gen']?.modelCode || 'mock'
      const res = await imageGenGenerate({
        prompt: imgForm.prompt,
        size: sizeMap[imgForm.ratio] || '1024x1024',
        model: modelCode,
      })
      const images = res.data?.images || []
      for (const url of images) {
        imgResult.value.push({ url })
      }
      if (!images.length) ElMessage.warning('未返回图片，请检查 SILICONFLOW_API_KEY 配置')
      else ElMessage.success(`生成完成 (${images.length} 张)`)
    } catch (e) {
      ElMessage.error('生成失败：' + (e.message || ''))
    } finally {
      imgLoading.value = false
    }
  })
}

function clearImageResult() {
  imgResult.value = []
  ElMessage.success('结果已清空')
}

// ============ 图片理解 ============
const analyzeImgUrl = ref('')
const analyzePrompt = ref('')
const analyzeLoading = ref(false)
const analyzeResult = ref('')

async function uploadAnalyzeImg(file) {
  const fd = new FormData(); fd.append('file', file)
  try {
    const r = await uploadImage(fd)
    analyzeImgUrl.value = r.data?.url || r.url || ''
    ElMessage.success('图片已上传')
  } catch (e) { ElMessage.error('图片上传失败: ' + (e.message || '')) }
  return false
}

async function analyzeImage() {
  if (!analyzeImgUrl.value) { ElMessage.warning('请先上传图片'); return }
  analyzeLoading.value = true
  try {
    const modelCode = selectedModuleModel['image-understand']?.modelCode || 'minimax-vision'
    const r = await visionAnalyze(
      analyzeImgUrl.value,
      analyzePrompt.value || '请描述这张图片的内容',
      modelCode
    )
    analyzeResult.value = r.data?.description || r.data?.answer || r.data || '分析完成'
    ElMessage.success('分析完成')
  } catch (e) {
    analyzeResult.value = ''
    ElMessage.error('图片分析失败：' + (e.message || '请检查 vision API 配置'))
  } finally {
    analyzeLoading.value = false
  }
}

// ============ TTS ============
const ttsFormRef = ref(null)
const ttsForm = reactive({ text: '', speed: 1.0, pitch: 0 })
const ttsLoading = ref(false)
const ttsUrl = ref('')

const ttsFormRules = {
  text: [
    { required: true, message: '请输入文本', trigger: 'blur' },
    { min: 1, max: 2000, message: '文本长度 1-2000 字符', trigger: 'blur' }
  ]
}

async function doTts() {
  if (!ttsFormRef.value) return
  await ttsFormRef.value.validate(async (valid) => {
    if (!valid) return
    ttsLoading.value = true
    try {
      const voiceId = selectedModuleModel['tts']?.voiceId || 'zh-CN-XiaoxiaoNeural'
      const r = await audioTts({
        text: ttsForm.text,
        voice: voiceId,
        speed: ttsForm.speed,
        pitch: ttsForm.pitch,
      })
      ttsUrl.value = r.data?.audio || r.data?.audioUrl || ''
      if (!ttsUrl.value) ElMessage.warning('未返回音频 URL，请检查 TTS API 配置')
      else ElMessage.success('合成完成')
    } catch (e) {
      ElMessage.error('合成失败：' + (e.message || ''))
    } finally {
      ttsLoading.value = false
    }
  })
}

// ============ ASR ============
const asrLoading = ref(false)
const asrResult = ref('')

async function uploadAsrAudio(file) {
  asrLoading.value = true
  try {
    const fd = new FormData(); fd.append('file', file)
    const modelCode = selectedModuleModel['asr']?.modelCode || 'whisper-1'
    fd.append('model', modelCode)
    const r = await audioAsr(fd)
    asrResult.value = r.data?.text || ''
    if (asrResult.value) ElMessage.success('识别完成')
    else ElMessage.warning('未识别到文本内容')
  } catch (e) {
    asrResult.value = ''
    ElMessage.error('识别失败：' + (e.message || ''))
  } finally {
    asrLoading.value = false
  }
  return false
}

// ============ 人脸识别 (V7.3) ============
const faceVideoEl = ref(null)
const faceCanvasEl = ref(null)
const faceCameraOn = ref(false)
const faceCameraStream = ref(null)
const faceLoading = ref(false)
const faceResult = ref(null)
const facePrompt = ref('请描述这张图片中的人物外貌特征和表情')

async function startCamera() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false })
    faceCameraStream.value = stream
    faceCameraOn.value = true
    await nextTick()
    if (faceVideoEl.value) {
      faceVideoEl.value.srcObject = stream
    }
  } catch (e) {
    ElMessage.error('摄像头开启失败: ' + (e.message || '请检查浏览器权限'))
  }
}

function stopCamera() {
  if (faceCameraStream.value) {
    faceCameraStream.value.getTracks().forEach(t => t.stop())
    faceCameraStream.value = null
  }
  faceCameraOn.value = false
}

async function captureAndAnalyze() {
  if (!faceVideoEl.value || !faceCanvasEl.value) return
  faceLoading.value = true
  faceResult.value = null
  try {
    const video = faceVideoEl.value
    const canvas = faceCanvasEl.value
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0)
    const base64 = canvas.toDataURL('image/jpeg', 0.85).split(',')[1]
    const model = selectedModuleModel['face-recognition']?.modelCode || 'gpt-4o-mini'
    const r = await faceAnalyze(base64, facePrompt.value, model)
    const data = r.data || r
    faceResult.value = { ...data, base64 }
    ElMessage.success('分析完成')
  } catch (e) {
    ElMessage.error('分析失败: ' + (e?.message || e?.data?.message || ''))
  } finally {
    faceLoading.value = false
  }
}

// ============ 视频理解 (V7.3) ============
const videoPrompt = ref('请详细描述这段视频的内容，包括场景、人物、动作、对话等关键信息')
const videoUnderstLoading = ref(false)
const videoProgress = ref(0)
const videoProgressColor = ref('#67c23a')
const videoMeta = ref(null)
const videoResult = ref(null)
const videoUploadEl = ref(null)

function videoProgressColorFn(percentage) {
  if (percentage < 30) return '#909399'
  if (percentage < 70) return '#e6a23c'
  return '#67c23a'
}

async function uploadVideoFile(file) {
  videoUnderstLoading.value = true
  videoProgress.value = 0
  videoResult.value = null
  videoMeta.value = null
  try {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('prompt', videoPrompt.value)
    const model = selectedModuleModel['video-understand']?.modelCode || 'gpt-4o-mini'
    fd.append('model', model)

    // 模拟进度
    const ticker = trackInterval(() => {
      if (videoProgress.value < 85) videoProgress.value += 15
    }, 500)

    const r = await videoUnderstand(fd, (e) => {
      if (e.total) videoProgress.value = Math.min(90, Math.round(e.loaded / e.total * 100))
    })
    untrackInterval(ticker)
    videoProgress.value = 100
    videoProgressColor.value = '#67c23a'

    const data = r.data || r
    videoMeta.value = {
      durationMs: data.durationMs,
      width: data.width,
      height: data.height,
      format: data.format,
      bitrate: data.bitrate,
      audioTracks: data.audioTracks,
    }
    videoResult.value = data
    ElMessage.success('视频理解完成')
  } catch (e) {
    videoProgressColor.value = '#f56c6c'
    videoProgress.value = 0
    ElMessage.error('视频分析失败: ' + (e?.message || ''))
  } finally {
    videoUnderstLoading.value = false
  }
  return false  // 阻止 el-upload 默认行为
}

// ============ 视频生成 ============
const vidFormRef = ref(null)
const vidForm = reactive({ prompt: '', duration: '6', resolution: '768P' })
const vidLoading = ref(false)
const vidResult = ref('')

const vidFormRules = {
  prompt: [
    { required: true, message: '请输入视频描述', trigger: 'blur' },
    { min: 2, max: 1000, message: '描述长度 2-1000 字符', trigger: 'blur' }
  ]
}

async function generateVideo() {
  if (!vidFormRef.value) return
  await vidFormRef.value.validate(async (valid) => {
    if (!valid) return
    vidLoading.value = true
    try {
      const fd = new FormData()
      fd.append('prompt', vidForm.prompt)
      fd.append('duration', vidForm.duration)
      fd.append('resolution', vidForm.resolution)
      const modelCode = selectedModuleModel['video-gen']?.modelCode || 'hunyuan-video'
      fd.append('model', modelCode)
      const r = await videoGenerate(fd)
      vidResult.value = r.data?.videoUrl || ''
      if (!vidResult.value) ElMessage.warning('视频生成需要较长时间，请稍后刷新')
      else ElMessage.success('生成完成')
    } catch (e) {
      ElMessage.error('视频生成失败：' + (e.message || ''))
    } finally {
      vidLoading.value = false
    }
  })
}

// ============ 文档理解 ============
const docFile = ref(null)
const docQuestion = ref('')
const docLoading = ref(false)
const docResult = ref('')
let currentDocId = null

async function uploadDoc(file) {
  docFile.value = { name: file.name, type: file.type }
  const fd = new FormData(); fd.append('file', file)
  fd.append('title', file.name)
  try {
    const r = await docUpload(fd)
    currentDocId = r.data?.docId || null
    if (currentDocId) ElMessage.success('文档已上传')
    else ElMessage.warning('上传成功但未返回 docId')
  } catch (e) {
    ElMessage.error('文档上传失败: ' + (e.message || ''))
  }
  return false
}

async function askDoc() {
  if (!docQuestion.value.trim()) { ElMessage.warning('请输入问题'); return }
  docLoading.value = true
  try {
    const modelCode = selectedModuleModel['doc']?.modelCode || 'gpt-4o-mini'
    if (currentDocId) {
      const fd = new FormData()
      fd.append('docId', currentDocId)
      fd.append('question', docQuestion.value)
      fd.append('model', modelCode)
      const r = await docAsk(fd)
      docResult.value = r.data?.answer || ''
    } else {
      ElMessage.warning('请先上传文档')
      docLoading.value = false
      return
    }
    if (docResult.value) ElMessage.success('回答完成')
    else ElMessage.warning('未返回回答内容')
  } catch (e) {
    docResult.value = ''
    ElMessage.error('问答失败: ' + (e.message || ''))
  } finally {
    docLoading.value = false
  }
}

// ============ 音乐生成 ============
const musicFormRef = ref(null)
const musicForm = reactive({ prompt: '', lyrics: '' })
const musicLoading = ref(false)
const musicResult = ref('')

const musicFormRules = {
  prompt: [
    { required: true, message: '请输入音乐描述', trigger: 'blur' },
    { min: 2, max: 1000, message: '描述长度 2-1000 字符', trigger: 'blur' }
  ]
}

async function generateMusic() {
  if (!musicFormRef.value) return
  await musicFormRef.value.validate(async (valid) => {
    if (!valid) return
    musicLoading.value = true
    try {
      const modelCode = selectedModuleModel['music']?.modelCode || 'music-gen'
      const r = await musicGenerate({
        prompt: musicForm.prompt,
        lyrics: musicForm.lyrics,
        model: modelCode,
      })
      musicResult.value = r.data?.audioUrl || ''
      if (musicResult.value) ElMessage.success('生成完成')
      else ElMessage.warning('未返回音频 URL')
    } catch (e) {
      ElMessage.error('生成失败：' + (e.message || ''))
    } finally {
      musicLoading.value = false
    }
  })
}

// ============ 工具 ============
async function copyUrl(url) {
  if (!url) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url)
      ElMessage.success('已复制到剪贴板')
    } else {
      // 降级方案
      const textarea = document.createElement('textarea')
      textarea.value = url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      ElMessage.success('已复制到剪贴板')
    }
  } catch (e) {
    ElMessage.error('复制失败: ' + (e.message || '浏览器不支持剪贴板 API'))
  }
}

function downloadImg(url) {
  if (!url) {
    ElMessage.warning('没有可下载的文件')
    return
  }
  const a = document.createElement('a')
  a.href = url
  a.download = ''
  a.target = '_blank'
  a.rel = 'noopener noreferrer'
  a.click()
  ElMessage.success('开始下载')
}
</script>

<style lang="scss" scoped>
.page-card { background: #fff; border-radius: 8px; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
  h2 { margin: 0; font-size: 16px; } }

.service-badge {
  font-size: 12px; padding: 2px 8px; border-radius: 12px;
  &.ok { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
  &.loading { background: #f4f4f5; color: #909399; border: 1px solid #e4e7ed; }
}

.service-row { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; }
.svc-chip {
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
  display: flex; align-items: center; gap: 4px;
  &.chip-ok { background: #f0f9eb; color: #67c23a; }
  &.chip-fail { background: #fef0f0; color: #f56c6c; }
}

// 默认桌面: 4列
.mod-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

// 平板: 3列
@media (max-width: 900px) {
  .mod-grid { grid-template-columns: repeat(3, 1fr); }
}

// 大手机: 2列
@media (max-width: 600px) {
  .mod-grid { grid-template-columns: repeat(2, 1fr); gap: 8px; }
}

// 小手机: 1列
@media (max-width: 400px) {
  .mod-grid { grid-template-columns: 1fr; gap: 8px; }
}

.mod-card { cursor: pointer; transition: transform 0.15s, box-shadow 0.15s;
  &:hover { transform: translateY(-3px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
}

.cur-model-tag {
  font-size: 11px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 10px;
  padding: 2px 8px;
  margin-bottom: 4px;
}

// 图片网格: 桌面3列, 手机1列
.img-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
@media (max-width: 600px) {
  .img-grid { grid-template-columns: repeat(2, 1fr); gap: 8px; }
}
@media (max-width: 400px) {
  .img-grid { grid-template-columns: 1fr; }
}

.img-item { position: relative; overflow: hidden; border-radius: 8px; }
.img-actions {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,0.6); padding: 4px 8px;
  display: flex; justify-content: center; gap: 8px;
  :deep(.el-button--small.is-link) { color: #fff; }
  opacity: 0; transition: opacity 0.2s;
}
.img-item:hover .img-actions { opacity: 1; }

// ============================================================
// H5 移动端适配 (max-width: 768px)
// ============================================================
@media (max-width: 768px) {
  // 工具栏: 允许换行, 缩小字号
  .el-card :deep(.el-card__header) {
    padding: 10px 12px;
    font-size: 13px;
    flex-wrap: wrap;
    gap: 6px;
  }

  // 模型选择器在卡片头部时换行
  .el-card :deep(.el-card__header) > div {
    flex-wrap: wrap !important;
    gap: 6px;
  }

  // 通用: 表单元素全宽
  .el-card :deep(.el-form-item) {
    margin-bottom: 12px;
  }
  .el-card :deep(.el-input),
  .el-card :deep(.el-select),
  .el-card :deep(.el-textarea) {
    width: 100% !important;
  }

  // 摄像头区域: 视频自适应屏幕
  .camera-area video {
    width: 100% !important;
    max-width: 100% !important;
    border-radius: 8px;
  }

  // 拍照按钮组: 换行
  .el-card :deep(.el-button) {
    font-size: 13px;
    padding: 8px 12px;
  }

  // 视频上传拖拽区
  .el-upload-dragger {
    padding: 20px 10px;
  }

  // 上传提示文字缩小
  .el-upload__tip {
    font-size: 12px;
    line-height: 1.4;
  }

  // 分析结果卡片
  .el-card :deep(.el-card__body) {
    padding: 12px;
  }
  .el-card :deep(pre) {
    font-size: 12px;
    overflow-x: auto;
  }

  // 进度条
  .el-progress {
    width: 100%;
  }

  // 音频/视频播放器全宽
  audio, video {
    width: 100%;
    max-width: 100%;
  }
}

// 极小屏幕额外处理
@media (max-width: 400px) {
  .el-card :deep(.el-card__header) {
    font-size: 12px;
    padding: 8px 10px;
  }
  .el-card :deep(.el-button) {
    font-size: 12px;
    padding: 6px 10px;
  }
}
</style>
