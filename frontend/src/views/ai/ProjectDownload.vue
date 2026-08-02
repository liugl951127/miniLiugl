<!--
  @file views/ai/ProjectDownload.vue (V3.5.75 标准化模板)
  @auto-migrated 2026-08-01 by scripts/migrate-view-style.js
-->
<!--
  @file views/ai/ProjectDownload.vue (V3.5.48)
  @description 项目代码生成 + ZIP 下载 - 用户目标能力之一
  - 2 端点: download (GET + POST)
  - 一键生成 spring-boot 项目 ZIP, 浏览器直接下载
  - 模板: controller / service / entity / mapper / pom.xml / Dockerfile
-->
<template>
  <div class="page-project-download page">
    <el-card>
      <template #header>
        <div class="header">
          <span>📦 项目代码生成 <el-tag size="small" type="success">V3.5.48</el-tag></span>
          <el-tag type="warning">项目模板</el-tag>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          一键生成 Spring Boot 微服务项目, 包含 Controller / Service / Entity / Mapper / pom.xml / Dockerfile
        </template>
      </el-alert>

      <el-form :model="form" label-width="140px" label-position="right">
        <el-form-item label="项目名">
          <el-input v-model="form.projectName" placeholder="minimax-erp / minimax-cms" style="width: 320px">
            <template #append>.zip</template>
          </el-input>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.version" placeholder="1.0.0" style="width: 200px" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="spring-boot">Spring Boot</el-radio-button>
            <el-radio-button value="spring-cloud">Spring Cloud</el-radio-button>
            <el-radio-button value="flask">Flask</el-radio-button>
            <el-radio-button value="express">Express</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="包名">
          <el-input v-model="form.packageName" placeholder="com.minimax.app" style="width: 320px" />
        </el-form-item>
        <el-form-item label="数据库">
          <el-radio-group v-model="form.database">
            <el-radio-button value="mysql">MySQL</el-radio-button>
            <el-radio-button value="mariadb">MariaDB</el-radio-button>
            <el-radio-button value="postgresql">PostgreSQL</el-radio-button>
            <el-radio-button value="h2">H2 (内存)</el-radio-button>
            <el-radio-button value="oracle">Oracle</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="onDownload" :loading="downloading" :icon="Download">
            🚀 生成 + 下载 ZIP
          </el-button>
          <el-button size="large" @click="onPreview">👁 预览文件清单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预览 -->
    <el-card v-if="fileList.length" style="margin-top: 16px">
      <template #header>
        <div class="header">
          <span>📁 文件清单 ({{ fileList.length }} 个)</span>
          <el-tag size="small" type="info">下载时包含</el-tag>
        </div>
      </template>
      <el-table :data="fileList" stripe max-height="400">
        <el-table-column prop="path" label="路径" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'java' ? 'primary' : 'info'">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
// ───── 依赖导入 ─────
import { reactive, ref } from 'vue'
import { useToast } from '@/composables/useToast'

import { Download } from '@element-plus/icons-vue'
import { projectDownloadGet, projectDownloadPost } from '@/api/ai'

const form = reactive({
  projectName: 'minimax-erp',
  version: '1.0.0',
  type: 'spring-boot',
  packageName: 'com.minimax.erp',
  database: 'mysql'
})

const downloading = ref(false)
const fileList = ref([])

async function onDownload() {
  if (!form.projectName) {
    toast.warning('请输入项目名')
    return
  }
  downloading.value = true
  try {
    // 用 POST 拿 blob
    const blob = await projectDownloadPost(form)
    // blob 转下载
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${form.projectName}-${form.version}.zip`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    setTimeout(() => URL.revokeObjectURL(url), 100)
    toast.success(`${form.projectName}-${form.version}.zip 下载完成`)
  } catch (e) {
    // 错误已统一处理
  } finally {
    downloading.value = false
  }
}

function onPreview() {
  // 模拟预览文件清单 (实际生成才能知道)
  const pkg = form.packageName.replace(/\./g, '/')
  const list = [
    { path: `pom.xml`, type: 'xml', description: 'Maven 依赖 (Spring Boot Web + MyBatis-Plus + MariaDB)' },
    { path: `src/main/java/${pkg}/${form.projectName.split('-').pop() || 'app'}Application.java`, type: 'java', description: '启动类' },
    { path: `src/main/java/${pkg}/controller/UserController.java`, type: 'java', description: '示例 Controller' },
    { path: `src/main/java/${pkg}/service/UserService.java`, type: 'java', description: '示例 Service' },
    { path: `src/main/java/${pkg}/entity/User.java`, type: 'java', description: '示例 Entity (含 MyBatis-Plus)' },
    { path: `src/main/java/${pkg}/mapper/UserMapper.java`, type: 'java', description: '示例 Mapper' },
    { path: `src/main/resources/application.yml`, type: 'yaml', description: `${form.database} 数据源配置` },
    { path: `src/main/resources/application-dev.yml`, type: 'yaml', description: '开发环境配置' },
    { path: `src/main/resources/application-prod.yml`, type: 'yaml', description: '生产环境配置' },
    { path: `src/main/resources/mapper/UserMapper.xml`, type: 'xml', description: 'MyBatis XML 映射' },
    { path: `src/main/resources/db/schema.sql`, type: 'sql', description: `${form.database} 建表 SQL` },
    { path: `src/test/java/${pkg}/UserServiceTest.java`, type: 'java', description: '单元测试模板' },
    { path: `Dockerfile`, type: 'docker', description: 'Docker 镜像构建' },
    { path: `docker-compose.yml`, type: 'yaml', description: 'docker-compose 编排' },
    { path: `.gitignore`, type: 'git', description: 'Git 忽略文件' },
    { path: `README.md`, type: 'md', description: '项目说明文档' }
  ]
  fileList.value = list
  toast.success(`预览 ${list.length} 个文件`)
}
</script>

<style scoped>
.page { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
