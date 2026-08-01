# V3.5.74+ View 重写模板指南

## 1. 目标

83 view 全部按 Element Plus 2.4 标准 design 重写, 统一布局 / 统一组件 / 统一变量.

## 2. 模板结构 (5 段)

每个 view 模板按 5 段结构组织:

```vue
<template>
  <div class="page-{name}">
    <!-- 1. page-header: 标题 + 副标题 + 主操作 -->
    <header class="page-header">
      <div>
        <h2 class="page-title">📊 标题</h2>
        <p class="page-subtitle">副标题描述</p>
      </div>
      <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
    </header>

    <!-- 2. section (可多个): 每个区块一个 section -->
    <section class="section">
      <h3 class="section-title">区块标题</h3>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="6" v-for="x in xs" :key="x.id">
          <el-card shadow="hover">...</el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 3. 表格区: el-card + el-table -->
    <section class="section">
      <el-card shadow="hover">
        <el-table :data="rows" stripe>...</el-table>
      </el-card>
    </section>

    <!-- 4. 表单区: el-card + el-form -->
    <section class="section">
      <el-card shadow="hover">
        <el-form :model="form" label-width="100px">...</el-form>
      </el-card>
    </section>
  </div>
</template>
```

## 3. 6 个设计原则

### 3.1 优先用 Element Plus 组件
- 标签: el-tag (不用自定义 pill)
- 按钮: el-button + type (primary / success / warning / danger / info)
- 容器: el-card shadow="hover" (统一阴影)
- 表格: el-table stripe + el-table-column
- 表单: el-form + el-form-item
- 弹窗: el-dialog v-model
- 加载: el-skeleton / v-loading
- 空: el-empty
- 错: el-alert type="error"

### 3.2 CSS variable 引用 design token
所有颜色 / 间距 / 圆角用 var(--liugl-*):

```scss
.kpi-card {
  background: var(--liugl-bg-elevated);
  border: 1px solid var(--liugl-border);
  border-radius: var(--liugl-radius);
  padding: var(--liugl-spacing-md);
  color: var(--liugl-text);
}
```

不用硬编码 #5b8def 或 8px 这种.

### 3.3 响应式断点
el-col 用 4 档断点:
- :xs="24" 手机 (<768px) 整行
- :sm="12" 平板 (768-992px) 半行
- :md="8" 小桌面 (992-1200px) 1/3
- :lg="6" 大桌面 (1200-1920px) 1/4
- :xl="4" 超大 (>1920px) 1/6

### 3.4 图表统一用 v-chart
```vue
<v-chart :option="trendOption" autoresize style="height: 280px" />
```

### 3.5 加载/空/错三态
```vue
<el-skeleton v-if="loading" :rows="5" animated />
<el-empty v-else-if="!rows.length" description="暂无数据" />
<el-table v-else :data="rows" />
```

### 3.6 i18n 不硬编码
```vue
<el-button>{{ $t('common.refresh') }}</el-button>
```

## 4. 重写步骤

每个 view 按这个流程改:

1. 读旧 view - 了解功能 + API + 数据
2. 备份 - cp view.vue view.vue.bak
3. 写新 template - 按 5 段结构
4. 写 script - 数据驱动 (kpis / quickActions 用 computed)
5. 写 style - 最小化自定义 CSS, 全用 var()
6. build 验证 - npx vite build 不能错
7. dev 验证 - 浏览器访问

## 5. 优先级 (建议)

| 优先级 | view | 原因 |
|--------|------|------|
| P0 | auth/Login.vue | 用户登录第一关 |
| P0 | auth/H5Login.vue | 移动端入口 |
| P0 | chat/Index.vue | 核心功能 |
| P1 | admin/Dashboard.vue | 已有样板 (V3.5.74) |
| P1 | ai/AiChat.vue | 核心 AI 功能 |
| P2 | admin/* (8 个) | 管理后台 |
| P2 | ai/* (12 个) | AI 功能群 |
| P3 | analytics/* (5) | 数据分析 |
| P3 | pipeline/* (3) | 工作流 |
| P3 | showcase/* (10) | 演示页 |
| P3 | 其它 30+ | 业务定制 |

## 6. 完整样板

参考: src/views/admin/Dashboard.vue (V3.5.74 重写版, 332 行).

5 段结构 + 6 原则 + 响应式 + design token + ECharts + 加载态.

## 7. 验证清单

每个 view 改完跑:
- [ ] npx vite build 无错
- [ ] 浏览器访问页面无 console error
- [ ] 移动端 (375px) 布局正常
- [ ] 暗色模式 (prefers-color-scheme: dark) 正常
- [ ] i18n zh + en 切换正常
- [ ] 加载/空/错 三态都测过
